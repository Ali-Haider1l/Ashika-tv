package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.AshikaDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelFilterTab
import com.example.data.model.FamilyProfileEntity
import com.example.data.model.MatchEntryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.StreamHealthInfo
import com.example.data.model.WatchStatEntity
import com.example.data.network.PlaylistFetcher
import com.example.data.parser.M3uParser
import com.example.data.repository.AshikaRepository
import com.example.ui.util.QrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class AshikaUiState(
    val allChannels: List<ChannelEntity> = emptyList(),
    val filteredChannels: List<ChannelEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val profiles: List<FamilyProfileEntity> = emptyList(),
    val activeProfile: String = PreferencesManager.DEFAULT_PROFILE,
    val favoriteUrls: Set<String> = emptySet(),
    val recentUrls: List<String> = emptyList(),
    val categories: List<String> = listOf("All", "News", "Sports", "Entertainment", "Music"),
    val selectedCategory: String = "All",
    val selectedTab: ChannelFilterTab = ChannelFilterTab.ALL,
    val searchQuery: String = "",
    val currentChannel: ChannelEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val showPlayerChannelDrawer: Boolean = false,
    val streamHealth: StreamHealthInfo = StreamHealthInfo(),
    val watchStats: List<WatchStatEntity> = emptyList(),
    val matches: List<MatchEntryEntity> = emptyList(),
    val hasAdminPin: Boolean = false,
    val isAdminUnlocked: Boolean = false,
    val lanSyncUrl: String = "",
    val isBusy: Boolean = false,
    val finderQuery: String = "",
    val finderResults: List<ChannelEntity> = emptyList(),
    val pairingCode: String? = null,
    val pairingQrBitmap: Bitmap? = null
)

private data class LocalControlsState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedTab: ChannelFilterTab = ChannelFilterTab.ALL,
    val currentChannel: ChannelEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val showPlayerChannelDrawer: Boolean = false,
    val streamHealth: StreamHealthInfo = StreamHealthInfo(),
    val isAdminUnlocked: Boolean = false,
    val isBusy: Boolean = false,
    val finderQuery: String = "",
    val finderResults: List<ChannelEntity> = emptyList(),
    val pairingCode: String? = null,
    val pairingQrBitmap: Bitmap? = null
)

class AshikaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AshikaDatabase.getInstance(application)
    private val prefs = PreferencesManager(application)
    private val repository = AshikaRepository(database.ashikaDao(), prefs, PlaylistFetcher())

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    private val controls = MutableStateFlow(LocalControlsState())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var videoWidth = 0
    private var videoHeight = 0

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            controls.update { it.copy(isPlaying = isPlaying) }
            refreshStreamHealth()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            controls.update {
                it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }
            refreshStreamHealth()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoWidth = videoSize.width
            videoHeight = videoSize.height
            refreshStreamHealth()
        }

        override fun onPlayerError(error: PlaybackException) {
            val current = controls.value.currentChannel
            controls.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    streamHealth = StreamHealthInfo(
                        channelName = current?.name.orEmpty(),
                        channelUrl = current?.url.orEmpty(),
                        playbackStateLabel = "Error (${error.errorCodeName})",
                        isPlaying = false,
                        bufferedPercentage = exoPlayer.bufferedPercentage,
                        videoResolution = resolutionString(),
                        errorMessage = error.localizedMessage ?: "Stream unavailable or unsupported format",
                        checkedAt = System.currentTimeMillis()
                    )
                )
            }
            emitMessage("Playback error: ${error.errorCodeName}")
        }
    }

    private val libraryFlow = combine(
        repository.channelsFlow,
        repository.playlistsFlow,
        repository.profilesFlow,
        repository.activeProfileFlow,
        repository.activeFavoriteUrlsFlow
    ) { channels, playlists, profiles, activeProfile, favUrls ->
        LibrarySnapshot(channels, playlists, profiles, activeProfile, favUrls.toSet())
    }

    private val activityFlow = combine(
        repository.activeRecentUrlsFlow,
        repository.watchStatsFlow,
        repository.matchesFlow,
        repository.hasAdminPinFlow,
        repository.lanSyncUrlFlow
    ) { recentUrls, stats, matches, hasPin, lanUrl ->
        ActivitySnapshot(recentUrls, stats, matches, hasPin, lanUrl)
    }

    val uiState: StateFlow<AshikaUiState> = combine(
        libraryFlow,
        activityFlow,
        controls
    ) { lib, act, ctrl ->
        val baseCategories = listOf("All", "News", "Sports", "Entertainment", "Music")
        val dynamicCategories = lib.channels
            .map { it.groupTitle.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .filter { cat -> baseCategories.none { it.equals(cat, ignoreCase = true) } }
            .sorted()
        val categories = baseCategories + dynamicCategories

        val query = ctrl.searchQuery.trim().lowercase()
        val recentIndexMap = act.recentUrls.withIndex().associate { it.value to it.index }

        val tabFiltered = when (ctrl.selectedTab) {
            ChannelFilterTab.ALL -> lib.channels
            ChannelFilterTab.FAVORITES -> lib.channels.filter { lib.favoriteUrls.contains(it.url) }
            ChannelFilterTab.RECENT -> lib.channels
                .filter { recentIndexMap.containsKey(it.url) }
                .sortedBy { recentIndexMap[it.url] ?: Int.MAX_VALUE }
        }

        val filtered = tabFiltered.filter { channel ->
            val catMatch = ctrl.selectedCategory == "All" ||
                channel.groupTitle.contains(ctrl.selectedCategory, ignoreCase = true)
            val queryMatch = query.isEmpty() ||
                channel.name.lowercase().contains(query) ||
                channel.groupTitle.lowercase().contains(query)
            catMatch && queryMatch
        }

        AshikaUiState(
            allChannels = lib.channels,
            filteredChannels = filtered,
            playlists = lib.playlists,
            profiles = lib.profiles,
            activeProfile = lib.activeProfile,
            favoriteUrls = lib.favoriteUrls,
            recentUrls = act.recentUrls,
            categories = categories,
            selectedCategory = ctrl.selectedCategory,
            selectedTab = ctrl.selectedTab,
            searchQuery = ctrl.searchQuery,
            currentChannel = ctrl.currentChannel,
            isPlaying = ctrl.isPlaying,
            isBuffering = ctrl.isBuffering,
            isFullscreen = ctrl.isFullscreen,
            showPlayerChannelDrawer = ctrl.showPlayerChannelDrawer,
            streamHealth = ctrl.streamHealth,
            watchStats = act.stats,
            matches = act.matches,
            hasAdminPin = act.hasAdminPin,
            isAdminUnlocked = ctrl.isAdminUnlocked,
            lanSyncUrl = act.lanSyncUrl,
            isBusy = ctrl.isBusy,
            finderQuery = ctrl.finderQuery,
            finderResults = ctrl.finderResults,
            pairingCode = ctrl.pairingCode,
            pairingQrBitmap = ctrl.pairingQrBitmap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AshikaUiState()
    )

    init {
        exoPlayer.addListener(playerListener)
        viewModelScope.launch {
            val seedResult = repository.ensureInitialized()
            seedResult?.onSuccess { count ->
                emitMessage("Loaded $count starter channels")
            }?.onFailure {
                emitMessage("Offline or playlist unreachable. Add a playlist or local .m3u file.")
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                val current = controls.value.currentChannel
                if (current != null && exoPlayer.isPlaying) {
                    repository.addWatchSeconds(current.url, current.name, 5L)
                    refreshStreamHealth()
                }
            }
        }
    }

    fun playChannel(channel: ChannelEntity) {
        videoWidth = 0
        videoHeight = 0
        controls.update {
            it.copy(
                currentChannel = channel,
                isBuffering = true,
                streamHealth = StreamHealthInfo(
                    channelName = channel.name,
                    channelUrl = channel.url,
                    playbackStateLabel = "Connecting…",
                    isPlaying = false,
                    bufferedPercentage = 0,
                    videoResolution = "—"
                )
            )
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        viewModelScope.launch {
            repository.recordRecentWatch(uiState.value.activeProfile, channel.url)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && controls.value.currentChannel != null) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun stopPlayback() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        videoWidth = 0
        videoHeight = 0
        controls.update {
            it.copy(
                currentChannel = null,
                isPlaying = false,
                isBuffering = false,
                isFullscreen = false,
                showPlayerChannelDrawer = false,
                streamHealth = StreamHealthInfo()
            )
        }
    }

    fun skipChannel(step: Int) {
        val list = uiState.value.filteredChannels.ifEmpty { uiState.value.allChannels }
        if (list.isEmpty()) return
        val currentUrl = controls.value.currentChannel?.url
        val index = list.indexOfFirst { it.url == currentUrl }.let { if (it < 0) 0 else it }
        val nextIndex = (index + step + list.size) % list.size
        playChannel(list[nextIndex])
    }

    fun setFullscreen(fullscreen: Boolean) {
        controls.update { it.copy(isFullscreen = fullscreen) }
    }

    fun togglePlayerChannelDrawer() {
        controls.update { it.copy(showPlayerChannelDrawer = !it.showPlayerChannelDrawer) }
    }

    fun updateSearchQuery(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String) {
        controls.update { it.copy(selectedCategory = category) }
    }

    fun selectTab(tab: ChannelFilterTab) {
        controls.update { it.copy(selectedTab = tab) }
    }

    fun toggleFavorite(channel: ChannelEntity) {
        val profile = uiState.value.activeProfile
        viewModelScope.launch {
            repository.toggleFavorite(profile, channel.url)
        }
    }

    fun saveChannel(
        name: String,
        url: String,
        category: String,
        logoUrl: String,
        previousUrl: String? = null
    ) {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName.isEmpty() || !M3uParser.isValidStreamUrl(cleanUrl)) {
            emitMessage("Enter a channel name and valid HTTP/HTTPS stream URL.")
            return
        }
        viewModelScope.launch {
            if (!previousUrl.isNullOrBlank() && previousUrl != cleanUrl) {
                repository.deleteChannel(previousUrl)
            }
            repository.saveChannel(
                ChannelEntity(
                    url = cleanUrl,
                    name = cleanName,
                    groupTitle = category.trim().ifEmpty { "Other" },
                    logoUrl = logoUrl.trim(),
                    source = "manual"
                )
            )
            emitMessage("Saved \"$cleanName\"")
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            if (controls.value.currentChannel?.url == channel.url) {
                stopPlayback()
            }
            repository.deleteChannel(channel.url)
            emitMessage("Deleted \"${channel.name}\"")
        }
    }

    fun loadPlaylistUrl(url: String) {
        viewModelScope.launch {
            controls.update { it.copy(isBusy = true) }
            repository.loadPlaylistFromUrl(url)
                .onSuccess { count -> emitMessage("Loaded $count channels from playlist") }
                .onFailure { err -> emitMessage("Could not load playlist: ${err.message}") }
            controls.update { it.copy(isBusy = false) }
        }
    }

    fun importLocalM3u(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            controls.update { it.copy(isBusy = true) }
            repository.importLocalM3u(contentResolver, uri)
                .onSuccess { count -> emitMessage("Imported $count channels from M3U file") }
                .onFailure { err -> emitMessage("File import failed: ${err.message}") }
            controls.update { it.copy(isBusy = false) }
        }
    }

    fun removePlaylist(url: String) {
        viewModelScope.launch {
            repository.removePlaylist(url)
            emitMessage("Removed saved playlist")
        }
    }

    fun syncFromLan(endpointUrl: String) {
        viewModelScope.launch {
            controls.update { it.copy(isBusy = true) }
            repository.syncFromLanCompanion(endpointUrl)
                .onSuccess { count -> emitMessage("Synced $count channels from companion server") }
                .onFailure { err -> emitMessage("Companion sync failed: ${err.message}") }
            controls.update { it.copy(isBusy = false) }
        }
    }

    fun switchProfile(name: String) {
        viewModelScope.launch {
            repository.switchProfile(name)
            emitMessage("Switched to $name profile")
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            repository.createProfile(name)
                .onSuccess { emitMessage("Profile \"${name.trim()}\" created") }
                .onFailure { err -> emitMessage(err.message ?: "Invalid profile name") }
        }
    }

    fun deleteProfile(name: String) {
        viewModelScope.launch {
            repository.deleteProfile(name)
            emitMessage("Deleted profile \"$name\"")
        }
    }

    fun addMatch(teams: String, time: String, score: String) {
        if (teams.isBlank()) {
            emitMessage("Enter team names first.")
            return
        }
        viewModelScope.launch {
            repository.addMatch(teams, time, score)
            emitMessage("Match added to scoreboard")
        }
    }

    fun deleteMatch(id: Long) {
        viewModelScope.launch {
            repository.deleteMatch(id)
        }
    }

    fun clearWatchStats() {
        viewModelScope.launch {
            repository.clearWatchStats()
            emitMessage("Watch statistics cleared")
        }
    }

    fun refreshStreamHealth() {
        val current = controls.value.currentChannel
        if (current == null) {
            controls.update { it.copy(streamHealth = StreamHealthInfo()) }
            return
        }
        val stateLabel = when (exoPlayer.playbackState) {
            Player.STATE_IDLE -> "Idle"
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> if (exoPlayer.isPlaying) "Healthy • Live" else "Paused"
            Player.STATE_ENDED -> "Ended"
            else -> "Unknown"
        }
        controls.update {
            it.copy(
                streamHealth = StreamHealthInfo(
                    channelName = current.name,
                    channelUrl = current.url,
                    playbackStateLabel = stateLabel,
                    isPlaying = exoPlayer.isPlaying,
                    bufferedPercentage = exoPlayer.bufferedPercentage,
                    videoResolution = resolutionString(),
                    errorMessage = exoPlayer.playerError?.localizedMessage,
                    checkedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun runChannelFinder(queryInput: String) {
        val trimmed = queryInput.trim()
        val all = uiState.value.allChannels
        if (trimmed.isEmpty()) {
            controls.update { it.copy(finderQuery = "", finderResults = emptyList()) }
            return
        }
        val q = trimmed.lowercase()
        val favs = uiState.value.favoriteUrls
        val recents = uiState.value.recentUrls.toSet()

        val matches = when {
            q.contains("favorite") || q.contains("favourite") ->
                all.filter { favs.contains(it.url) }
            q.contains("recent") ->
                all.filter { recents.contains(it.url) }
            else -> {
                val words = q.split(Regex("""\s+""")).filter { it.length > 2 }
                val direct = all.filter { channel ->
                    val haystack = "${channel.name} ${channel.groupTitle}".lowercase()
                    haystack.contains(q) || (words.isNotEmpty() && words.any { haystack.contains(it) })
                }
                if (direct.isNotEmpty()) direct
                else if (Regex("sport|cricket|football|match").containsMatchIn(q)) {
                    all.filter {
                        Regex("sport|cricket|football", RegexOption.IGNORE_CASE)
                            .containsMatchIn("${it.name} ${it.groupTitle}")
                    }
                } else if (q.contains("news")) {
                    all.filter { it.groupTitle.contains("news", ignoreCase = true) || it.name.contains("news", ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
        }.take(20)

        controls.update { it.copy(finderQuery = trimmed, finderResults = matches) }
    }

    fun generatePairingCode() {
        val code = Random.nextInt(100_000, 999_999).toString()
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                QrGenerator.generateQrBitmap("ashika://pair?code=$code")
            }
            controls.update {
                it.copy(pairingCode = code, pairingQrBitmap = bitmap)
            }
        }
    }

    fun submitAdminPin(pin: String) {
        val clean = pin.trim()
        if (clean.length < 4) {
            emitMessage("PIN must be at least 4 digits.")
            return
        }
        viewModelScope.launch {
            if (!uiState.value.hasAdminPin) {
                repository.setAdminPin(clean)
                controls.update { it.copy(isAdminUnlocked = true) }
                emitMessage("Admin PIN created and unlocked.")
            } else if (repository.verifyAdminPin(clean)) {
                controls.update { it.copy(isAdminUnlocked = true) }
            } else {
                emitMessage("Incorrect Admin PIN.")
            }
        }
    }

    fun lockAdmin() {
        controls.update { it.copy(isAdminUnlocked = false) }
    }

    fun saveAndRefreshAdminPlaylists(rawText: String) {
        val urls = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            controls.update { it.copy(isBusy = true) }
            repository.saveBulkPlaylistsAndRefresh(urls)
                .onSuccess { total -> emitMessage("Refreshed playlists ($total channels synced)") }
                .onFailure { err -> emitMessage("Refresh failed: ${err.message}") }
            controls.update { it.copy(isBusy = false) }
        }
    }

    private fun resolutionString(): String {
        return if (videoWidth > 0 && videoHeight > 0) "${videoWidth}×${videoHeight}" else "Audio / Pending"
    }

    private fun emitMessage(message: String) {
        _messages.tryEmit(message)
    }

    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        super.onCleared()
    }

    private data class LibrarySnapshot(
        val channels: List<ChannelEntity>,
        val playlists: List<PlaylistEntity>,
        val profiles: List<FamilyProfileEntity>,
        val activeProfile: String,
        val favoriteUrls: Set<String>
    )

    private data class ActivitySnapshot(
        val recentUrls: List<String>,
        val stats: List<WatchStatEntity>,
        val matches: List<MatchEntryEntity>,
        val hasAdminPin: Boolean,
        val lanSyncUrl: String
    )
}
