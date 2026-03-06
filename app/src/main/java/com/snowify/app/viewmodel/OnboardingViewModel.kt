package com.snowify.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.local.UserPreferences
import com.snowify.app.data.repository.PlaylistRepository
import com.snowify.app.data.repository.SongRepository
import com.snowify.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {
    val onboardingComplete: StateFlow<Boolean> = userPreferences.onboardingCompleteFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Toast/notification messages
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val message: SharedFlow<String> = _message

    init {
        // If already signed in, load cloud state on startup
        if (userRepository.isSignedIn()) {
            viewModelScope.launch {
                performCloudSync("Startup")
            }
        }
    }

    private suspend fun performCloudSync(source: String) {
        try {
            val cloudState = userRepository.loadCloudState()
            if (cloudState != null) {
                songRepository.importCloudState(cloudState)
                // Import playlists
                for (pl in cloudState.playlists) {
                    try {
                        playlistRepository.importPlaylist(pl)
                    } catch (e: Exception) {
                        Log.e("OnboardingVM", "Failed to import playlist ${pl.title}: ${e.message}")
                    }
                }
                // Import followed artists
                if (cloudState.followedArtists.isNotEmpty()) {
                    userPreferences.setFollowedArtists(
                        cloudState.followedArtists.map {
                            UserPreferences.FollowedArtistLocal(
                                artistId = it.artistId,
                                name = it.name,
                                avatar = it.avatar,
                            )
                        }
                    )
                    Log.d("OnboardingVM", "$source: imported ${cloudState.followedArtists.size} followed artists")
                }
                Log.d("OnboardingVM", "$source cloud sync: ${cloudState.likedSongs.size} liked, ${cloudState.recentTracks.size} recent, ${cloudState.playlists.size} playlists, ${cloudState.followedArtists.size} followed")
                val total = cloudState.likedSongs.size + cloudState.recentTracks.size + cloudState.playlists.size
                if (total > 0) {
                    _message.tryEmit("Synced: ${cloudState.likedSongs.size} liked, ${cloudState.recentTracks.size} recent, ${cloudState.playlists.size} playlists")
                } else {
                    _message.tryEmit("No cloud data found")
                }
            } else {
                Log.w("OnboardingVM", "$source cloud sync: no data returned")
                if (source != "Startup") _message.tryEmit("No cloud data found")
            }
        } catch (e: Exception) {
            Log.e("OnboardingVM", "$source cloud sync failed", e)
            _message.tryEmit("Cloud sync failed: ${e.message?.take(40)}")
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = userRepository.signIn(email, password)
            result.onSuccess {
                performCloudSync("Sign-in")
                userPreferences.setOnboardingComplete(true)
            }.onFailure { e ->
                _error.value = friendlyAuthError(e)
            }
            _isLoading.value = false
        }
    }

    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = userRepository.createAccount(email, password)
            result.onSuccess {
                userPreferences.setOnboardingComplete(true)
            }.onFailure { e ->
                _error.value = friendlyAuthError(e)
            }
            _isLoading.value = false
        }
    }

    private fun friendlyAuthError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("wrong-password") || msg.contains("invalid-credential") ->
                "Incorrect email or password."
            msg.contains("user-not-found") || msg.contains("USER_NOT_FOUND") ->
                "No account found with that email."
            msg.contains("email-already-in-use") ->
                "An account with this email already exists."
            msg.contains("invalid-email") ->
                "Please enter a valid email address."
            msg.contains("weak-password") ->
                "Password must be at least 6 characters."
            msg.contains("network-request-failed") || msg.contains("NETWORK_ERROR") ->
                "Network error. Check your connection."
            msg.contains("INVALID_API_KEY") || msg.contains("invalid API key") ->
                "Firebase is not configured. Please add a valid google-services.json."
            msg.contains("app-not-authorized") || msg.contains("API key not valid") ->
                "This app is not authorized in Firebase. Add the SHA-1 fingerprint in the Firebase Console."
            else -> "Sign in failed: $msg"
        }
    }
    fun continueWithoutAccount() {
        viewModelScope.launch {
            userPreferences.setOnboardingComplete(true)
        }
    }
    fun clearError() {
        _error.value = null
    }
}
