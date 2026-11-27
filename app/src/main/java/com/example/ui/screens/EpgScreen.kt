package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelEpgStatus
import com.example.data.model.EpgProgramEntity
import com.example.ui.AshikaUiState
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.TimeFormat
import com.example.ui.util.ToffeeStrings

@Composable
fun EpgScreen(
    uiState: AshikaUiState,
    onLoadEpgUrl: (String) -> Unit,
    onImportLocalEpg: (Uri) -> Unit,
    onRefreshEpg: () -> Unit,
    onClearEpg: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = uiState.isBanglaMode
    val lang = uiState.appLanguage
    var epgUrlInput by remember(uiState.epgUrl) { mutableStateOf(uiState.epgUrl) }

    val matchedChannels = remember(uiState.allChannels, uiState.epgMap) {
        uiState.allChannels.filter { uiState.epgMap.containsKey(it.url) }
    }

    var selectedChannelUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(matchedChannels, uiState.currentChannel) {
        if (selectedChannelUrl == null || matchedChannels.none { it.url == selectedChannelUrl }) {
            selectedChannelUrl = uiState.currentChannel?.takeIf { uiState.epgMap.containsKey(it.url) }?.url
                ?: matchedChannels.firstOrNull()?.url
        }
    }

    val selectedChannel = matchedChannels.firstOrNull { it.url == selectedChannelUrl }
    val selectedStatus: ChannelEpgStatus? = selectedChannel?.let { uiState.epgMap[it.url] }

    val epgFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLocalEpg(uri)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.isLoadingEpg) {
            item {
                LinearProgressIndicator(
                    color = ToffeeMagenta,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = ToffeeViolet.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.large
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ToffeeViolet.copy(alpha = 0.28f),
                                    ToffeeMagenta.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = ToffeeMagenta,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = ToffeeStrings.epgTitle(lang),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ToffeeMagenta.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (isBn) "${matchedChannels.size} টি চ্যানেল" else "${matchedChannels.size} matched",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ToffeeMagenta,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = if (uiState.epgProgramsCount > 0) {
                                if (isBn) "${uiState.epgProgramsCount} টি অনুষ্ঠান • আপডেট: ${TimeFormat.formatRelativeUpdate(uiState.epgLastUpdatedAt)}"
                                else "${uiState.epgProgramsCount} programs stored • Updated ${TimeFormat.formatRelativeUpdate(uiState.epgLastUpdatedAt)}"
                            } else {
                                if (isBn) "XMLTV (.xml বা .xml.gz) প্রোগ্রাম গাইড URL অথবা লোকাল ফাইল ইম্পোর্ট করুন।"
                                else "Import an XMLTV (.xml or .xml.gz) program guide URL or local file."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("XMLTV EPG Source", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = epgUrlInput,
                        onValueChange = { epgUrlInput = it },
                        label = { Text("XMLTV URL (.xml or .xml.gz)") },
                        placeholder = { Text("https://example.com/epg.xml.gz") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("epg_url_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onLoadEpgUrl(epgUrlInput) },
                            enabled = !uiState.isLoadingEpg,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ToffeeMagenta,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_epg_url_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBn) "EPG লোড" else "Load EPG")
                        }
                        FilledTonalButton(
                            onClick = {
                                epgFilePicker.launch(
                                    arrayOf(
                                        "text/xml",
                                        "application/xml",
                                        "application/gzip",
                                        "application/x-gzip",
                                        "*/*"
                                    )
                                )
                            },
                            enabled = !uiState.isLoadingEpg,
                            modifier = Modifier.testTag("import_epg_file_button")
                        ) {
                            Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBn) "ফাইল" else "File")
                        }
                    }

                    if (uiState.epgProgramsCount > 0 || uiState.epgUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.epgUrl.startsWith("http", ignoreCase = true)) {
                                OutlinedButton(
                                    onClick = onRefreshEpg,
                                    enabled = !uiState.isLoadingEpg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("refresh_epg_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh")
                                }
                            }
                            OutlinedButton(
                                onClick = onClearEpg,
                                enabled = !uiState.isLoadingEpg,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clear_epg_button")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear Guide")
                            }
                        }
                    }
                }
            }
        }

        if (matchedChannels.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = ToffeeMagenta,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = ToffeeStrings.epgEmptyTitle(lang),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = ToffeeStrings.epgEmptySub(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = ToffeeStrings.epgOnNowAcross(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(matchedChannels, key = { "epg_now_${it.url}" }) { ch ->
                        val status = uiState.epgMap[ch.url]
                        val isSelected = ch.url == selectedChannelUrl
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest
                            else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .width(220.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ToffeeMagenta else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable { selectedChannelUrl = ch.url }
                                .testTag("epg_channel_card_${ch.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ch.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onPlayChannel(ch) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Rounded.PlayArrow,
                                            contentDescription = "Watch ${ch.name}",
                                            tint = ToffeeMagenta
                                        )
                                    }
                                }
                                Text(
                                    text = status?.now?.title ?: status?.next?.title ?: "Scheduled",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (status?.now != null) {
                                    LinearProgressIndicator(
                                        progress = { status.progressFraction(uiState.nowMillis) },
                                        color = ToffeeMagenta,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(matchedChannels, key = { "epg_chip_${it.url}" }) { ch ->
                        val selected = ch.url == selectedChannelUrl
                        FilterChip(
                            selected = selected,
                            onClick = { selectedChannelUrl = ch.url },
                            label = { Text(ch.name) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ToffeeMagenta,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (selectedChannel != null && selectedStatus != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedChannel.name} Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedStatus.upcoming.size} upcoming programs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { onPlayChannel(selectedChannel) },
                            modifier = Modifier.testTag("epg_watch_selected_channel")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Watch Live")
                        }
                    }
                }

                items(
                    items = selectedStatus.upcoming,
                    key = { "${it.id}_${it.startMillis}_${it.title}" }
                ) { program ->
                    val isNow = uiState.nowMillis in program.startMillis until program.endMillis
                    EpgProgramRow(
                        program = program,
                        isNow = isNow,
                        progress = if (isNow) selectedStatus.progressFraction(uiState.nowMillis) else 0f
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgProgramRow(
    program: EpgProgramEntity,
    isNow: Boolean,
    progress: Float
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isNow) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isNow) ToffeeMagenta else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimeFormat.formatRange(program.startMillis, program.endMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isNow) ToffeeMagenta else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (program.category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = program.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isNow) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LiveCrimson
                        ) {
                            Text(
                                text = "ON AIR",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isNow) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = ToffeeMagenta,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
