package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PreferencesManager
import com.example.data.model.ChannelEntity
import com.example.ui.AshikaUiState

enum class AshikaTool(val title: String) {
    FINDER("Finder"),
    HEALTH("Health"),
    STATS("Stats"),
    MATCHES("Matches"),
    PROFILES("Profiles"),
    CONNECT("Connect")
}

@Composable
fun ToolsScreen(
    uiState: AshikaUiState,
    onRunFinder: (String) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onRefreshHealth: () -> Unit,
    onClearStats: () -> Unit,
    onAddMatch: (teams: String, time: String, score: String) -> Unit,
    onDeleteMatch: (Long) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onGeneratePairCode: () -> Unit,
    initialTool: AshikaTool = AshikaTool.FINDER,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTool by remember(initialTool) { mutableStateOf(initialTool) }

    if (onBack != null) {
        BackHandler { onBack() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Menu")
                    }
                }
                Text("Ashika Tools", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AshikaTool.entries) { tool ->
                    FilterChip(
                        selected = selectedTool == tool,
                        onClick = { selectedTool = tool },
                        label = { Text(tool.title) },
                        leadingIcon = {
                            val icon = when (tool) {
                                AshikaTool.FINDER -> Icons.Rounded.AutoAwesome
                                AshikaTool.HEALTH -> Icons.Rounded.MonitorHeart
                                AshikaTool.STATS -> Icons.Rounded.BarChart
                                AshikaTool.MATCHES -> Icons.Rounded.SportsSoccer
                                AshikaTool.PROFILES -> Icons.Rounded.Groups
                                AshikaTool.CONNECT -> Icons.Rounded.QrCode2
                            }
                            Icon(icon, null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("tool_chip_${tool.name.lowercase()}")
                    )
                }
            }
        }

        item {
            when (selectedTool) {
                AshikaTool.FINDER -> ChannelFinderCard(
                    query = uiState.finderQuery,
                    results = uiState.finderResults,
                    onRunFinder = onRunFinder,
                    onPlayChannel = onPlayChannel
                )

                AshikaTool.HEALTH -> StreamHealthCard(
                    uiState = uiState,
                    onRefreshHealth = onRefreshHealth
                )

                AshikaTool.STATS -> WatchStatsCard(
                    uiState = uiState,
                    onClearStats = onClearStats
                )

                AshikaTool.MATCHES -> MatchCenterCard(
                    uiState = uiState,
                    onAddMatch = onAddMatch,
                    onDeleteMatch = onDeleteMatch
                )

                AshikaTool.PROFILES -> FamilyProfilesCard(
                    uiState = uiState,
                    onSwitchProfile = onSwitchProfile,
                    onCreateProfile = onCreateProfile,
                    onDeleteProfile = onDeleteProfile
                )

                AshikaTool.CONNECT -> ConnectAndPairingCard(
                    uiState = uiState,
                    onGeneratePairCode = onGeneratePairCode
                )
            }
        }
    }
}

@Composable
private fun ChannelFinderCard(
    query: String,
    results: List<ChannelEntity>,
    onRunFinder: (String) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit
) {
    var input by remember(query) { mutableStateOf(query) }
    val suggestions = listOf("Sports", "News", "Music", "Favorites", "Recent")

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Smart Channel Finder", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Quickly locate channels in your local playlist by keyword, category, favorites, or recent history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Ask e.g. \"Find sports channels\"") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finder_query_input")
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { chip ->
                    FilledTonalButton(
                        onClick = {
                            input = chip
                            onRunFinder(chip)
                        }
                    ) {
                        Text(chip)
                    }
                }
            }
            Button(
                onClick = { onRunFinder(input) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finder_search_button")
            ) {
                Text("Find Channels")
            }

            if (query.isNotBlank() && results.isEmpty()) {
                Text(
                    text = "No channels matched \"$query\". Try loading a playlist or using another category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            results.forEach { channel ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayChannel(channel) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = channel.groupTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onPlayChannel(channel) }) {
                            Icon(Icons.Rounded.PlayArrow, "Play ${channel.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamHealthCard(
    uiState: AshikaUiState,
    onRefreshHealth: () -> Unit
) {
    val health = uiState.streamHealth
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stream Health Checker", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRefreshHealth, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.Refresh, "Refresh stream health")
                }
            }

            if (uiState.currentChannel == null) {
                Text(
                    text = "Play any channel first to inspect live ExoPlayer buffering, resolution, and stream status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HealthRow("Channel", health.channelName)
                HealthRow("Playback State", health.playbackStateLabel)
                HealthRow("Bitrate", health.bitrateLabel)
                HealthRow("Video Resolution", health.videoResolution)
                HealthRow("Frame Rate", health.frameRateLabel)
                HealthRow("Buffer", "${health.bufferedPercentage}%")
                HealthRow("Codec", health.codecLabel)
                HealthRow("Dropped Frames", health.droppedFrames.toString())
                health.errorMessage?.let { err ->
                    HealthRow("Last Error", err)
                }
            }
        }
    }
}

@Composable
private fun HealthRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WatchStatsCard(
    uiState: AshikaUiState,
    onClearStats: () -> Unit
) {
    val totalSeconds = uiState.watchStats.sumOf { it.totalSeconds }
    val maxChannelSeconds = uiState.watchStats.maxOfOrNull { it.totalSeconds }?.coerceAtLeast(1L) ?: 1L

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Watch Statistics", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Total watch time: ${formatDuration(totalSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (uiState.watchStats.isNotEmpty()) {
                    OutlinedButton(onClick = onClearStats) {
                        Text("Reset")
                    }
                }
            }

            if (uiState.watchStats.isEmpty()) {
                Text(
                    text = "No watch time recorded yet. Playback duration updates automatically while watching.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.watchStats.take(15).forEachIndexed { idx, stat ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${idx + 1}. ${stat.channelName}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatDuration(stat.totalSeconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (stat.totalSeconds.toFloat() / maxChannelSeconds.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCenterCard(
    uiState: AshikaUiState,
    onAddMatch: (teams: String, time: String, score: String) -> Unit,
    onDeleteMatch: (Long) -> Unit
) {
    var teams by remember { mutableStateOf("") }
    var matchTime by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Live Match Center", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Track manual match notes and scores while watching sports channels.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = teams,
                onValueChange = { teams = it },
                label = { Text("Teams (e.g. Bangladesh vs India)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = matchTime,
                    onValueChange = { matchTime = it },
                    label = { Text("Status / Time") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Score") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    onAddMatch(teams, matchTime, score)
                    teams = ""
                    matchTime = ""
                    score = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Match")
            }

            if (uiState.matches.isEmpty()) {
                Text(
                    text = "No matches added yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.matches.forEach { match ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(match.teams, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${match.score}  •  ${match.matchTime}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onDeleteMatch(match.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Delete, "Delete match", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyProfilesCard(
    uiState: AshikaUiState,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var newProfileName by remember { mutableStateOf("") }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Family Profiles", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Active: ${uiState.activeProfile} • ${uiState.favoriteUrls.size} favorites • ${uiState.recentUrls.size} recent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            uiState.profiles.forEach { profile ->
                val isSelected = profile.name == uiState.activeProfile
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchProfile(profile.name) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(profile.name, style = MaterialTheme.typography.titleSmall)
                        }
                        if (profile.name != PreferencesManager.DEFAULT_PROFILE) {
                            IconButton(
                                onClick = { onDeleteProfile(profile.name) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Delete, "Delete profile", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("New profile name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onCreateProfile(newProfileName)
                        newProfileName = ""
                    }
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun ConnectAndPairingCard(
    uiState: AshikaUiState,
    onGeneratePairCode: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Local QR Pairing Display",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Generates an offline 6-digit pairing code and QR bitmap locally on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onGeneratePairCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_pair_code_button")
                ) {
                    Text("Generate Pairing Code")
                }

                uiState.pairingCode?.let { code ->
                    Text(
                        text = "CODE: $code",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.pairingQrBitmap?.let { bmp ->
                    Surface(
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_qr_code),
                            modifier = Modifier
                                .size(180.dp)
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Toffee Live Portal", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Open Toffee Live in your device browser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://toffeelive.com"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Toffee Live")
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
