package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import android.util.Rational
import com.example.data.model.AppLanguage
import com.example.data.network.ToffeeExtractor
import com.example.data.parser.EpgParser
import com.example.data.parser.M3uParser
import com.example.ui.util.PipHelper
import com.example.ui.util.ToffeeStrings
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ashika Live TV", appName)
  }

  @Test
  fun `m3u parser extracts tvgId and header epg url`() {
    val m3u = """
      #EXTM3U url-tvg="https://example.com/guide.xml.gz"
      #EXTINF:-1 tvg-id="sports.1" tvg-logo="https://example.com/logo.png" group-title="Sports",T Sports HD
      https://example.com/stream.m3u8
    """.trimIndent()

    val parsed = M3uParser.parseWithMetadata(m3u)
    assertEquals(listOf("https://example.com/guide.xml.gz"), parsed.epgUrls)
    assertEquals(1, parsed.channels.size)
    assertEquals("sports.1", parsed.channels.first().tvgId)
    assertEquals("T Sports HD", parsed.channels.first().name)
  }

  @Test
  fun `m3u parser extracts exthttp and extvlcopt headers and toffee json`() {
    val m3u = """
      #EXTM3U
      #EXTINF:-1 tvg-logo="https://toffee.cdn/somoy.png" group-title="News",Somoy TV
      #EXTVLCOPT:http-user-agent=ToffeeAndroid/3.0
      #EXTHTTP:{"cookie":"Edge-Cache-Cookie=abc123","referer":"https://toffeelive.com/"}
      https://bldcmprod-cdn.toffeelive.com/cdn/live/somoy/playlist.m3u8
    """.trimIndent()

    val m3uResult = M3uParser.parseWithMetadata(m3u)
    assertEquals(1, m3uResult.channels.size)
    val hdrs = M3uParser.parseHeadersMap(m3uResult.channels.first().headersJson)
    assertEquals("ToffeeAndroid/3.0", hdrs["User-Agent"])
    assertEquals("Edge-Cache-Cookie=abc123", hdrs["Cookie"])
    assertEquals("https://toffeelive.com/", hdrs["Referer"])

    val jsonFeed = """
      {
        "channels": [
          {
            "name": "Toffee Sports",
            "category": "Sports",
            "logo": "https://toffee.cdn/sports.png",
            "link": "https://bldcmprod-cdn.toffeelive.com/cdn/live/sports/playlist.m3u8",
            "headers": {
              "Cookie": "Edge-Cache-Cookie=xyz789",
              "User-Agent": "Toffee/1.0"
            }
          }
        ]
      }
    """.trimIndent()
    val jsonResult = M3uParser.parseWithMetadata(jsonFeed)
    assertEquals(1, jsonResult.channels.size)
    assertEquals("Toffee Sports", jsonResult.channels.first().name)
    assertEquals("Sports", jsonResult.channels.first().groupTitle)
    val jsonHdrs = M3uParser.parseHeadersMap(jsonResult.channels.first().headersJson)
    assertEquals("Edge-Cache-Cookie=xyz789", jsonHdrs["Cookie"])
  }

  @Test
  fun `m3u parser handles extgrp, quoted commas, single quotes, and pipe headers`() {
    val m3u = """
      #EXTM3U
      #EXTINF:-1 tvg-name='Channel, One' tvg-logo='https://img.test/1.png',Channel, One HD
      #EXTGRP:Bangla Entertainment
      https://stream.test/live1.m3u8|User-Agent=CustomAgent&Referer=https%3A%2F%2Fexample.com%2F
      #EXTINF:-1 group-title="Sports;Live",Sports Arena
      https://stream.test/sports.m3u8
    """.trimIndent()

    val parsed = M3uParser.parseWithMetadata(m3u)
    assertEquals(2, parsed.channels.size)
    assertEquals("Channel, One HD", parsed.channels[0].name)
    assertEquals("Bangla Entertainment", parsed.channels[0].groupTitle)
    assertEquals("https://img.test/1.png", parsed.channels[0].logoUrl)
    assertEquals("https://stream.test/live1.m3u8", parsed.channels[0].url)
    val hdrs = M3uParser.parseHeadersMap(parsed.channels[0].headersJson)
    assertEquals("CustomAgent", hdrs["User-Agent"])
    assertEquals("https://example.com/", hdrs["Referer"])
    assertEquals("Sports", parsed.channels[1].groupTitle)
  }

  @Test
  fun `toffee extractor parses next data json channels and metadata`() {
    val html = """
      <html><head>
      <script id="__NEXT_DATA__" type="application/json">
      {
        "props": {
          "pageProps": {
            "categories": [
              {
                "category_name": "News",
                "channels": [
                  {
                    "channel_name": "Jamuna TV",
                    "channel_logo": "/images/jamuna.png",
                    "hls_url": "https://cdn.example.com/live/jamuna.m3u8"
                  },
                  {
                    "channel_name": "Ekattor TV",
                    "channel_logo": "https://cdn.toffee.test/ekattor.png"
                  }
                ]
              }
            ]
          }
        }
      }
      </script>
      </head><body></body></html>
    """.trimIndent()

    val extractor = ToffeeExtractor(OkHttpClient())
    val extracted = extractor.extractFromHtml(html)
    assertEquals(1, extracted.playableChannels.size)
    assertEquals("Jamuna TV", extracted.playableChannels.first().name)
    assertEquals("https://toffeelive.com/images/jamuna.png", extracted.playableChannels.first().logoUrl)
    assertTrue(extracted.channelMetadata.containsKey(ToffeeExtractor.normalizeChannelKey("Ekattor TV")))
  }

  @Test
  fun `pip aspect ratio stays within android bounds`() {
    assertEquals(Rational(16, 9), PipHelper.buildSafeAspectRatio(0, 0))
    val ultraWide = PipHelper.buildSafeAspectRatio(4000, 1000)
    assertTrue(ultraWide.toFloat() <= 2.39f)
    val ultraTall = PipHelper.buildSafeAspectRatio(100, 1000)
    assertTrue(ultraTall.toFloat() >= 100f / 239f)
  }

  @Test
  fun `epg parser reads plain and gzipped xmltv feeds`() {
    val now = System.currentTimeMillis()
    val fmt = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val startStr = fmt.format(Date(now - 10 * 60 * 1000L))
    val stopStr = fmt.format(Date(now + 50 * 60 * 1000L))

    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <tv>
        <channel id="sports.1">
          <display-name>T Sports HD</display-name>
        </channel>
        <programme start="$startStr" stop="$stopStr" channel="sports.1">
          <title>Live Cricket Match</title>
          <desc>Championship coverage</desc>
          <category>Sports</category>
        </programme>
      </tv>
    """.trimIndent()

    val plainParsed = EpgParser.parseStream(ByteArrayInputStream(xml.toByteArray()), nowMillis = now)
    assertEquals(1, plainParsed.channels.size)
    assertEquals("T Sports HD", plainParsed.channels.first().displayName)
    assertEquals(1, plainParsed.programs.size)
    assertEquals("Live Cricket Match", plainParsed.programs.first().title)

    val baos = ByteArrayOutputStream()
    GZIPOutputStream(baos).use { it.write(xml.toByteArray()) }
    val gzParsed = EpgParser.parseStream(ByteArrayInputStream(baos.toByteArray()), nowMillis = now)
    assertEquals(1, gzParsed.programs.size)
    assertTrue(gzParsed.programs.first().endMillis > gzParsed.programs.first().startMillis)
  }

  @Test
  fun `multi language strings and diagnostics labels resolve for all supported languages`() {
    assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
    assertEquals(AppLanguage.URDU, AppLanguage.fromCode("ur"))
    assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
    assertEquals(AppLanguage.BANGLA, AppLanguage.fromCode("unknown"))

    AppLanguage.entries.forEach { lang ->
      assertTrue(ToffeeStrings.navHome(lang).isNotBlank())
      assertTrue(ToffeeStrings.diagnosticsTitle(lang).isNotBlank())
      assertTrue(ToffeeStrings.diagBitrateLabel(lang).isNotBlank())
      assertTrue(ToffeeStrings.diagResolutionLabel(lang).isNotBlank())
      assertTrue(ToffeeStrings.diagFrameRateLabel(lang).isNotBlank())
    }
  }
}
