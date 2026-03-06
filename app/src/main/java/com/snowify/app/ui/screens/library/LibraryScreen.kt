package com.snowify.app.ui.screens.library
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.snowify.app.data.model.Playlist
import com.snowify.app.data.model.Song
import com.snowify.app.ui.components.*
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.LibraryViewModel
import com.snowify.app.viewmodel.PlayerViewModel
@Composable
fun LibraryScreen(
    onPlaylistClick: (String) -> Unit,
    onLikedSongsClick: () -> Unit = {},
    onSearch: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val likedSongs by libraryViewModel.likedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().background(colors.bgBase)
    ) {
        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Text("Your Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            SearchPill(onClick = onSearch)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            // Liked Songs section
            item {
                SectionHeader(title = "Liked Songs")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(colors.accentDim, colors.bgElevated)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onLikedSongsClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = colors.accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Liked Songs", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("${likedSongs.size} songs", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = colors.textSubdued)
                }
            }
            // Playlists section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Playlists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, "Create playlist", tint = colors.accent)
                    }
                }
            }
            if (playlists.isEmpty()) {
                item {
                    EmptyState(
                        message = "No playlists yet. Create one to organize your music.",
                        icon = { Icon(Icons.Filled.QueueMusic, null, tint = colors.textSubdued, modifier = Modifier.size(48.dp)) },
                    )
                }
            } else {
                items(playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                    )
                }
            }
        }
    }
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                )
            },
            confirmButton = {
                AccentButton(
                    text = "Create",
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            libraryViewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated,
        )
    }
}
@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    val colors = SnowifyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playlist.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = playlist.thumbnailUrl,
                contentDescription = playlist.title,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.size(56.dp).background(colors.bgHighlight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.QueueMusic, null, tint = colors.textSubdued, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(playlist.title, fontWeight = FontWeight.Medium, color = colors.textPrimary, maxLines = 1)
            val count = if (playlist.songs.isNotEmpty()) playlist.songs.size else playlist.trackCount
            Text(
                "$count songs",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.textSubdued)
    }
}
