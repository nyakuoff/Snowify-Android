package com.snowify.app.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.model.Playlist
import com.snowify.app.data.model.Song
import com.snowify.app.data.repository.PlaylistRepository
import com.snowify.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : ViewModel() {
    val playlists: Flow<List<Playlist>> = playlistRepository.getAllPlaylists()
    val likedSongs: Flow<List<Song>> = songRepository.getLikedSongs()
    fun createPlaylist(title: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(title)
        }
    }
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }
    fun toggleLike(song: Song) {
        viewModelScope.launch {
            songRepository.toggleLike(song)
        }
    }
}
