package com.snowify.app.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : ViewModel() {

    val playlistId: String = savedStateHandle["playlistId"] ?: ""

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pl = playlistRepository.getPlaylistWithSongs(playlistId)
                _playlist.value = pl
                Log.d("PlaylistDetailVM", "Loaded playlist: ${pl?.title}, ${pl?.songs?.size} songs")
            } catch (e: Exception) {
                Log.e("PlaylistDetailVM", "Failed to load playlist", e)
            }
            _isLoading.value = false
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
            loadPlaylist()
        }
    }

    fun deletePlaylist(onDone: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            onDone()
        }
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            songRepository.toggleLike(song)
        }
    }
}

