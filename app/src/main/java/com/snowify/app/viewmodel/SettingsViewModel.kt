package com.snowify.app.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.local.UserPreferences
import com.snowify.app.data.local.dao.RecentSongDao
import com.snowify.app.data.local.dao.SearchHistoryDao
import com.snowify.app.data.repository.UserRepository
import com.snowify.app.util.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository,
    private val recentSongDao: RecentSongDao,
    private val searchHistoryDao: SearchHistoryDao,
) : ViewModel() {
    val theme: StateFlow<AppTheme> = userPreferences.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DARK)
    val animationsEnabled: StateFlow<Boolean> = userPreferences.animationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val effectsEnabled: StateFlow<Boolean> = userPreferences.effectsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val autoplayEnabled: StateFlow<Boolean> = userPreferences.autoplayFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val audioQuality: StateFlow<String> = userPreferences.audioQualityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "bestaudio")
    val country: StateFlow<String> = userPreferences.countryFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "US")
    val isSignedIn: StateFlow<Boolean> = userRepository.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setTheme(theme: AppTheme) = viewModelScope.launch { userPreferences.setTheme(theme) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setAnimations(enabled) }
    fun setEffectsEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setEffects(enabled) }
    fun setAutoplayEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setAutoplay(enabled) }
    fun setAudioQuality(quality: String) = viewModelScope.launch { userPreferences.setAudioQuality(quality) }
    fun setCountry(country: String) = viewModelScope.launch { userPreferences.setCountry(country) }
    fun clearPlayHistory() = viewModelScope.launch { recentSongDao.clearHistory() }
    fun clearSearchHistory() = viewModelScope.launch { searchHistoryDao.clearHistory() }
    fun signOut() = viewModelScope.launch { userRepository.signOut() }
}
