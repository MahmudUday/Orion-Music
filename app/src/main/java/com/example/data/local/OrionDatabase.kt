package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        ExcludedFolderEntity::class,
        SafFolderEntity::class,
        CustomLyricsEntity::class,
        CustomEqPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OrionDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun folderRuleDao(): FolderRuleDao
    abstract fun customLyricsDao(): CustomLyricsDao
    abstract fun customEqPresetDao(): CustomEqPresetDao

    companion object {
        @Volatile
        private var INSTANCE: OrionDatabase? = null

        fun getInstance(context: Context): OrionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OrionDatabase::class.java,
                    "orion_music_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
