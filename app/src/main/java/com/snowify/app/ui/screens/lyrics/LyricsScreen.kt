package com.snowify.app.ui.screens.lyrics
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowify.app.ui.components.SnowifyTopBar
import com.snowify.app.ui.theme.SnowifyTheme
import com.snowify.app.viewmodel.LyricsViewModel
import com.snowify.app.viewmodel.PlayerViewModel
@Composable
fun LyricsScreen(
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
) {
    val colors = SnowifyTheme.colors
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by lyricsViewModel.lyrics.collectAsStateWithLifecycle()
    val isLoading by lyricsViewModel.isLoading.collectAsStateWithLifecycle()
    val currentLineIndex by lyricsViewModel.currentLineIndex.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(currentSong) {
        currentSong?.let { lyricsViewModel.loadLyrics(it) }
    }
    LaunchedEffect(playbackState.positionMs) {
        lyricsViewModel.updateCurrentLine(playbackState.positionMs)
    }
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = (currentLineIndex - 3).coerceAtLeast(0),
            )
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Spacer(Modifier.height(24.dp))
        SnowifyTopBar(
            title = currentSong?.title ?: "Lyrics",
            onBack = onBack,
        )
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            lyrics == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lyrics available", color = colors.textSubdued)
                }
            }
            lyrics!!.isInstrumental -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MusicNote, null, tint = colors.textSubdued, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("This track appears to be instrumental", color = colors.textSubdued, textAlign = TextAlign.Center)
                    }
                }
            }
            lyrics!!.hasSynced -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(lyrics!!.synced) { index, line ->
                        val isActive = index == currentLineIndex
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isActive) 20.sp else 16.sp,
                            color = if (isActive) colors.accent else colors.textSubdued,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerViewModel.seekTo(line.startMs)
                                }
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            }
            lyrics!!.plain.isNotBlank() -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = lyrics!!.plain,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lyrics found", color = colors.textSubdued)
                }
            }
        }
    }
}
