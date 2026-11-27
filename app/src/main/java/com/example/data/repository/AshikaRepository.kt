package com.example.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.data.local.AshikaDao
import com.example.data.local.PreferencesManager
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.EpgChannelEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.FamilyProfileEntity
import com.example.data.model.MatchEntryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.ProfileRecentEntity
import com.example.data.model.WatchStatEntity
import com.example.data.network.PlaylistFetcher
import com.example.data.parser.EpgParser
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
class AshikaRepository(
    private val dao: AshikaDao,
    private val prefs: PreferencesManager,
    private val fetcher: PlaylistFetcher
) {
    val channelsFlow: Flow<List<ChannelEntity>> = dao.observeChannels()
    val playlistsFlow: Flow<List<PlaylistEntity>> = dao.observePlaylists()
    val profilesFlow: Flow<List<FamilyProfileEntity>> = dao.observeProfiles()
    val activeProfileFlow: Flow<String> = prefs.activeProfileFlow
    val hasAdminPinFlow: Flow<Boolean> = prefs.hasAdminPinFlow
    val lanSyncUrlFlow: Flow<String> = prefs.lanSyncUrlFlow
    val watchStatsFlow: Flow<List<WatchStatEntity>> = dao.observeWatchStats()
    val matchesFlow: Flow<List<MatchEntryEntity>> = dao.observeMatches()
    val epgChannelsFlow: Flow<List<EpgChannelEntity>> = dao.observeEpgChannels()
    val epgProgramsFlow: Flow<List<EpgProgramEntity>> = dao.observeEpgPrograms()
    val epgUrlFlow: Flow<String> = prefs.epgUrlFlow
    val epgLastUpdatedFlow: Flow<Long> = prefs.epgLastUpdatedFlow
    val isBanglaModeFlow: Flow<Boolean> = prefs.isBanglaModeFlow
    val appLanguageFlow: Flow<AppLanguage> = prefs.appLanguageFlow

    val activeFavoriteUrlsFlow: Flow<List<String>> = activeProfileFlow.flatMapLatest { profile ->
        dao.observeFavoriteUrls(profile)
    }

    val activeFavoriteChannelsFlow: Flow<List<ChannelEntity>> = activeProfileFlow.flatMapLatest { profile ->
        dao.observeFavoriteChannels(profile)
    }

    val activeRecentUrlsFlow: Flow<List<String>> = activeProfileFlow.flatMapLatest { profile ->
        dao.observeRecentUrls(profile)
    }

    suspend fun ensureInitialized(): Result<Int>? {
        dao.insertProfile(FamilyProfileEntity(PreferencesManager.DEFAULT_PROFILE))
        if (!prefs.hasSeededDefaultPlaylist()) {
            prefs.markDefaultPlaylistSeeded()
            return syncToffeeAndAllChannels()
        }
        val existingPlaylists = dao.getPlaylistsSnapshot()
        if (existingPlaylists.none { it.url.equals(FREEHUNTER_HDTV_URL, ignoreCase = true) }) {
            return loadPlaylistFromUrl(FREEHUNTER_HDTV_URL)
        }
        return null
    }

    suspend fun syncToffeeAndAllChannels(): Result<Int> {
        val result = fetcher.fetchToffeeAndBdBundle(BD_AND_TOFFEE_FEEDS)
        return result.map { bundle ->
            dao.upsertChannels(bundle.channels)
            dao.syncChannelFavoriteFlagsForProfile(prefs.activeProfileFlow.first())

            val now = System.currentTimeMillis()
            if (bundle.toffeeExtractedCount > 0) {
                dao.upsertPlaylist(
                    PlaylistEntity(
                        url = TOFFEE_SITE_URL,
                        title = "Toffee Live (toffeelive.com)",
                        channelCount = bundle.toffeeExtractedCount,
                        lastRefreshedAt = now
                    )
                )
            }
            bundle.sourceCounts.forEach { (feedUrl, count) ->
                val title = friendlyPlaylistTitle(feedUrl)
                dao.upsertPlaylist(
                    PlaylistEntity(
                        url = feedUrl,
                        title = title,
                        channelCount = count,
                        lastRefreshedAt = now
                    )
                )
            }
            if (bundle.epgUrls.isNotEmpty() && prefs.epgUrlFlow.first().isBlank()) {
                loadEpgFromUrl(bundle.epgUrls.first())
            }
            bundle.channels.size
        }
    }

    suspend fun loadPlaylistFromUrl(url: String): Result<Int> {
        val cleanUrl = url.trim()
        if (!M3uParser.isValidStreamUrl(cleanUrl)) {
            return Result.failure(IllegalArgumentException("Enter a valid HTTP or HTTPS playlist URL."))
        }
        val result = fetcher.fetchM3u(cleanUrl)
        return result.map { parsed ->
            val loaded = parsed.channels
            dao.upsertChannels(loaded)
            dao.syncChannelFavoriteFlagsForProfile(prefs.activeProfileFlow.first())
            val title = friendlyPlaylistTitle(cleanUrl)
            dao.upsertPlaylist(
                PlaylistEntity(
                    url = cleanUrl,
                    title = title,
                    channelCount = loaded.size,
                    lastRefreshedAt = System.currentTimeMillis()
                )
            )
            if (parsed.epgUrls.isNotEmpty() && prefs.epgUrlFlow.first().isBlank()) {
                loadEpgFromUrl(parsed.epgUrls.first())
            }
            loaded.size
        }
    }

    private fun friendlyPlaylistTitle(feedUrl: String): String = when {
        feedUrl.contains("FreeHunter", ignoreCase = true) -> "FreeHunter HDTV Live"
        feedUrl.contains("Mrgify-BDIX-IPTV", ignoreCase = true) -> "BDIX & BD Live TV (Mrgify)"
        feedUrl.contains("countries/bd.m3u", ignoreCase = true) -> "Bangladesh Free-to-Air (iptv-org)"
        feedUrl.contains("languages/ben.m3u", ignoreCase = true) -> "Bengali TV Channels (iptv-org)"
        else -> runCatching {
            URI(feedUrl).host?.removePrefix("www.") ?: "M3U Playlist"
        }.getOrDefault("M3U Playlist")
    }

    suspend fun importLocalM3u(contentResolver: ContentResolver, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file."))
                val parsed = M3uParser.parseWithMetadata(text, source = "file")
                val channels = parsed.channels
                if (channels.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("No valid HTTP/HTTPS channels found in file."))
                }
                dao.upsertChannels(channels)
                dao.syncChannelFavoriteFlagsForProfile(prefs.activeProfileFlow.first())
                if (parsed.epgUrls.isNotEmpty() && prefs.epgUrlFlow.first().isBlank()) {
                    loadEpgFromUrl(parsed.epgUrls.first())
                }
                Result.success(channels.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun loadEpgFromUrl(url: String): Result<Int> {
        val cleanUrl = url.trim()
        if (!M3uParser.isValidStreamUrl(cleanUrl)) {
            return Result.failure(IllegalArgumentException("Enter a valid HTTP or HTTPS XMLTV EPG URL."))
        }
        val result = fetcher.fetchXmltvEpg(cleanUrl)
        return result.map { parsed ->
            dao.replaceEpgData(parsed.channels, parsed.programs)
            prefs.setEpgMetadata(cleanUrl, System.currentTimeMillis())
            parsed.programs.size
        }
    }

    suspend fun importLocalEpg(contentResolver: ContentResolver, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val parsed = contentResolver.openInputStream(uri)?.use { stream ->
                    EpgParser.parseStream(stream)
                } ?: return@withContext Result.failure(IllegalStateException("Unable to read EPG file."))
                if (parsed.programs.isEmpty() && parsed.channels.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("No EPG channels or programs found in file."))
                }
                dao.replaceEpgData(parsed.channels, parsed.programs)
                prefs.setEpgMetadata("Local XMLTV File", System.currentTimeMillis())
                Result.success(parsed.programs.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun clearEpgData() {
        dao.replaceEpgData(emptyList(), emptyList())
        prefs.setEpgMetadata("", 0L)
    }

    suspend fun saveChannel(channel: ChannelEntity) {
        dao.upsertChannel(channel)
        dao.syncChannelFavoriteFlagsForProfile(prefs.activeProfileFlow.first())
    }

    suspend fun deleteChannel(url: String) {
        dao.deleteChannelByUrl(url)
    }

    suspend fun removePlaylist(url: String) {
        dao.deletePlaylistByUrl(url)
    }

    suspend fun saveBulkPlaylistsAndRefresh(urls: List<String>): Result<Int> {
        val distinct = urls.map { it.trim() }.filter { M3uParser.isValidStreamUrl(it) }.distinct()
        if (distinct.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid playlist URLs provided."))
        }
        var total = 0
        var lastError: Throwable? = null
        distinct.forEach { url ->
            loadPlaylistFromUrl(url)
                .onSuccess { count -> total += count }
                .onFailure { lastError = it }
        }
        return if (total > 0) Result.success(total)
        else Result.failure(lastError ?: IllegalStateException("Failed to refresh playlists."))
    }

    suspend fun toggleFavorite(profileName: String, channelUrl: String): Boolean {
        return dao.toggleFavoriteForProfile(profileName, channelUrl)
    }

    suspend fun recordRecentWatch(profileName: String, channelUrl: String) {
        dao.upsertRecent(ProfileRecentEntity(profileName, channelUrl, System.currentTimeMillis()))
    }

    suspend fun addWatchSeconds(channelUrl: String, channelName: String, seconds: Long) {
        if (seconds > 0) {
            dao.incrementWatchSeconds(channelUrl, channelName, seconds)
        }
    }

    suspend fun clearWatchStats() {
        dao.clearWatchStats()
    }

    suspend fun switchProfile(name: String) {
        dao.insertProfile(FamilyProfileEntity(name))
        prefs.setActiveProfile(name)
        dao.syncChannelFavoriteFlagsForProfile(name)
    }

    suspend fun createProfile(rawName: String): Result<Unit> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a profile name."))
        }
        dao.insertProfile(FamilyProfileEntity(trimmed))
        prefs.setActiveProfile(trimmed)
        return Result.success(Unit)
    }

    suspend fun deleteProfile(name: String) {
        if (name == PreferencesManager.DEFAULT_PROFILE) return
        dao.deleteAllFavoritesForProfile(name)
        dao.deleteAllRecentsForProfile(name)
        dao.deleteProfile(name)
        prefs.setActiveProfile(PreferencesManager.DEFAULT_PROFILE)
    }

    suspend fun addMatch(teams: String, matchTime: String, score: String) {
        dao.insertMatch(
            MatchEntryEntity(
                teams = teams.trim(),
                matchTime = matchTime.trim().ifEmpty { "Scheduled" },
                score = score.trim().ifEmpty { "0 - 0" }
            )
        )
    }

    suspend fun deleteMatch(id: Long) {
        dao.deleteMatch(id)
    }

    suspend fun setAdminPin(pin: String) = prefs.saveAdminPin(pin)

    suspend fun verifyAdminPin(pin: String): Boolean = prefs.verifyAdminPin(pin)

    suspend fun setBanglaMode(enabled: Boolean) = prefs.setBanglaMode(enabled)

    suspend fun setAppLanguage(language: AppLanguage) = prefs.setAppLanguage(language)

    suspend fun syncFromLanCompanion(endpointUrl: String): Result<Int> {
        val clean = endpointUrl.trim()
        if (!M3uParser.isValidStreamUrl(clean)) {
            return Result.failure(IllegalArgumentException("Enter a valid HTTP/HTTPS companion URL."))
        }
        prefs.setLanSyncUrl(clean)
        return fetcher.fetchLanCompanionChannels(clean).map { incoming ->
            dao.deleteChannelsBySource("lan")
            dao.upsertChannels(incoming)
            incoming.size
        }
    }

    companion object {
        const val TOFFEE_SITE_URL = "https://toffeelive.com/bn/live-tv"
        const val FREEHUNTER_HDTV_URL =
            "https://raw.githubusercontent.com/ahan443/FreeHunter/refs/heads/main/HDTV.m3u"
        const val DEFAULT_PLAYLIST_URL =
            "https://github.com/abusaeeidx/Mrgify-BDIX-IPTV/raw/main/playlist.m3u"
        val BD_AND_TOFFEE_FEEDS = listOf(
            FREEHUNTER_HDTV_URL,
            DEFAULT_PLAYLIST_URL,
            "https://iptv-org.github.io/iptv/countries/bd.m3u",
            "https://iptv-org.github.io/iptv/languages/ben.m3u"
        )
    }
}
