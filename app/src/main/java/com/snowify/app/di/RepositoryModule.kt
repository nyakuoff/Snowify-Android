package com.snowify.app.di

import com.google.gson.Gson
import com.snowify.app.data.local.UserPreferences
import com.snowify.app.data.local.dao.*
import com.snowify.app.data.remote.FirestoreService
import com.snowify.app.data.remote.LrcLibService
import com.snowify.app.data.remote.NewPipeHelper
import com.snowify.app.data.remote.YTMusicApiService
import com.snowify.app.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSongRepository(
        songDao: SongDao,
        recentSongDao: RecentSongDao,
        ytMusicService: YTMusicApiService,
        newPipeHelper: NewPipeHelper,
    ): SongRepository = SongRepository(songDao, recentSongDao, ytMusicService, newPipeHelper)

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistDao: PlaylistDao,
        songDao: SongDao,
        firestoreService: FirestoreService,
    ): PlaylistRepository = PlaylistRepository(playlistDao, songDao, firestoreService)

    @Provides
    @Singleton
    fun provideLyricsRepository(
        lrcLibService: LrcLibService,
        lyricsDao: LyricsDao,
        gson: Gson,
    ): LyricsRepository = LyricsRepository(lrcLibService, lyricsDao, gson)
}

