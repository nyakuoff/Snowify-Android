package com.snowify.app.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
import com.snowify.app.data.local.dao.*
import com.snowify.app.data.local.entity.*
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentSongEntity::class,
        SearchHistoryEntity::class,
        CachedAlbumEntity::class,
        CachedLyricsEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentSongDao(): RecentSongDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun lyricsDao(): LyricsDao
}
