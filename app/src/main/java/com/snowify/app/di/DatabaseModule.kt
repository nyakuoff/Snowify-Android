package com.snowify.app.di
import android.content.Context
import androidx.room.Room
import com.snowify.app.data.local.AppDatabase
import com.snowify.app.data.local.UserPreferences
import com.snowify.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "snowify_db"
        ).fallbackToDestructiveMigration().build()
    }
    @Provides
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides
    fun provideRecentSongDao(db: AppDatabase): RecentSongDao = db.recentSongDao()
    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides
    fun provideLyricsDao(db: AppDatabase): LyricsDao = db.lyricsDao()
    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)
}
