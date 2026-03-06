package com.snowify.app.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowify.app.audio.MusicController
import com.snowify.app.data.model.PlaybackState
import com.snowify.app.data.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicController: MusicController,
) : ViewModel() {
    val playbackState: StateFlow<PlaybackState> = musicController.playbackState
    val currentSong: StateFlow<Song?> = musicController.currentSong
    val isPlaying: StateFlow<Boolean> = musicController.isPlaying
    val queue: StateFlow<List<Song>> = musicController.queue
    init {
        viewModelScope.launch {
            musicController.connectIfNeeded()
            musicController.startProgressUpdates()
        }
    }
    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        musicController.playSong(song, queue)
    }
    fun playPause() {
        if (musicController.isPlaying.value) {
            musicController.pause()
        } else {
            musicController.resume()
        }
    }
    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)
    fun skipToNext() = musicController.skipToNext()
    fun skipToPrevious() = musicController.skipToPrevious()
    fun toggleShuffle() = musicController.toggleShuffle()
    fun cycleRepeatMode() = musicController.cycleRepeatMode()
    fun setVolume(volume: Float) = musicController.setVolume(volume)
    fun addToQueue(song: Song) = musicController.addToQueue(song)
    fun addNext(song: Song) = musicController.addNext(song)
    fun removeFromQueue(index: Int) = musicController.removeFromQueue(index)
    fun clearQueue() = musicController.clearQueue()
}
