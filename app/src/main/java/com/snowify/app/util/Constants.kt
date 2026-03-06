package com.snowify.app.util

object Constants {
    const val YTMUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1/"
    const val LRCLIB_BASE_URL = "https://lrclib.net/api/"
    const val STREAM_URL_CACHE_TTL_MS = 4 * 60 * 60 * 1000L // 4 hours
    const val NEW_RELEASES_CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    const val MAX_SEARCH_HISTORY = 5
    const val MAX_RECENT_SONGS = 50
    const val AUTOPLAY_MAX_TRACKS = 20
    const val CONSECUTIVE_FAILURE_LIMIT = 5
    const val STATE_DEBOUNCE_MS = 300L
    const val SEARCH_DEBOUNCE_MS = 250L
    const val LYRICS_SYNC_INTERVAL_MS = 100L
    const val VOLUME_CAP = 0.5f
    const val YTMUSIC_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
}
