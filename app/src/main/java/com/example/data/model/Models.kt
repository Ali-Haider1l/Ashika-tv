package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val url: String,
    val name: String,
    val groupTitle: String = "Other",
    val logoUrl: String = "",
    val tvgId: String = "",
    val source: String = "playlist",
    val headersJson: String = "",
    val isFavorite: Boolean = false,
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

@Entity(tableName = "epg_channels")
data class EpgChannelEntity(
    @PrimaryKey val epgId: String,
    val displayName: String
)

@Entity(
    tableName = "epg_programs",
    indices = [Index("channelId"), Index("startMillis")]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val channelId: String,
    val title: String,
    val description: String = "",
    val category: String = "",
    val startMillis: Long,
    val endMillis: Long
)

data class ChannelEpgStatus(
    val now: EpgProgramEntity? = null,
    val next: EpgProgramEntity? = null,
    val upcoming: List<EpgProgramEntity> = emptyList()
) {
    fun progressFraction(nowMillis: Long = System.currentTimeMillis()): Float {
        val current = now ?: return 0f
        val duration = (current.endMillis - current.startMillis).coerceAtLeast(1L)
        return ((nowMillis - current.startMillis).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
}

data class StreamHealthInfo(
    val channelName: String = "",
    val channelUrl: String = "",
    val playbackStateLabel: String = "Idle",
    val isPlaying: Boolean = false,
    val bufferedPercentage: Int = 0,
    val videoResolution: String = "—",
    val bitrateLabel: String = "—",
    val frameRateLabel: String = "—",
    val codecLabel: String = "—",
    val bitrateBps: Long = 0L,
    val frameRateFps: Float = 0f,
    val droppedFrames: Int = 0,
    val errorMessage: String? = null,
    val checkedAt: Long = System.currentTimeMillis()
)

enum class ChannelFilterTab {
    ALL, FAVORITES, RECENT
}

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val badgeText: String
) {
    BANGLA("bn", "বাংলা", "Bangla", "বাংলা"),
    ENGLISH("en", "English", "English", "EN"),
    HINDI("hi", "हिन्दी", "Hindi", "हिन्दी"),
    URDU("ur", "اردو", "Urdu", "اردو"),
    ARABIC("ar", "العربية", "Arabic", "عربي");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BANGLA
    }
}
