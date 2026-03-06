package com.snowify.app.ui.screens.artist

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.snowify.app.data.model.Song
import com.snowify.app.ui.components.*
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.ArtistUiState
import com.snowify.app.viewmodel.ArtistViewModel
import com.snowify.app.viewmodel.PlayerViewModel

@Composable
fun ArtistScreen(
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    artistViewModel: ArtistViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val uiState by artistViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }

            is ArtistUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(state.message, color = colors.red)
                    Spacer(Modifier.height(16.dp))
                    AccentButton(text = "Retry", onClick = { artistViewModel.loadArtist() })
                }
            }

            is ArtistUiState.Success -> {
                val artist = state.artist
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    // ── Banner + Back button ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                        ) {
                            // Banner image or gradient fallback
                            if (artist.bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = artist.bannerUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else if (artist.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = artist.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.3f,
                                )
                            }
                            // Gradient fade to background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                colors.bgBase.copy(alpha = 0.6f),
                                                colors.bgBase,
                                            ),
                                            startY = 100f,
                                        )
                                    ),
                            )
                            // Back button
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    "Back",
                                    tint = Color.White,
                                )
                            }
                            // Avatar + name overlapping bottom of banner
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AsyncImage(
                                    model = artist.thumbnailUrl,
                                    contentDescription = artist.name,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, colors.accent, CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "ARTIST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSubdued,
                                    letterSpacing = 2.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (artist.subscriberCount.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = artist.subscriberCount,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                        }
                    }

                    // ── Action buttons ──
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (artist.topSongs.isNotEmpty()) {
                                AccentButton(
                                    text = "Play",
                                    onClick = {
                                        playerViewModel.playSong(artist.topSongs.first(), artist.topSongs)
                                        onSongClick(artist.topSongs.first())
                                    },
                                )
                                Spacer(Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        val shuffled = artist.topSongs.shuffled()
                                        playerViewModel.playSong(shuffled.first(), shuffled)
                                        onSongClick(shuffled.first())
                                    },
                                    shape = RoundedCornerShape(500.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                ) {
                                    Icon(Icons.Filled.Shuffle, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                    }

                    // ── Popular Songs ──
                    if (artist.topSongs.isNotEmpty()) {
                        item { SectionHeader(title = "Popular") }
                        items(artist.topSongs.take(10)) { song ->
                            SongCard(
                                song = song,
                                onClick = {
                                    playerViewModel.playSong(song, artist.topSongs)
                                    onSongClick(song)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }

                    // ── Albums ──
                    if (artist.albums.isNotEmpty()) {
                        item { SectionHeader(title = "Albums") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(artist.albums) { album ->
                                    AlbumCard(
                                        title = album.title,
                                        subtitle = album.year.ifBlank { album.artistName },
                                        thumbnailUrl = album.thumbnailUrl,
                                        onClick = { /* navigate to album */ },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── Singles & EPs ──
                    if (artist.singles.isNotEmpty()) {
                        item { SectionHeader(title = "Singles & EPs") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(artist.singles) { single ->
                                    AlbumCard(
                                        title = single.title,
                                        subtitle = single.year.ifBlank { single.artistName },
                                        thumbnailUrl = single.thumbnailUrl,
                                        onClick = { /* navigate to album */ },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── About ──
                    if (artist.description.isNotBlank()) {
                        item { SectionHeader(title = "About") }
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            val isLong = artist.description.length > 150
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = artist.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    maxLines = if (expanded || !isLong) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isLong) {
                                    Text(
                                        text = if (expanded) "Show less" else "Show more",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accent,
                                        modifier = Modifier
                                            .clickable { expanded = !expanded }
                                            .padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

