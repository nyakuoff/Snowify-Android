package com.snowify.app.data.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.snowify.app.data.local.dao.RecentSongDao
import com.snowify.app.data.local.dao.SongDao
import com.snowify.app.data.local.entity.RecentSongEntity
import com.snowify.app.data.local.entity.SongEntity
import com.snowify.app.data.model.*
import com.snowify.app.data.remote.CloudState
import com.snowify.app.data.remote.NewPipeHelper
import com.snowify.app.data.remote.YTMusicApiService
import com.snowify.app.data.remote.YTMusicParser
import com.snowify.app.util.toThumbnailUrl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val recentSongDao: RecentSongDao,
    private val ytMusicService: YTMusicApiService,
    private val newPipeHelper: NewPipeHelper,
) {
    fun getLikedSongs(): Flow<List<Song>> =
        songDao.getLikedSongs().map { entities -> entities.map { it.toSong() } }

    fun getRecentSongs(): Flow<List<Song>> =
        recentSongDao.getRecentSongs().map { entities -> entities.map { it.toSong() } }

    suspend fun toggleLike(song: Song) {
        val existing = songDao.getSongById(song.id)
        if (existing != null) {
            songDao.setLiked(song.id, !existing.isLiked)
        } else {
            songDao.insertSong(song.toEntity().copy(isLiked = true))
        }
    }

    suspend fun addToRecentlyPlayed(song: Song) {
        recentSongDao.insertRecentSong(
            RecentSongEntity(
                id = song.id,
                videoId = song.videoId,
                title = song.title,
                artistName = song.artistName,
                thumbnailUrl = song.thumbnailUrl,
                durationMs = song.durationMs,
            )
        )
        recentSongDao.trimHistory()
    }

    suspend fun importCloudState(cloudState: CloudState) {
        Log.d("SongRepo", "Importing cloud state: ${cloudState.likedSongs.size} liked, ${cloudState.recentTracks.size} recent, ${cloudState.playlists.size} playlists")
        // Import liked songs
        var likedCount = 0
        for (song in cloudState.likedSongs) {
            try {
                val existing = songDao.getSongById(song.id)
                if (existing == null) {
                    songDao.insertSong(song.toEntity().copy(isLiked = true))
                    likedCount++
                } else if (!existing.isLiked) {
                    songDao.setLiked(song.id, true)
                    likedCount++
                }
            } catch (e: Exception) {
                Log.e("SongRepo", "Failed to import liked song ${song.id}: ${e.message}")
            }
        }
        Log.d("SongRepo", "Imported $likedCount liked songs")
        // Import recent tracks — batch insert to avoid one-by-one Flow emissions
        val recentEntities = cloudState.recentTracks.mapNotNull { song ->
            try {
                RecentSongEntity(
                    id = song.id, videoId = song.videoId, title = song.title,
                    artistName = song.artistName, thumbnailUrl = song.thumbnailUrl,
                    durationMs = song.durationMs,
                )
            } catch (e: Exception) {
                Log.e("SongRepo", "Failed to map recent song ${song.id}: ${e.message}")
                null
            }
        }
        if (recentEntities.isNotEmpty()) {
            recentSongDao.insertRecentSongsBatch(recentEntities)
        }
        recentSongDao.trimHistory()
        Log.d("SongRepo", "Imported ${recentEntities.size} recent songs (batch)")
    }

    suspend fun getStreamUrl(videoId: String, preferBest: Boolean = true): Result<String> =
        newPipeHelper.getStreamUrl(videoId, preferBest)

    suspend fun getHomeFeed(): HomeFeed {
        val body = buildBrowseBody("FEmusic_home")
        val response = withTimeout(15_000) { ytMusicService.browse(body) }
        return parseHomeFeed(response)
    }

    suspend fun getExploreFeed(): List<Song> {
        val body = buildBrowseBody("FEmusic_explore")
        val response = withTimeout(15_000) { ytMusicService.browse(body) }
        return parseSongsFromBrowse(response)
    }

    suspend fun getRelatedSongs(videoId: String): List<Song> {
        return try {
            val body = buildNextBody(videoId)
            val response = withTimeout(10_000) { ytMusicService.next(body) }
            parseRelatedSongs(response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun search(query: String, musicOnly: Boolean = true): SearchResults {
        // Do parallel searches: one for songs, one for artists
        // YTMusic filter params:
        //   Songs:   EgWKAQIIAQ%3D%3D
        //   Artists: EgWKAQIgAQ%3D%3D
        //   Albums:  EgWKAQIYAQ%3D%3D
        return coroutineScope {
            val songsDeferred = async {
                try {
                    val body = buildSearchBody(query, "EgWKAQIIAQ%3D%3D")
                    val resp = withTimeout(15_000) { ytMusicService.search(body) }
                    parseSearchResults(resp)
                } catch (e: Exception) {
                    Log.w("SongRepo", "Song search failed: ${e.message}")
                    SearchResults()
                }
            }
            val artistsDeferred = async {
                try {
                    val body = buildSearchBody(query, "EgWKAQIgAQ%3D%3D")
                    val resp = withTimeout(15_000) { ytMusicService.search(body) }
                    parseSearchResults(resp)
                } catch (e: Exception) {
                    Log.w("SongRepo", "Artist search failed: ${e.message}")
                    SearchResults()
                }
            }
            val albumsDeferred = async {
                try {
                    val body = buildSearchBody(query, "EgWKAQIYAQ%3D%3D")
                    val resp = withTimeout(10_000) { ytMusicService.search(body) }
                    parseSearchResults(resp)
                } catch (e: Exception) {
                    Log.w("SongRepo", "Album search failed: ${e.message}")
                    SearchResults()
                }
            }

            val songResults = songsDeferred.await()
            val artistResults = artistsDeferred.await()
            val albumResults = albumsDeferred.await()

            SearchResults(
                songs = songResults.songs,
                artists = artistResults.artists.distinctBy { it.channelId },
                albums = albumResults.albums.ifEmpty { songResults.albums },
            )
        }
    }

    suspend fun getAlbumTracks(browseId: String): Album {
        return try {
            val body = buildBrowseBody(browseId)
            val response = ytMusicService.browse(body)
            parseAlbum(response, browseId)
        } catch (e: Exception) {
            Album(id = browseId, browseId = browseId)
        }
    }

    suspend fun getArtistInfo(channelId: String): Artist {
        val body = buildBrowseBody(channelId)
        val response = withTimeout(15_000) { ytMusicService.browse(body) }
        return parseArtist(response, channelId)
    }

    private fun buildContext() = """
        "context": {
            "client": {
                "clientName": "WEB_REMIX",
                "clientVersion": "1.20241015.01.00",
                "hl": "en",
                "gl": "US",
                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36,gzip(gfe)",
                "platform": "DESKTOP",
                "utcOffsetMinutes": 0
            }
        }
    """.trimIndent()

    private fun buildBrowseBody(browseId: String): RequestBody {
        val json = """
            {
                "browseId": "$browseId",
                ${buildContext()}
            }
        """.trimIndent()
        return json.toRequestBody("application/json".toMediaType())
    }

    private fun buildSearchBody(query: String, params: String?): RequestBody {
        val paramsField = if (params != null) """"params": "$params",""" else ""
        val json = """
            {
                "query": "$query",
                $paramsField
                ${buildContext()}
            }
        """.trimIndent()
        return json.toRequestBody("application/json".toMediaType())
    }

    private fun buildNextBody(videoId: String): RequestBody {
        val json = """
            {
                "videoId": "$videoId",
                "isAudioOnly": true,
                ${buildContext()}
            }
        """.trimIndent()
        return json.toRequestBody("application/json".toMediaType())
    }

    // ─── Parsers ─────────────────────────────────────────────────────────────

    private fun parseSearchResults(response: JsonObject): SearchResults {
        val songs = mutableListOf<Song>()
        val albums = mutableListOf<Album>()
        val artists = mutableListOf<Artist>()
        try {
            val sectionContents: JsonArray? = response
                .getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")
                ?: response.getAsJsonObject("contents")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents")

            sectionContents ?: return SearchResults(songs, albums, artists)

            for (section in sectionContents) {
                val sectionObj = section.asJsonObject

                // musicCardShelfRenderer — featured top result card
                val cardShelf = sectionObj.getAsJsonObject("musicCardShelfRenderer")
                if (cardShelf != null) {
                    val headerNav = cardShelf.getAsJsonObject("title")
                        ?.getAsJsonArray("runs")
                        ?.getOrNull(0)?.asJsonObject
                        ?.getAsJsonObject("navigationEndpoint")

                    // Check if top result is an artist (browseEndpoint with UC channel + music pageType)
                    val browseEndpoint = headerNav?.getAsJsonObject("browseEndpoint")
                    val browseId = browseEndpoint?.get("browseId")?.asString
                    val pageType = browseEndpoint
                        ?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                        ?.getAsJsonObject("browseEndpointContextMusicConfig")
                        ?.get("pageType")?.asString ?: ""
                    val isMusicArtist = pageType == "MUSIC_PAGE_TYPE_ARTIST" || pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL"

                    // Also check subtitle for "Artist" text
                    val subtitleRuns = cardShelf.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
                    val subtitleText = subtitleRuns?.let { runs ->
                        (0 until runs.size()).mapNotNull { i ->
                            runs.get(i)?.asJsonObject?.get("text")?.asString
                        }.joinToString("")
                    } ?: ""
                    val hasArtistLabel = subtitleText.contains("Artist", ignoreCase = true)

                    if (browseId != null && browseId.startsWith("UC") && (isMusicArtist || hasArtistLabel)) {
                        val name = cardShelf.getAsJsonObject("title")
                            ?.getAsJsonArray("runs")
                            ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: ""
                        val thumb = cardShelf.getAsJsonObject("thumbnail")
                            ?.getAsJsonObject("musicThumbnailRenderer")
                            ?.getAsJsonObject("thumbnail")
                            ?.getAsJsonArray("thumbnails")
                            ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
                        val subCount = subtitleRuns?.firstOrNull { run ->
                            val txt = run.asJsonObject.get("text")?.asString ?: ""
                            txt.contains("subscriber", ignoreCase = true)
                        }?.asJsonObject?.get("text")?.asString ?: ""
                        artists.add(Artist(id = browseId, channelId = browseId, name = name,
                            thumbnailUrl = thumb, subscriberCount = subCount))
                    } else if (browseId == null || !browseId.startsWith("UC")) {
                        // It's a song top result
                        val headerVideoId = headerNav?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString
                        if (headerVideoId != null) {
                            val title = cardShelf.getAsJsonObject("title")
                                ?.getAsJsonArray("runs")
                                ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: ""
                            val subtitleRuns = cardShelf.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
                            val artist = subtitleRuns?.firstOrNull { run ->
                                val txt = run.asJsonObject.get("text")?.asString ?: ""
                                txt.isNotBlank() && txt != " • " && txt.toLongOrNull() == null
                            }?.asJsonObject?.get("text")?.asString ?: ""
                            val thumb = cardShelf.getAsJsonObject("thumbnail")
                                ?.getAsJsonObject("musicThumbnailRenderer")
                                ?.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                                ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
                            songs.add(Song(id = headerVideoId, videoId = headerVideoId,
                                title = title, artistName = artist, thumbnailUrl = thumb.toThumbnailUrl(400)))
                        }
                    }
                    // Also parse contents list inside card shelf
                    cardShelf.getAsJsonArray("contents")?.forEach { item ->
                        val renderer = item.asJsonObject
                            .getAsJsonObject("musicResponsiveListItemRenderer")
                            ?: return@forEach
                        // Try artist first, then song
                        YTMusicParser.parseArtistFromRenderer(renderer)?.let { artists.add(it) }
                            ?: YTMusicParser.parseSongFromRenderer(renderer)?.let { songs.add(it) }
                    }
                    continue
                }

                // musicShelfRenderer — standard results shelf
                val shelf = sectionObj.getAsJsonObject("musicShelfRenderer") ?: continue
                val items = shelf.getAsJsonArray("contents") ?: continue
                val shelfTitle = shelf.getAsJsonObject("title")
                    ?.getAsJsonArray("runs")
                    ?.getOrNull(0)?.asJsonObject?.get("text")?.asString?.lowercase() ?: ""

                for (item in items) {
                    val renderer = item.asJsonObject
                        .getAsJsonObject("musicResponsiveListItemRenderer")
                        ?: item.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer")
                        ?: continue

                    // Check for top-level browseId and pageType to detect type
                    val topBrowseEndpoint = renderer.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("browseEndpoint")
                    val topBrowseId = topBrowseEndpoint?.get("browseId")?.asString
                    val topPageType = topBrowseEndpoint
                        ?.getAsJsonObject("browseEndpointContextSupportedConfigs")
                        ?.getAsJsonObject("browseEndpointContextMusicConfig")
                        ?.get("pageType")?.asString ?: ""
                    val isTopMusicArtist = topPageType == "MUSIC_PAGE_TYPE_ARTIST" || topPageType == "MUSIC_PAGE_TYPE_USER_CHANNEL"

                    when {
                        shelfTitle.contains("artist") || (topBrowseId != null && topBrowseId.startsWith("UC") && isTopMusicArtist) -> {
                            YTMusicParser.parseArtistFromRenderer(renderer)?.let {
                                Log.d("SongRepo", "  Parsed artist from shelf: ${it.name} (${it.channelId})")
                                artists.add(it)
                            }
                        }
                        shelfTitle.contains("album") || shelfTitle.contains("ep") || shelfTitle.contains("single") ->
                            YTMusicParser.parseAlbumFromRenderer(renderer)?.let { albums.add(it) }
                        else -> {
                            // Mixed shelf: try artist first, then song
                            val artistParsed = YTMusicParser.parseArtistFromRenderer(renderer)
                            if (artistParsed != null) {
                                Log.d("SongRepo", "  Parsed artist from mixed: ${artistParsed.name} (${artistParsed.channelId})")
                                artists.add(artistParsed)
                            } else {
                                YTMusicParser.parseSongFromRenderer(renderer)?.let { songs.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongRepo", "parseSearchResults failed", e)
        }
        Log.d("SongRepo", "Search results: ${songs.size} songs, ${albums.size} albums, ${artists.size} artists")
        if (artists.isNotEmpty()) {
            artists.forEach { Log.d("SongRepo", "  Artist: ${it.name} (${it.channelId}) thumb=${it.thumbnailUrl.take(60)}") }
        }
        // Deduplicate artists by channelId
        val uniqueArtists = artists.distinctBy { it.channelId }
        return SearchResults(songs = songs, albums = albums, artists = uniqueArtists)
    }

    private fun JsonArray.getOrNull(index: Int) =
        if (index in 0 until size()) get(index) else null

    private fun parseHomeFeed(response: JsonObject): HomeFeed {
        val allShelves = mutableListOf<List<Song>>()
        try {
            val contents = response.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")
                ?: response.getAsJsonObject("contents")
                    ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                    ?.getAsJsonArray("tabs")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("tabRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents")
                ?: return HomeFeed()

            for (section in contents) {
                val sectionObj = section.asJsonObject
                val shelf = sectionObj.getAsJsonObject("musicCarouselShelfRenderer")
                    ?: sectionObj.getAsJsonObject("musicImmersiveCarouselShelfRenderer")
                    ?: sectionObj.getAsJsonObject("musicShelfRenderer")
                    ?: continue

                val shelfSongs = mutableListOf<Song>()
                val items = shelf.getAsJsonArray("contents") ?: continue
                for (item in items) {
                    val itemObj = item.asJsonObject
                    val renderer = itemObj.getAsJsonObject("musicTwoRowItemRenderer")
                        ?: itemObj.getAsJsonObject("musicResponsiveListItemRenderer")
                        ?: continue
                    YTMusicParser.parseSongFromRenderer(renderer)?.let { shelfSongs.add(it) }
                }
                if (shelfSongs.isNotEmpty()) allShelves.add(shelfSongs)
            }
        } catch (_: Exception) {}

        // First shelf → quick picks (up to 12), subsequent shelves → recommended (up to 10)
        return HomeFeed(
            quickPicks = allShelves.getOrElse(0) { emptyList() }.take(12),
            recommended = allShelves.drop(1).flatten().take(10),
        )
    }

    private fun parseSongsFromBrowse(response: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            // Explore feed uses singleColumnBrowseResultsRenderer
            val contents = response.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")
                ?: response.getAsJsonObject("contents")
                    ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                    ?.getAsJsonArray("tabs")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("tabRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents")
                ?: return songs

            for (section in contents) {
                val sectionObj = section.asJsonObject
                val shelf = sectionObj.getAsJsonObject("musicCarouselShelfRenderer")
                    ?: sectionObj.getAsJsonObject("musicImmersiveCarouselShelfRenderer")
                    ?: sectionObj.getAsJsonObject("musicShelfRenderer")
                    ?: continue

                val items = shelf.getAsJsonArray("contents") ?: continue
                for (item in items) {
                    val itemObj = item.asJsonObject
                    val renderer = itemObj.getAsJsonObject("musicTwoRowItemRenderer")
                        ?: itemObj.getAsJsonObject("musicResponsiveListItemRenderer")
                        ?: continue
                    YTMusicParser.parseSongFromRenderer(renderer)?.let { songs.add(it) }
                }
            }
        } catch (_: Exception) {}
        return songs
    }

    private fun parseRelatedSongs(response: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val queue = response.getAsJsonObject("continuationContents")
                ?.getAsJsonObject("musicQueueRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("playlistPanelRenderer")
                ?.getAsJsonArray("contents")
                ?: response.getAsJsonObject("contents")
                    ?.getAsJsonObject("singleColumnMusicWatchNextResultsRenderer")
                    ?.getAsJsonObject("tabbedRenderer")
                    ?.getAsJsonObject("watchNextTabbedResultsRenderer")
                    ?.getAsJsonArray("tabs")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("tabRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("musicQueueRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("playlistPanelRenderer")
                    ?.getAsJsonArray("contents")
                ?: return songs

            for (item in queue) {
                val renderer = item.asJsonObject.getAsJsonObject("playlistPanelVideoRenderer") ?: continue
                val videoId = renderer.get("videoId")?.asString ?: continue
                val title = renderer.getAsJsonObject("title")
                    ?.getAsJsonArray("runs")
                    ?.get(0)?.asJsonObject?.get("text")?.asString ?: continue
                val artistName = renderer.getAsJsonObject("longBylineText")
                    ?.getAsJsonArray("runs")
                    ?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                val thumbnail = renderer.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject
                    ?.get("url")?.asString ?: ""
                songs.add(Song(id = videoId, videoId = videoId, title = title, artistName = artistName, thumbnailUrl = thumbnail))
            }
        } catch (_: Exception) {}
        return songs
    }

    private fun parseAlbum(response: JsonObject, browseId: String): Album {
        return Album(id = browseId, browseId = browseId)
    }

    private fun parseArtist(response: JsonObject, channelId: String): Artist {
        var name = ""
        var thumbnailUrl = ""
        var bannerUrl = ""
        var subscriberCount = ""
        var description = ""
        val topSongs = mutableListOf<Song>()
        val albums = mutableListOf<Album>()
        val singles = mutableListOf<Album>()

        try {
            // Header can be musicImmersiveHeaderRenderer or musicVisualHeaderRenderer
            val header = response.getAsJsonObject("header")
            val immersiveHeader = header?.getAsJsonObject("musicImmersiveHeaderRenderer")
            val visualHeader = header?.getAsJsonObject("musicVisualHeaderRenderer")
            val headerRenderer = immersiveHeader ?: visualHeader

            if (headerRenderer != null) {
                name = headerRenderer.getAsJsonObject("title")
                    ?.getAsJsonArray("runs")
                    ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: ""

                subscriberCount = headerRenderer.getAsJsonObject("subscriptionButton")
                    ?.getAsJsonObject("subscribeButtonRenderer")
                    ?.get("subscriberCountText")?.asJsonObject
                    ?.getAsJsonArray("runs")
                    ?.getOrNull(0)?.asJsonObject?.get("text")?.asString
                    ?: headerRenderer.getAsJsonObject("subscriptionButton")
                        ?.getAsJsonObject("subscribeButtonRenderer")
                        ?.get("subscriberCountWithSubscribeText")?.asJsonObject
                        ?.getAsJsonArray("runs")
                        ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: ""

                thumbnailUrl = headerRenderer.getAsJsonObject("thumbnail")
                    ?.getAsJsonObject("musicThumbnailRenderer")
                    ?.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")
                    ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""

                val bannerThumbs = immersiveHeader?.getAsJsonObject("thumbnail")
                    ?.getAsJsonObject("musicThumbnailRenderer")
                    ?.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")
                if (bannerThumbs != null && bannerThumbs.size() > 0) {
                    bannerUrl = bannerThumbs.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
                }

                description = headerRenderer.getAsJsonObject("description")
                    ?.getAsJsonArray("runs")
                    ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: ""
            }

            // Parse shelves from content
            val contents = response.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.getOrNull(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")

            if (contents != null) {
                for (section in contents) {
                    val sectionObj = section.asJsonObject
                    val shelf = sectionObj.getAsJsonObject("musicShelfRenderer")
                        ?: sectionObj.getAsJsonObject("musicCarouselShelfRenderer")
                        ?: continue

                    val shelfTitle = shelf.getAsJsonObject("header")
                        ?.getAsJsonObject("musicCarouselShelfBasicHeaderRenderer")
                        ?.getAsJsonObject("title")
                        ?.getAsJsonArray("runs")
                        ?.getOrNull(0)?.asJsonObject?.get("text")?.asString?.lowercase()
                        ?: shelf.getAsJsonObject("title")
                            ?.getAsJsonArray("runs")
                            ?.getOrNull(0)?.asJsonObject?.get("text")?.asString?.lowercase()
                        ?: ""

                    val items = shelf.getAsJsonArray("contents") ?: continue

                    when {
                        shelfTitle.contains("song") || topSongs.isEmpty() && shelfTitle.isEmpty() -> {
                            for (item in items) {
                                val renderer = item.asJsonObject
                                    .getAsJsonObject("musicResponsiveListItemRenderer")
                                    ?: item.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer")
                                    ?: continue
                                YTMusicParser.parseSongFromRenderer(renderer)?.let { topSongs.add(it) }
                            }
                        }
                        shelfTitle.contains("album") -> {
                            for (item in items) {
                                val renderer = item.asJsonObject
                                    .getAsJsonObject("musicTwoRowItemRenderer") ?: continue
                                YTMusicParser.parseAlbumFromRenderer(renderer)?.let { albums.add(it) }
                            }
                        }
                        shelfTitle.contains("single") -> {
                            for (item in items) {
                                val renderer = item.asJsonObject
                                    .getAsJsonObject("musicTwoRowItemRenderer") ?: continue
                                YTMusicParser.parseAlbumFromRenderer(renderer)?.let { singles.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongRepo", "parseArtist failed", e)
        }

        return Artist(
            id = channelId,
            channelId = channelId,
            name = name,
            subscriberCount = subscriberCount,
            thumbnailUrl = thumbnailUrl,
            bannerUrl = bannerUrl,
            description = description,
            topSongs = topSongs,
            albums = albums,
            singles = singles,
        )
    }
}

fun SongEntity.toSong() = Song(
    id = id, videoId = videoId, title = title, artistName = artistName,
    artistId = artistId, albumTitle = albumTitle, albumId = albumId,
    thumbnailUrl = thumbnailUrl, durationMs = durationMs, isLiked = isLiked, year = year,
)

fun Song.toEntity() = SongEntity(
    id = id, videoId = videoId, title = title, artistName = artistName,
    artistId = artistId, albumTitle = albumTitle, albumId = albumId,
    thumbnailUrl = thumbnailUrl, durationMs = durationMs, isLiked = isLiked, year = year,
)

fun RecentSongEntity.toSong() = Song(
    id = id, videoId = videoId, title = title, artistName = artistName,
    thumbnailUrl = thumbnailUrl, durationMs = durationMs,
)
