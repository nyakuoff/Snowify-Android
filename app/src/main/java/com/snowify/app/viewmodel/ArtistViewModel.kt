package com.snowify.app.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.model.Artist
import com.snowify.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ArtistUiState {
    object Loading : ArtistUiState()
    data class Success(val artist: Artist) : ArtistUiState()
    data class Error(val message: String) : ArtistUiState()
}

@HiltViewModel
class ArtistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
) : ViewModel() {

    private val artistId: String = savedStateHandle.get<String>("artistId") ?: ""

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState

    init {
        if (artistId.isNotBlank()) loadArtist()
    }

    fun loadArtist() {
        viewModelScope.launch {
            _uiState.value = ArtistUiState.Loading
            try {
                val artist = songRepository.getArtistInfo(artistId)
                Log.d("ArtistVM", "Loaded artist: ${artist.name}, songs=${artist.topSongs.size}, albums=${artist.albums.size}")
                _uiState.value = ArtistUiState.Success(artist)
            } catch (e: Exception) {
                Log.e("ArtistVM", "Failed to load artist", e)
                _uiState.value = ArtistUiState.Error(e.message ?: "Failed to load artist")
            }
        }
    }
}

