package com.example.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.data.local.AshikaDao
import com.example.data.local.PreferencesManager
import com.example.data.model.ChannelEntity
import com.example.data.model.FamilyProfileEntity
import com.example.data.model.MatchEntryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.ProfileFavoriteEntity
import com.example.data.model.ProfileRecentEntity
import com.example.data.model.WatchStatEntity
import com.example.data.network.PlaylistFetcher
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

    val activeFavoriteUrlsFlow: Flow<List<String>> = activeProfileFlow.flatMapLatest { profile ->
        dao.observeFavoriteUrls(profile)
    }

    val activeRecentUrlsFlow: Flow<List<String>> = activeProfileFlow.flatMapLatest { profile ->
        dao.observeRecentUrls(profile)
    }

    suspend fun ensureInitialized(): Result<Int>? {
        dao.insertProfile(FamilyProfileEntity(PreferencesManager.DEFAULT_PROFILE))
        if (!prefs.hasSeededDefaultPlaylist()) {
            prefs.markDefaultPlaylistSeeded()
            return loadPlaylistFromUrl(DEFAULT_PLAYLIST_URL)
        }
        return null
    }

    suspend fun loadPlaylistFromUrl(url: String): Result<Int> {
        val cleanUrl = url.trim()
        if (!M3uParser.isValidStreamUrl(cleanUrl)) {
            return Result.failure(IllegalArgumentException("Enter a valid HTTP or HTTPS playlist URL."))
        }
        val result = fetcher.fetchM3u(cleanUrl)
        return result.map { loaded ->
            dao.upsertChannels(loaded)
            val title = runCatching {
                URI(cleanUrl).host?.removePrefix("www.") ?: "M3U Playlist"
            }.getOrDefault("M3U Playlist")
            dao.upsertPlaylist(
                PlaylistEntity(
                    url = cleanUrl,
                    title = title,
                    channelCount = loaded.size,
                    lastRefreshedAt = System.currentTimeMillis()
                )
            )
            loaded.size
        }
    }

    suspend fun importLocalM3u(contentResolver: ContentResolver, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file."))
                val channels = M3uParser.parse(text, source = "file")
                if (channels.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("No valid HTTP/HTTPS channels found in file."))
                }
                dao.upsertChannels(channels)
                Result.success(channels.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun saveChannel(channel: ChannelEntity) {
        dao.upsertChannel(channel)
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

    suspend fun toggleFavorite(profileName: String, channelUrl: String) {
        if (dao.isFavorite(profileName, channelUrl)) {
            dao.deleteFavorite(profileName, channelUrl)
        } else {
            dao.insertFavorite(ProfileFavoriteEntity(profileName, channelUrl))
        }
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
        const val DEFAULT_PLAYLIST_URL =
            "https://github.com/abusaeeidx/Mrgify-BDIX-IPTV/raw/main/playlist.m3u"
    }
}
