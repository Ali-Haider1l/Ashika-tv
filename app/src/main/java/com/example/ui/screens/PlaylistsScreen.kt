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
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.AshikaRepository
import com.example.ui.AshikaUiState

@Composable
fun PlaylistsScreen(
    uiState: AshikaUiState,
    onLoadPlaylistUrl: (String) -> Unit,
    onImportLocalFile: (Uri) -> Unit,
    onRemovePlaylist: (String) -> Unit,
    onSyncLan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistUrlInput by remember { mutableStateOf(AshikaRepository.DEFAULT_PLAYLIST_URL) }
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
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Load M3U Playlist URL", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Enter an M3U or M3U8 playlist URL from a source you are authorized to watch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = playlistUrlInput,
                        onValueChange = { playlistUrlInput = it },
                        label = { Text("Playlist URL") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_url_input")
                    )
                    Button(
                        onClick = { onLoadPlaylistUrl(playlistUrlInput) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_playlist_button")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Load Playlist")
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
                    Text("Import Local .M3U / .M3U8 File", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Uses Android's built-in document picker with zero storage permissions required.",
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
                        Text("Choose M3U File")
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
                text = "Saved Playlists (${uiState.playlists.size})",
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
