package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.ChannelEntity
import com.example.data.model.StreamHealthInfo
import com.example.ui.theme.BufferingAmber
import com.example.ui.theme.HealthyEmerald
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ObsidianNight

@Composable
fun DockedLivePlayer(
    exoPlayer: ExoPlayer,
    currentChannel: ChannelEntity,
    channels: List<ChannelEntity>,
    isPlaying: Boolean,
    isBuffering: Boolean,
    streamHealth: StreamHealthInfo,
    showChannelDrawer: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleChannelDrawer: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onStop: () -> Unit,
    onSelectChannel: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
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
            }

            // Transport bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
fun FullscreenPlayerOverlay(
    exoPlayer: ExoPlayer,
    currentChannel: ChannelEntity,
    channels: List<ChannelEntity>,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectChannel: (ChannelEntity) -> Unit,
    onExitFullscreen: () -> Unit,
    onStop: () -> Unit
) {
    BackHandler { onExitFullscreen() }

    var showControls by remember { mutableStateOf(true) }
    var showChannelList by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                            Text(
                                text = currentChannel.groupTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row {
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
