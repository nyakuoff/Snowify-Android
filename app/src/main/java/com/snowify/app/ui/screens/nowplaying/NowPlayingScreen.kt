package com.snowify.app.ui.screens.nowplaying
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.snowify.app.data.model.RepeatMode
import com.snowify.app.ui.components.LikeButton
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.util.toTimeString
import com.snowify.app.viewmodel.LibraryViewModel
import com.snowify.app.viewmodel.PlayerViewModel
@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val song = currentSong ?: return
    val isSongLiked = likedSongs.any { it.id == song.id || it.videoId == song.videoId }
    var showRemainingTime by remember { mutableStateOf(false) }
    val progress = if (playbackState.durationMs > 0)
        (playbackState.positionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f)
    else 0f
    // Scale animation for artwork
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "artwork_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.bgElevated,
                        colors.bgBase,
                        colors.bgBase,
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) onDismiss()
                }
            }
    ) {
        // Blurred backdrop
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .alpha(0.15f),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 24.dp)
                    .size(40.dp, 4.dp)
                    .background(colors.textSubdued, RoundedCornerShape(500.dp))
            )
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Minimize", tint = colors.textSecondary)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { /* More options */ }) {
                    Icon(Icons.Filled.MoreVert, "More", tint = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(24.dp))
            // Album artwork
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(300.dp)
                    .scale(artworkScale)
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(24.dp, RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(32.dp))
            // Song info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            if (song.artistId.isNotBlank()) onArtistClick(song.artistId)
                        },
                    )
                }
                LikeButton(
                    isLiked = isSongLiked,
                    onToggle = { libraryViewModel.toggleLike(song) },
                )
            }
            Spacer(Modifier.height(24.dp))
            // Progress bar
            Column {
                Slider(
                    value = progress,
                    onValueChange = { newProgress ->
                        playerViewModel.seekTo((newProgress * playbackState.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.bgHighlight,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = playbackState.positionMs.toTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSubdued,
                    )
                    Text(
                        text = if (showRemainingTime)
                            "-${(playbackState.durationMs - playbackState.positionMs).toTimeString()}"
                        else
                            playbackState.durationMs.toTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSubdued,
                        modifier = Modifier.clickable { showRemainingTime = !showRemainingTime },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle
                IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffled) colors.accent else colors.textSecondary,
                    )
                }
                // Previous
                IconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = colors.textPrimary, modifier = Modifier.size(36.dp))
                }
                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable { playerViewModel.playPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                // Next
                IconButton(
                    onClick = { playerViewModel.skipToNext() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.SkipNext, "Next", tint = colors.textPrimary, modifier = Modifier.size(36.dp))
                }
                // Repeat
                IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                    Icon(
                        imageVector = when (playbackState.repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) colors.accent else colors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            // Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.VolumeDown, contentDescription = null, tint = colors.textSubdued, modifier = Modifier.size(18.dp))
                Slider(
                    value = 0.8f,
                    onValueChange = { playerViewModel.setVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.textSecondary,
                        activeTrackColor = colors.textSecondary,
                        inactiveTrackColor = colors.bgHighlight,
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = colors.textSubdued, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(16.dp))
            // Bottom actions — large tappable buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    onClick = onOpenLyrics,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(500.dp),
                    color = colors.bgHighlight,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.MusicNote, "Lyrics", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lyrics", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    onClick = onOpenQueue,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(500.dp),
                    color = colors.bgHighlight,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlaylistPlay, "Queue", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Queue", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
