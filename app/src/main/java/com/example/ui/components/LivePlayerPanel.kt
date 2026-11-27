package com.example.ui.components

import android.graphics.Rect
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelEpgStatus
import com.example.data.model.StreamHealthInfo
import com.example.ui.theme.BufferingAmber
import com.example.ui.theme.HealthyEmerald
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.StarGold
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.PipHelper.findActivity
import com.example.ui.util.TimeFormat
import com.example.ui.util.ToffeeStrings
import kotlin.math.roundToInt

@Composable
fun DockedLivePlayer(
    exoPlayer: ExoPlayer,
    currentChannel: ChannelEntity,
    channels: List<ChannelEntity>,
    epgStatus: ChannelEpgStatus? = null,
    nowMillis: Long = System.currentTimeMillis(),
    appLanguage: AppLanguage = AppLanguage.BANGLA,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isFavorite: Boolean,
    showDiagnostics: Boolean,
    streamHealth: StreamHealthInfo,
    showChannelDrawer: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleChannelDrawer: () -> Unit,
    onToggleDiagnostics: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onMinimizePip: () -> Unit,
    onEnterSystemPip: () -> Unit,
    onStop: () -> Unit,
    onSelectChannel: (ChannelEntity) -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp
    ) {
        Column {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val showSidePanel = showDiagnostics && maxWidth >= 500.dp
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                            .onGloballyPositioned { coords ->
                                onVideoBoundsChanged(coords.boundsInWindow().toAndroidRectF().toRect())
                            }
                    ) {
                        ExoPlayerSurface(
                            exoPlayer = exoPlayer,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top status overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                LiveStatusPill(
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    hasError = streamHealth.errorMessage != null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentChannel.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = currentChannel.groupTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }

                        streamHealth.errorMessage?.let { err ->
                            Surface(
                                color = ObsidianNight.copy(alpha = 0.85f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Compact overlay HUD when width < 500.dp
                        if (showDiagnostics && !showSidePanel) {
                            StreamDiagnosticsPanel(
                                streamHealth = streamHealth,
                                appLanguage = appLanguage,
                                isCompactOverlay = true,
                                onClose = onToggleDiagnostics,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .width(196.dp)
                            )
                        }
                    }

                    // Adaptive Side Panel when width >= 500.dp
                    if (showSidePanel) {
                        StreamDiagnosticsPanel(
                            streamHealth = streamHealth,
                            appLanguage = appLanguage,
                            isCompactOverlay = false,
                            onClose = onToggleDiagnostics,
                            modifier = Modifier
                                .width(192.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }

            epgStatus?.now?.let { nowProgram ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ON NOW: ${nowProgram.title}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = TimeFormat.formatRange(nowProgram.startMillis, nowProgram.endMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { epgStatus.progressFraction(nowMillis) },
                        color = ToffeeMagenta,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    epgStatus.next?.let { nextProgram ->
                        Text(
                            text = "Up Next: ${nextProgram.title} (${TimeFormat.formatTime(nextProgram.startMillis)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Transport bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous_channel)
                        )
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.cd_next_channel)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_star_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = stringResource(R.string.cd_favorite_toggle),
                            tint = if (isFavorite) StarGold else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onToggleDiagnostics,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorHeart,
                            contentDescription = ToffeeStrings.cdDiagnosticsToggle(appLanguage),
                            tint = if (showDiagnostics) HealthyEmerald else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onToggleChannelDrawer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_channels_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = stringResource(R.string.cd_channel_drawer),
                            tint = if (showChannelDrawer) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onMinimizePip,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_mini_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPicture,
                            contentDescription = stringResource(R.string.cd_mini_pip)
                        )
                    }
                    IconButton(
                        onClick = onEnterSystemPip,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_system_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = stringResource(R.string.cd_system_pip)
                        )
                    }
                    IconButton(
                        onClick = onEnterFullscreen,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = stringResource(R.string.cd_fullscreen)
                        )
                    }
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = stringResource(R.string.cd_stop_player),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showChannelDrawer && channels.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Quick Switch (${channels.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(channels, key = { _, ch -> ch.url }) { index, channel ->
                            val selected = channel.url == currentChannel.url
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { onSelectChannel(channel) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. ${channel.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = channel.groupTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingInAppPipOverlay(
    exoPlayer: ExoPlayer,
    currentChannel: ChannelEntity,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onExpand: () -> Unit,
    onEnterSystemPip: () -> Unit,
    onStop: () -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var showOverlayControls by remember { mutableStateOf(true) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 72.dp)
    ) {
        val pipWidthDp = 216.dp
        val pipHeightDp = 122.dp
        val maxDragX = with(density) { (maxWidth - pipWidthDp).coerceAtLeast(0.dp).toPx() }
        val maxDragY = with(density) { (maxHeight - pipHeightDp).coerceAtLeast(0.dp).toPx() }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset {
                    IntOffset(
                        x = offsetX.coerceIn(-maxDragX, 0f).roundToInt(),
                        y = offsetY.coerceIn(-maxDragY, 0f).roundToInt()
                    )
                }
                .width(pipWidthDp)
                .height(pipHeightDp)
                .shadow(14.dp, RoundedCornerShape(14.dp))
                .pointerInput(maxDragX, maxDragY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-maxDragX, 0f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-maxDragY, 0f)
                    }
                }
                .onGloballyPositioned { coords ->
                    onVideoBoundsChanged(coords.boundsInWindow().toAndroidRectF().toRect())
                }
                .testTag("floating_in_app_pip"),
            shape = RoundedCornerShape(14.dp),
            color = Color.Black,
            border = BorderStroke(1.5.dp, ToffeeMagenta.copy(alpha = 0.85f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showOverlayControls = !showOverlayControls }
            ) {
                ExoPlayerSurface(
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center),
                        color = ToffeeMagenta,
                        strokeWidth = 2.5.dp
                    )
                }

                // Channel label pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) HealthyEmerald else LiveCrimson)
                    )
                    Text(
                        text = currentChannel.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(
                    visible = showOverlayControls,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianNight.copy(alpha = 0.84f))
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pip_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                tint = ToffeeMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onExpand,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pip_expand_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInFull,
                                contentDescription = stringResource(R.string.cd_expand_pip),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onEnterSystemPip,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pip_system_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = stringResource(R.string.cd_system_pip),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pip_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_stop_player),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemPipVideoScreen(
    exoPlayer: ExoPlayer,
    isBuffering: Boolean,
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ExoPlayerSurface(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center),
                color = ToffeeMagenta,
                strokeWidth = 3.dp
            )
        }
        errorMessage?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(ObsidianNight.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun StreamDiagnosticsPanel(
    streamHealth: StreamHealthInfo,
    appLanguage: AppLanguage,
    isCompactOverlay: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val healthDotColor = when {
        streamHealth.errorMessage != null -> MaterialTheme.colorScheme.error
        !streamHealth.isPlaying -> BufferingAmber
        else -> HealthyEmerald
    }

    Surface(
        modifier = modifier.testTag("stream_diagnostics_panel"),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianNight.copy(alpha = if (isCompactOverlay) 0.86f else 0.96f),
        border = BorderStroke(
            width = 1.dp,
            color = ToffeeViolet.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(healthDotColor)
                    )
                    Text(
                        text = ToffeeStrings.diagnosticsTitle(appLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onClose)
                        .testTag("diagnostics_close_button")
                )
            }

            DiagnosticStatRow(
                label = ToffeeStrings.diagResolutionLabel(appLanguage),
                value = streamHealth.videoResolution
            )
            DiagnosticStatRow(
                label = ToffeeStrings.diagBitrateLabel(appLanguage),
                value = streamHealth.bitrateLabel,
                accentColor = ToffeeMagenta
            )
            DiagnosticStatRow(
                label = ToffeeStrings.diagFrameRateLabel(appLanguage),
                value = streamHealth.frameRateLabel
            )
            DiagnosticStatRow(
                label = ToffeeStrings.diagBufferLabel(appLanguage),
                value = "${streamHealth.bufferedPercentage}%",
                accentColor = if (streamHealth.bufferedPercentage >= 20) HealthyEmerald else BufferingAmber
            )
            DiagnosticStatRow(
                label = ToffeeStrings.diagCodecLabel(appLanguage),
                value = streamHealth.codecLabel
            )
            if (streamHealth.droppedFrames > 0) {
                DiagnosticStatRow(
                    label = ToffeeStrings.diagDroppedLabel(appLanguage),
                    value = streamHealth.droppedFrames.toString(),
                    accentColor = BufferingAmber
                )
            }
        }
    }
}

@Composable
private fun DiagnosticStatRow(
    label: String,
    value: String,
    accentColor: Color = HealthyEmerald
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun FullscreenPlayerOverlay(
    exoPlayer: ExoPlayer,
    currentChannel: ChannelEntity,
    channels: List<ChannelEntity>,
    epgStatus: ChannelEpgStatus? = null,
    appLanguage: AppLanguage = AppLanguage.BANGLA,
    streamHealth: StreamHealthInfo = StreamHealthInfo(),
    showDiagnostics: Boolean = false,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectChannel: (ChannelEntity) -> Unit,
    onToggleDiagnostics: () -> Unit = {},
    onEnterSystemPip: () -> Unit,
    onExitFullscreen: () -> Unit,
    onStop: () -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit = {}
) {
    BackHandler { onExitFullscreen() }

    // Use immersive system UI while the player is fullscreen, restoring it on exit.
    val playerView = LocalView.current
    DisposableEffect(playerView) {
        val window = playerView.context.findActivity()?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, playerView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var showChannelList by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coords ->
                onVideoBoundsChanged(coords.boundsInWindow().toAndroidRectF().toRect())
            }
            .clickable { showControls = !showControls }
    ) {
        ExoPlayerSurface(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showDiagnostics) {
            StreamDiagnosticsPanel(
                streamHealth = streamHealth,
                appLanguage = appLanguage,
                isCompactOverlay = true,
                onClose = onToggleDiagnostics,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .systemBarsPadding()
                    .padding(start = 16.dp)
                    .width(210.dp)
            )
        }

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .systemBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        LiveStatusPill(isPlaying = isPlaying, isBuffering = isBuffering, hasError = false)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentChannel.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = epgStatus?.now?.let {
                                "${it.title} • ${TimeFormat.formatRange(it.startMillis, it.endMillis)}"
                            } ?: currentChannel.groupTitle
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = onToggleDiagnostics,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fullscreen_diagnostics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonitorHeart,
                                contentDescription = ToffeeStrings.cdDiagnosticsToggle(appLanguage),
                                tint = if (showDiagnostics) HealthyEmerald else Color.White
                            )
                        }
                        IconButton(
                            onClick = onEnterSystemPip,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fullscreen_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = stringResource(R.string.cd_system_pip),
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { showChannelList = !showChannelList },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                contentDescription = stringResource(R.string.cd_channel_drawer),
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onExitFullscreen,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("fullscreen_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FullscreenExit,
                                contentDescription = stringResource(R.string.cd_fullscreen),
                                tint = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(32.dp))
                        .background(ObsidianNight.copy(alpha = 0.86f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.cd_previous_channel), tint = Color.White)
                    }
                    IconButton(onClick = onTogglePlayPause, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            stringResource(R.string.cd_play_pause),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.SkipNext, stringResource(R.string.cd_next_channel), tint = Color.White)
                    }
                    IconButton(onClick = onStop, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Stop, stringResource(R.string.cd_stop_player), tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (showChannelList) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(260.dp)
                            .fillMaxHeight(0.75f),
                        shape = MaterialTheme.shapes.large,
                        color = ObsidianNight.copy(alpha = 0.94f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Channels", style = MaterialTheme.typography.titleSmall)
                                IconButton(onClick = { showChannelList = false }) {
                                    Icon(Icons.Rounded.Close, stringResource(R.string.cd_close))
                                }
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(channels, key = { _, ch -> ch.url }) { idx, channel ->
                                    val active = channel.url == currentChannel.url
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (active) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectChannel(channel) }
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${channel.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerSurface(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                player = exoPlayer
            }
        },
        update = { view ->
            if (view.player !== exoPlayer) {
                view.player = exoPlayer
            }
        }
    )
}

@Composable
private fun LiveStatusPill(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasError: Boolean
) {
    val dotColor = when {
        hasError -> MaterialTheme.colorScheme.error
        isBuffering -> BufferingAmber
        isPlaying -> HealthyEmerald
        else -> LiveCrimson
    }
    val label = when {
        hasError -> "ERROR"
        isBuffering -> "BUFFER"
        isPlaying -> "LIVE"
        else -> "PAUSED"
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LiveCrimson.copy(alpha = if (hasError) 0.35f else 0.9f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
