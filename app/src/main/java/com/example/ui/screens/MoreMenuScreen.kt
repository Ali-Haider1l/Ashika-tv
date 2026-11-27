package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.ui.AshikaUiState
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.theme.ToffeeViolet
import com.example.ui.util.ToffeeStrings

private enum class MenuSectionState {
    ROOT,
    TOOLS,
    ADMIN
}

@Composable
fun MoreMenuScreen(
    uiState: AshikaUiState,
    onOpenLanguagePicker: () -> Unit,
    onOpenEpg: () -> Unit,
    onOpenAddChannel: () -> Unit,
    onEditChannel: (ChannelEntity) -> Unit,
    onDeleteChannel: (ChannelEntity) -> Unit,
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
    onSubmitAdminPin: (String) -> Unit,
    onLockAdmin: () -> Unit,
    onRefreshBulkPlaylists: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn = uiState.isBanglaMode
    val lang = uiState.appLanguage
    var section by rememberSaveable { mutableStateOf(MenuSectionState.ROOT) }
    var selectedTool by rememberSaveable { mutableStateOf(AshikaTool.PROFILES) }

    when (section) {
        MenuSectionState.TOOLS -> {
            ToolsScreen(
                uiState = uiState,
                onRunFinder = onRunFinder,
                onPlayChannel = onPlayChannel,
                onRefreshHealth = onRefreshHealth,
                onClearStats = onClearStats,
                onAddMatch = onAddMatch,
                onDeleteMatch = onDeleteMatch,
                onSwitchProfile = onSwitchProfile,
                onCreateProfile = onCreateProfile,
                onDeleteProfile = onDeleteProfile,
                onGeneratePairCode = onGeneratePairCode,
                initialTool = selectedTool,
                onBack = { section = MenuSectionState.ROOT },
                modifier = modifier
            )
        }

        MenuSectionState.ADMIN -> {
            AdminScreen(
                uiState = uiState,
                onSubmitPin = onSubmitAdminPin,
                onLockAdmin = onLockAdmin,
                onOpenAddChannel = onOpenAddChannel,
                onEditChannel = onEditChannel,
                onDeleteChannel = onDeleteChannel,
                onRefreshBulkPlaylists = onRefreshBulkPlaylists,
                onBack = { section = MenuSectionState.ROOT },
                modifier = modifier
            )
        }

        MenuSectionState.ROOT -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = ToffeeMagenta.copy(alpha = 0.35f),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.activeProfile,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBn) {
                                            "${uiState.allChannels.size} চ্যানেল • ${uiState.favoriteChannels.size} পছন্দ • ${uiState.epgProgramsCount} EPG"
                                        } else {
                                            "${uiState.allChannels.size} channels • ${uiState.favoriteChannels.size} starred • ${uiState.epgProgramsCount} EPG"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ToffeeMagenta.copy(alpha = 0.18f),
                                    modifier = Modifier.clickable {
                                        selectedTool = AshikaTool.PROFILES
                                        section = MenuSectionState.TOOLS
                                    }
                                ) {
                                    Text(
                                        text = if (isBn) "বদলান" else "Switch",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ToffeeMagenta,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    MenuGroupHeader(ToffeeStrings.menuStreamingSection(lang))
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.Translate,
                        title = ToffeeStrings.languageTitle(lang),
                        subtitle = ToffeeStrings.languageSub(lang),
                        onClick = onOpenLanguagePicker,
                        testTag = "menu_item_language"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.CalendarMonth,
                        title = ToffeeStrings.epgTitle(lang),
                        subtitle = if (uiState.epgMap.isNotEmpty()) {
                            if (isBn) "${uiState.epgMap.size} টি চ্যানেলের লাইভ সময়সূচি"
                            else "${uiState.epgMap.size} channels with live schedule"
                        } else {
                            if (isBn) "XMLTV প্রোগ্রাম গাইড লোড করুন"
                            else "Load XMLTV program guide"
                        },
                        onClick = onOpenEpg,
                        testTag = "menu_item_epg"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.AddCircleOutline,
                        title = if (isBn) "চ্যানেল যোগ করুন" else "Add Channel",
                        subtitle = if (isBn) "নতুন HTTP/HTTPS স্ট্রিম লিংক যোগ করুন"
                        else "Add a custom HTTP/HTTPS stream URL",
                        onClick = onOpenAddChannel,
                        testTag = "menu_item_add_channel"
                    )
                }

                item {
                    MenuGroupHeader(ToffeeStrings.menuToolsSection(lang))
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.Groups,
                        title = if (isBn) "ফ্যামিলি প্রোফাইল" else "Family Profiles",
                        subtitle = if (isBn) "সক্রিয়: ${uiState.activeProfile}" else "Active: ${uiState.activeProfile}",
                        onClick = {
                            selectedTool = AshikaTool.PROFILES
                            section = MenuSectionState.TOOLS
                        },
                        testTag = "menu_item_profiles"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = if (isBn) "স্মার্ট চ্যানেল ফাইন্ডার" else "Smart Channel Finder",
                        subtitle = if (isBn) "নাম, ক্যাটাগরি বা পছন্দ দিয়ে খুঁজুন"
                        else "Search channels by genre, name, or favorites",
                        onClick = {
                            selectedTool = AshikaTool.FINDER
                            section = MenuSectionState.TOOLS
                        },
                        testTag = "menu_item_finder"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.BarChart,
                        title = if (isBn) "ওয়াচ পরিসংখ্যান" else "Watch Statistics",
                        subtitle = if (isBn) "প্রতি চ্যানেলে দেখার সময়ের হিসাব"
                        else "Per-channel viewing duration breakdown",
                        onClick = {
                            selectedTool = AshikaTool.STATS
                            section = MenuSectionState.TOOLS
                        },
                        testTag = "menu_item_stats"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.MonitorHeart,
                        title = if (isBn) "স্ট্রিম হেলথ" else "Stream Health",
                        subtitle = if (isBn) "বাফারিং ও ভিডিও রেজোলিউশন দেখুন"
                        else "Inspect ExoPlayer buffering and video resolution",
                        onClick = {
                            selectedTool = AshikaTool.HEALTH
                            section = MenuSectionState.TOOLS
                        },
                        testTag = "menu_item_health"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.SportsSoccer,
                        title = if (isBn) "লাইভ ম্যাচ সেন্টার" else "Live Match Center",
                        subtitle = if (isBn) "${uiState.matches.size} টি ম্যাচ স্কোরকার্ড সংরক্ষিত"
                        else "${uiState.matches.size} match scorecards saved",
                        onClick = {
                            selectedTool = AshikaTool.MATCHES
                            section = MenuSectionState.TOOLS
                        },
                        testTag = "menu_item_matches"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.QrCode2,
                        title = if (isBn) "QR পেয়ারিং ও কানেক্ট" else "QR Pairing & Connect",
                        subtitle = if (isBn) "৬-ডিজিটের পেয়ারিং কোড ও QR স্ক্যান"
                        else "Offline 6-digit pairing code and QR display",
                        onClick = {
                            selectedTool = AshikaTool.CONNECT
                            section = MenuSectionState.TOOLS
                        },
                        testTag = "menu_item_connect"
                    )
                }

                item {
                    MenuGroupHeader(ToffeeStrings.menuSystemSection(lang))
                }
                item {
                    MenuItemRow(
                        icon = Icons.Rounded.AdminPanelSettings,
                        title = if (isBn) "অ্যাডমিন প্যানেল" else "Admin Panel",
                        subtitle = if (uiState.isAdminUnlocked) {
                            if (isBn) "আনলক করা আছে • চ্যানেল ও বাল্ক URL" else "Unlocked • Manage channels & bulk URLs"
                        } else {
                            if (isBn) "পিন-সুরক্ষিত চ্যানেল ও প্লেলিস্ট ম্যানেজার" else "PIN-protected channel & playlist manager"
                        },
                        onClick = { section = MenuSectionState.ADMIN },
                        testTag = "menu_item_admin"
                    )
                }
                item {
                    MenuItemRow(
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        title = if (isBn) "টফি লাইভ (toffeelive.com/bn)" else "Toffee Live Web",
                        subtitle = if (isBn) "ব্রাউজারে toffeelive.com/bn খুলুন"
                        else "Open toffeelive.com/bn in browser",
                        onClick = {
                            val url = if (isBn) "https://toffeelive.com/bn" else "https://toffeelive.com"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        testTag = "menu_item_toffee_web"
                    )
                }
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Info, null, tint = ToffeeMagenta)
                                Spacer(Modifier.width(10.dp))
                                Text("Ashika Live TV • v1.0.0", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = if (isBn) "আমাদের সম্পর্কে • ব্যবহারের শর্তাবলী • গোপনীয়তা নীতি"
                                else "About Us • Terms of Use • Privacy Policy",
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

@Composable
private fun MenuGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = ToffeeMagenta,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ToffeeMagenta.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ToffeeMagenta,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
