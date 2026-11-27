package com.example.data.network

import com.example.data.model.ChannelEntity
import com.example.data.parser.EpgParser
import com.example.data.parser.M3uParser
import com.example.data.parser.ParsedEpgData
import com.example.data.parser.ParsedM3uResult
import com.example.data.service.M3uPlaylistService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ToffeeAndBdBundleResult(
    val channels: List<ChannelEntity>,
    val sourceCounts: Map<String, Int>,
    val epgUrls: List<String>,
    val toffeeExtractedCount: Int
)

class PlaylistFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val toffeeExtractor = ToffeeExtractor(client)
    val m3uPlaylistService = M3uPlaylistService(client)

    suspend fun fetchM3u(url: String): Result<ParsedM3uResult> {
        return m3uPlaylistService.parseFromUrl(url).map { it.toParsedM3uResult(source = "playlist") }
    }

    suspend fun fetchToffeeAndBdBundle(playlistUrls: List<String>): Result<ToffeeAndBdBundleResult> =
        withContext(Dispatchers.IO) {
            try {
                coroutineScope {
                    val toffeeDeferred = async { toffeeExtractor.extractFromToffeeSite() }
                    val playlistDeferreds = playlistUrls.map { url ->
                        async { url to fetchM3u(url) }
                    }

                    val toffeeResult = toffeeDeferred.await()
                    val playlistResults = playlistDeferreds.awaitAll()

                    val merged = LinkedHashMap<String, ChannelEntity>()
                    val sourceCounts = LinkedHashMap<String, Int>()
                    val epgUrls = LinkedHashSet<String>()

                    fun addEnriched(raw: ChannelEntity) {
                        val normKey = ToffeeExtractor.normalizeChannelKey(raw.name)
                        val meta = toffeeResult.channelMetadata[normKey]
                        val enriched = raw.copy(
                            logoUrl = raw.logoUrl.ifBlank { meta?.logoUrl.orEmpty() },
                            groupTitle = if (raw.groupTitle.equals("Other", ignoreCase = true) && !meta?.category.isNullOrBlank()) {
                                meta!!.category
                            } else {
                                raw.groupTitle
                            }
                        )
                        if (!merged.containsKey(enriched.url)) {
                            merged[enriched.url] = enriched
                        }
                    }

                    toffeeResult.playableChannels.forEach { addEnriched(it) }

                    playlistResults.forEach { (url, res) ->
                        res.onSuccess { parsed ->
                            sourceCounts[url] = parsed.channels.size
                            parsed.epgUrls.forEach { epgUrls.add(it) }
                            parsed.channels.forEach { addEnriched(it) }
                        }
                    }

                    if (merged.isEmpty()) {
                        return@coroutineScope Result.failure(
                            IllegalStateException("Could not reach Toffee or BD channel feeds. Check internet connection.")
                        )
                    }

                    Result.success(
                        ToffeeAndBdBundleResult(
                            channels = merged.values.toList(),
                            sourceCounts = sourceCounts,
                            epgUrls = epgUrls.toList(),
                            toffeeExtractedCount = toffeeResult.playableChannels.size
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchXmltvEpg(url: String): Result<ParsedEpgData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url.trim())
                .header("User-Agent", "AshikaLiveTV-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val stream = response.body?.byteStream()
                    ?: return@withContext Result.failure(IllegalStateException("Empty EPG response"))
                val parsed = EpgParser.parseStream(stream)
                if (parsed.programs.isEmpty() && parsed.channels.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("No EPG channels or programs found in feed."))
                }
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLanCompanionChannels(endpointUrl: String): Result<List<ChannelEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(endpointUrl.trim())
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    val raw = response.body?.string().orEmpty()
                    val json = JSONObject(raw)
                    val array = json.optJSONArray("channels")
                        ?: return@withContext Result.failure(IllegalStateException("Missing 'channels' array"))

                    val list = mutableListOf<ChannelEntity>()
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val name = obj.optString("name").trim()
                        val url = obj.optString("url").trim()
                        val group = obj.optString("group").trim().ifEmpty { "Other" }
                        val logo = obj.optString("logo").trim()
                        if (name.isNotEmpty() && M3uParser.isValidStreamUrl(url)) {
                            list.add(
                                ChannelEntity(
                                    url = url,
                                    name = name,
                                    groupTitle = group,
                                    logoUrl = logo,
                                    source = "lan"
                                )
                            )
                        }
                    }
                    Result.success(list)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
