package com.snowify.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.model.Song
import com.snowify.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(val trending: List<Song>, val newReleases: List<Song>) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val songRepository: SongRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState

    // Cache to avoid re-fetching on navigation
    private var cachedTrending: List<Song>? = null
    private var cachedNewReleases: List<Song>? = null
    private var cacheTimeMs = 0L
    private val cacheTtlMs = 10 * 60 * 1000L // 10 minutes

    init {
        loadExploreFeed()
    }

    fun loadExploreFeed(forceRefresh: Boolean = false) {
        // Return cached data if still valid
        if (!forceRefresh && cachedTrending != null && System.currentTimeMillis() - cacheTimeMs < cacheTtlMs) {
            _uiState.value = ExploreUiState.Success(
                trending = cachedTrending!!,
                newReleases = cachedNewReleases ?: emptyList(),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading
            try {
                // Run explore and home feed in parallel so fallback is instant
                val exploreDeferred = async {
                    try { songRepository.getExploreFeed() } catch (e: Exception) {
                        Log.w("ExploreVM", "Explore feed failed: ${e.message}")
                        emptyList()
                    }
                }
                val homeDeferred = async {
                    try { songRepository.getHomeFeed() } catch (e: Exception) {
                        Log.w("ExploreVM", "Home feed failed: ${e.message}")
                        null
                    }
                }

                val songs = exploreDeferred.await()
                val homeFeed = homeDeferred.await()

                val trending: List<Song>
                val newReleases: List<Song>

                if (songs.isNotEmpty()) {
                    trending = songs.take(20)
                    newReleases = songs.drop(20).take(20)
                } else if (homeFeed != null) {
                    val allSongs = homeFeed.quickPicks + homeFeed.recommended
                    trending = allSongs.take(20)
                    newReleases = allSongs.drop(20).take(20)
                } else {
                    trending = emptyList()
                    newReleases = emptyList()
                }

                cachedTrending = trending
                cachedNewReleases = newReleases
                cacheTimeMs = System.currentTimeMillis()

                _uiState.value = ExploreUiState.Success(
                    trending = trending,
                    newReleases = newReleases,
                )
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error(e.message ?: "Failed to load explore feed")
            }
        }
    }
}

