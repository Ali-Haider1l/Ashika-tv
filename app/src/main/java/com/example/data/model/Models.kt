package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val url: String,
    val name: String,
    val groupTitle: String = "Other",
    val logoUrl: String = "",
    val source: String = "playlist",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val url: String,
    val title: String,
    val channelCount: Int = 0,
    val lastRefreshedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "family_profiles")
data class FamilyProfileEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "profile_favorites",
    primaryKeys = ["profileName", "channelUrl"]
)
data class ProfileFavoriteEntity(
    val profileName: String,
    val channelUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "profile_recents",
    primaryKeys = ["profileName", "channelUrl"]
)
data class ProfileRecentEntity(
    val profileName: String,
    val channelUrl: String,
    val watchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_stats")
data class WatchStatEntity(
    @PrimaryKey val channelUrl: String,
    val channelName: String,
    val totalSeconds: Long = 0L,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_entries")
data class MatchEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teams: String,
    val matchTime: String,
    val score: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class StreamHealthInfo(
    val channelName: String = "",
    val channelUrl: String = "",
    val playbackStateLabel: String = "Idle",
    val isPlaying: Boolean = false,
    val bufferedPercentage: Int = 0,
    val videoResolution: String = "—",
    val errorMessage: String? = null,
    val checkedAt: Long = System.currentTimeMillis()
)

enum class ChannelFilterTab {
    ALL, FAVORITES, RECENT
}
