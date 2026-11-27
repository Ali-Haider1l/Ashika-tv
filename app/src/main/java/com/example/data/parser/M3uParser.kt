package com.example.data.parser

import com.example.data.model.ChannelEntity

object M3uParser {

    private val groupRegex = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val logoRegex = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val httpRegex = Regex("""^https?://""", RegexOption.IGNORE_CASE)

    fun parse(content: String, source: String = "playlist"): List<ChannelEntity> {
        val lines = content.lineSequence()
        val parsed = LinkedHashMap<String, ChannelEntity>()

        var pendingName = ""
        var pendingGroup = "Other"
        var pendingLogo = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val commaIdx = line.lastIndexOf(',')
                pendingName = if (commaIdx >= 0 && commaIdx + 1 < line.length) {
                    line.substring(commaIdx + 1).trim().ifEmpty { "Unknown Channel" }
                } else {
                    "Unknown Channel"
                }
                pendingGroup = groupRegex.find(line)?.groupValues?.getOrNull(1)
                    ?.trim()?.ifEmpty { "Other" } ?: "Other"
                pendingLogo = logoRegex.find(line)?.groupValues?.getOrNull(1)?.trim() ?: ""
            } else if (!line.startsWith("#") && pendingName.isNotEmpty()) {
                if (httpRegex.containsMatchIn(line) && !parsed.containsKey(line)) {
                    parsed[line] = ChannelEntity(
                        url = line,
                        name = pendingName,
                        groupTitle = pendingGroup,
                        logoUrl = pendingLogo,
                        source = source
                    )
                }
                pendingName = ""
                pendingGroup = "Other"
                pendingLogo = ""
            }
        }
        return parsed.values.toList()
    }

    fun isValidStreamUrl(url: String): Boolean {
        val trimmed = url.trim()
        return httpRegex.containsMatchIn(trimmed) && trimmed.length > 10
    }
}
