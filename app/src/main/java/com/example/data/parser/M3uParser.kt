package com.example.data.parser

import com.example.data.model.ChannelEntity
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

data class ParsedM3uResult(
    val channels: List<ChannelEntity>,
    val epgUrls: List<String>
)

object M3uParser {

    private val groupRegex = Regex("""group-title=(?:["']([^"']*)["']|([^\s,]+))""", RegexOption.IGNORE_CASE)
    private val logoRegex = Regex("""tvg-logo=(?:["']([^"']*)["']|([^\s,]+))""", RegexOption.IGNORE_CASE)
    private val tvgIdRegex = Regex("""tvg-id=(?:["']([^"']*)["']|([^\s,]+))""", RegexOption.IGNORE_CASE)
    private val tvgNameRegex = Regex("""tvg-name=(?:["']([^"']*)["']|([^\s,]+))""", RegexOption.IGNORE_CASE)
    private val headerTvgUrlRegex = Regex("""(?:url-tvg|x-tvg-url)=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    private val httpRegex = Regex("""^https?://""", RegexOption.IGNORE_CASE)

    private fun MatchResult?.firstNonEmptyGroup(): String {
        if (this == null) return ""
        return groupValues.drop(1).firstOrNull { it.isNotEmpty() }.orEmpty().trim()
    }

    fun parse(content: String, source: String = "playlist"): List<ChannelEntity> {
        return parseWithMetadata(content, source).channels
    }

    fun parseWithMetadata(content: String, source: String = "playlist"): ParsedM3uResult {
        val trimmed = content.trim().trimStart('\uFEFF')
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val jsonChannels = parseToffeeJsonPlaylist(trimmed, source)
            if (jsonChannels.isNotEmpty()) {
                return ParsedM3uResult(channels = jsonChannels, epgUrls = emptyList())
            }
        }

        val lines = content.lineSequence()
        val parsed = LinkedHashMap<String, ChannelEntity>()
        val epgUrls = LinkedHashSet<String>()

        var isM3uContext = false
        var pendingName = ""
        var pendingGroup = "Other"
        var pendingLogo = ""
        var pendingTvgId = ""
        val pendingHeaders = LinkedHashMap<String, String>()

        for (rawLine in lines) {
            val line = rawLine.trim().trimStart('\uFEFF')
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    isM3uContext = true
                    headerTvgUrlRegex.findAll(line).forEach { match ->
                        match.groupValues.getOrNull(1)
                            ?.split(',')
                            ?.map { it.trim() }
                            ?.filter { isValidStreamUrl(it) }
                            ?.forEach { epgUrls.add(it) }
                    }
                }

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    isM3uContext = true
                    val commaIdx = findExtinfCommaIndex(line)
                    val attrName = tvgNameRegex.find(line).firstNonEmptyGroup()
                    val tailName = if (commaIdx >= 0 && commaIdx + 1 < line.length) {
                        line.substring(commaIdx + 1).trim()
                    } else {
                        ""
                    }
                    pendingName = tailName.ifEmpty { attrName }.ifEmpty { "Unknown Channel" }
                    pendingGroup = normalizeGroupTitle(
                        groupRegex.find(line).firstNonEmptyGroup()
                    )
                    pendingLogo = logoRegex.find(line).firstNonEmptyGroup()
                    pendingTvgId = tvgIdRegex.find(line).firstNonEmptyGroup()
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val grp = normalizeGroupTitle(line.substringAfter(':', ""))
                    if (grp != "Other") {
                        pendingGroup = grp
                    }
                }

                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val opt = line.substringAfter(':', "").trim()
                    val eqIdx = opt.indexOf('=')
                    if (eqIdx > 0 && eqIdx + 1 < opt.length) {
                        val key = opt.substring(0, eqIdx).trim().lowercase()
                        val value = opt.substring(eqIdx + 1).trim()
                        if (value.isNotEmpty()) {
                            when (key) {
                                "http-user-agent" -> pendingHeaders["User-Agent"] = value
                                "http-referrer", "http-referer" -> pendingHeaders["Referer"] = value
                                "http-origin" -> pendingHeaders["Origin"] = value
                            }
                        }
                    }
                }

                line.startsWith("#EXTHTTP:", ignoreCase = true) -> {
                    val rawJson = line.substringAfter(':', "").trim()
                    runCatching {
                        val json = JSONObject(rawJson)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = json.optString(k).trim()
                            if (v.isNotEmpty()) {
                                pendingHeaders[normalizeHeaderName(k)] = v
                            }
                        }
                    }
                }

                line.startsWith("#KODIPROP:inputstream.adaptive.stream_headers=", ignoreCase = true) -> {
                    val rawQuery = line.substringAfter('=', "").trim()
                    parseQueryHeadersInto(rawQuery, pendingHeaders)
                }

                !line.startsWith("#") && (pendingName.isNotEmpty() || isM3uContext) -> {
                    val pipeIdx = line.indexOf('|')
                    val cleanUrl = (if (pipeIdx > 0) line.substring(0, pipeIdx) else line).trim()
                    if (pipeIdx > 0 && pipeIdx + 1 < line.length) {
                        parseQueryHeadersInto(line.substring(pipeIdx + 1), pendingHeaders)
                    }
                    if (isValidStreamUrl(cleanUrl) && !parsed.containsKey(cleanUrl)) {
                        val resolvedName = pendingName.ifEmpty { deriveNameFromUrl(cleanUrl) }
                        parsed[cleanUrl] = ChannelEntity(
                            url = cleanUrl,
                            name = resolvedName,
                            groupTitle = pendingGroup,
                            logoUrl = pendingLogo,
                            tvgId = pendingTvgId,
                            source = source,
                            headersJson = encodeHeadersJson(pendingHeaders)
                        )
                    }
                    pendingName = ""
                    pendingGroup = "Other"
                    pendingLogo = ""
                    pendingTvgId = ""
                    pendingHeaders.clear()
                }
            }
        }
        return ParsedM3uResult(
            channels = parsed.values.toList(),
            epgUrls = epgUrls.toList()
        )
    }

    private fun findExtinfCommaIndex(line: String): Int {
        var inDoubleQuotes = false
        var inSingleQuotes = false
        for (i in line.indices) {
            when (line[i]) {
                '"' -> if (!inSingleQuotes) inDoubleQuotes = !inDoubleQuotes
                '\'' -> if (!inDoubleQuotes) inSingleQuotes = !inSingleQuotes
                ',' -> if (!inDoubleQuotes && !inSingleQuotes) return i
            }
        }
        return line.lastIndexOf(',')
    }

    private fun normalizeGroupTitle(raw: String): String {
        val cleaned = raw.trim()
            .substringBefore(';')
            .trim()
        return if (cleaned.isEmpty() || cleaned.equals("undefined", ignoreCase = true)) {
            "Other"
        } else {
            cleaned
        }
    }

    private fun parseQueryHeadersInto(raw: String, target: MutableMap<String, String>) {
        raw.split('&').forEach { pair ->
            val eqIdx = pair.indexOf('=')
            if (eqIdx > 0 && eqIdx + 1 < pair.length) {
                val k = runCatching { URLDecoder.decode(pair.substring(0, eqIdx).trim(), "UTF-8") }
                    .getOrDefault(pair.substring(0, eqIdx).trim())
                val v = runCatching { URLDecoder.decode(pair.substring(eqIdx + 1).trim(), "UTF-8") }
                    .getOrDefault(pair.substring(eqIdx + 1).trim())
                if (k.isNotEmpty() && v.isNotEmpty()) {
                    target[normalizeHeaderName(k)] = v
                }
            }
        }
    }

    private fun deriveNameFromUrl(url: String): String {
        val seg = url.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
            .replace('-', ' ').replace('_', ' ').trim()
        return seg.ifEmpty { "Live Channel" }
    }

    private fun parseToffeeJsonPlaylist(rawJson: String, source: String): List<ChannelEntity> {
        val result = LinkedHashMap<String, ChannelEntity>()
        val array: JSONArray = when {
            rawJson.startsWith("[") -> runCatching { JSONArray(rawJson) }.getOrNull() ?: return emptyList()
            rawJson.startsWith("{") -> {
                val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return emptyList()
                root.optJSONArray("channels")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("items")
                    ?: return emptyList()
            }
            else -> return emptyList()
        }

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = sequenceOf("name", "title", "channel_name", "channelName")
                .map { obj.optString(it).trim() }
                .firstOrNull { it.isNotEmpty() }
                ?: continue
            val url = sequenceOf("link", "url", "stream_url", "streamUrl", "hls_url", "hlsUrl")
                .map { obj.optString(it).trim() }
                .firstOrNull { isValidStreamUrl(it) }
                ?: continue

            val group = normalizeGroupTitle(
                sequenceOf("category", "category_name", "group", "group_title", "genre")
                    .map { obj.optString(it).trim() }
                    .firstOrNull { it.isNotEmpty() }
                    ?: "Toffee Live"
            )

            val logo = sequenceOf("logo", "icon", "tvg_logo", "image", "thumbnail", "poster")
                .map { obj.optString(it).trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()

            val tvgId = sequenceOf("tvg_id", "tvgId", "epg_id", "id")
                .map { obj.optString(it).trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()

            val headersMap = LinkedHashMap<String, String>()
            obj.optJSONObject("headers")?.let { hdrObj ->
                val keys = hdrObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = hdrObj.optString(k).trim()
                    if (v.isNotEmpty()) {
                        headersMap[normalizeHeaderName(k)] = v
                    }
                }
            }
            val directCookie = obj.optString("cookie").trim()
            if (directCookie.isNotEmpty() && !headersMap.containsKey("Cookie")) {
                headersMap["Cookie"] = directCookie
            }
            val directUa = obj.optString("user_agent").trim().ifEmpty { obj.optString("userAgent").trim() }
            if (directUa.isNotEmpty() && !headersMap.containsKey("User-Agent")) {
                headersMap["User-Agent"] = directUa
            }

            if (!result.containsKey(url)) {
                result[url] = ChannelEntity(
                    url = url,
                    name = name,
                    groupTitle = group,
                    logoUrl = logo,
                    tvgId = tvgId,
                    source = source,
                    headersJson = encodeHeadersJson(headersMap)
                )
            }
        }
        return result.values.toList()
    }

    fun encodeHeadersJson(headers: Map<String, String>): String {
        if (headers.isEmpty()) return ""
        val obj = JSONObject()
        headers.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) {
                obj.put(k, v)
            }
        }
        return if (obj.length() == 0) "" else obj.toString()
    }

    fun parseHeadersMap(headersJson: String): Map<String, String> {
        if (headersJson.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(headersJson)
            val map = LinkedHashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.optString(key).trim()
                if (value.isNotEmpty()) {
                    map[key] = value
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun normalizeHeaderName(raw: String): String = when (raw.trim().lowercase()) {
        "cookie" -> "Cookie"
        "user-agent", "useragent" -> "User-Agent"
        "referer", "referrer" -> "Referer"
        "origin" -> "Origin"
        else -> raw.trim()
    }

    fun isValidStreamUrl(url: String): Boolean {
        val trimmed = url.trim()
        return httpRegex.containsMatchIn(trimmed) && trimmed.length > 10
    }
}
