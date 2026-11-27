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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.data.remote.CloudSyncUiState
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
    onSyncCloud: () -> Unit = {},
    onSignOut: () -> Unit = {},
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
                    CloudAccountSyncCard(
                        displayName = uiState.userDisplayName,
                        email = uiState.userEmail,
                        cloudSyncState = uiState.cloudSyncState,
                        isBusy = uiState.isBusy,
                        isBn = isBn,
                        onSyncCloud = onSyncCloud,
                        onSignOut = onSignOut
                    )
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
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_contact_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Info, null, tint = ToffeeMagenta)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (isBn) "Ashika TV সম্পর্কে" else "About Ashika TV",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isBn) {
                                    "Ashika TV একটি সহজ ও ব্যবহারবান্ধব লাইভ টিভি প্লেয়ার। নিজের M3U প্লেলিস্ট যোগ করে চ্যানেল দেখুন, পছন্দের চ্যানেল সংরক্ষণ করুন এবং EPG গাইড ব্যবহার করুন।"
                                } else {
                                    "Ashika TV is an easy-to-use live TV player. Add your own M3U playlists, watch channels, save favorites, and browse the EPG program guide."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBn) "যোগাযোগ করুন" else "Contact Us",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$facebook"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ToffeeMagenta,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("contact_facebook_button")
                            ) {
                                Text(
                                    text = if (isBn) "Facebook প্রোফাইলে যোগাযোগ করুন" else "Visit our Facebook Profile",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Ashika TV • v6.1.0",
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

@Composable
private fun CloudAccountSyncCard(
    displayName: String,
    email: String,
    cloudSyncState: CloudSyncUiState,
    isBusy: Boolean,
    isBn: Boolean,
    onSyncCloud: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_account_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ToffeeMagenta.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = ToffeeMagenta,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifBlank {
                            if (isBn) "গুগল অ্যাকাউন্ট" else "Google Account"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val isError = cloudSyncState is CloudSyncUiState.Error
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (cloudSyncState) {
                        is CloudSyncUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ToffeeMagenta
                            )
                            Text(
                                text = if (isBn) "ফায়ারবেসে সিঙ্ক হচ্ছে…" else "Syncing with Firebase…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is CloudSyncUiState.Synced -> {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = ToffeeMagenta,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBn) {
                                    "সিঙ্ক সম্পন্ন • ${cloudSyncState.favoritesCount} পছন্দ • ${cloudSyncState.customChannelsCount} কাস্টম • ${cloudSyncState.playlistsCount} প্লেলিস্ট"
                                } else {
                                    "Cloud synced • ${cloudSyncState.favoritesCount} favorites • ${cloudSyncState.customChannelsCount} custom • ${cloudSyncState.playlistsCount} playlists"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is CloudSyncUiState.Error -> {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = cloudSyncState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncCloud,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ToffeeMagenta,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("cloud_sync_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "এখন সিঙ্ক করুন" else "Sync Now",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "সাইন আউট" else "Sign Out",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
