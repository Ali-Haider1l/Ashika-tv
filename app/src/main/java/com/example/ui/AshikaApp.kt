package com.example.ui

import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.AshikaDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.network.PlaylistFetcher
import com.example.data.remote.CloudSyncRepository
import com.example.data.repository.AshikaRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutWithCredentialCleanup
import com.example.ui.components.ChannelEditorDialog
import com.example.ui.components.ChannelPreviewBottomSheet
import com.example.ui.components.DockedLivePlayer
import com.example.ui.components.FloatingInAppPipOverlay
import com.example.ui.components.FullscreenPlayerOverlay
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.ProfileSwitcherDialog
import com.example.ui.components.SystemPipVideoScreen
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.EpgScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.MoreMenuScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.PipHelper
import com.example.ui.util.PipHelper.findActivity
import com.example.ui.util.ToffeeStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ROUTE_CHANNELS = "channels"
const val ROUTE_EPG = "epg"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_PLAYLISTS = "playlists"
const val ROUTE_MORE = "more"

private data class NavDestination(
    val route: String,
    val icon: ImageVector
)

private val TopDestinations = listOf(
    NavDestination(ROUTE_CHANNELS, Icons.Rounded.Home),
    NavDestination(ROUTE_EPG, Icons.Rounded.CalendarMonth),
    NavDestination(ROUTE_FAVORITES, Icons.Rounded.Star),
    NavDestination(ROUTE_PLAYLISTS, Icons.Rounded.PlaylistPlay),
    NavDestination(ROUTE_MORE, Icons.Rounded.Menu)
)

private fun destinationLabel(route: String, appLanguage: AppLanguage): String = when (route) {
    ROUTE_CHANNELS -> ToffeeStrings.navHome(appLanguage)
    ROUTE_EPG -> ToffeeStrings.navGuide(appLanguage)
    ROUTE_FAVORITES -> ToffeeStrings.navFavorites(appLanguage)
    ROUTE_PLAYLISTS -> ToffeeStrings.navPlaylists(appLanguage)
    ROUTE_MORE -> ToffeeStrings.navMenu(appLanguage)
    else -> route
}

@Composable
fun AshikaAuthGate(
    isInSystemPipMode: Boolean = false
) {
    val appContext = LocalContext.current.applicationContext
    val deviceId = remember {
        android.provider.Settings.Secure.getString(
            appContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )?.takeIf { it.isNotBlank() } ?: "ashika-tv-device"
    }
    key(deviceId) {
        val viewModel: AshikaViewModel = viewModel(
            key = deviceId,
            factory = viewModelFactory {
                initializer {
                    val db = FirebaseFirestore.getInstance(
                        appContext.getString(R.string.firestore_database_id)
                    )
                    val localDb = AshikaDatabase.getInstance(appContext)
                    val prefs = PreferencesManager(appContext)
                    val repository = AshikaRepository(
                        dao = localDb.ashikaDao(),
                        prefs = prefs,
                        fetcher = PlaylistFetcher()
                    )
                    AshikaViewModel(
                        appContext = appContext,
                        repository = repository,
                        cloudSyncRepository = CloudSyncRepository(db),
                        currentUserId = deviceId,
                        userDisplayName = "Guest",
                        userEmail = ""
                    )
                }
            }
        )
        AshikaApp(
            isInSystemPipMode = isInSystemPipMode,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshikaApp(
    isInSystemPipMode: Boolean = false,
    viewModel: AshikaViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CHANNELS
    val snackbarHostState = remember { SnackbarHostState() }

    val onSignOut: () -> Unit = {}

    var showSplash by remember { mutableStateOf(true) }
    var showChannelEditor by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var inspectingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var videoSourceRectHint by remember { mutableStateOf<Rect?>(null) }

    val pipAspectRatio = remember(uiState.videoWidth, uiState.videoHeight) {
        viewModel.getSafePipAspectRatio()
    }
    val shouldAutoEnterPip = uiState.currentChannel != null && uiState.isPlaying

    LaunchedEffect(activity, shouldAutoEnterPip, pipAspectRatio, videoSourceRectHint) {
        if (activity != null) {
            PipHelper.updateSystemPipParams(
                activity = activity,
                shouldAutoEnter = shouldAutoEnterPip,
                aspectRatio = pipAspectRatio,
                sourceRectHint = videoSourceRectHint
            )
        }
    }

    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S && activity != null) {
        DisposableEffect(activity, shouldAutoEnterPip, pipAspectRatio, videoSourceRectHint) {
            val listener = Runnable {
                if (shouldAutoEnterPip) {
                    PipHelper.enterSystemPip(activity, pipAspectRatio, videoSourceRectHint)
                }
            }
            activity.addOnUserLeaveHintListener(listener)
            onDispose {
                activity.removeOnUserLeaveHintListener(listener)
            }
        }
    }

    val triggerSystemPip: () -> Unit = {
        val currentAct = activity
        val entered = if (currentAct != null) {
            PipHelper.enterSystemPip(currentAct, pipAspectRatio, videoSourceRectHint)
        } else {
            false
        }
        if (!entered) {
            scope.launch {
                snackbarHostState.showSnackbar(ToffeeStrings.pipUnavailableMsg(uiState.appLanguage))
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isInSystemPipMode && uiState.currentChannel != null) {
        SystemPipVideoScreen(
            exoPlayer = viewModel.exoPlayer,
            isBuffering = uiState.isBuffering,
            errorMessage = uiState.streamHealth.errorMessage
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp
            val useSideBySidePlayer = maxWidth >= 840.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ToffeeMagenta, ToffeeViolet)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LiveTv,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "ASHIKA TV",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LiveCrimson
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.live_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ToffeeMagenta.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showLanguageDialog = true }
                                    .testTag("top_language_toggle")
                            ) {
                                Text(
                                    text = uiState.appLanguage.badgeText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ToffeeMagenta,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (currentRoute != ROUTE_CHANNELS) {
                                        navigateTo(ROUTE_CHANNELS)
                                        viewModel.setSearchExpanded(true)
                                    } else {
                                        viewModel.toggleSearchExpanded()
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("top_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = stringResource(R.string.cd_search),
                                    tint = if (uiState.isSearchExpanded) ToffeeMagenta
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = { showProfileDialog = true },
                                label = { Text(uiState.activeProfile) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Person,
                                        stringResource(R.string.cd_profile_switcher),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("top_profile_chip")
                            )
                        }
                    )
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            TopDestinations.forEach { dest ->
                                val label = destinationLabel(dest.route, uiState.appLanguage)
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        DestinationIconWithBadge(
                                            dest = dest,
                                            label = label,
                                            favoritesCount = uiState.favoriteChannels.size
                                        )
                                    },
                                    label = { Text(label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ToffeeMagenta,
                                        selectedTextColor = ToffeeMagenta,
                                        indicatorColor = ToffeeMagenta.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("nav_bar_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            TopDestinations.forEach { dest ->
                                val label = destinationLabel(dest.route, uiState.appLanguage)
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        DestinationIconWithBadge(
                                            dest = dest,
                                            label = label,
                                            favoritesCount = uiState.favoriteChannels.size
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}")
                                )
                            }
                        }
                    }

                    val activePlayerChannel = uiState.currentChannel
                    if (useSideBySidePlayer && activePlayerChannel != null && !uiState.isFullscreen && !uiState.isInAppPip) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DockedLivePlayer(
                                exoPlayer = viewModel.exoPlayer,
                                currentChannel = activePlayerChannel,
                                channels = uiState.filteredChannels.ifEmpty { uiState.allChannels },
                                epgStatus = uiState.epgMap[activePlayerChannel.url],
                                nowMillis = uiState.nowMillis,
                                appLanguage = uiState.appLanguage,
                                isPlaying = uiState.isPlaying,
                                isBuffering = uiState.isBuffering,
                                isFavorite = uiState.favoriteUrls.contains(activePlayerChannel.url),
                                showDiagnostics = uiState.showDiagnosticsOverlay,
                                streamHealth = uiState.streamHealth,
                                showChannelDrawer = uiState.showPlayerChannelDrawer,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onToggleFavorite = { viewModel.toggleFavorite(activePlayerChannel) },
                                onPrevious = { viewModel.skipChannel(-1) },
                                onNext = { viewModel.skipChannel(1) },
                                onToggleChannelDrawer = viewModel::togglePlayerChannelDrawer,
                                onToggleDiagnostics = viewModel::toggleDiagnosticsOverlay,
                                onEnterFullscreen = { viewModel.setFullscreen(true) },
                                onMinimizePip = { viewModel.setInAppPip(true) },
                                onEnterSystemPip = triggerSystemPip,
                                onStop = viewModel::stopPlayback,
                                onSelectChannel = viewModel::playChannel,
                                onVideoBoundsChanged = { videoSourceRectHint = it },
                                modifier = Modifier.weight(0.45f)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .fillMaxHeight()
                            ) {
                                AshikaNavHost(
                                    navController = navController,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenFavorites = { navigateTo(ROUTE_FAVORITES) },
                                    onOpenChannels = { navigateTo(ROUTE_CHANNELS) },
                                    onOpenEpg = { navigateTo(ROUTE_EPG) },
                                    onOpenPlaylists = { navigateTo(ROUTE_PLAYLISTS) },
                                    onOpenLanguagePicker = { showLanguageDialog = true },
                                    onOpenAddChannel = {
                                        editingChannel = null
                                        showChannelEditor = true
                                    },
                                    onEditChannel = { ch ->
                                        editingChannel = ch
                                        showChannelEditor = true
                                    },
                                    onInspectChannel = { ch -> inspectingChannel = ch },
                                    onImportLocalM3u = { uri ->
                                        viewModel.importLocalM3u(context.contentResolver, uri)
                                    },
                                    onImportLocalEpg = { uri ->
                                        viewModel.importLocalEpg(context.contentResolver, uri)
                                    },
                                    onSyncCloud = viewModel::syncAllToCloud,
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (activePlayerChannel != null && !uiState.isFullscreen && !uiState.isInAppPip) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DockedLivePlayer(
                                        exoPlayer = viewModel.exoPlayer,
                                        currentChannel = activePlayerChannel,
                                        channels = uiState.filteredChannels.ifEmpty { uiState.allChannels },
                                        epgStatus = uiState.epgMap[activePlayerChannel.url],
                                        nowMillis = uiState.nowMillis,
                                        appLanguage = uiState.appLanguage,
                                        isPlaying = uiState.isPlaying,
                                        isBuffering = uiState.isBuffering,
                                        isFavorite = uiState.favoriteUrls.contains(activePlayerChannel.url),
                                        showDiagnostics = uiState.showDiagnosticsOverlay,
                                        streamHealth = uiState.streamHealth,
                                        showChannelDrawer = uiState.showPlayerChannelDrawer,
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onToggleFavorite = { viewModel.toggleFavorite(activePlayerChannel) },
                                        onPrevious = { viewModel.skipChannel(-1) },
                                        onNext = { viewModel.skipChannel(1) },
                                        onToggleChannelDrawer = viewModel::togglePlayerChannelDrawer,
                                        onToggleDiagnostics = viewModel::toggleDiagnosticsOverlay,
                                        onEnterFullscreen = { viewModel.setFullscreen(true) },
                                        onMinimizePip = { viewModel.setInAppPip(true) },
                                        onEnterSystemPip = triggerSystemPip,
                                        onStop = viewModel::stopPlayback,
                                        onSelectChannel = viewModel::playChannel,
                                        onVideoBoundsChanged = { videoSourceRectHint = it },
                                        modifier = Modifier.widthIn(max = 600.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                AshikaNavHost(
                                    navController = navController,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenFavorites = { navigateTo(ROUTE_FAVORITES) },
                                    onOpenChannels = { navigateTo(ROUTE_CHANNELS) },
                                    onOpenEpg = { navigateTo(ROUTE_EPG) },
                                    onOpenPlaylists = { navigateTo(ROUTE_PLAYLISTS) },
                                    onOpenLanguagePicker = { showLanguageDialog = true },
                                    onOpenAddChannel = {
                                        editingChannel = null
                                        showChannelEditor = true
                                    },
                                    onEditChannel = { ch ->
                                        editingChannel = ch
                                        showChannelEditor = true
                                    },
                                    onInspectChannel = { ch -> inspectingChannel = ch },
                                    onImportLocalM3u = { uri ->
                                        viewModel.importLocalM3u(context.contentResolver, uri)
                                    },
                                    onImportLocalEpg = { uri ->
                                        viewModel.importLocalEpg(context.contentResolver, uri)
                                    },
                                    onSyncCloud = viewModel::syncAllToCloud,
                                    onSignOut = onSignOut,
                                    modifier = Modifier.widthIn(max = 840.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val pipChannel = uiState.currentChannel
        if (uiState.isInAppPip && pipChannel != null && !uiState.isFullscreen) {
            FloatingInAppPipOverlay(
                exoPlayer = viewModel.exoPlayer,
                currentChannel = pipChannel,
                isPlaying = uiState.isPlaying,
                isBuffering = uiState.isBuffering,
                onTogglePlayPause = viewModel::togglePlayPause,
                onExpand = { viewModel.setInAppPip(false) },
                onEnterSystemPip = triggerSystemPip,
                onStop = viewModel::stopPlayback,
                onVideoBoundsChanged = { videoSourceRectHint = it }
            )
        }

        val currentFullscreenChannel = uiState.currentChannel
        if (uiState.isFullscreen && currentFullscreenChannel != null) {
            FullscreenPlayerOverlay(
                exoPlayer = viewModel.exoPlayer,
                currentChannel = currentFullscreenChannel,
                channels = uiState.filteredChannels.ifEmpty { uiState.allChannels },
                epgStatus = uiState.epgMap[currentFullscreenChannel.url],
                appLanguage = uiState.appLanguage,
                streamHealth = uiState.streamHealth,
                showDiagnostics = uiState.showDiagnosticsOverlay,
                isPlaying = uiState.isPlaying,
                isBuffering = uiState.isBuffering,
                onTogglePlayPause = viewModel::togglePlayPause,
                onPrevious = { viewModel.skipChannel(-1) },
                onNext = { viewModel.skipChannel(1) },
                onSelectChannel = viewModel::playChannel,
                onToggleDiagnostics = viewModel::toggleDiagnosticsOverlay,
                onEnterSystemPip = triggerSystemPip,
                onExitFullscreen = { viewModel.setFullscreen(false) },
                onStop = viewModel::stopPlayback,
                onVideoBoundsChanged = { videoSourceRectHint = it }
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SplashOverlay(onSkip = { showSplash = false })
        }
    }

    if (showChannelEditor) {
        ChannelEditorDialog(
            editingChannel = editingChannel,
            onDismiss = { showChannelEditor = false },
            onSave = viewModel::saveChannel
        )
    }

    inspectingChannel?.let { channel ->
        ChannelPreviewBottomSheet(
            channel = channel,
            isFavorite = uiState.favoriteUrls.contains(channel.url),
            epgStatus = uiState.epgMap[channel.url],
            nowMillis = uiState.nowMillis,
            onDismiss = { inspectingChannel = null },
            onPlay = { viewModel.playChannel(channel) },
            onToggleFavorite = { viewModel.toggleFavorite(channel) },
            onEdit = {
                editingChannel = channel
                showChannelEditor = true
            },
            onDelete = { viewModel.deleteChannel(channel) }
        )
    }

    if (showProfileDialog) {
        ProfileSwitcherDialog(
            profiles = uiState.profiles,
            activeProfile = uiState.activeProfile,
            onSwitchProfile = viewModel::switchProfile,
            onCreateProfile = viewModel::createProfile,
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = uiState.appLanguage,
            onSelectLanguage = viewModel::setAppLanguage,
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun DestinationIconWithBadge(
    dest: NavDestination,
    label: String,
    favoritesCount: Int
) {
    if (dest.route == ROUTE_FAVORITES && favoritesCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = ToffeeMagenta, contentColor = Color.White) {
                    Text(if (favoritesCount > 99) "99+" else favoritesCount.toString())
                }
            }
        ) {
            Icon(dest.icon, label)
        }
    } else {
        Icon(dest.icon, label)
    }
}

@Composable
private fun AshikaNavHost(
    navController: androidx.navigation.NavHostController,
    uiState: AshikaUiState,
    viewModel: AshikaViewModel,
    onOpenFavorites: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenEpg: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenAddChannel: () -> Unit,
    onEditChannel: (ChannelEntity) -> Unit,
    onInspectChannel: (ChannelEntity) -> Unit,
    onImportLocalM3u: (android.net.Uri) -> Unit,
    onImportLocalEpg: (android.net.Uri) -> Unit,
    onSyncCloud: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_CHANNELS,
        modifier = modifier
    ) {
        composable(ROUTE_CHANNELS) {
            ChannelsScreen(
                uiState = uiState,
                onSearchChange = viewModel::updateSearchQuery,
                onCloseSearch = { viewModel.setSearchExpanded(false) },
                onSelectCategory = viewModel::selectCategory,
                onSelectTab = viewModel::selectTab,
                onPlayChannel = viewModel::playChannel,
                onToggleFavorite = viewModel::toggleFavorite,
                onInspectChannel = onInspectChannel,
                onOpenAddPlaylist = onOpenPlaylists,
                onOpenAddChannel = onOpenAddChannel,
                onOpenFavorites = onOpenFavorites,
                onOpenEpg = onOpenEpg
            )
        }
        composable(ROUTE_EPG) {
            EpgScreen(
                uiState = uiState,
                onLoadEpgUrl = viewModel::loadEpgUrl,
                onImportLocalEpg = onImportLocalEpg,
                onRefreshEpg = viewModel::refreshEpg,
                onClearEpg = viewModel::clearEpg,
                onPlayChannel = viewModel::playChannel
            )
        }
        composable(ROUTE_FAVORITES) {
            FavoritesScreen(
                uiState = uiState,
                onPlayChannel = viewModel::playChannel,
                onToggleFavorite = viewModel::toggleFavorite,
                onInspectChannel = onInspectChannel,
                onBrowseChannels = onOpenChannels
            )
        }
        composable(ROUTE_PLAYLISTS) {
            PlaylistsScreen(
                uiState = uiState,
                onSyncToffeeAndBd = viewModel::syncToffeeAndAllChannels,
                onLoadPlaylistUrl = viewModel::loadPlaylistUrl,
                onImportLocalFile = onImportLocalM3u,
                onLoadEpgUrl = viewModel::loadEpgUrl,
                onImportLocalEpg = onImportLocalEpg,
                onRemovePlaylist = viewModel::removePlaylist,
                onSyncLan = viewModel::syncFromLan
            )
        }
        composable(ROUTE_MORE) {
            MoreMenuScreen(
                uiState = uiState,
                onOpenLanguagePicker = onOpenLanguagePicker,
                onOpenEpg = onOpenEpg,
                onOpenAddChannel = onOpenAddChannel,
                onEditChannel = onEditChannel,
                onDeleteChannel = viewModel::deleteChannel,
                onRunFinder = viewModel::runChannelFinder,
                onPlayChannel = viewModel::playChannel,
                onRefreshHealth = viewModel::refreshStreamHealth,
                onClearStats = viewModel::clearWatchStats,
                onAddMatch = viewModel::addMatch,
                onDeleteMatch = viewModel::deleteMatch,
                onSwitchProfile = viewModel::switchProfile,
                onCreateProfile = viewModel::createProfile,
                onDeleteProfile = viewModel::deleteProfile,
                onGeneratePairCode = viewModel::generatePairingCode,
                onSubmitAdminPin = viewModel::submitAdminPin,
                onLockAdmin = viewModel::lockAdmin,
                onRefreshBulkPlaylists = viewModel::saveAndRefreshAdminPlaylists,
                onSyncCloud = onSyncCloud,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun SplashOverlay(onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        ToffeeViolet.copy(alpha = 0.25f),
                        ToffeeMagenta.copy(alpha = 0.16f)
                    )
                )
            )
            .clickable(onClick = onSkip),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(ToffeeMagenta, ToffeeViolet))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LiveTv,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = "ASHIKA TV",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "● LIVE STREAMING",
                style = MaterialTheme.typography.labelMedium,
                color = ToffeeMagenta
            )
        }
    }
}
