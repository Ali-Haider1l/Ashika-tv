package com.example.data.network

import com.example.data.model.ChannelEntity
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class ToffeeChannelMeta(
    val name: String,
    val logoUrl: String,
    val category: String
)

data class ToffeeExtractionResult(
    val playableChannels: List<ChannelEntity>,
    val channelMetadata: Map<String, ToffeeChannelMeta>
)

class ToffeeExtractor(
    private val client: OkHttpClient
) {
    suspend fun extractFromToffeeSite(): ToffeeExtractionResult = withContext(Dispatchers.IO) {
        val allPlayable = LinkedHashMap<String, ChannelEntity>()
        val allMeta = LinkedHashMap<String, ToffeeChannelMeta>()

        for (pageUrl in TOFFEE_PAGES) {
            runCatching {
                val req = Request.Builder()
                    .url(pageUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "bn-BD,bn;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val html = resp.body?.string().orEmpty()
                        val parsed = extractFromHtml(html)
                        parsed.playableChannels.forEach { ch ->
                            allPlayable.putIfAbsent(ch.url, ch)
                        }
                        parsed.channelMetadata.forEach { (normKey, meta) ->
                            allMeta.putIfAbsent(normKey, meta)
                        }
                    }
                }
            }
        }

        ToffeeExtractionResult(
            playableChannels = allPlayable.values.toList(),
            channelMetadata = allMeta
        )
    }

    fun extractFromHtml(html: String): ToffeeExtractionResult {
        if (html.isBlank()) {
            return ToffeeExtractionResult(emptyList(), emptyMap())
        }

        val playable = LinkedHashMap<String, ChannelEntity>()
        val metadata = LinkedHashMap<String, ToffeeChannelMeta>()

        nextDataRegex.findAll(html).forEach { match ->
            val jsonText = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (jsonText.isNotEmpty()) {
                runCatching {
                    val root = JSONObject(jsonText)
                    walkJsonValue(root, playable, metadata, inheritedCategory = "")
                }
            }
        }

        scriptJsonRegex.findAll(html).forEach { match ->
            val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (raw.startsWith("{")) {
                runCatching {
                    walkJsonValue(JSONObject(raw), playable, metadata, inheritedCategory = "")
                }
            } else if (raw.startsWith("[")) {
                runCatching {
                    walkJsonValue(JSONArray(raw), playable, metadata, inheritedCategory = "")
                }
            }
        }

        m3u8Regex.findAll(html).forEach { match ->
            val url = match.value.replace("\\/", "/").trim()
            if (M3uParser.isValidStreamUrl(url) && !playable.containsKey(url)) {
                val derivedName = deriveNameFromUrl(url)
                playable[url] = ChannelEntity(
                    url = url,
                    name = derivedName,
                    groupTitle = "Toffee Live",
                    source = "toffee",
                    headersJson = M3uParser.encodeHeadersJson(
                        mapOf(
                            "Referer" to TOFFEE_REFERER,
                            "Origin" to TOFFEE_ORIGIN
                        )
                    )
                )
            }
        }

        return ToffeeExtractionResult(
            playableChannels = playable.values.toList(),
            channelMetadata = metadata
        )
    }

    private fun walkJsonValue(
        node: Any?,
        playable: MutableMap<String, ChannelEntity>,
        metadata: MutableMap<String, ToffeeChannelMeta>,
        inheritedCategory: String
    ) {
        when (node) {
            is JSONObject -> {
                val categoryHint = sequenceOf("category", "category_name", "categoryName", "genre", "group")
                    .map { node.optString(it).trim() }
                    .firstOrNull { it.isNotEmpty() }
                    ?: inheritedCategory

                val name = sequenceOf(
                    "channel_name",
                    "channelName",
                    "program_name",
                    "programName",
                    "content_name",
                    "name",
                    "title"
                ).map { node.optString(it).trim() }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("http", ignoreCase = true) }

                val rawLogo = sequenceOf(
                    "channel_logo",
                    "logo",
                    "logo_url",
                    "logoUrl",
                    "image",
                    "image_url",
                    "thumbnail",
                    "poster",
                    "icon"
                ).map { node.optString(it).trim() }
                    .firstOrNull { it.isNotEmpty() }
                    .orEmpty()
                val logo = normalizeToffeeAssetUrl(rawLogo)

                val explicitStreamUrl = sequenceOf(
                    "hls_url",
                    "hlsUrl",
                    "stream_url",
                    "streamUrl",
                    "playback_url",
                    "playbackUrl",
                    "video_url",
                    "videoUrl",
                    "live_url",
                    "link",
                    "m3u8"
                ).map { node.optString(it).replace("\\/", "/").trim() }
                    .firstOrNull { M3uParser.isValidStreamUrl(it) }

                val genericUrl = node.optString("url").replace("\\/", "/").trim()
                    .takeIf { M3uParser.isValidStreamUrl(it) && looksLikePlayableStream(it) }

                val streamUrl = explicitStreamUrl ?: genericUrl

                if (!name.isNullOrBlank()) {
                    val resolvedCategory = categoryHint.ifEmpty { "Toffee Live" }
                    val normKey = normalizeChannelKey(name)
                    if (normKey.isNotEmpty() && (logo.isNotEmpty() || resolvedCategory != "Toffee Live")) {
                        metadata.putIfAbsent(
                            normKey,
                            ToffeeChannelMeta(
                                name = name,
                                logoUrl = logo,
                                category = resolvedCategory
                            )
                        )
                    }
                    if (!streamUrl.isNullOrBlank() && !playable.containsKey(streamUrl)) {
                        val headers = LinkedHashMap<String, String>()
                        headers["Referer"] = TOFFEE_REFERER
                        headers["Origin"] = TOFFEE_ORIGIN
                        node.optJSONObject("headers")?.let { hdr ->
                            val hdrKeys = hdr.keys()
                            while (hdrKeys.hasNext()) {
                                val hk = hdrKeys.next()
                                val hv = hdr.optString(hk).trim()
                                if (hv.isNotEmpty()) headers[hk] = hv
                            }
                        }
                        val cookie = node.optString("cookie").trim()
                        if (cookie.isNotEmpty()) headers["Cookie"] = cookie

                        playable[streamUrl] = ChannelEntity(
                            url = streamUrl,
                            name = name,
                            groupTitle = resolvedCategory,
                            logoUrl = logo,
                            tvgId = node.optString("tvg_id").trim().ifEmpty { node.optString("slug").trim() },
                            source = "toffee",
                            headersJson = M3uParser.encodeHeadersJson(headers)
                        )
                    }
                }

                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    walkJsonValue(node.opt(key), playable, metadata, categoryHint)
                }
            }

            is JSONArray -> {
                for (i in 0 until node.length()) {
                    walkJsonValue(node.opt(i), playable, metadata, inheritedCategory)
                }
            }
        }
    }

    private fun looksLikePlayableStream(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains("/hls/") ||
            lower.contains("/live/")
    }

    private fun normalizeToffeeAssetUrl(raw: String): String {
        if (raw.isBlank()) return ""
        val clean = raw.replace("\\/", "/").trim()
        return when {
            clean.startsWith("http://", ignoreCase = true) ||
                clean.startsWith("https://", ignoreCase = true) -> clean
            clean.startsWith("//") -> "https:$clean"
            clean.startsWith("/") -> "$TOFFEE_ORIGIN$clean"
            else -> ""
        }
    }

    private fun deriveNameFromUrl(url: String): String {
        val segment = url.substringBefore('?').substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
        return segment.ifEmpty { "Toffee Channel" }
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    companion object {
        const val TOFFEE_ORIGIN = "https://toffeelive.com"
        const val TOFFEE_REFERER = "https://toffeelive.com/"
        val TOFFEE_PAGES = listOf(
            "https://toffeelive.com/bn/live-tv",
            "https://toffeelive.com/live-tv"
        )

        private val nextDataRegex = Regex(
            """<script[^>]*id="__NEXT_DATA__"[^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val scriptJsonRegex = Regex(
            """<script[^>]*type="application/(?:ld\+)?json"[^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val m3u8Regex = Regex(
            """https?:\\?/\\?/[^\s"'<>\\]+\.m3u8(?:\?[^\s"'<>\\]*)?""",
            RegexOption.IGNORE_CASE
        )

        fun normalizeChannelKey(raw: String): String {
            return raw.lowercase()
                .replace(Regex("""\b(hd|fhd|sd|uhd|4k|live|tv|bangla|bd)\b"""), "")
                .replace(Regex("""[^a-z0-9]"""), "")
        }
    }
}
