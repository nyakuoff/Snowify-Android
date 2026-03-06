package com.snowify.app.ui.screens.search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.snowify.app.data.model.Song
import com.snowify.app.ui.components.*
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.PlayerViewModel
import com.snowify.app.viewmodel.SearchViewModel
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    onBack: () -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val query by searchViewModel.searchQuery.collectAsStateWithLifecycle()
    val results by searchViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by searchViewModel.isSearching.collectAsStateWithLifecycle()
    val history by searchViewModel.searchHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
    ) {
        Spacer(Modifier.height(24.dp))
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = colors.textPrimary)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { searchViewModel.setQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Search...", color = colors.textSubdued) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.bgHighlight,
                    cursorColor = colors.accent,
                    focusedContainerColor = colors.bgElevated,
                    unfocusedContainerColor = colors.bgElevated,
                ),
                shape = RoundedCornerShape(500.dp),
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { searchViewModel.clearQuery() }) {
                            Icon(Icons.Filled.Clear, "Clear", tint = colors.textSubdued)
                        }
                    }
                },
            )
        }
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (query.isBlank()) {
            // Show search history
            if (history.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Recent searches", style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
                        TextButton(onClick = { searchViewModel.clearHistory() }) {
                            Text("Clear all", color = colors.accent)
                        }
                    }
                    history.forEach { historyQuery ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchViewModel.setQuery(historyQuery)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.History, null, tint = colors.textSubdued, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(historyQuery, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { searchViewModel.deleteFromHistory(historyQuery) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Close, "Remove", tint = colors.textSubdued, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                EmptyState(
                    message = "Search for songs, artists, and albums",
                    icon = { Icon(Icons.Filled.Search, null, tint = colors.textSubdued, modifier = Modifier.size(48.dp)) },
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                // Artists section — horizontal scrollable panel
                if (results.artists.isNotEmpty()) {
                    item { SectionHeader(title = "Artists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(results.artists) { artist ->
                                Column(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable {
                                            searchViewModel.saveToHistory(query)
                                            onArtistClick(artist.channelId)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = artist.name,
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (artist.subscriberCount.isNotBlank()) {
                                        Text(
                                            text = artist.subscriberCount,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    } else {
                                        Text(
                                            text = "Artist",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                // Songs section
                if (results.songs.isNotEmpty()) {
                    item { SectionHeader(title = "Songs") }
                    items(results.songs) { song ->
                        SongCard(
                            song = song,
                            onClick = {
                                searchViewModel.saveToHistory(query)
                                playerViewModel.playSong(song, results.songs)
                                onSongClick(song)
                            },
                            onArtistClick = if (song.artistId.isNotBlank()) ({
                                searchViewModel.saveToHistory(query)
                                onArtistClick(song.artistId)
                            }) else null,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                // Albums section
                if (results.albums.isNotEmpty()) {
                    item { SectionHeader(title = "Albums") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(results.albums) { album ->
                                AlbumCard(
                                    title = album.title,
                                    subtitle = album.artistName,
                                    thumbnailUrl = album.thumbnailUrl,
                                    onClick = { searchViewModel.saveToHistory(query) },
                                )
                            }
                        }
                    }
                }
                if (results.songs.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No results for \"$query\"",
                            modifier = Modifier.padding(top = 48.dp),
                        )
                    }
                }
            }
        }
    }
}
