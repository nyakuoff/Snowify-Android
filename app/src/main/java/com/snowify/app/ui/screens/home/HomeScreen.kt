package com.snowify.app.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
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
import com.snowify.app.viewmodel.HomeUiState
import com.snowify.app.viewmodel.HomeViewModel
import com.snowify.app.viewmodel.PlayerViewModel

@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Spacer(Modifier.height(24.dp))
        // Top: greeting + search pill
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Text(
                text = if (uiState is HomeUiState.Success) (uiState as HomeUiState.Success).greeting else "Snowify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(12.dp))
            SearchPill(onClick = onSearch)
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }

            is HomeUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message, color = colors.red)
                    Spacer(Modifier.height(16.dp))
                    AccentButton(text = "Retry", onClick = { homeViewModel.loadHomeFeed() })
                }
            }

            is HomeUiState.Success -> {
                val feed = state.feed
                LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {


                    // ── Recently Played — always shown ──
                    item { SectionHeader(title = "Recently Played") }
                    if (feed.recentlyPlayed.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = colors.textSubdued,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Your recently played tracks will show up here.",
                                    color = colors.textSubdued,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "Start by searching for something!",
                                    color = colors.textSubdued,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(feed.recentlyPlayed.take(20)) { song ->
                                    AlbumCard(
                                        title = song.title,
                                        subtitle = song.artistName,
                                        thumbnailUrl = song.thumbnailUrl,
                                        onClick = {
                                            playerViewModel.playSong(song, feed.recentlyPlayed)
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

                    // ── New From Artists You Follow — only if data ──
                    if (feed.newFromArtists.isNotEmpty()) {
                        item { SectionHeader(title = "New From Artists You Follow") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(feed.newFromArtists) { album ->
                                    AlbumCard(
                                        title = album.title,
                                        subtitle = album.artistName,
                                        thumbnailUrl = album.thumbnailUrl,
                                        onClick = { /* navigate to album */ },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── Recommended Songs — only if data, shown as song list ──
                    if (feed.recommended.isNotEmpty()) {
                        item { SectionHeader(title = "Recommended Songs") }
                        items(feed.recommended.take(10)) { song ->
                            SongCard(
                                song = song,
                                onClick = {
                                    playerViewModel.playSong(song, feed.recommended)
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
    }
}
