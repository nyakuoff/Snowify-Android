package com.snowify.app.ui.navigation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.snowify.app.ui.screens.artist.ArtistScreen
import com.snowify.app.ui.screens.explore.ExploreScreen
import com.snowify.app.ui.screens.home.HomeScreen
import com.snowify.app.ui.screens.library.LibraryScreen
import com.snowify.app.ui.screens.library.LikedSongsScreen
import com.snowify.app.ui.screens.lyrics.LyricsScreen
import com.snowify.app.ui.screens.nowplaying.NowPlayingScreen
import com.snowify.app.ui.screens.onboarding.OnboardingScreen
import com.snowify.app.ui.screens.playlist.PlaylistDetailScreen
import com.snowify.app.ui.screens.queue.QueueScreen
import com.snowify.app.ui.screens.search.SearchScreen
import com.snowify.app.ui.screens.settings.SettingsScreen
@Composable
fun SnowifyNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen()
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onSearch = { navController.navigate(Screen.Search.route) },
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(Screen.Explore.route) {
            ExploreScreen(
                onSearch = { navController.navigate(Screen.Search.route) },
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onLikedSongsClick = { navController.navigate(Screen.LikedSongs.route) },
                onSearch = { navController.navigate(Screen.Search.route) },
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(
                onDismiss = { navController.popBackStack() },
                onOpenLyrics = { navController.navigate(Screen.Lyrics.route) },
                onOpenQueue = { navController.navigate(Screen.Queue.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(Screen.Lyrics.route) {
            LyricsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Queue.route) {
            QueueScreen(
                onBack = { navController.popBackStack() },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(Screen.LikedSongs.route) {
            LikedSongsScreen(
                onBack = { navController.popBackStack() },
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                },
            )
        }
        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                androidx.compose.material3.Text("Album: $albumId")
            }
        }
        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
        ) {
            ArtistScreen(
                onBack = { navController.popBackStack() },
                onSongClick = { navController.navigate(Screen.NowPlaying.route) },
            )
        }
    }
}
