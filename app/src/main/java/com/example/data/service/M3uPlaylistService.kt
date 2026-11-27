package com.example.data.service

import android.content.ContentResolver
import android.net.Uri
import com.example.data.model.ChannelEntity
import com.example.data.parser.M3uParser
import com.example.data.parser.ParsedM3uResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class M3uChannel(
    val channelName: String,
    val groupTitle: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val tvgId: String = "",
    val headers: Map<String, String> = emptyMap()
) {
    fun toChannelEntity(source: String = "playlist"): ChannelEntity = ChannelEntity(
        url = streamUrl,
        name = channelName,
        groupTitle = groupTitle,
        logoUrl = logoUrl,
        tvgId = tvgId,
        source = source,
        headersJson = M3uParser.encodeHeadersJson(headers)
    )
}

data class M3uPlaylistResult(
    val channels: List<M3uChannel>,
    val epgUrls: List<String> = emptyList(),
    val sourceLabel: String = ""
) {
    fun toChannelEntities(source: String = "playlist"): List<ChannelEntity> =
        channels.map { it.toChannelEntity(source = source) }

    fun toParsedM3uResult(source: String = "playlist"): ParsedM3uResult = ParsedM3uResult(
        channels = toChannelEntities(source = source),
        epgUrls = epgUrls
    )
}

class M3uPlaylistService(
    private val client: OkHttpClient = defaultHttpClient()
) {

    suspend fun parseFromUrl(playlistUrl: String): Result<M3uPlaylistResult> =
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = playlistUrl.trim()
                if (!M3uParser.isValidStreamUrl(cleanUrl)) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Enter a valid HTTP or HTTPS M3U playlist URL.")
                    )
                }

                val request = Request.Builder()
                    .url(cleanUrl)
                    .header("User-Agent", "AshikaLiveTV-Android/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    val body = response.body?.string().orEmpty()
                    val parsed = parseContent(body, sourceLabel = cleanUrl, sourceType = "playlist")
                    if (parsed.channels.isEmpty()) {
                        return@withContext Result.failure(
                            IllegalStateException("No playable channels found in playlist.")
                        )
                    }
                    Result.success(parsed)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun parseFromLocalFile(file: File): Result<M3uPlaylistResult> =
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || !file.isFile || !file.canRead()) {
                    return@withContext Result.failure(
                        IOException("Local M3U file is missing or unreadable: ${file.name}")
                    )
                }
                val content = file.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseContent(content, sourceLabel = file.name, sourceType = "file")
                if (parsed.channels.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalStateException("No valid HTTP/HTTPS channels found in file.")
                    )
                }
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun parseFromLocalUri(
        contentResolver: ContentResolver,
        uri: Uri
    ): Result<M3uPlaylistResult> = withContext(Dispatchers.IO) {
        try {
            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(
                    IllegalStateException("Unable to read selected file.")
                )
            stream.use { input ->
                parseFromInputStream(
                    inputStream = input,
                    sourceLabel = uri.lastPathSegment ?: "local_file",
                    sourceType = "file"
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseFromInputStream(
        inputStream: InputStream,
        sourceLabel: String = "stream",
        sourceType: String = "file"
    ): Result<M3uPlaylistResult> = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = parseContent(content, sourceLabel = sourceLabel, sourceType = sourceType)
            if (parsed.channels.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("No valid HTTP/HTTPS channels found in playlist.")
                )
            }
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseContent(
        content: String,
        sourceLabel: String = "playlist",
        sourceType: String = "playlist"
    ): M3uPlaylistResult {
        val rawParsed = M3uParser.parseWithMetadata(content, source = sourceType)
        val mapped = rawParsed.channels.map { entity ->
            M3uChannel(
                channelName = entity.name,
                groupTitle = entity.groupTitle,
                streamUrl = entity.url,
                logoUrl = entity.logoUrl,
                tvgId = entity.tvgId,
                headers = M3uParser.parseHeadersMap(entity.headersJson)
            )
        }
        return M3uPlaylistResult(
            channels = mapped,
            epgUrls = rawParsed.epgUrls,
            sourceLabel = sourceLabel
        )
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
