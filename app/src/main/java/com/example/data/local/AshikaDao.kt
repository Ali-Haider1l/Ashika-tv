package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.ChannelEntity
import com.example.data.model.EpgChannelEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.FamilyProfileEntity
import com.example.data.model.MatchEntryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.ProfileFavoriteEntity
import com.example.data.model.ProfileRecentEntity
import com.example.data.model.WatchStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AshikaDao {

    @Query("SELECT * FROM channels ORDER BY name COLLATE NOCASE ASC")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT c.* FROM channels c
        INNER JOIN profile_favorites f ON c.url = f.channelUrl
        WHERE f.profileName = :profileName
        ORDER BY f.addedAt DESC
        """
    )
    fun observeFavoriteChannels(profileName: String): Flow<List<ChannelEntity>>

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateChannelFavoriteFlag(url: String, isFavorite: Boolean)

    @Query("UPDATE channels SET isFavorite = EXISTS(SELECT 1 FROM profile_favorites f WHERE f.profileName = :profileName AND f.channelUrl = channels.url)")
    suspend fun syncChannelFavoriteFlagsForProfile(profileName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE url = :url")
    suspend fun deleteChannelByUrl(url: String)

    @Query("DELETE FROM channels WHERE source = :source")
    suspend fun deleteChannelsBySource(source: String)

    @Query("SELECT * FROM playlists ORDER BY lastRefreshedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getPlaylistsSnapshot(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE url = :url")
    suspend fun deletePlaylistByUrl(url: String)

    @Query("SELECT * FROM family_profiles ORDER BY createdAt ASC")
    fun observeProfiles(): Flow<List<FamilyProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfile(profile: FamilyProfileEntity)

    @Query("DELETE FROM family_profiles WHERE name = :name")
    suspend fun deleteProfile(name: String)

    @Query("SELECT channelUrl FROM profile_favorites WHERE profileName = :profileName ORDER BY addedAt DESC")
    fun observeFavoriteUrls(profileName: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM profile_favorites WHERE profileName = :profileName AND channelUrl = :channelUrl)")
    suspend fun isFavorite(profileName: String, channelUrl: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: ProfileFavoriteEntity)

    @Query("DELETE FROM profile_favorites WHERE profileName = :profileName AND channelUrl = :channelUrl")
    suspend fun deleteFavorite(profileName: String, channelUrl: String)

    @Query("DELETE FROM profile_favorites WHERE profileName = :profileName")
    suspend fun deleteAllFavoritesForProfile(profileName: String)

    @Transaction
    suspend fun toggleFavoriteForProfile(profileName: String, channelUrl: String): Boolean {
        val currentlyFav = isFavorite(profileName, channelUrl)
        val nextState = !currentlyFav
        if (nextState) {
            insertFavorite(ProfileFavoriteEntity(profileName, channelUrl))
        } else {
            deleteFavorite(profileName, channelUrl)
        }
        updateChannelFavoriteFlag(channelUrl, nextState)
        return nextState
    }

    @Query("SELECT channelUrl FROM profile_recents WHERE profileName = :profileName ORDER BY watchedAt DESC LIMIT 30")
    fun observeRecentUrls(profileName: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecent(recent: ProfileRecentEntity)

    @Query("DELETE FROM profile_recents WHERE profileName = :profileName")
    suspend fun deleteAllRecentsForProfile(profileName: String)

    @Query("SELECT * FROM watch_stats ORDER BY totalSeconds DESC")
    fun observeWatchStats(): Flow<List<WatchStatEntity>>

    @Query("SELECT * FROM watch_stats WHERE channelUrl = :channelUrl LIMIT 1")
    suspend fun getWatchStat(channelUrl: String): WatchStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchStat(stat: WatchStatEntity)

    @Query("DELETE FROM watch_stats")
    suspend fun clearWatchStats()

    @Transaction
    suspend fun incrementWatchSeconds(channelUrl: String, channelName: String, deltaSeconds: Long) {
        val existing = getWatchStat(channelUrl)
        val updated = if (existing != null) {
            existing.copy(
                channelName = channelName,
                totalSeconds = existing.totalSeconds + deltaSeconds,
                lastWatchedAt = System.currentTimeMillis()
            )
        } else {
            WatchStatEntity(
                channelUrl = channelUrl,
                channelName = channelName,
                totalSeconds = deltaSeconds,
                lastWatchedAt = System.currentTimeMillis()
            )
        }
        upsertWatchStat(updated)
    }

    @Query("SELECT * FROM match_entries ORDER BY createdAt DESC")
    fun observeMatches(): Flow<List<MatchEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntryEntity)

    @Query("DELETE FROM match_entries WHERE id = :id")
    suspend fun deleteMatch(id: Long)

    @Query("SELECT * FROM epg_channels")
    fun observeEpgChannels(): Flow<List<EpgChannelEntity>>

    @Query("SELECT * FROM epg_programs ORDER BY startMillis ASC")
    fun observeEpgPrograms(): Flow<List<EpgProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgChannels(channels: List<EpgChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_channels")
    suspend fun clearEpgChannels()

    @Query("DELETE FROM epg_programs")
    suspend fun clearEpgPrograms()

    @Transaction
    suspend fun replaceEpgData(
        channels: List<EpgChannelEntity>,
        programs: List<EpgProgramEntity>
    ) {
        clearEpgPrograms()
        clearEpgChannels()
        if (channels.isNotEmpty()) {
            insertEpgChannels(channels)
        }
        if (programs.isNotEmpty()) {
            programs.chunked(500).forEach { chunk ->
                insertEpgPrograms(chunk)
            }
        }
    }
}
