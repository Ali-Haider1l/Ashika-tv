package com.example.data.parser

import android.util.Xml
import com.example.data.model.EpgChannelEntity
import com.example.data.model.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

data class ParsedEpgData(
    val channels: List<EpgChannelEntity>,
    val programs: List<EpgProgramEntity>
)

object EpgParser {

    private const val SIX_HOURS_MS = 6L * 60L * 60L * 1000L
    private const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L

    fun parseStream(inputStream: InputStream, nowMillis: Long = System.currentTimeMillis()): ParsedEpgData {
        val buffered = BufferedInputStream(inputStream, 8192)
        buffered.mark(4)
        val header = ByteArray(2)
        val read = buffered.read(header)
        buffered.reset()

        val isGzip = read == 2 && header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()
        val effectiveStream = if (isGzip) GZIPInputStream(buffered) else buffered

        return effectiveStream.use { stream ->
            parseXmltv(stream, nowMillis)
        }
    }

    private fun parseXmltv(stream: InputStream, nowMillis: Long): ParsedEpgData {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        val channelsMap = LinkedHashMap<String, EpgChannelEntity>()
        val programs = ArrayList<EpgProgramEntity>()

        val minKeepMillis = nowMillis - SIX_HOURS_MS
        val maxKeepMillis = nowMillis + SEVEN_DAYS_MS

        var eventType = parser.eventType
        var currentTag = ""

        var inChannel = false
        var channelId = ""
        var channelDisplayName = ""

        var inProgramme = false
        var progChannelId = ""
        var progStart = 0L
        var progEnd = 0L
        var progTitle = ""
        var progDesc = ""
        var progCategory = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty().lowercase(Locale.ROOT)
                    when (currentTag) {
                        "channel" -> {
                            inChannel = true
                            channelId = parser.getAttributeValue(null, "id")?.trim().orEmpty()
                            channelDisplayName = ""
                        }
                        "programme" -> {
                            inProgramme = true
                            progChannelId = parser.getAttributeValue(null, "channel")?.trim().orEmpty()
                            progStart = parseXmltvTimestamp(parser.getAttributeValue(null, "start"))
                            progEnd = parseXmltvTimestamp(parser.getAttributeValue(null, "stop"))
                            progTitle = ""
                            progDesc = ""
                            progCategory = ""
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        if (inChannel && currentTag == "display-name" && channelDisplayName.isEmpty()) {
                            channelDisplayName = text
                        } else if (inProgramme) {
                            when (currentTag) {
                                "title" -> if (progTitle.isEmpty()) progTitle = text
                                "desc" -> if (progDesc.isEmpty()) progDesc = text
                                "category" -> if (progCategory.isEmpty()) progCategory = text
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val endTag = parser.name.orEmpty().lowercase(Locale.ROOT)
                    if (endTag == "channel" && inChannel) {
                        if (channelId.isNotEmpty() && !channelsMap.containsKey(channelId)) {
                            channelsMap[channelId] = EpgChannelEntity(
                                epgId = channelId,
                                displayName = channelDisplayName.ifEmpty { channelId }
                            )
                        }
                        inChannel = false
                    } else if (endTag == "programme" && inProgramme) {
                        val effectiveEnd = if (progEnd > progStart) progEnd else progStart + 30 * 60 * 1000L
                        if (
                            progChannelId.isNotEmpty() &&
                            progTitle.isNotEmpty() &&
                            progStart > 0L &&
                            effectiveEnd >= minKeepMillis &&
                            progStart <= maxKeepMillis
                        ) {
                            programs.add(
                                EpgProgramEntity(
                                    channelId = progChannelId,
                                    title = progTitle,
                                    description = progDesc,
                                    category = progCategory,
                                    startMillis = progStart,
                                    endMillis = effectiveEnd
                                )
                            )
                        }
                        inProgramme = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return ParsedEpgData(
            channels = channelsMap.values.toList(),
            programs = programs
        )
    }

    fun parseXmltvTimestamp(raw: String?): Long {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return 0L
        val normalized = value.replace(Regex("""\s+"""), " ")

        val formats = listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmmssZ",
            "yyyyMMddHHmm Z",
            "yyyyMMddHHmmZ",
            "yyyyMMddHHmmss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (!pattern.contains("Z")) {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }
                val parsed = sdf.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return 0L
    }
}
