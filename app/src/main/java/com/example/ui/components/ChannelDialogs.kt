package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelEpgStatus
import com.example.data.model.FamilyProfileEntity
import com.example.ui.theme.StarGold
import com.example.ui.theme.ToffeeMagenta
import com.example.ui.util.TimeFormat
import com.example.ui.util.ToffeeStrings

@Composable
fun ChannelEditorDialog(
    editingChannel: ChannelEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, category: String, logoUrl: String, previousUrl: String?) -> Unit
) {
    var name by remember(editingChannel) { mutableStateOf(editingChannel?.name.orEmpty()) }
    var url by remember(editingChannel) { mutableStateOf(editingChannel?.url.orEmpty()) }
    var category by remember(editingChannel) { mutableStateOf(editingChannel?.groupTitle ?: "Entertainment") }
    var logoUrl by remember(editingChannel) { mutableStateOf(editingChannel?.logoUrl.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingChannel == null) "Add Channel" else "Edit Channel",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Authorized Stream URL (http/https)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_url_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (News, Sports, Music…)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_category_input")
                )
                OutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("Logo URL (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_logo_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, url, category, logoUrl, editingChannel?.url)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_channel_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelPreviewBottomSheet(
    channel: ChannelEntity,
    isFavorite: Boolean,
    epgStatus: ChannelEpgStatus? = null,
    nowMillis: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = stringResource(R.string.cd_channel_logo),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.groupTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    text = channel.url,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            if (epgStatus != null && epgStatus.upcoming.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = ToffeeMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Program Schedule (EPG)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        epgStatus.now?.let { nowProg ->
                            LinearProgressIndicator(
                                progress = { epgStatus.progressFraction(nowMillis) },
                                color = ToffeeMagenta,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            if (nowProg.description.isNotBlank()) {
                                Text(
                                    text = nowProg.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(epgStatus.upcoming, key = { "${it.id}_${it.startMillis}" }) { prog ->
                                val isNow = epgStatus.now?.let { it.startMillis == prog.startMillis } == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            if (isNow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prog.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = TimeFormat.formatRange(prog.startMillis, prog.endMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isNow) {
                                        Text(
                                            text = "NOW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ToffeeMagenta,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onPlay()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_play_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play Stream")
                }

                FilledTonalButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("preview_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = null,
                        tint = if (isFavorite) StarGold else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFavorite) "Starred" else "Star")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_edit_button")
                ) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_delete_button")
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProfileSwitcherDialog(
    profiles: List<FamilyProfileEntity>,
    activeProfile: String,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Family Profiles", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Each profile keeps its own Favorites and Recently Watched channels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                profiles.forEach { profile ->
                    val selected = profile.name == activeProfile
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSwitchProfile(profile.name)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(profile.name, style = MaterialTheme.typography.titleSmall)
                            if (selected) {
                                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New profile") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_profile_dialog_input")
                    )
                    FilledTonalButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreateProfile(newName)
                                newName = ""
                            }
                        },
                        modifier = Modifier.testTag("create_profile_dialog_button")
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun LanguageSelectorDialog(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = ToffeeStrings.chooseLanguageTitle(currentLanguage),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    val selected = language == currentLanguage
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                onSelectLanguage(language)
                                onDismiss()
                            }
                            .testTag("language_option_${language.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = language.nativeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "${language.englishName} (${language.code.uppercase()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = ToffeeMagenta
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
