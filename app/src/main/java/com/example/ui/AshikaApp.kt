package com.example.ui

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
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.ChannelEntity
import com.example.ui.components.ChannelEditorDialog
import com.example.ui.components.ChannelPreviewBottomSheet
import com.example.ui.components.DockedLivePlayer
import com.example.ui.components.FullscreenPlayerOverlay
import com.example.ui.components.ProfileSwitcherDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.LiveCrimson
import kotlinx.coroutines.delay

const val ROUTE_CHANNELS = "channels"
const val ROUTE_PLAYLISTS = "playlists"
const val ROUTE_TOOLS = "tools"
const val ROUTE_ADMIN = "admin"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val TopDestinations = listOf(
    NavDestination(ROUTE_CHANNELS, R.string.nav_channels, Icons.Rounded.Tv),
    NavDestination(ROUTE_PLAYLISTS, R.string.nav_playlists, Icons.Rounded.PlaylistPlay),
    NavDestination(ROUTE_TOOLS, R.string.nav_tools, Icons.Rounded.AutoAwesome),
    NavDestination(ROUTE_ADMIN, R.string.nav_admin, Icons.Rounded.AdminPanelSettings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshikaApp(
    viewModel: AshikaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CHANNELS
    val snackbarHostState = remember { SnackbarHostState() }

    var showSplash by remember { mutableStateOf(true) }
    var showChannelEditor by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var inspectingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1100)
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
                                Text(
                                    text = "ASHIKA LIVE TV",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LiveCrimson
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
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
                                val label = stringResource(dest.labelRes)
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = { Icon(dest.icon, label) },
                                    label = { Text(label) },
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
                                val label = stringResource(dest.labelRes)
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = { Icon(dest.icon, label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("nav_rail_${dest.route}")
                                )
                            }
                        }
                    }

                    val activePlayerChannel = uiState.currentChannel
                    if (useSideBySidePlayer && activePlayerChannel != null && !uiState.isFullscreen) {
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
                                isPlaying = uiState.isPlaying,
                                isBuffering = uiState.isBuffering,
                                streamHealth = uiState.streamHealth,
                                showChannelDrawer = uiState.showPlayerChannelDrawer,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onPrevious = { viewModel.skipChannel(-1) },
                                onNext = { viewModel.skipChannel(1) },
                                onToggleChannelDrawer = viewModel::togglePlayerChannelDrawer,
                                onEnterFullscreen = { viewModel.setFullscreen(true) },
                                onStop = viewModel::stopPlayback,
                                onSelectChannel = viewModel::playChannel,
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
                                    onOpenPlaylists = { navigateTo(ROUTE_PLAYLISTS) },
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
                                    }
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (activePlayerChannel != null && !uiState.isFullscreen) {
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
                                        isPlaying = uiState.isPlaying,
                                        isBuffering = uiState.isBuffering,
                                        streamHealth = uiState.streamHealth,
                                        showChannelDrawer = uiState.showPlayerChannelDrawer,
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onPrevious = { viewModel.skipChannel(-1) },
                                        onNext = { viewModel.skipChannel(1) },
                                        onToggleChannelDrawer = viewModel::togglePlayerChannelDrawer,
                                        onEnterFullscreen = { viewModel.setFullscreen(true) },
                                        onStop = viewModel::stopPlayback,
                                        onSelectChannel = viewModel::playChannel,
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
                                    onOpenPlaylists = { navigateTo(ROUTE_PLAYLISTS) },
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
                                    modifier = Modifier.widthIn(max = 840.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val currentFullscreenChannel = uiState.currentChannel
        if (uiState.isFullscreen && currentFullscreenChannel != null) {
            FullscreenPlayerOverlay(
                exoPlayer = viewModel.exoPlayer,
                currentChannel = currentFullscreenChannel,
                channels = uiState.filteredChannels.ifEmpty { uiState.allChannels },
                isPlaying = uiState.isPlaying,
                isBuffering = uiState.isBuffering,
                onTogglePlayPause = viewModel::togglePlayPause,
                onPrevious = { viewModel.skipChannel(-1) },
                onNext = { viewModel.skipChannel(1) },
                onSelectChannel = viewModel::playChannel,
                onExitFullscreen = { viewModel.setFullscreen(false) },
                onStop = viewModel::stopPlayback
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
}

@Composable
private fun AshikaNavHost(
    navController: androidx.navigation.NavHostController,
    uiState: AshikaUiState,
    viewModel: AshikaViewModel,
    onOpenPlaylists: () -> Unit,
    onOpenAddChannel: () -> Unit,
    onEditChannel: (ChannelEntity) -> Unit,
    onInspectChannel: (ChannelEntity) -> Unit,
    onImportLocalM3u: (android.net.Uri) -> Unit,
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
                onSelectCategory = viewModel::selectCategory,
                onSelectTab = viewModel::selectTab,
                onPlayChannel = viewModel::playChannel,
                onToggleFavorite = viewModel::toggleFavorite,
                onInspectChannel = onInspectChannel,
                onOpenAddPlaylist = onOpenPlaylists,
                onOpenAddChannel = onOpenAddChannel
            )
        }
        composable(ROUTE_PLAYLISTS) {
            PlaylistsScreen(
                uiState = uiState,
                onLoadPlaylistUrl = viewModel::loadPlaylistUrl,
                onImportLocalFile = onImportLocalM3u,
                onRemovePlaylist = viewModel::removePlaylist,
                onSyncLan = viewModel::syncFromLan
            )
        }
        composable(ROUTE_TOOLS) {
            ToolsScreen(
                uiState = uiState,
                onRunFinder = viewModel::runChannelFinder,
                onPlayChannel = viewModel::playChannel,
                onRefreshHealth = viewModel::refreshStreamHealth,
                onClearStats = viewModel::clearWatchStats,
                onAddMatch = viewModel::addMatch,
                onDeleteMatch = viewModel::deleteMatch,
                onSwitchProfile = viewModel::switchProfile,
                onCreateProfile = viewModel::createProfile,
                onDeleteProfile = viewModel::deleteProfile,
                onGeneratePairCode = viewModel::generatePairingCode
            )
        }
        composable(ROUTE_ADMIN) {
            AdminScreen(
                uiState = uiState,
                onSubmitPin = viewModel::submitAdminPin,
                onLockAdmin = viewModel::lockAdmin,
                onOpenAddChannel = onOpenAddChannel,
                onEditChannel = onEditChannel,
                onDeleteChannel = viewModel::deleteChannel,
                onRefreshBulkPlaylists = viewModel::saveAndRefreshAdminPlaylists
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
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.surfaceContainerLow
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
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = "ASHIKA LIVE TV",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "● LOADING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
