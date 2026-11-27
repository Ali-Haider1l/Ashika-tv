package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelEpgStatus
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.StarGold
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.TimeFormat
import com.example.ui.util.ToffeeStrings
import kotlinx.coroutines.delay

data class ToffeeHeroSlide(
    val id: String,
    @DrawableRes val bannerRes: Int,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val channel: ChannelEntity?,
    val isEpgAction: Boolean = false
)

@Composable
fun ToffeeHeroCarousel(
    slides: List<ToffeeHeroSlide>,
    epgMap: Map<String, ChannelEpgStatus>,
    appLanguage: AppLanguage = AppLanguage.BANGLA,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenEpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (slides.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pagerState.settledPage, slides.size) {
        if (slides.size > 1) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("toffee_hero_carousel")
        ) { page ->
            val slide = slides[page]
            val epgNow = slide.channel?.let { epgMap[it.url]?.now }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.large)
                    .border(
                        width = 1.dp,
                        color = ToffeeMagenta.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.large
                    )
                    .clickable {
                        if (slide.channel != null) onPlayChannel(slide.channel)
                        else if (slide.isEpgAction) onOpenEpg()
                    }
                    .testTag("hero_slide_${slide.id}")
            ) {
                Image(
                    painter = painterResource(id = slide.bannerRes),
                    contentDescription = slide.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Cinematic gradient scrim for crisp readable text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianNight.copy(alpha = 0.25f),
                                    ObsidianNight.copy(alpha = 0.55f),
                                    ObsidianNight.copy(alpha = 0.94f)
                                )
                            )
                        )
                )

                // Top LIVE badge + channel logo pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LiveCrimson
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = slide.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    slide.channel?.takeIf { it.logoUrl.isNotBlank() }?.let { ch ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianNight.copy(alpha = 0.72f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            AsyncImage(
                                model = ch.logoUrl,
                                contentDescription = stringResource(R.string.cd_channel_logo),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(5.dp)
                            )
                        }
                    }
                }

                // Bottom headline, EPG info, and Watch Now CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = slide.channel?.name ?: slide.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val desc = epgNow?.let {
                            "${ToffeeStrings.onNowLabel(appLanguage)} ${it.title} • ${TimeFormat.formatRange(it.startMillis, it.endMillis)}"
                        } ?: slide.subtitle
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (slide.channel != null) onPlayChannel(slide.channel)
                            else onOpenEpg()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ToffeeMagenta,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("hero_cta_${slide.id}")
                    ) {
                        Icon(
                            imageVector = if (slide.channel != null) Icons.Rounded.PlayArrow else Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = ToffeeStrings.watchNow(appLanguage),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Toffee pagination dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (active) 20.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) ToffeeMagenta
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
fun ToffeeStarredAvatarStrip(
    starredChannels: List<ChannelEntity>,
    currentChannelUrl: String?,
    appLanguage: AppLanguage = AppLanguage.BANGLA,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = StarGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = ToffeeStrings.starredQuickAccess(appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${ToffeeStrings.seeAll(appLanguage)} ›",
                style = MaterialTheme.typography.labelLarge,
                color = ToffeeMagenta,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenFavorites)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("quick_access_see_all_button")
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(starredChannels.take(14), key = { "avatar_${it.url}" }) { channel ->
                val active = channel.url == currentChannelUrl
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlayChannel(channel) }
                        .padding(vertical = 4.dp)
                        .testTag("quick_star_chip_${channel.name}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = if (active) 2.dp else 1.5.dp,
                                color = if (active) ToffeeMagenta else StarGold.copy(alpha = 0.65f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channel.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = channel.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.LiveTv,
                                contentDescription = null,
                                tint = ToffeeMagenta,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = if (active) ToffeeMagenta else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ToffeeChannelCard(
    channel: ChannelEntity,
    epgStatus: ChannelEpgStatus?,
    nowMillis: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
    appLanguage: AppLanguage = AppLanguage.BANGLA,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                color = if (isPlaying) ToffeeMagenta
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onPlay)
            .testTag("channel_card_${channel.name}"),
        color = if (isPlaying) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = if (isPlaying) 4.dp else 1.dp
    ) {
        Column {
            // Toffee-style 16:9 dark spotlight studio stage for logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ToffeeViolet.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = stringResource(R.string.cd_channel_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.LiveTv,
                        contentDescription = null,
                        tint = ToffeeMagenta,
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.Center)
                    )
                }

                // Top-left LIVE pill
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = if (isPlaying) ToffeeMagenta else LiveCrimson,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isPlaying) "PLAYING" else "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Top-right Star + More buttons (48dp accessible touch targets)
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("favorite_toggle_${channel.name}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = stringResource(R.string.cd_favorite_toggle),
                            tint = if (isFavorite) StarGold else Color.White.copy(alpha = 0.85f)
                        )
                    }
                    IconButton(
                        onClick = onInspect,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("inspect_channel_${channel.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Subtle bottom-right play affordance
                FilledTonalIconButton(
                    onClick = onPlay,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = ObsidianNight.copy(alpha = 0.72f),
                        contentColor = ToffeeMagenta
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (epgStatus?.now != null) {
                    Text(
                        text = "${ToffeeStrings.onNowLabel(appLanguage)} ${epgStatus.now.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { epgStatus.progressFraction(nowMillis) },
                        color = ToffeeMagenta,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                } else {
                    Text(
                        text = ToffeeStrings.categoryLabel(channel.groupTitle, appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
