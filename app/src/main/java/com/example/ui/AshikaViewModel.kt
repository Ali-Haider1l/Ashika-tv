package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Rational
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaLoadData
import com.example.data.local.AshikaDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelEpgStatus
import com.example.data.model.ChannelFilterTab
import com.example.data.model.EpgChannelEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.FamilyProfileEntity
import com.example.data.model.MatchEntryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.StreamHealthInfo
import com.example.data.model.WatchStatEntity
import com.example.data.network.PlaylistFetcher
import com.example.data.parser.M3uParser
import com.example.data.repository.AshikaRepository
import com.example.ui.util.PipHelper
import com.example.ui.util.QrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

data class AshikaUiState(
    val allChannels: List<ChannelEntity> = emptyList(),
    val filteredChannels: List<ChannelEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val profiles: List<FamilyProfileEntity> = emptyList(),
    val activeProfile: String = PreferencesManager.DEFAULT_PROFILE,
    val favoriteChannels: List<ChannelEntity> = emptyList(),
    val favoriteUrls: Set<String> = emptySet(),
    val recentUrls: List<String> = emptyList(),
    val categories: List<String> = listOf("All", "News", "Sports", "Entertainment", "Music"),
    val selectedCategory: String = "All",
    val selectedTab: ChannelFilterTab = ChannelFilterTab.ALL,
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val currentChannel: ChannelEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val isInAppPip: Boolean = false,
    val showDiagnosticsOverlay: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
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
    val pairingQrBitmap: Bitmap? = null,
    val epgUrl: String = "",
    val epgLastUpdatedAt: Long = 0L,
    val epgProgramsCount: Int = 0,
    val epgMap: Map<String, ChannelEpgStatus> = emptyMap(),
    val isLoadingEpg: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
    val appLanguage: AppLanguage = AppLanguage.BANGLA,
    val isBanglaMode: Boolean = true
)

private data class LocalControlsState(
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val selectedCategory: String = "All",
    val selectedTab: ChannelFilterTab = ChannelFilterTab.ALL,
    val currentChannel: ChannelEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val isInAppPip: Boolean = false,
    val showDiagnosticsOverlay: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val showPlayerChannelDrawer: Boolean = false,
    val streamHealth: StreamHealthInfo = StreamHealthInfo(),
    val isAdminUnlocked: Boolean = false,
    val isBusy: Boolean = false,
    val isLoadingEpg: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
    val finderQuery: String = "",
    val finderResults: List<ChannelEntity> = emptyList(),
    val pairingCode: String? = null,
    val pairingQrBitmap: Bitmap? = null
)

@OptIn(UnstableApi::class)
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
    private var measuredBitrateEstimateBps = 0L
    private var downstreamBitrateBps = 0L
    private var videoFormatBitrateBps = 0L
    private var audioFormatBitrateBps = 0L
    private var videoFrameRateFps = 0f
    private var videoCodecLabel = ""
    private var totalDroppedFrames = 0

    private val analyticsListener = object : AnalyticsListener {
        override fun onBandwidthEstimate(
            eventTime: AnalyticsListener.EventTime,
            totalLoadTimeMs: Int,
            totalBytesLoaded: Long,
            bitrateEstimate: Long
        ) {
            if (bitrateEstimate > 0L) {
                measuredBitrateEstimateBps = bitrateEstimate
            }
        }

        override fun onDownstreamFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            mediaLoadData: MediaLoadData
        ) {
            val fmt = mediaLoadData.trackFormat ?: return
            val br = fmt.bitrate.takeIf { it > 0 } ?: fmt.averageBitrate.takeIf { it > 0 } ?: 0
            if (br > 0) {
                downstreamBitrateBps = br.toLong()
            }
            if (fmt.frameRate > 0f) {
                videoFrameRateFps = fmt.frameRate
            }
            refreshStreamHealth()
        }

        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?
        ) {
            if (format.width > 0 && format.height > 0) {
                videoWidth = format.width
                videoHeight = format.height
            }
            val br = format.bitrate.takeIf { it > 0 } ?: format.averageBitrate.takeIf { it > 0 } ?: 0
            if (br > 0) videoFormatBitrateBps = br.toLong()
            if (format.frameRate > 0f) videoFrameRateFps = format.frameRate
            videoCodecLabel = format.codecs?.substringBefore('.')?.uppercase()
                ?: format.sampleMimeType?.substringAfter('/')?.uppercase().orEmpty()
            refreshStreamHealth()
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?
        ) {
            val br = format.bitrate.takeIf { it > 0 } ?: format.averageBitrate.takeIf { it > 0 } ?: 0
            if (br > 0) audioFormatBitrateBps = br.toLong()
            refreshStreamHealth()
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            if (droppedFrames > 0) {
                totalDroppedFrames += droppedFrames
            }
        }
    }

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
            controls.update {
                it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
            }
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
                        bitrateLabel = formatBitrateLabel(effectiveBitrateBps()),
                        frameRateLabel = formatFrameRateLabel(effectiveFrameRate()),
                        codecLabel = videoCodecLabel.ifBlank { "—" },
                        bitrateBps = effectiveBitrateBps(),
                        frameRateFps = effectiveFrameRate(),
                        droppedFrames = totalDroppedFrames,
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
        repository.activeFavoriteChannelsFlow
    ) { channels, playlists, profiles, activeProfile, favChannels ->
        LibrarySnapshot(channels, playlists, profiles, activeProfile, favChannels)
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

    private val epgFlow = combine(
        repository.epgChannelsFlow,
        repository.epgProgramsFlow,
        repository.epgUrlFlow,
        repository.epgLastUpdatedFlow
    ) { epgChannels, epgPrograms, epgUrl, updatedAt ->
        EpgSnapshot(epgChannels, epgPrograms, epgUrl, updatedAt)
    }

    val uiState: StateFlow<AshikaUiState> = combine(
        libraryFlow,
        activityFlow,
        epgFlow,
        repository.appLanguageFlow,
        controls
    ) { lib, act, epg, appLanguage, ctrl ->
        val dynamicCategories = lib.channels
            .map { it.groupTitle.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedWith(
                compareBy<String>(
                    { it.equals("Other", ignoreCase = true) },
                    { -lib.channels.count { ch -> ch.groupTitle.equals(it, ignoreCase = true) } },
                    { it.lowercase() }
                )
            )
        val categories = if (dynamicCategories.isEmpty()) {
            listOf("All", "News", "Sports", "Entertainment", "Music")
        } else {
            listOf("All") + dynamicCategories
        }

        val epgMap = buildChannelEpgMap(
            channels = lib.channels,
            epgChannels = epg.channels,
            epgPrograms = epg.programs,
            nowMillis = ctrl.nowMillis
        )

        val query = ctrl.searchQuery.trim().lowercase()
        val recentIndexMap = act.recentUrls.withIndex().associate { it.value to it.index }

        val favUrls = lib.favoriteChannels.map { it.url }.toSet()
        val tabFiltered = when (ctrl.selectedTab) {
            ChannelFilterTab.ALL -> lib.channels
            ChannelFilterTab.FAVORITES -> lib.favoriteChannels
            ChannelFilterTab.RECENT -> lib.channels
                .filter { recentIndexMap.containsKey(it.url) }
                .sortedBy { recentIndexMap[it.url] ?: Int.MAX_VALUE }
        }

        val filtered = tabFiltered.filter { channel ->
            val catMatch = ctrl.selectedCategory == "All" ||
                channel.groupTitle.equals(ctrl.selectedCategory, ignoreCase = true) ||
                channel.groupTitle.contains(ctrl.selectedCategory, ignoreCase = true)
            val epgNowTitle = epgMap[channel.url]?.now?.title.orEmpty().lowercase()
            val queryMatch = query.isEmpty() ||
                channel.name.lowercase().contains(query) ||
                channel.groupTitle.lowercase().contains(query) ||
                epgNowTitle.contains(query)
            catMatch && queryMatch
        }

        AshikaUiState(
            allChannels = lib.channels,
            filteredChannels = filtered,
            playlists = lib.playlists,
            profiles = lib.profiles,
            activeProfile = lib.activeProfile,
            favoriteChannels = lib.favoriteChannels,
            favoriteUrls = favUrls,
            recentUrls = act.recentUrls,
            categories = categories,
            selectedCategory = ctrl.selectedCategory,
            selectedTab = ctrl.selectedTab,
            searchQuery = ctrl.searchQuery,
            isSearchExpanded = ctrl.isSearchExpanded,
            currentChannel = ctrl.currentChannel,
            isPlaying = ctrl.isPlaying,
            isBuffering = ctrl.isBuffering,
            isFullscreen = ctrl.isFullscreen,
            isInAppPip = ctrl.isInAppPip,
            showDiagnosticsOverlay = ctrl.showDiagnosticsOverlay,
            videoWidth = ctrl.videoWidth,
            videoHeight = ctrl.videoHeight,
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
            pairingQrBitmap = ctrl.pairingQrBitmap,
            epgUrl = epg.url,
            epgLastUpdatedAt = epg.lastUpdatedAt,
            epgProgramsCount = epg.programs.size,
            epgMap = epgMap,
            isLoadingEpg = ctrl.isLoadingEpg,
            nowMillis = ctrl.nowMillis,
            appLanguage = appLanguage,
            isBanglaMode = appLanguage == AppLanguage.BANGLA
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AshikaUiState()
    )

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.addAnalyticsListener(analyticsListener)
        viewModelScope.launch {
            val seedResult = repository.ensureInitialized()
            seedResult?.onSuccess { count ->
                emitMessage("Loaded $count starter channels")
            }?.onFailure {
                emitMessage("Offline or playlist unreachable. Add a playlist or local .m3u file.")
            }
        }
        viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(1_000)
                tickCount++
                if (tickCount % 30 == 0) {
                    controls.update { it.copy(nowMillis = System.currentTimeMillis()) }
                }
                val current = controls.value.currentChannel
                if (current != null) {
                    refreshStreamHealth()
                    if (exoPlayer.isPlaying && tickCount % 5 == 0) {
                        repository.addWatchSeconds(current.url, current.name, 5L)
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun playChannel(channel: ChannelEntity) {
        videoWidth = 0
        videoHeight = 0
        measuredBitrateEstimateBps = 0L
        downstreamBitrateBps = 0L
        videoFormatBitrateBps = 0L
        audioFormatBitrateBps = 0L
        videoFrameRateFps = 0f
        videoCodecLabel = ""
        totalDroppedFrames = 0
        controls.update {
            it.copy(
                currentChannel = channel,
                isBuffering = true,
                videoWidth = 0,
                videoHeight = 0,
                streamHealth = StreamHealthInfo(
                    channelName = channel.name,
                    channelUrl = channel.url,
                    playbackStateLabel = "Connecting…",
                    isPlaying = false,
                    bufferedPercentage = 0,
                    videoResolution = "—",
                    bitrateLabel = "Measuring…",
                    frameRateLabel = "—"
                )
            )
        }
        val headers = M3uParser.parseHeadersMap(channel.headersJson)
        if (headers.isNotEmpty()) {
            val userAgent = headers.entries.firstOrNull {
                it.key.equals("User-Agent", ignoreCase = true)
            }?.value ?: "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36"
            val extraHeaders = headers.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
            val httpFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(userAgent)
                .apply {
                    if (extraHeaders.isNotEmpty()) {
                        setDefaultRequestProperties(extraHeaders)
                    }
                }
            val mediaSource = DefaultMediaSourceFactory(getApplication<Application>())
                .setDataSourceFactory(httpFactory)
                .createMediaSource(MediaItem.fromUri(channel.url))
            exoPlayer.setMediaSource(mediaSource)
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
        }
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
                isInAppPip = false,
                videoWidth = 0,
                videoHeight = 0,
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
        controls.update {
            it.copy(
                isFullscreen = fullscreen,
                isInAppPip = if (fullscreen) false else it.isInAppPip
            )
        }
    }

    fun setInAppPip(enabled: Boolean) {
        controls.update {
            it.copy(
                isInAppPip = enabled && it.currentChannel != null,
                isFullscreen = if (enabled) false else it.isFullscreen,
                showPlayerChannelDrawer = if (enabled) false else it.showPlayerChannelDrawer
            )
        }
    }

    fun toggleInAppPip() {
        setInAppPip(!controls.value.isInAppPip)
    }

    fun getSafePipAspectRatio(): Rational {
        return PipHelper.buildSafeAspectRatio(videoWidth, videoHeight)
    }

    fun togglePlayerChannelDrawer() {
        controls.update { it.copy(showPlayerChannelDrawer = !it.showPlayerChannelDrawer) }
    }

    fun toggleDiagnosticsOverlay() {
        controls.update { it.copy(showDiagnosticsOverlay = !it.showDiagnosticsOverlay) }
        refreshStreamHealth()
    }

    fun updateSearchQuery(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
            emitMessage("${language.nativeName} (${language.englishName})")
        }
    }

    fun toggleBanglaMode() {
        val next = if (uiState.value.appLanguage == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
        setAppLanguage(next)
    }

    fun toggleSearchExpanded() {
        controls.update {
            val next = !it.isSearchExpanded
            it.copy(
                isSearchExpanded = next,
                searchQuery = if (!next) "" else it.searchQuery
            )
        }
    }

    fun setSearchExpanded(expanded: Boolean) {
        controls.update {
            it.copy(
                isSearchExpanded = expanded,
                searchQuery = if (!expanded) "" else it.searchQuery
            )
        }
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
            val isNowStarred = repository.toggleFavorite(profile, channel.url)
            emitMessage(
                if (isNowStarred) "Starred \"${channel.name}\""
                else "Removed \"${channel.name}\" from Favorites"
            )
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
            val existingTvgId = previousUrl?.let { prev ->
                uiState.value.allChannels.firstOrNull { it.url == prev }?.tvgId
            }.orEmpty()
            if (!previousUrl.isNullOrBlank() && previousUrl != cleanUrl) {
                repository.deleteChannel(previousUrl)
            }
            repository.saveChannel(
                ChannelEntity(
                    url = cleanUrl,
                    name = cleanName,
                    groupTitle = category.trim().ifEmpty { "Other" },
                    logoUrl = logoUrl.trim(),
                    tvgId = existingTvgId,
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

    fun syncToffeeAndAllChannels() {
        viewModelScope.launch {
            controls.update { it.copy(isBusy = true) }
            repository.syncToffeeAndAllChannels()
                .onSuccess { count -> emitMessage("Synced $count Toffee & Bangladesh TV channels") }
                .onFailure { err -> emitMessage("Sync failed: ${err.message}") }
            controls.update { it.copy(isBusy = false) }
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

    fun loadEpgUrl(url: String) {
        viewModelScope.launch {
            controls.update { it.copy(isLoadingEpg = true) }
            repository.loadEpgFromUrl(url)
                .onSuccess { count ->
                    controls.update { it.copy(nowMillis = System.currentTimeMillis()) }
                    emitMessage("Loaded $count EPG program entries")
                }
                .onFailure { err ->
                    emitMessage("Could not load EPG: ${err.message}")
                }
            controls.update { it.copy(isLoadingEpg = false) }
        }
    }

    fun importLocalEpg(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            controls.update { it.copy(isLoadingEpg = true) }
            repository.importLocalEpg(contentResolver, uri)
                .onSuccess { count ->
                    controls.update { it.copy(nowMillis = System.currentTimeMillis()) }
                    emitMessage("Imported $count EPG programs")
                }
                .onFailure { err ->
                    emitMessage("EPG import failed: ${err.message}")
                }
            controls.update { it.copy(isLoadingEpg = false) }
        }
    }

    fun refreshEpg() {
        val url = uiState.value.epgUrl
        if (M3uParser.isValidStreamUrl(url)) {
            loadEpgUrl(url)
        } else {
            emitMessage("Enter an XMLTV EPG URL first.")
        }
    }

    fun clearEpg() {
        viewModelScope.launch {
            repository.clearEpgData()
            emitMessage("Cleared EPG guide data")
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
        exoPlayer.videoFormat?.let { vf ->
            if (vf.width > 0 && vf.height > 0) {
                videoWidth = vf.width
                videoHeight = vf.height
            }
            val br = vf.bitrate.takeIf { it > 0 } ?: vf.averageBitrate.takeIf { it > 0 } ?: 0
            if (br > 0) videoFormatBitrateBps = br.toLong()
            if (vf.frameRate > 0f) videoFrameRateFps = vf.frameRate
            if (videoCodecLabel.isBlank()) {
                videoCodecLabel = vf.codecs?.substringBefore('.')?.uppercase()
                    ?: vf.sampleMimeType?.substringAfter('/')?.uppercase().orEmpty()
            }
        }
        exoPlayer.audioFormat?.let { af ->
            val br = af.bitrate.takeIf { it > 0 } ?: af.averageBitrate.takeIf { it > 0 } ?: 0
            if (br > 0) audioFormatBitrateBps = br.toLong()
        }

        val stateLabel = when (exoPlayer.playbackState) {
            Player.STATE_IDLE -> "Idle"
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> if (exoPlayer.isPlaying) "Healthy • Live" else "Paused"
            Player.STATE_ENDED -> "Ended"
            else -> "Unknown"
        }
        val bitrateBps = effectiveBitrateBps()
        val fps = effectiveFrameRate()

        controls.update {
            it.copy(
                streamHealth = StreamHealthInfo(
                    channelName = current.name,
                    channelUrl = current.url,
                    playbackStateLabel = stateLabel,
                    isPlaying = exoPlayer.isPlaying,
                    bufferedPercentage = exoPlayer.bufferedPercentage,
                    videoResolution = resolutionString(),
                    bitrateLabel = formatBitrateLabel(bitrateBps),
                    frameRateLabel = formatFrameRateLabel(fps),
                    codecLabel = videoCodecLabel.ifBlank { "—" },
                    bitrateBps = bitrateBps,
                    frameRateFps = fps,
                    droppedFrames = totalDroppedFrames,
                    errorMessage = exoPlayer.playerError?.localizedMessage,
                    checkedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun effectiveBitrateBps(): Long {
        val combinedTrackBitrate = videoFormatBitrateBps + audioFormatBitrateBps
        return when {
            combinedTrackBitrate > 0L -> combinedTrackBitrate
            downstreamBitrateBps > 0L -> downstreamBitrateBps
            measuredBitrateEstimateBps > 0L -> measuredBitrateEstimateBps
            else -> 0L
        }
    }

    private fun effectiveFrameRate(): Float {
        val formatFps = exoPlayer.videoFormat?.frameRate ?: 0f
        return when {
            formatFps > 0f -> formatFps
            videoFrameRateFps > 0f -> videoFrameRateFps
            else -> 0f
        }
    }

    private fun formatBitrateLabel(bps: Long): String {
        return when {
            bps <= 0L -> "Measuring…"
            bps >= 1_000_000L -> String.format(Locale.US, "%.2f Mbps", bps / 1_000_000.0)
            else -> "${bps / 1_000L} kbps"
        }
    }

    private fun formatFrameRateLabel(fps: Float): String {
        return when {
            fps <= 0f -> "Auto"
            (fps % 1f) < 0.05f -> "${fps.toInt()} fps"
            else -> String.format(Locale.US, "%.2f fps", fps)
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

    private fun buildChannelEpgMap(
        channels: List<ChannelEntity>,
        epgChannels: List<EpgChannelEntity>,
        epgPrograms: List<EpgProgramEntity>,
        nowMillis: Long
    ): Map<String, ChannelEpgStatus> {
        if (epgPrograms.isEmpty() || channels.isEmpty()) return emptyMap()

        val programsByIdLower = HashMap<String, MutableList<EpgProgramEntity>>()
        for (prog in epgPrograms) {
            val key = prog.channelId.trim().lowercase()
            programsByIdLower.getOrPut(key) { ArrayList() }.add(prog)
        }

        val epgIdByNormName = HashMap<String, String>()
        for (epgCh in epgChannels) {
            val normDisplay = normalizeEpgKey(epgCh.displayName)
            if (normDisplay.isNotEmpty()) {
                epgIdByNormName.putIfAbsent(normDisplay, epgCh.epgId.trim().lowercase())
            }
            val normId = normalizeEpgKey(epgCh.epgId)
            if (normId.isNotEmpty()) {
                epgIdByNormName.putIfAbsent(normId, epgCh.epgId.trim().lowercase())
            }
        }
        for (idKey in programsByIdLower.keys) {
            val normId = normalizeEpgKey(idKey)
            if (normId.isNotEmpty()) {
                epgIdByNormName.putIfAbsent(normId, idKey)
            }
        }

        val result = HashMap<String, ChannelEpgStatus>(channels.size)
        for (channel in channels) {
            val matchedKey = when {
                channel.tvgId.isNotBlank() && programsByIdLower.containsKey(channel.tvgId.trim().lowercase()) ->
                    channel.tvgId.trim().lowercase()
                else -> epgIdByNormName[normalizeEpgKey(channel.name)]
            } ?: continue

            val progs = programsByIdLower[matchedKey] ?: continue
            val currentIdx = progs.indexOfFirst { nowMillis in it.startMillis until it.endMillis }
            val nowProg = if (currentIdx >= 0) progs[currentIdx] else null
            val upcomingList = progs.filter { it.endMillis > nowMillis }.take(24)
            val nextProg = if (nowProg != null) {
                progs.firstOrNull { it.startMillis >= nowProg.endMillis }
                    ?: progs.firstOrNull { it.startMillis > nowProg.startMillis }
            } else {
                upcomingList.firstOrNull()
            }

            if (nowProg != null || nextProg != null || upcomingList.isNotEmpty()) {
                result[channel.url] = ChannelEpgStatus(
                    now = nowProg,
                    next = nextProg,
                    upcoming = upcomingList
                )
            }
        }
        return result
    }

    private fun normalizeEpgKey(raw: String): String {
        return raw.lowercase()
            .replace(Regex("""\b(hd|fhd|sd|uhd|4k|live|tv)\b"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
    }

    private fun resolutionString(): String {
        return if (videoWidth > 0 && videoHeight > 0) "${videoWidth}×${videoHeight}" else "Audio / Pending"
    }

    private fun emitMessage(message: String) {
        _messages.tryEmit(message)
    }

    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
        exoPlayer.removeAnalyticsListener(analyticsListener)
        exoPlayer.release()
        super.onCleared()
    }

    private data class LibrarySnapshot(
        val channels: List<ChannelEntity>,
        val playlists: List<PlaylistEntity>,
        val profiles: List<FamilyProfileEntity>,
        val activeProfile: String,
        val favoriteChannels: List<ChannelEntity>
    )

    private data class ActivitySnapshot(
        val recentUrls: List<String>,
        val stats: List<WatchStatEntity>,
        val matches: List<MatchEntryEntity>,
        val hasAdminPin: Boolean,
        val lanSyncUrl: String
    )

    private data class EpgSnapshot(
        val channels: List<EpgChannelEntity>,
        val programs: List<EpgProgramEntity>,
        val url: String,
        val lastUpdatedAt: Long
    )
}
