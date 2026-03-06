package com.snowify.app.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.data.model.LyricsResult
import com.snowify.app.data.model.Song
import com.snowify.app.data.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {
    private val _lyrics = MutableStateFlow<LyricsResult?>(null)
    val lyrics: StateFlow<LyricsResult?> = _lyrics
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex
    fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _isLoading.value = true
            _lyrics.value = lyricsRepository.getLyrics(
                songId = song.id,
                title = song.title,
                artist = song.artistName,
                durationMs = song.durationMs,
            )
            _isLoading.value = false
        }
    }
    fun updateCurrentLine(positionMs: Long) {
        val lyricsResult = _lyrics.value ?: return
        if (!lyricsResult.hasSynced) return
        val lines = lyricsResult.synced
        val index = lines.indexOfLast { it.startMs <= positionMs }
        if (index != _currentLineIndex.value) {
            _currentLineIndex.value = index
        }
    }
}
