package com.snowify.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.local.UserPreferences
import com.snowify.app.data.model.Album
import com.snowify.app.data.model.HomeFeed
import com.snowify.app.data.model.Song
import com.snowify.app.data.repository.SongRepository
import com.snowify.app.util.getGreeting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val feed: HomeFeed, val greeting: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private var hasLoadedRecommendations = false
    private var hasLoadedNewReleases = false

    init {
        // Initial load — show recently played immediately
        loadHomeFeed()

        // Watch for recent songs changes (cloud sync). When they arrive and we haven't
        // loaded recommendations yet, reload to pick up the seed track.
        viewModelScope.launch {
            songRepository.getRecentSongs().collect { recent ->
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    // Always update the recently played section reactively
                    _uiState.value = current.copy(
                        feed = current.feed.copy(recentlyPlayed = recent),
                        greeting = getGreeting(),
                    )
                    // If we got new recent songs and never loaded recommendations, do it now
                    if (!hasLoadedRecommendations && recent.isNotEmpty()) {
                        loadRecommendations(recent)
                    }
                } else if (current is HomeUiState.Loading && recent.isNotEmpty()) {
                    // If still loading but we got recents, kickstart
                    loadHomeFeed()
                }
            }
        }

        // Watch for followed artists changes (cloud sync). When they arrive, load new releases.
        viewModelScope.launch {
            userPreferences.followedArtistsFlow.collect { artists ->
                if (!hasLoadedNewReleases && artists.isNotEmpty()) {
                    loadNewReleases(artists)
                }
            }
        }
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val recentSongsList = songRepository.getRecentSongs().first()
                val followedArtists = userPreferences.followedArtistsFlow.first()
                Log.d("HomeVM", "loadHomeFeed: recent=${recentSongsList.size} followed=${followedArtists.size}")

                // Fetch recommended songs (based on most recent track)
                val seedVideoId = recentSongsList.firstOrNull()?.videoId
                val recommended = if (seedVideoId != null) {
                    try {
                        val related = songRepository.getRelatedSongs(seedVideoId)
                        val recentIds = recentSongsList.map { it.videoId }.toSet()
                        hasLoadedRecommendations = true
                        related.filter { it.videoId !in recentIds }.take(10)
                    } catch (e: Exception) {
                        Log.e("HomeVM", "getRelatedSongs failed", e)
                        emptyList()
                    }
                } else emptyList()

                // Fetch new releases from followed artists
                val newReleases = if (followedArtists.isNotEmpty()) {
                    try {
                        val releases = fetchNewReleasesFromFollowed(followedArtists)
                        hasLoadedNewReleases = true
                        releases
                    } catch (e: Exception) {
                        Log.e("HomeVM", "fetchNewReleases failed", e)
                        emptyList()
                    }
                } else emptyList()

                Log.d("HomeVM", "Feed: recent=${recentSongsList.size} rec=${recommended.size} releases=${newReleases.size}")

                _uiState.value = HomeUiState.Success(
                    feed = HomeFeed(
                        recentlyPlayed = recentSongsList,
                        recommended = recommended,
                        newFromArtists = newReleases,
                    ),
                    greeting = getGreeting(),
                )
            } catch (e: Exception) {
                Log.e("HomeVM", "loadHomeFeed failed", e)
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    private fun loadRecommendations(recentSongs: List<Song>) {
        viewModelScope.launch {
            val seedVideoId = recentSongs.firstOrNull()?.videoId ?: return@launch
            try {
                Log.d("HomeVM", "Loading recommendations from seed: $seedVideoId")
                val related = songRepository.getRelatedSongs(seedVideoId)
                val recentIds = recentSongs.map { it.videoId }.toSet()
                val filtered = related.filter { it.videoId !in recentIds }.take(10)
                hasLoadedRecommendations = true
                Log.d("HomeVM", "Got ${filtered.size} recommendations")

                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    _uiState.value = current.copy(
                        feed = current.feed.copy(recommended = filtered)
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeVM", "loadRecommendations failed", e)
            }
        }
    }

    private fun loadNewReleases(artists: List<UserPreferences.FollowedArtistLocal>) {
        viewModelScope.launch {
            try {
                Log.d("HomeVM", "Loading new releases from ${artists.size} followed artists")
                val releases = fetchNewReleasesFromFollowed(artists)
                hasLoadedNewReleases = true
                Log.d("HomeVM", "Got ${releases.size} new releases")

                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    _uiState.value = current.copy(
                        feed = current.feed.copy(newFromArtists = releases)
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeVM", "loadNewReleases failed", e)
            }
        }
    }

    private suspend fun fetchNewReleasesFromFollowed(
        artists: List<UserPreferences.FollowedArtistLocal>
    ): List<Album> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val releases = mutableListOf<Album>()
        val seen = mutableSetOf<String>()

        val deferreds = artists.take(10).map { artist ->
            viewModelScope.async {
                try {
                    songRepository.getArtistInfo(artist.artistId)
                } catch (e: Exception) {
                    Log.w("HomeVM", "Failed to fetch artist ${artist.name}: ${e.message}")
                    null
                }
            }
        }

        val artistInfos = deferreds.awaitAll()
        for ((idx, info) in artistInfos.withIndex()) {
            if (info == null) continue
            val allAlbums = info.albums + info.singles
            for (album in allAlbums) {
                val year = album.year.toIntOrNull() ?: 0
                if (year >= currentYear && album.id !in seen) {
                    seen.add(album.id)
                    releases.add(album.copy(
                        artistName = info.name.ifBlank { artists.getOrNull(idx)?.name ?: "" }
                    ))
                }
            }
        }

        return releases.sortedByDescending { it.year.toIntOrNull() ?: 0 }
    }
}
