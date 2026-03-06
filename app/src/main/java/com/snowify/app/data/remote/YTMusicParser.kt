package com.snowify.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.snowify.app.data.model.*
import com.snowify.app.util.toThumbnailUrl

object YTMusicParser {

    /**
     * Parses a Song from:
     *  - musicResponsiveListItemRenderer  (home quick-picks, search results)
     *  - musicTwoRowItemRenderer          (carousel items — albums/playlists, no direct videoId)
     */
    fun parseSongFromRenderer(renderer: JsonObject): Song? {
        return try {
            if (renderer.has("thumbnailRenderer")) {
                parseTwoRowItem(renderer)
            } else {
                parseResponsiveListItem(renderer)
            }
        } catch (_: Exception) {
            null
        }
    }

    // ─── musicResponsiveListItemRenderer ──────────────────────────────────────
    // videoId lives in playlistItemData.videoId  OR  overlay.content.musicPlayButtonRenderer.playNavigationEndpoint.watchEndpoint.videoId
    private fun parseResponsiveListItem(r: JsonObject): Song? {
        // Primary path: playlistItemData.videoId  (home feed)
        val videoId = r.getAsJsonObject("playlistItemData")?.get("videoId")?.asString
            // Fallback path: overlay → content → musicPlayButtonRenderer → playNavigationEndpoint → watchEndpoint → videoId  (search)
            ?: r.getAsJsonObject("overlay")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("musicPlayButtonRenderer")
                ?.getAsJsonObject("playNavigationEndpoint")
                ?.getAsJsonObject("watchEndpoint")
                ?.get("videoId")?.asString
            ?: return null

        val flexCols = r.getAsJsonArray("flexColumns") ?: return null

        val title = flexCols[0]?.asJsonObject
            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs")
            ?.get(0)?.asJsonObject?.get("text")?.asString
            ?: return null

        // Artist: prefer runs with browseEndpoint navigation (actual artist names)
        val artistRuns = flexCols.getOrNull(1)?.asJsonObject
            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs")

        val artistName = artistRuns?.let { runs ->
            // First try: collect only runs with browseEndpoint (artist links)
            val browseParts = runs.mapNotNull { run ->
                val obj = run.asJsonObject
                val text = obj.get("text")?.asString ?: return@mapNotNull null
                if (text.isBlank() || text == " • ") return@mapNotNull null
                val nav = obj.getAsJsonObject("navigationEndpoint")
                if (nav?.has("browseEndpoint") == true) text else null
            }
            if (browseParts.isNotEmpty()) browseParts.joinToString(", ")
            else {
                // Fallback: first run that isn't a bullet or play-count
                runs.firstOrNull { run ->
                    val text = run.asJsonObject.get("text")?.asString ?: ""
                    text.isNotBlank() && text != " • " &&
                        !text.endsWith("plays") && !text.endsWith("views") &&
                        text.toLongOrNull() == null
                }?.asJsonObject?.get("text")?.asString ?: ""
            }
        } ?: ""

        // Extract artist browseId for navigation
        val artistId = artistRuns?.firstNotNullOfOrNull { run ->
            run.asJsonObject.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")
                ?.get("browseId")?.asString
        } ?: ""

        val thumbnailUrl = r.getAsJsonObject("thumbnail")
            ?.getAsJsonObject("musicThumbnailRenderer")
            ?.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")
            ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""

        val durationText = r.getAsJsonArray("fixedColumns")
            ?.getOrNull(0)?.asJsonObject
            ?.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs")
            ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: "0:00"

        return Song(
            id = videoId,
            videoId = videoId,
            title = title,
            artistName = artistName,
            artistId = artistId,
            thumbnailUrl = thumbnailUrl.toThumbnailUrl(400),
            durationMs = parseDurationToMs(durationText),
        )
    }

    // ─── musicTwoRowItemRenderer ───────────────────────────────────────────────
    // Used in explore/carousel shelves; navigationEndpoint may be browse (album) or watch (song)
    private fun parseTwoRowItem(r: JsonObject): Song? {
        val videoId = r.getAsJsonObject("navigationEndpoint")
            ?.getAsJsonObject("watchEndpoint")
            ?.get("videoId")?.asString
            ?: r.getAsJsonObject("onTap")
                ?.getAsJsonObject("watchEndpoint")
                ?.get("videoId")?.asString
            ?: return null   // if no watchEndpoint, it's an album/playlist, skip

        val title = r.getAsJsonObject("title")
            ?.getAsJsonArray("runs")
            ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: return null

        val subtitleRuns = r.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
        val artistName = subtitleRuns?.let { runs ->
            runs.firstOrNull { run ->
                val text = run.asJsonObject.get("text")?.asString ?: ""
                text.isNotBlank() && text != " • " && text.toLongOrNull() == null &&
                    !text.endsWith("plays") && !text.endsWith("views")
            }?.asJsonObject?.get("text")?.asString
        } ?: ""

        val artistId = subtitleRuns?.firstNotNullOfOrNull { run ->
            run.asJsonObject.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")
                ?.get("browseId")?.asString
        } ?: ""

        val thumbnailUrl = r.getAsJsonObject("thumbnailRenderer")
            ?.getAsJsonObject("musicThumbnailRenderer")
            ?.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")
            ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""

        return Song(
            id = videoId,
            videoId = videoId,
            title = title,
            artistName = artistName,
            artistId = artistId,
            thumbnailUrl = thumbnailUrl.toThumbnailUrl(400),
            durationMs = 0L,
        )
    }

    fun parseAlbumFromRenderer(renderer: JsonObject): Album? {
        return try {
            val browseId = renderer.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")
                ?.get("browseId")?.asString ?: return null

            val title = renderer.getAsJsonObject("title")
                ?.getAsJsonArray("runs")
                ?.getOrNull(0)?.asJsonObject?.get("text")?.asString ?: ""

            val subtitleRuns = renderer.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
            val artistName = subtitleRuns?.firstOrNull { run ->
                val text = run.asJsonObject.get("text")?.asString ?: ""
                text.isNotBlank() && text != " • " && text.toLongOrNull() == null
            }?.asJsonObject?.get("text")?.asString ?: ""

            val thumbnailUrl = renderer.getAsJsonObject("thumbnailRenderer")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.lastOrNull()?.asJsonObject?.get("url")?.asString
                ?: renderer.getAsJsonObject("thumbnail")
                    ?.getAsJsonObject("musicThumbnailRenderer")
                    ?.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")
                    ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""

            Album(id = browseId, browseId = browseId, title = title, artistName = artistName,
                thumbnailUrl = thumbnailUrl.toThumbnailUrl(400))
        } catch (_: Exception) { null }
    }

    fun parseArtistFromRenderer(renderer: JsonObject): Artist? {
        return try {
            // Path 1: musicTwoRowItemRenderer (carousel items)
            val browseEndpoint = renderer.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")
            // Path 2: musicResponsiveListItemRenderer (search results)
            val browseEndpoint2 = renderer.getAsJsonArray("flexColumns")
                ?.getOrNull(0)?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.getOrNull(0)?.asJsonObject
                ?.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")

            val activeBrowse = browseEndpoint ?: browseEndpoint2 ?: return null
            val channelId = activeBrowse.get("browseId")?.asString ?: return null

            // Must be a channel ID (starts with UC)
            if (!channelId.startsWith("UC")) return null

            // ── Key filter: check pageType to ensure this is a music artist ──
            // YouTube Music sets pageType = "MUSIC_PAGE_TYPE_ARTIST" or "MUSIC_PAGE_TYPE_USER_CHANNEL"
            // for real artists. Regular YouTube channels don't have this.
            val pageType = activeBrowse
                .getAsJsonObject("browseEndpointContextSupportedConfigs")
                ?.getAsJsonObject("browseEndpointContextMusicConfig")
                ?.get("pageType")?.asString ?: ""

            val isMusicArtist = pageType == "MUSIC_PAGE_TYPE_ARTIST" || pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL"

            // Also check subtitle text: YTMusic search results include "Artist" in the second flex column
            val subtitleRuns = renderer.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
                ?: renderer.getAsJsonArray("flexColumns")
                    ?.getOrNull(1)?.asJsonObject
                    ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")

            val subtitleText = subtitleRuns?.let { runs ->
                (0 until runs.size()).mapNotNull { i ->
                    runs.get(i)?.asJsonObject?.get("text")?.asString
                }.joinToString("")
            } ?: ""

            val hasArtistLabel = subtitleText.contains("Artist", ignoreCase = true)

            // Reject if neither pageType nor subtitle confirm this is a music artist
            if (!isMusicArtist && !hasArtistLabel) return null

            // Name: from title.runs or flexColumns
            val name = renderer.getAsJsonObject("title")
                ?.getAsJsonArray("runs")
                ?.getOrNull(0)?.asJsonObject?.get("text")?.asString
                ?: renderer.getAsJsonArray("flexColumns")
                    ?.getOrNull(0)?.asJsonObject
                    ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                    ?.getOrNull(0)?.asJsonObject?.get("text")?.asString
                ?: ""

            if (name.isBlank()) return null

            // Subscriber count from subtitle
            val subscriberCount = subtitleRuns?.firstOrNull { run ->
                val text = run.asJsonObject.get("text")?.asString ?: ""
                text.contains("subscriber", ignoreCase = true)
            }?.asJsonObject?.get("text")?.asString ?: ""

            // Thumbnail
            val thumbnailUrl = renderer.getAsJsonObject("thumbnailRenderer")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.lastOrNull()?.asJsonObject?.get("url")?.asString
                ?: renderer.getAsJsonObject("thumbnail")
                    ?.getAsJsonObject("musicThumbnailRenderer")
                    ?.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")
                    ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""

            Artist(id = channelId, channelId = channelId, name = name,
                subscriberCount = subscriberCount, thumbnailUrl = thumbnailUrl)
        } catch (_: Exception) { null }
    }

    fun parseDurationToMs(duration: String): Long {
        val parts = duration.split(":").map { it.toLongOrNull() ?: 0L }
        return when (parts.size) {
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            2 -> (parts[0] * 60 + parts[1]) * 1000
            else -> 0L
        }
    }

    private fun JsonArray.getOrNull(index: Int): JsonElement? =
        if (index in 0 until size()) get(index) else null
}
