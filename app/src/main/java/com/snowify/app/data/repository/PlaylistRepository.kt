package com.snowify.app.data.repository

import com.snowify.app.data.local.dao.PlaylistDao
import com.snowify.app.data.local.dao.SongDao
import com.snowify.app.data.local.entity.PlaylistEntity
import com.snowify.app.data.local.entity.PlaylistSongCrossRef
import com.snowify.app.data.model.Playlist
import com.snowify.app.data.model.Song
import com.snowify.app.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val firestoreService: FirestoreService,
) {
    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities -> entities.map { it.toPlaylist() } }
    suspend fun getPlaylistWithSongs(playlistId: String): Playlist? {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return null
        val songIds = playlistDao.getSongIdsForPlaylist(playlistId)
        val songs = songIds.mapNotNull { songDao.getSongById(it)?.toSong() }
        return entity.toPlaylist(songs)
    }
    suspend fun createPlaylist(title: String, ownerUid: String = ""): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            title = title,
            ownerUid = ownerUid,
            createdAt = System.currentTimeMillis(),
        )
        playlistDao.insertPlaylist(playlist.toEntity())
        return playlist
    }
    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.toEntity())
    }
    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylistById(playlistId)
    }
    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        songDao.insertSong(song.toEntity())
        val existingSongs = playlistDao.getSongIdsForPlaylist(playlistId)
        if (!existingSongs.contains(song.id)) {
            playlistDao.insertPlaylistSongCrossRef(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = song.id,
                    position = existingSongs.size,
                )
            )
        }
    }
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }
    suspend fun syncToFirestore(playlists: List<Playlist>, likedSongs: List<Song>) {
        try {
            firestoreService.cloudSave(playlists, likedSongs, emptyList(), emptyList<com.snowify.app.data.remote.FollowedArtist>())
        } catch (_: Exception) {}
    }

    suspend fun importPlaylist(playlist: Playlist) {
        android.util.Log.d("PlaylistRepo", "Importing playlist '${playlist.title}' with ${playlist.songs.size} songs (id=${playlist.id})")
        // Insert/update playlist entity
        val entity = playlist.toEntity().copy(trackCount = playlist.songs.size)
        playlistDao.insertPlaylist(entity)
        // Add songs to the playlist
        for ((index, song) in playlist.songs.withIndex()) {
            try {
                songDao.insertSong(song.toEntity())
                playlistDao.insertPlaylistSongCrossRef(
                    PlaylistSongCrossRef(
                        playlistId = playlist.id,
                        songId = song.id,
                        position = index,
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepo", "Failed to add song ${song.id} to playlist: ${e.message}")
            }
        }
        android.util.Log.d("PlaylistRepo", "Imported playlist '${playlist.title}': ${playlist.songs.size} songs added")
    }
}

fun PlaylistEntity.toPlaylist(songs: List<Song> = emptyList()) = Playlist(
    id = id, title = title, ownerUid = ownerUid,
    thumbnailUrl = thumbnailUrl, songs = songs, isPublic = isPublic,
    createdAt = createdAt, description = description, trackCount = trackCount,
)
fun Playlist.toEntity() = PlaylistEntity(
    id = id, title = title, ownerUid = ownerUid,
    thumbnailUrl = thumbnailUrl, isPublic = isPublic,
    createdAt = createdAt, description = description, trackCount = songs.size,
)
