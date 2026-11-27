package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.EpgChannelEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.FamilyProfileEntity
import com.example.data.model.MatchEntryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.ProfileFavoriteEntity
import com.example.data.model.ProfileRecentEntity
import com.example.data.model.WatchStatEntity

@Database(
    entities = [
        ChannelEntity::class,
        PlaylistEntity::class,
        FamilyProfileEntity::class,
        ProfileFavoriteEntity::class,
        ProfileRecentEntity::class,
        WatchStatEntity::class,
        MatchEntryEntity::class,
        EpgChannelEntity::class,
        EpgProgramEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AshikaDatabase : RoomDatabase() {
    abstract fun ashikaDao(): AshikaDao

    companion object {
        @Volatile
        private var INSTANCE: AshikaDatabase? = null

        fun getInstance(context: Context): AshikaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AshikaDatabase::class.java,
                    "ashika_live_tv.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
