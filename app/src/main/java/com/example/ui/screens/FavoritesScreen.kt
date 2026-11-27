package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChannelEntity
import com.example.ui.AshikaUiState
import com.example.ui.components.ToffeeChannelCard
import com.example.ui.theme.StarGold
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.ToffeeStrings

@Composable
fun FavoritesScreen(
    uiState: AshikaUiState,
    onPlayChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onInspectChannel: (ChannelEntity) -> Unit,
    onBrowseChannels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = uiState.isBanglaMode
    val lang = uiState.appLanguage
    var filterQuery by remember { mutableStateOf("") }

    val displayedFavorites = remember(uiState.favoriteChannels, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isEmpty()) uiState.favoriteChannels
        else uiState.favoriteChannels.filter {
            it.name.lowercase().contains(q) || it.groupTitle.lowercase().contains(q)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = StarGold.copy(alpha = 0.35f),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = StarGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = ToffeeStrings.favoritesTitle(lang),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isBn) "${uiState.activeProfile} প্রোফাইলের জন্য সংরক্ষিত"
                                else "Saved for ${uiState.activeProfile} profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = StarGold.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${uiState.favoriteChannels.size} ★",
                                style = MaterialTheme.typography.labelMedium,
                                color = StarGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.favoriteChannels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = {
                        Text(if (isBn) "পছন্দের চ্যানেল খুঁজুন…" else "Filter starred channels…")
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (filterQuery.isNotEmpty()) {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(Icons.Rounded.Clear, stringResource(R.string.cd_close))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorites_search_input")
                )
            }
        }

        if (displayedFavorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
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
                            imageVector = Icons.Rounded.StarOutline,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (uiState.favoriteChannels.isEmpty()) {
                                ToffeeStrings.favoritesEmptyTitle(lang)
                            } else {
                                if (isBn) "কোনো চ্যানেল মেলেনি" else "No matching starred channels"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (uiState.favoriteChannels.isEmpty()) {
                                ToffeeStrings.favoritesEmptySub(lang)
                            } else {
                                if (isBn) "অন্য নাম দিয়ে খুঁজে দেখুন।" else "Try clearing your search filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.favoriteChannels.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onBrowseChannels,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ToffeeMagenta,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("favorites_browse_channels_button")
                            ) {
                                Text(ToffeeStrings.browseChannels(lang))
                            }
                        }
                    }
                }
            }
        } else {
            items(displayedFavorites, key = { it.url }) { channel ->
                ToffeeChannelCard(
                    channel = channel,
                    epgStatus = uiState.epgMap[channel.url],
                    nowMillis = uiState.nowMillis,
                    isPlaying = uiState.currentChannel?.url == channel.url,
                    isFavorite = true,
                    appLanguage = lang,
                    onPlay = { onPlayChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                    onInspect = { onInspectChannel(channel) }
                )
            }
        }
    }
}
