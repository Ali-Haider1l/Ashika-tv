package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TvOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelFilterTab
import com.example.ui.AshikaUiState
import com.example.ui.components.ToffeeChannelCard
import com.example.ui.components.ToffeeHeroCarousel
import com.example.ui.components.ToffeeHeroSlide
import com.example.ui.components.ToffeeStarredAvatarStrip
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.ToffeeStrings

@Composable
fun ChannelsScreen(
    uiState: AshikaUiState,
    onSearchChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectTab: (ChannelFilterTab) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onInspectChannel: (ChannelEntity) -> Unit,
    onOpenAddPlaylist: () -> Unit,
    onOpenAddChannel: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenEpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = uiState.isBanglaMode
    val lang = uiState.appLanguage

    val heroSlides = remember(uiState.allChannels, uiState.currentChannel, lang) {
        val all = uiState.allChannels
        val sportsChannel = all.firstOrNull {
            it.groupTitle.contains("sport", ignoreCase = true) ||
                it.name.contains("sport", ignoreCase = true)
        } ?: uiState.currentChannel ?: all.firstOrNull()

        val entChannel = all.firstOrNull {
            it.groupTitle.contains("entertain", ignoreCase = true) ||
                it.groupTitle.contains("movie", ignoreCase = true) ||
                it.groupTitle.contains("drama", ignoreCase = true)
        } ?: all.getOrNull(1) ?: sportsChannel

        val newsChannel = all.firstOrNull {
            it.groupTitle.contains("news", ignoreCase = true) ||
                it.name.contains("news", ignoreCase = true) ||
                it.name.contains("somoy", ignoreCase = true) ||
                it.name.contains("jamuna", ignoreCase = true)
        } ?: all.getOrNull(2) ?: sportsChannel

        listOf(
            ToffeeHeroSlide(
                id = "sports",
                bannerRes = R.drawable.img_banner_sports,
                badgeText = "LIVE",
                title = ToffeeStrings.bannerSportsTitle(lang),
                subtitle = ToffeeStrings.bannerSportsSub(lang),
                channel = sportsChannel
            ),
            ToffeeHeroSlide(
                id = "entertainment",
                bannerRes = R.drawable.img_banner_entertainment,
                badgeText = if (isBn) "বিনোদন" else "FEATURED",
                title = ToffeeStrings.bannerEntertainmentTitle(lang),
                subtitle = ToffeeStrings.bannerEntertainmentSub(lang),
                channel = entChannel
            ),
            ToffeeHeroSlide(
                id = "news",
                bannerRes = R.drawable.img_banner_news,
                badgeText = if (isBn) "নিউজ • EPG" else "NEWS • EPG",
                title = ToffeeStrings.bannerNewsTitle(lang),
                subtitle = ToffeeStrings.bannerNewsSub(lang),
                channel = newsChannel,
                isEpgAction = true
            )
        )
    }

    val showToffeeCarousels = uiState.selectedTab == ChannelFilterTab.ALL &&
        uiState.selectedCategory.equals("All", ignoreCase = true) &&
        uiState.searchQuery.isBlank() &&
        uiState.allChannels.isNotEmpty()

    val categoryShelves = remember(uiState.allChannels, uiState.categories) {
        if (uiState.allChannels.isEmpty()) emptyList()
        else uiState.categories
            .filter { !it.equals("All", ignoreCase = true) }
            .mapNotNull { cat ->
                val exact = uiState.allChannels.filter { it.groupTitle.equals(cat, ignoreCase = true) }
                val list = exact.ifEmpty {
                    uiState.allChannels.filter { it.groupTitle.contains(cat, ignoreCase = true) }
                }
                if (list.isNotEmpty()) cat to list.take(20) else null
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(visible = uiState.isSearchExpanded || uiState.searchQuery.isNotEmpty()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(ToffeeStrings.searchHint(lang)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.searchQuery.isNotEmpty()) onSearchChange("")
                                else onCloseSearch()
                            }
                        ) {
                            Icon(Icons.Rounded.Clear, stringResource(R.string.cd_close))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_search_input")
                )
            }
        }

        // Toffee Hero Banner Carousel
        item(span = { GridItemSpan(maxLineSpan) }) {
            ToffeeHeroCarousel(
                slides = heroSlides,
                epgMap = uiState.epgMap,
                appLanguage = lang,
                onPlayChannel = onPlayChannel,
                onOpenEpg = onOpenEpg
            )
        }

        // Toffee Category Pills + Tab Switcher
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories) { cat ->
                        val selected = uiState.selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(cat) },
                            label = {
                                Text(
                                    text = ToffeeStrings.categoryLabel(cat, lang),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ToffeeMagenta,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("category_chip_${cat.lowercase()}")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChannelFilterTab.entries.forEach { tab ->
                        val selected = uiState.selectedTab == tab
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTab(tab) },
                            label = { Text(ToffeeStrings.tabLabel(tab, lang)) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ToffeeViolet.copy(alpha = 0.34f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("filter_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Starred Circular Quick Access Row
        if (uiState.favoriteChannels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ToffeeStarredAvatarStrip(
                    starredChannels = uiState.favoriteChannels,
                    currentChannelUrl = uiState.currentChannel?.url,
                    appLanguage = lang,
                    onPlayChannel = onPlayChannel,
                    onOpenFavorites = onOpenFavorites
                )
            }
        }

        // Toffee Horizontal Carousels when on Home / All view
        if (showToffeeCarousels) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ToffeeCategoryCarouselRow(
                    title = ToffeeStrings.popularLiveTv(lang),
                    channels = uiState.allChannels.take(12),
                    uiState = uiState,
                    appLanguage = lang,
                    onSeeAll = null,
                    onPlayChannel = onPlayChannel,
                    onToggleFavorite = onToggleFavorite,
                    onInspectChannel = onInspectChannel
                )
            }

            items(categoryShelves, key = { "shelf_${it.first}" }, span = { GridItemSpan(maxLineSpan) }) { (cat, list) ->
                ToffeeCategoryCarouselRow(
                    title = ToffeeStrings.categoryLabel(cat, lang),
                    channels = list,
                    uiState = uiState,
                    appLanguage = lang,
                    onSeeAll = { onSelectCategory(cat) },
                    onPlayChannel = onPlayChannel,
                    onToggleFavorite = onToggleFavorite,
                    onInspectChannel = onInspectChannel
                )
            }
        }

        // Section Heading for Grid
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heading = when (uiState.selectedTab) {
                    ChannelFilterTab.ALL -> {
                        if (uiState.selectedCategory.equals("All", ignoreCase = true)) {
                            ToffeeStrings.allChannelsTitle(lang)
                        } else {
                            ToffeeStrings.categoryLabel(uiState.selectedCategory, lang)
                        }
                    }
                    ChannelFilterTab.FAVORITES -> ToffeeStrings.favoritesTitle(lang)
                    ChannelFilterTab.RECENT -> ToffeeStrings.continueWatching(lang)
                }
                Text(heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = ToffeeStrings.channelsCount(uiState.filteredChannels.size, lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = ToffeeMagenta
                )
            }
        }

        if (uiState.filteredChannels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyChannelsCard(
                    isBangla = isBn,
                    onOpenAddPlaylist = onOpenAddPlaylist,
                    onOpenAddChannel = onOpenAddChannel
                )
            }
        } else {
            items(uiState.filteredChannels, key = { it.url }) { channel ->
                ToffeeChannelCard(
                    channel = channel,
                    epgStatus = uiState.epgMap[channel.url],
                    nowMillis = uiState.nowMillis,
                    isPlaying = uiState.currentChannel?.url == channel.url,
                    isFavorite = uiState.favoriteUrls.contains(channel.url),
                    appLanguage = lang,
                    onPlay = { onPlayChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                    onInspect = { onInspectChannel(channel) }
                )
            }
        }
    }
}

@Composable
private fun ToffeeCategoryCarouselRow(
    title: String,
    channels: List<ChannelEntity>,
    uiState: AshikaUiState,
    appLanguage: AppLanguage,
    onSeeAll: (() -> Unit)?,
    onPlayChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onInspectChannel: (ChannelEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (onSeeAll != null) {
                Text(
                    text = "${ToffeeStrings.seeAll(appLanguage)} ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = ToffeeMagenta,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(channels, key = { "row_${title}_${it.url}" }) { channel ->
                ToffeeChannelCard(
                    channel = channel,
                    epgStatus = uiState.epgMap[channel.url],
                    nowMillis = uiState.nowMillis,
                    isPlaying = uiState.currentChannel?.url == channel.url,
                    isFavorite = uiState.favoriteUrls.contains(channel.url),
                    appLanguage = appLanguage,
                    onPlay = { onPlayChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                    onInspect = { onInspectChannel(channel) },
                    modifier = Modifier.width(188.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChannelsCard(
    isBangla: Boolean,
    onOpenAddPlaylist: () -> Unit,
    onOpenAddChannel: () -> Unit
) {
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
                imageVector = Icons.Rounded.TvOff,
                contentDescription = null,
                tint = ToffeeMagenta,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = if (isBangla) "কোনো চ্যানেল পাওয়া যায়নি" else stringResource(R.string.empty_channels_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isBangla) "M3U প্লেলিস্ট URL লোড করুন বা নতুন চ্যানেল যোগ করুন।"
                else stringResource(R.string.empty_channels_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenAddPlaylist) {
                    Text(if (isBangla) "প্লেলিস্ট যোগ" else stringResource(R.string.add_playlist))
                }
                FilledTonalButton(onClick = onOpenAddChannel) {
                    Text(if (isBangla) "চ্যানেল যোগ" else stringResource(R.string.add_channel))
                }
            }
        }
    }
}
