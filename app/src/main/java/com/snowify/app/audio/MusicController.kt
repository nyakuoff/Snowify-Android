package com.snowify.app.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.snowify.app.data.model.*
import com.snowify.app.data.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("MusicCtrl", "Uncaught coroutine exception", throwable)
        isLoadingStream = false
        _toastMessage.tryEmit("Playback error: ${throwable.message?.take(50) ?: "Unknown"}")
    }
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val toastMessage: SharedFlow<String> = _toastMessage

    private var mediaController: MediaController? = null
    private var _currentQueue = mutableListOf<Song>()
    private var _originalQueue = mutableListOf<Song>()
    private var _queueIndex = 0
    private var _isShuffled = false
    private var _repeatMode = RepeatMode.OFF
    private var consecutiveFailures = 0
    @Volatile private var isLoadingStream = false

    @Volatile private var isConnecting = false

    suspend fun connectIfNeeded() {
        if (mediaController?.isConnected == true) return
        if (isConnecting) {
            // Wait for the other connect to finish
            var waitCount = 0
            while (isConnecting && waitCount < 50) {
                delay(100)
                waitCount++
            }
            if (mediaController?.isConnected == true) return
        }
        isConnecting = true
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )
            mediaController = MediaController.Builder(context, sessionToken).buildAsync().await()
            Log.d("MusicCtrl", "MediaController connected: ${mediaController?.isConnected}")

            mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    Log.d("MusicCtrl", "onPlaybackStateChanged: $state")
                    when (state) {
                        androidx.media3.common.Player.STATE_ENDED -> handleTrackEnded()
                        androidx.media3.common.Player.STATE_READY -> {
                            _isPlaying.value = mediaController?.isPlaying ?: false
                            updatePlaybackState()
                        }
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    updatePlaybackState()
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("MusicCtrl", "Player error: ${error.errorCodeName} — ${error.message}", error)
                    isLoadingStream = false
                    consecutiveFailures++
                    val errorMsg = error.message?.take(50) ?: "Unknown error"
                    _toastMessage.tryEmit("Playback error: $errorMsg")
                    // Stop playback gracefully instead of cascading to next track
                    _isPlaying.value = false
                    updatePlaybackState()
                }
            })
        } catch (e: Exception) {
            Log.e("MusicCtrl", "Failed to connect MediaController", e)
            _toastMessage.tryEmit("Failed to start player service")
        } finally {
            isConnecting = false
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        if (isLoadingStream) {
            Log.d("MusicCtrl", "playSong ignored — still loading previous stream")
            _toastMessage.tryEmit("Loading, please wait...")
            return
        }
        try {
            _currentSong.value = song
            _currentQueue = if (queue.isNotEmpty()) {
                val idx = queue.indexOfFirst { it.id == song.id || it.videoId == song.videoId }
                if (idx >= 0) queue.toMutableList()
                else (listOf(song) + queue).toMutableList()
            } else {
                mutableListOf(song)
            }
            _originalQueue = _currentQueue.toMutableList()
            _queueIndex = _currentQueue.indexOfFirst { it.id == song.id || it.videoId == song.videoId }.coerceAtLeast(0)
            updatePlaybackState()

            controllerScope.launch {
                try {
                    connectIfNeeded()
                    loadAndPlayCurrentSong()
                } catch (e: Exception) {
                    Log.e("MusicCtrl", "playSong launch failed", e)
                    isLoadingStream = false
                    _toastMessage.tryEmit("Error: ${e.message?.take(50)}")
                }
            }
        } catch (e: Exception) {
            Log.e("MusicCtrl", "playSong crashed", e)
            _toastMessage.tryEmit("Playback error: ${e.message?.take(50)}")
        }
    }

    private suspend fun loadAndPlayCurrentSong() {
        val song = _currentSong.value ?: return
        if (isLoadingStream) {
            Log.d("MusicCtrl", "Already loading a stream, ignoring duplicate request")
            return
        }
        isLoadingStream = true
        updatePlaybackState()

        try {
            Log.d("MusicCtrl", "Loading stream for: ${song.title} (${song.videoId})")

            // Fetch stream URL on IO thread
            val result = try {
                withContext(Dispatchers.IO) {
                    songRepository.getStreamUrl(song.videoId, true)
                }
            } catch (e: Exception) {
                Log.e("MusicCtrl", "getStreamUrl threw", e)
                Result.failure(e)
            }

            result.fold(
                onSuccess = { streamUrl ->
                    Log.d("MusicCtrl", "Got stream URL (${streamUrl.length} chars)")
                    consecutiveFailures = 0
                    isLoadingStream = false

                    try {
                        val mc = mediaController
                        if (mc == null || !mc.isConnected) {
                            // Try reconnecting once
                            connectIfNeeded()
                        }
                        val mc2 = mediaController
                        if (mc2 == null || !mc2.isConnected) {
                            Log.e("MusicCtrl", "MediaController not connected after retry")
                            _toastMessage.tryEmit("Player not connected, try again")
                            return
                        }

                        val streamUri = Uri.parse(streamUrl)
                        val mediaItem = MediaItem.Builder()
                            .setUri(streamUri)
                            .setMediaId(song.videoId)
                            .setRequestMetadata(
                                MediaItem.RequestMetadata.Builder()
                                    .setMediaUri(streamUri)
                                    .build()
                            )
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artistName)
                                    .setAlbumTitle(song.albumTitle)
                                    .setArtworkUri(
                                        if (song.thumbnailUrl.isNotBlank()) Uri.parse(song.thumbnailUrl)
                                        else null
                                    )
                                    .build()
                            )
                            .build()

                        mc2.stop()
                        mc2.clearMediaItems()
                        mc2.setMediaItem(mediaItem)
                        mc2.prepare()
                        mc2.play()
                        _isPlaying.value = true
                        updatePlaybackState()
                        Log.d("MusicCtrl", "Playback started for ${song.title}")
                    } catch (e: Exception) {
                        Log.e("MusicCtrl", "Failed to set media item", e)
                        _toastMessage.tryEmit("Playback failed: ${e.message?.take(50)}")
                    }

                    // Save to recently played (non-blocking)
                    controllerScope.launch(Dispatchers.IO) {
                        try { songRepository.addToRecentlyPlayed(song) } catch (_: Exception) {}
                    }

                    // Preload next track stream URL
                    preloadNextTrack()
                },
                onFailure = { error ->
                    Log.e("MusicCtrl", "Stream URL failed: ${error.message}", error)
                    isLoadingStream = false
                    consecutiveFailures++
                    _toastMessage.tryEmit("Can't play: ${error.message?.take(50) ?: "Unknown error"}")
                    _isPlaying.value = false
                    updatePlaybackState()
                }
            )
        } catch (e: Exception) {
            Log.e("MusicCtrl", "loadAndPlayCurrentSong crashed", e)
            isLoadingStream = false
            _toastMessage.tryEmit("Error: ${e.message?.take(50) ?: "Unknown"}")
        }
    }

    private fun preloadNextTrack() {
        val nextIndex = _queueIndex + 1
        if (nextIndex < _currentQueue.size) {
            val nextSong = _currentQueue[nextIndex]
            controllerScope.launch(Dispatchers.IO) {
                try {
                    Log.d("MusicCtrl", "Preloading next track: ${nextSong.title}")
                    songRepository.getStreamUrl(nextSong.videoId, true)
                    Log.d("MusicCtrl", "Preloaded next track stream URL")
                } catch (e: Exception) {
                    Log.w("MusicCtrl", "Preload failed for next track: ${e.message}")
                }
            }
        }
    }

    private fun handleTrackEnded() {
        controllerScope.launch {
            try {
                when (_repeatMode) {
                    RepeatMode.ONE -> {
                        connectIfNeeded()
                        loadAndPlayCurrentSong()
                    }
                    RepeatMode.ALL -> {
                        _queueIndex = if (_queueIndex < _currentQueue.size - 1) _queueIndex + 1 else 0
                        _currentSong.value = _currentQueue.getOrNull(_queueIndex)
                        updatePlaybackState()
                        connectIfNeeded()
                        loadAndPlayCurrentSong()
                    }
                    RepeatMode.OFF -> {
                        if (_queueIndex < _currentQueue.size - 1) {
                            _queueIndex++
                            _currentSong.value = _currentQueue.getOrNull(_queueIndex)
                            updatePlaybackState()
                            connectIfNeeded()
                            loadAndPlayCurrentSong()
                        } else {
                            _isPlaying.value = false
                            updatePlaybackState()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicCtrl", "handleTrackEnded crashed", e)
                _isPlaying.value = false
                isLoadingStream = false
                updatePlaybackState()
            }
        }
    }

    fun pause() {
        try { mediaController?.pause() } catch (_: Exception) {}
        _isPlaying.value = false
        updatePlaybackState()
    }

    fun resume() {
        try { mediaController?.play() } catch (_: Exception) {}
        _isPlaying.value = true
        updatePlaybackState()
    }

    fun seekTo(positionMs: Long) {
        try { mediaController?.seekTo(positionMs) } catch (_: Exception) {}
    }

    fun skipToNext() {
        if (_queueIndex < _currentQueue.size - 1) {
            _queueIndex++
        } else if (_repeatMode == RepeatMode.ALL && _currentQueue.isNotEmpty()) {
            _queueIndex = 0
        } else {
            return
        }
        _currentSong.value = _currentQueue.getOrNull(_queueIndex)
        updatePlaybackState()
        controllerScope.launch {
            try {
                connectIfNeeded()
                loadAndPlayCurrentSong()
            } catch (e: Exception) {
                Log.e("MusicCtrl", "skipToNext failed", e)
            }
        }
    }

    fun skipToPrevious() {
        val positionMs = try { mediaController?.currentPosition ?: 0 } catch (_: Exception) { 0L }
        if (positionMs > 3000) {
            try { mediaController?.seekTo(0) } catch (_: Exception) {}
        } else if (_queueIndex > 0) {
            _queueIndex--
            _currentSong.value = _currentQueue.getOrNull(_queueIndex)
            updatePlaybackState()
            controllerScope.launch {
                try {
                    connectIfNeeded()
                    loadAndPlayCurrentSong()
                } catch (e: Exception) {
                    Log.e("MusicCtrl", "skipToPrevious failed", e)
                }
            }
        }
    }

    fun toggleShuffle() {
        _isShuffled = !_isShuffled
        if (_isShuffled) {
            val current = _currentQueue.getOrNull(_queueIndex)
            val rest = _currentQueue.toMutableList().also {
                if (_queueIndex in it.indices) it.removeAt(_queueIndex)
            }.shuffled()
            _currentQueue = (if (current != null) listOf(current) + rest else rest).toMutableList()
            _queueIndex = 0
        } else {
            val cur = _currentQueue.getOrNull(_queueIndex)
            _currentQueue = _originalQueue.toMutableList()
            _queueIndex = _currentQueue.indexOfFirst { it.id == cur?.id }.coerceAtLeast(0)
        }
        updatePlaybackState()
    }

    fun cycleRepeatMode() {
        _repeatMode = when (_repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        updatePlaybackState()
    }

    fun setVolume(volume: Float) {
        try { mediaController?.volume = (volume * 0.5f).coerceIn(0f, 0.5f) } catch (_: Exception) {}
    }

    fun addToQueue(song: Song) {
        if (_currentQueue.none { it.id == song.id }) {
            _currentQueue.add(song)
            _toastMessage.tryEmit("Added to queue")
            updatePlaybackState()
        }
    }

    fun addNext(song: Song) {
        if (_currentQueue.none { it.id == song.id }) {
            _currentQueue.add(_queueIndex + 1, song)
            _toastMessage.tryEmit("Playing next")
            updatePlaybackState()
        }
    }

    fun removeFromQueue(index: Int) {
        if (index in _currentQueue.indices && index != _queueIndex) {
            _currentQueue.removeAt(index)
            if (index < _queueIndex) _queueIndex--
            updatePlaybackState()
        }
    }

    fun clearQueue() {
        val current = _currentQueue.getOrNull(_queueIndex)
        _currentQueue.clear()
        if (current != null) {
            _currentQueue.add(current)
            _queueIndex = 0
        }
        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        _queue.value = _currentQueue.toList()
        val pos = try { mediaController?.currentPosition ?: 0 } catch (_: Exception) { 0L }
        val dur = try { mediaController?.duration?.coerceAtLeast(0) ?: 0 } catch (_: Exception) { 0L }
        _playbackState.value = PlaybackState(
            song = _currentSong.value,
            isPlaying = _isPlaying.value,
            positionMs = pos,
            durationMs = dur,
            repeatMode = _repeatMode,
            isShuffled = _isShuffled,
            queue = _currentQueue.toList(),
            queueIndex = _queueIndex,
        )
    }

    fun startProgressUpdates() {
        controllerScope.launch {
            while (true) {
                delay(500)
                if (_isPlaying.value && mediaController?.isConnected == true) {
                    val pos = try { mediaController?.currentPosition ?: 0 } catch (_: Exception) { 0L }
                    val dur = try { mediaController?.duration?.coerceAtLeast(0) ?: 0 } catch (_: Exception) { 0L }
                    _playbackState.value = _playbackState.value.copy(positionMs = pos, durationMs = dur)
                }
            }
        }
    }
}
