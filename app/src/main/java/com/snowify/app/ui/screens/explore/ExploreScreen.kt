package com.snowify.app.ui.screens.explore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowify.app.data.model.Song
import com.snowify.app.ui.components.*
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.ExploreUiState
import com.snowify.app.viewmodel.ExploreViewModel
import com.snowify.app.viewmodel.PlayerViewModel

@Composable
fun ExploreScreen(
    onSearch: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit = {},
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val uiState by exploreViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Text(
                "Explore",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(12.dp))
            SearchPill(onClick = onSearch)
        }

        when (val state = uiState) {
            is ExploreUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.accent)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading explore content...", color = colors.textSecondary)
                    }
                }
            }

            is ExploreUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("Failed to load", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { exploreViewModel.loadExploreFeed() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        ) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            is ExploreUiState.Success -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                    if (state.trending.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Trending")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.trending) { song ->
                                    AlbumCard(
                                        title = song.title,
                                        subtitle = song.artistName,
                                        thumbnailUrl = song.thumbnailUrl,
                                        onClick = {
                                            playerViewModel.playSong(song, state.trending)
                                            onSongClick(song)
                                        },
                                        onSubtitleClick = if (song.artistId.isNotBlank()) ({
                                            onArtistClick(song.artistId)
                                        }) else null,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (state.newReleases.isNotEmpty()) {
                        item {
                            SectionHeader(title = "New Releases")
                        }
                        items(state.newReleases) { song ->
                            SongCard(
                                song = song,
                                onClick = {
                                    playerViewModel.playSong(song, state.newReleases)
                                    onSongClick(song)
                                },
                                onArtistClick = if (song.artistId.isNotBlank()) ({
                                    onArtistClick(song.artistId)
                                }) else null,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }

                    if (state.trending.isEmpty() && state.newReleases.isEmpty()) {
                        item {
                            EmptyState(
                                message = "No explore content available",
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
