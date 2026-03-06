package com.snowify.app.ui.screens.queue
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.snowify.app.ui.components.SnowifyTopBar
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.PlayerViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Spacer(Modifier.height(24.dp))
        SnowifyTopBar(
            title = "Queue",
            onBack = onBack,
            actions = {
                TextButton(onClick = { playerViewModel.clearQueue() }) {
                    Text("Clear", color = colors.accent)
                }
            }
        )
        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QueueMusic, null, tint = colors.textSubdued, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Queue is empty", color = colors.textSubdued)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    Text(
                        "Up Next",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                itemsIndexed(queue) { index, song ->
                    val isCurrent = song.id == currentSong?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrent) colors.accentDim else colors.bgBase)
                            .clickable { playerViewModel.playSong(song) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) colors.accent else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                song.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (song.artistId.isNotBlank()) colors.accent else colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (song.artistId.isNotBlank()) Modifier.clickable { onArtistClick(song.artistId) } else Modifier,
                            )
                        }
                        if (isCurrent) {
                            Icon(Icons.Filled.VolumeUp, "Now Playing", tint = colors.accent, modifier = Modifier.size(20.dp))
                        } else {
                            IconButton(
                                onClick = { playerViewModel.removeFromQueue(index) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Filled.Close, "Remove", tint = colors.textSubdued, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
