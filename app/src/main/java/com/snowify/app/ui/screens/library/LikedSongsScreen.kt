package com.snowify.app.ui.screens.library

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowify.app.data.model.Song
import com.snowify.app.ui.components.*
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.LibraryViewModel
import com.snowify.app.viewmodel.PlayerViewModel

@Composable
fun LikedSongsScreen(
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bgBase)
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
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Gradient heart icon
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(colors.accent, colors.accentHover)
                                ),
                                shape = RoundedCornerShape(20.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${likedSongs.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    if (likedSongs.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    playerViewModel.playSong(likedSongs.first(), likedSongs)
                                    onSongClick(likedSongs.first())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play All", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val shuffled = likedSongs.shuffled()
                                    playerViewModel.playSong(shuffled.first(), shuffled)
                                    onSongClick(shuffled.first())
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
            }

            // Song list
            if (likedSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, null, tint = colors.textSubdued, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No liked songs yet.\nTap the heart on any song to save it here.",
                            color = colors.textSubdued,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                itemsIndexed(likedSongs) { _, song ->
                    SongCard(
                        song = song,
                        onClick = {
                            playerViewModel.playSong(song, likedSongs)
                            onSongClick(song)
                        },
                        onArtistClick = if (song.artistId.isNotBlank()) ({
                            onArtistClick(song.artistId)
                        }) else null,
                        onLike = { libraryViewModel.toggleLike(song) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

