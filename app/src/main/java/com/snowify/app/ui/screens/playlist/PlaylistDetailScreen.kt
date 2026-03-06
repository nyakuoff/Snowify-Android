package com.snowify.app.ui.screens.playlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.snowify.app.data.model.Song
import com.snowify.app.ui.components.SongCard
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.PlayerViewModel
import com.snowify.app.viewmodel.PlaylistDetailViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
    ) {
        Spacer(Modifier.height(24.dp))
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, "Delete", tint = colors.red)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (playlist == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist not found", color = colors.textSecondary)
            }
        } else {
            val pl = playlist!!
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                // Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Playlist cover (2x2 grid or single icon)
                        if (pl.songs.size >= 4) {
                            // 2x2 grid from first 4 song thumbnails
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                Column {
                                    Row(Modifier.weight(1f)) {
                                        for (i in 0..1) {
                                            AsyncImage(
                                                model = pl.songs[i].thumbnailUrl,
                                                contentDescription = null,
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                    Row(Modifier.weight(1f)) {
                                        for (i in 2..3) {
                                            AsyncImage(
                                                model = pl.songs[i].thumbnailUrl,
                                                contentDescription = null,
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (pl.songs.isNotEmpty() && pl.songs.first().thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = pl.songs.first().thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(colors.bgHighlight, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.MusicNote, null, tint = colors.textSubdued, modifier = Modifier.size(64.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = pl.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${pl.songs.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Play All
                            Button(
                                onClick = {
                                    if (pl.songs.isNotEmpty()) {
                                        playerViewModel.playSong(pl.songs.first(), pl.songs)
                                        onSongClick(pl.songs.first())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play All", fontWeight = FontWeight.SemiBold)
                            }
                            // Shuffle
                            OutlinedButton(
                                onClick = {
                                    if (pl.songs.isNotEmpty()) {
                                        val shuffled = pl.songs.shuffled()
                                        playerViewModel.playSong(shuffled.first(), shuffled)
                                        onSongClick(shuffled.first())
                                    }
                                },
                                border = BorderStroke(1.dp, colors.bgHighlight),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Filled.Shuffle, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Shuffle", color = colors.textPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Song list
                if (pl.songs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = colors.textSubdued, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No songs in this playlist yet", color = colors.textSubdued, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    itemsIndexed(pl.songs) { index, song ->
                        SongCard(
                            song = song,
                            onClick = {
                                playerViewModel.playSong(song, pl.songs)
                                onSongClick(song)
                            },
                            onArtistClick = if (song.artistId.isNotBlank()) ({
                                onArtistClick(song.artistId)
                            }) else null,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Playlist", color = colors.textPrimary) },
            text = { Text("Are you sure you want to delete \"${playlist?.title}\"?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePlaylist { onBack() }
                }) {
                    Text("Delete", color = colors.red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated,
        )
    }
}


