package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.AshikaRepository
import com.example.ui.AshikaUiState
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.util.ToffeeStrings

@Composable
fun PlaylistsScreen(
    uiState: AshikaUiState,
    onSyncToffeeAndBd: () -> Unit,
    onLoadPlaylistUrl: (String) -> Unit,
    onImportLocalFile: (Uri) -> Unit,
    onLoadEpgUrl: (String) -> Unit,
    onImportLocalEpg: (Uri) -> Unit,
    onRemovePlaylist: (String) -> Unit,
    onSyncLan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val isBn = uiState.isBanglaMode
    val lang = uiState.appLanguage
    var playlistUrlInput by remember { mutableStateOf(AshikaRepository.DEFAULT_PLAYLIST_URL) }
    var epgUrlInput by remember(uiState.epgUrl) { mutableStateOf(uiState.epgUrl) }
    var lanUrlInput by remember(uiState.lanSyncUrl) {
        mutableStateOf(uiState.lanSyncUrl.ifEmpty { "http://192.168.0.103:8080/api/channels" })
    }

    val m3uFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLocalFile(uri)
        }
    }

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
        if (uiState.isBusy) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LiveTv,
                            contentDescription = null,
                            tint = ToffeeMagenta,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = ToffeeStrings.toffeeSyncTitle(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = ToffeeStrings.toffeeSyncSub(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSyncToffeeAndBd,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ToffeeMagenta,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_toffee_bd_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(ToffeeStrings.toffeeSyncButton(lang))
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
                    Text(
                        text = if (isBn) "M3U / Toffee JSON প্লেলিস্ট URL লোড করুন" else "Load M3U / Toffee JSON Playlist URL",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isBn) "M3U, M3U8, বা Toffee JSON প্লেলিস্ট লিংক দিন (কুকি/হেডার সহ সমর্থিত)।"
                        else "Enter an M3U, M3U8, or Toffee JSON playlist URL (supports #EXTHTTP and Cookie/User-Agent headers).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = playlistUrlInput,
                        onValueChange = { playlistUrlInput = it },
                        label = { Text(if (isBn) "প্লেলিস্ট URL" else "Playlist URL") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_url_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onLoadPlaylistUrl(playlistUrlInput) },
                            enabled = !uiState.isBusy && playlistUrlInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_playlist_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isBn) "লোড করুন" else "Load")
                        }
                        FilledTonalButton(
                            onClick = {
                                val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                if (pasted.isNotBlank()) playlistUrlInput = pasted
                            },
                            enabled = !uiState.isBusy,
                            modifier = Modifier.testTag("paste_playlist_button")
                        ) {
                            Text(if (isBn) "পেস্ট" else "Paste")
                        }
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
                    Text(
                        text = if (isBn) "লোকাল .M3U / .M3U8 ফাইল ইম্পোর্ট" else "Import Local .M3U / .M3U8 File",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isBn) "অতিরিক্ত স্টোরেজ পারমিশন ছাড়াই ডিভাইস থেকে ফাইল বেছে নিন।"
                        else "Uses Android's built-in document picker with zero storage permissions required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = {
                            m3uFilePicker.launch(
                                arrayOf(
                                    "audio/x-mpegurl",
                                    "application/vnd.apple.mpegurl",
                                    "application/x-mpegurl",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        enabled = !uiState.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_m3u_file_button")
                    ) {
                        Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isBn) "M3U ফাইল বেছে নিন" else "Choose M3U File")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("XMLTV Program Guide (EPG)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (uiState.epgProgramsCount > 0) {
                            "${uiState.epgProgramsCount} programs loaded across ${uiState.epgMap.size} matched channels."
                        } else {
                            "Load an XMLTV (.xml or .xml.gz) feed URL or file to show live & upcoming schedules."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = epgUrlInput,
                        onValueChange = { epgUrlInput = it },
                        label = { Text("XMLTV EPG URL") },
                        placeholder = { Text("https://example.com/epg.xml.gz") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_epg_url_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onLoadEpgUrl(epgUrlInput) },
                            enabled = !uiState.isLoadingEpg,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("playlist_load_epg_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Load EPG")
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
                            modifier = Modifier.testTag("playlist_import_epg_button")
                        ) {
                            Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("File")
                        }
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
                    Text("Phone-to-TV Companion Sync", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Sync channels from an Ashika companion server on your local Wi-Fi network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = lanUrlInput,
                        onValueChange = { lanUrlInput = it },
                        label = { Text("Companion JSON Endpoint") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lan_sync_url_input")
                    )
                    FilledTonalButton(
                        onClick = { onSyncLan(lanUrlInput) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lan_sync_button")
                    ) {
                        Icon(Icons.Rounded.PhoneAndroid, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync from Companion")
                    }
                }
            }
        }

        item {
            Text(
                text = if (isBn) "সংরক্ষিত প্লেলিস্ট (${uiState.playlists.size})"
                else "Saved Playlists (${uiState.playlists.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (uiState.playlists.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No saved playlists yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(uiState.playlists, key = { it.url }) { playlist ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = playlist.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.channelCount} channels",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onLoadPlaylistUrl(playlist.url) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Refresh, "Refresh playlist")
                        }
                        IconButton(
                            onClick = { onRemovePlaylist(playlist.url) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                "Remove playlist",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
