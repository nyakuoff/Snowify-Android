package com.snowify.app.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val title: String,
    val artistName: String,
    val artistId: String,
    val albumTitle: String,
    val albumId: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val isLiked: Boolean = false,
    val year: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val ownerUid: String,
    val thumbnailUrl: String,
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String = "",
    val trackCount: Int = 0,
)
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val position: Int,
)
@Entity(tableName = "recent_songs")
data class RecentSongEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val title: String,
    val artistName: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val playedAt: Long = System.currentTimeMillis(),
)
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)
@Entity(tableName = "cached_albums")
data class CachedAlbumEntity(
    @PrimaryKey val id: String,
    val browseId: String,
    val title: String,
    val artistName: String,
    val artistId: String,
    val year: String,
    val thumbnailUrl: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
@Entity(tableName = "cached_lyrics")
data class CachedLyricsEntity(
    @PrimaryKey val songId: String,
    val syncedJson: String,
    val plainText: String,
    val isInstrumental: Boolean,
    val hasSynced: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
