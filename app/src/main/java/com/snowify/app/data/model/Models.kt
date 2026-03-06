package com.snowify.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String = "",
    val videoId: String = "",
    val title: String = "",
    val artistName: String = "",
    val artistId: String = "",
    val albumTitle: String = "",
    val albumId: String = "",
    val thumbnailUrl: String = "",
    val durationMs: Long = 0L,
    val isLiked: Boolean = false,
    val isLocal: Boolean = false,
    val year: String = "",
    val explicit: Boolean = false,
)

@Serializable
data class Album(
    val id: String = "",
    val browseId: String = "",
    val title: String = "",
    val artistName: String = "",
    val artistId: String = "",
    val year: String = "",
    val thumbnailUrl: String = "",
    val songs: List<Song> = emptyList(),
    val trackCount: Int = 0,
)

@Serializable
data class Artist(
    val id: String = "",
    val channelId: String = "",
    val name: String = "",
    val subscriberCount: String = "",
    val thumbnailUrl: String = "",
    val bannerUrl: String = "",
    val description: String = "",
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList(),
    val topSongs: List<Song> = emptyList(),
    val isFollowed: Boolean = false,
)

@Serializable
data class Playlist(
    val id: String = "",
    val title: String = "",
    val ownerUid: String = "",
    val thumbnailUrl: String = "",
    val songs: List<Song> = emptyList(),
    val isPublic: Boolean = false,
    val createdAt: Long = 0L,
    val description: String = "",
    val trackCount: Int = 0,
)

@Serializable
data class LyricLine(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val text: String = "",
)

@Serializable
data class LyricsResult(
    val synced: List<LyricLine> = emptyList(),
    val plain: String = "",
    val isInstrumental: Boolean = false,
    val hasSynced: Boolean = false,
)

@Serializable
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bannerUrl: String = "",
    val bio: String = "",
    val friendCode: String = "",
    val isListeningActivityEnabled: Boolean = true,
)

@Serializable
data class FriendActivity(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isOnline: Boolean = false,
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val lastSeen: Long = 0L,
)

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val videos: List<Song> = emptyList(),
)

data class HomeFeed(
    val quickPicks: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val newFromArtists: List<Album> = emptyList(),
    val recommended: List<Song> = emptyList(),
    val trending: List<Song> = emptyList(),
)

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
)

