package com.example.data.network

import com.example.data.model.ChannelEntity
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlaylistFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchM3u(url: String): Result<List<ChannelEntity>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url.trim())
                .header("User-Agent", "AshikaLiveTV-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                val channels = M3uParser.parse(body, source = "playlist")
                if (channels.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("No playable channels found in playlist."))
                }
                Result.success(channels)
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
