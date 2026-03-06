package com.snowify.app.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Search : Screen("search")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object NowPlaying : Screen("now_playing")
    object Lyrics : Screen("lyrics")
    object Queue : Screen("queue")
    object LikedSongs : Screen("liked_songs")

    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }

    object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    object ArtistDetail : Screen("artist/{artistId}") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }

    object FriendProfile : Screen("friend/{friendUid}") {
        fun createRoute(friendUid: String) = "friend/$friendUid"
    }
}

