package com.snowify.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.snowify.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Firestore REST API directly instead of the Firebase SDK.
 * This bypasses the need for a registered Android app (valid mobilesdk_app_id).
 * Desktop path: users/{uid} — single document with all user data.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val auth: FirebaseAuth,
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val projectId = "snowify-dcda0"
    private val baseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    private val currentUid: String? get() = auth.currentUser?.uid

    private suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to get ID token", e)
            null
        }
    }

    // ─── REST helpers ──────────────────────────────────────────────────

    private suspend fun getDocument(path: String): JsonObject? = withContext(Dispatchers.IO) {
        val token = getIdToken() ?: return@withContext null
        val request = Request.Builder()
            .url("$baseUrl/$path")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("Firestore", "GET $path failed: ${response.code} ${response.body?.string()?.take(200)}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            JsonParser.parseString(body).asJsonObject
        } catch (e: Exception) {
            Log.e("Firestore", "GET $path exception", e)
            null
        }
    }

    private suspend fun patchDocument(path: String, fields: Map<String, Any?>): Boolean = withContext(Dispatchers.IO) {
        val token = getIdToken() ?: return@withContext false
        val firestoreFields = toFirestoreFields(fields)
        val body = gson.toJson(mapOf("fields" to firestoreFields))

        // Build URL with updateMask for each field
        val maskParams = fields.keys.joinToString("&") { "updateMask.fieldPaths=$it" }
        val url = "$baseUrl/$path?$maskParams"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .patch(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("Firestore", "PATCH $path failed: ${response.code} ${response.body?.string()?.take(200)}")
                return@withContext false
            }
            true
        } catch (e: Exception) {
            Log.e("Firestore", "PATCH $path exception", e)
            false
        }
    }

    // ─── Firestore value conversion ──────────────────────────────────

    private fun toFirestoreValue(value: Any?): Map<String, Any?> = when (value) {
        null -> mapOf("nullValue" to null)
        is String -> mapOf("stringValue" to value)
        is Boolean -> mapOf("booleanValue" to value)
        is Int -> mapOf("integerValue" to value.toString())
        is Long -> mapOf("integerValue" to value.toString())
        is Double -> mapOf("doubleValue" to value)
        is Float -> mapOf("doubleValue" to value.toDouble())
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            mapOf("mapValue" to mapOf("fields" to toFirestoreFields(value as Map<String, Any?>)))
        }
        is List<*> -> mapOf("arrayValue" to mapOf("values" to value.map { toFirestoreValue(it) }))
        else -> mapOf("stringValue" to value.toString())
    }

    private fun toFirestoreFields(map: Map<String, Any?>): Map<String, Any?> {
        return map.mapValues { (_, v) -> toFirestoreValue(v) }
    }

    private fun fromFirestoreValue(value: JsonObject): Any? {
        return when {
            value.has("stringValue") -> value.get("stringValue").asString
            value.has("integerValue") -> value.get("integerValue").asString.toLongOrNull() ?: 0L
            value.has("doubleValue") -> value.get("doubleValue").asDouble
            value.has("booleanValue") -> value.get("booleanValue").asBoolean
            value.has("nullValue") -> null
            value.has("mapValue") -> {
                val fields = value.getAsJsonObject("mapValue")?.getAsJsonObject("fields") ?: return null
                fields.entrySet().associate { (k, v) -> k to fromFirestoreValue(v.asJsonObject) }
            }
            value.has("arrayValue") -> {
                val values = value.getAsJsonObject("arrayValue")?.getAsJsonArray("values") ?: return emptyList<Any>()
                values.map { fromFirestoreValue(it.asJsonObject) }
            }
            else -> null
        }
    }

    private fun parseDocumentFields(doc: JsonObject): Map<String, Any?> {
        val fields = doc.getAsJsonObject("fields") ?: return emptyMap()
        return fields.entrySet().associate { (k, v) -> k to fromFirestoreValue(v.asJsonObject) }
    }

    // ─── Profile ──────────────────────────────────────────────────────

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = getDocument("users/$uid") ?: return null
        val data = parseDocumentFields(doc)
        if (data.isEmpty()) return null
        return UserProfile(
            uid = uid,
            displayName = data["displayName"]?.toString() ?: "",
            photoUrl = data["photoURL"]?.toString() ?: "",
            bannerUrl = data["banner"]?.toString() ?: "",
            bio = data["bio"]?.toString() ?: "",
            friendCode = data["friendCode"]?.toString() ?: "",
            isListeningActivityEnabled = data["showListeningActivity"] as? Boolean ?: true,
        )
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        val uid = currentUid ?: return
        patchDocument("users/$uid", mapOf(
            "displayName" to profile.displayName,
            "photoURL" to profile.photoUrl,
            "banner" to profile.bannerUrl,
            "bio" to profile.bio,
            "showListeningActivity" to profile.isListeningActivityEnabled,
        ))
    }

    suspend fun ensureFriendCode(uid: String): String {
        val doc = getDocument("users/$uid")
        val data = if (doc != null) parseDocumentFields(doc) else emptyMap()
        val existingCode = data["friendCode"]?.toString()
        if (!existingCode.isNullOrBlank()) return existingCode

        val newCode = com.snowify.app.util.generateFriendCode()
        patchDocument("users/$uid", mapOf("friendCode" to newCode))
        patchDocument("friendCodes/$newCode", mapOf("uid" to uid, "friendCode" to newCode))
        return newCode
    }

    // ─── Cloud State ──────────────────────────────────────────────────

    suspend fun cloudSave(playlists: List<Playlist>, likedSongs: List<Song>,
                          recentTracks: List<Song>, followedArtists: List<FollowedArtist>) {
        val uid = currentUid ?: return
        // Use desktop-compatible field names: id, title, artist, thumbnail, durationMs
        fun trackToMap(s: Song): Map<String, Any?> = mapOf(
            "id" to s.videoId,
            "title" to s.title,
            "artist" to s.artistName,
            "artistId" to s.artistId,
            "thumbnail" to s.thumbnailUrl,
            "duration" to s.durationMs,
            "durationMs" to s.durationMs,
            "album" to s.albumTitle,
            "albumId" to s.albumId,
        )
        val data = mapOf<String, Any?>(
            "playlists" to playlists.map { pl ->
                mapOf<String, Any?>(
                    "id" to pl.id,
                    "name" to pl.title,
                    "tracks" to pl.songs.map { trackToMap(it) }
                )
            },
            "likedSongs" to likedSongs.map { trackToMap(it) },
            "recentTracks" to recentTracks.take(50).map { trackToMap(it) },
            "followedArtists" to followedArtists.map { mapOf("artistId" to it.artistId, "name" to it.name, "avatar" to it.avatar) },
            "updatedAt" to System.currentTimeMillis(),
        )
        val ok = patchDocument("users/$uid", data)
        Log.d("Firestore", "Cloud save ${if (ok) "success" else "FAILED"} for $uid")
    }

    suspend fun cloudLoad(): CloudState? {
        val uid = currentUid ?: run {
            Log.w("Firestore", "cloudLoad: no current user")
            return null
        }
        val doc = getDocument("users/$uid") ?: run {
            Log.w("Firestore", "cloudLoad: document not found for $uid")
            return null
        }
        val data = parseDocumentFields(doc)
        Log.d("Firestore", "cloudLoad raw keys: ${data.keys}")
        // Log sizes of each collection
        val rawLiked = data["likedSongs"]
        val rawRecent = data["recentTracks"]
        val rawPlaylists = data["playlists"]
        val rawFollowed = data["followedArtists"]
        Log.d("Firestore", "cloudLoad raw types: liked=${rawLiked?.javaClass?.simpleName}(${(rawLiked as? List<*>)?.size}), recent=${rawRecent?.javaClass?.simpleName}(${(rawRecent as? List<*>)?.size}), playlists=${rawPlaylists?.javaClass?.simpleName}(${(rawPlaylists as? List<*>)?.size}), followed=${rawFollowed?.javaClass?.simpleName}(${(rawFollowed as? List<*>)?.size})")
        // Log first liked song to debug field names
        (rawLiked as? List<*>)?.firstOrNull()?.let { first ->
            Log.d("Firestore", "cloudLoad first liked item type=${first?.javaClass?.simpleName}, keys=${(first as? Map<*,*>)?.keys}")
        }

        val likedSongsList = parseTrackList(rawLiked, isLiked = true)
        val recentTracksList = parseTrackList(rawRecent)
        val playlistsList = parsePlaylists(rawPlaylists)
        val followedArtists = parseFollowedArtists(rawFollowed)
        val updatedAt = (data["updatedAt"] as? Long) ?: 0L

        Log.d("Firestore", "Cloud load: ${likedSongsList.size} liked, ${recentTracksList.size} recent, ${playlistsList.size} playlists")
        return CloudState(
            likedSongs = likedSongsList,
            recentTracks = recentTracksList,
            playlists = playlistsList,
            followedArtists = followedArtists,
            updatedAt = updatedAt,
        )
    }

    private fun parseTrackList(raw: Any?, isLiked: Boolean = false): List<Song> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> {
                    // Desktop stores videoId as "id", Android might store as "videoId" — handle both
                    val videoId = item["id"]?.toString()
                        ?: item["videoId"]?.toString()
                        ?: return@mapNotNull null
                    // Desktop stores artist name as "artist", Android might store as "artistName"
                    val artistName = item["artist"]?.toString()
                        ?: item["artistName"]?.toString() ?: ""
                    val artistId = item["artistId"]?.toString() ?: ""
                    val albumTitle = item["album"]?.toString()
                        ?: item["albumTitle"]?.toString() ?: ""
                    val albumId = item["albumId"]?.toString() ?: ""
                    // Desktop stores durationMs as number, or duration as formatted string
                    val durationMs = when (val d = item["durationMs"]) {
                        is Number -> d.toLong()
                        is String -> d.toLongOrNull() ?: 0L
                        else -> when (val d2 = item["duration"]) {
                            is Number -> d2.toLong()
                            is String -> d2.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                    }
                    Song(
                        id = videoId,
                        videoId = videoId,
                        title = item["title"]?.toString() ?: "",
                        artistName = artistName,
                        artistId = artistId,
                        albumTitle = albumTitle,
                        albumId = albumId,
                        thumbnailUrl = item["thumbnail"]?.toString()
                            ?: item["thumbnailUrl"]?.toString() ?: "",
                        durationMs = durationMs,
                        isLiked = isLiked,
                    )
                }
                is String -> Song(id = item, videoId = item, isLiked = isLiked)
                else -> null
            }
        }
    }

    private fun parsePlaylists(raw: Any?): List<Playlist> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val rawTracks = map["tracks"]
            Log.d("Firestore", "parsePlaylists: id=${map["id"]} name=${map["name"]} rawTracks type=${rawTracks?.javaClass?.simpleName} count=${(rawTracks as? List<*>)?.size}")
            val tracks = parseTrackList(rawTracks)
            Log.d("Firestore", "parsePlaylists: parsed ${tracks.size} tracks for '${map["name"]}'")
            Playlist(
                id = map["id"]?.toString() ?: return@mapNotNull null,
                title = map["name"]?.toString() ?: "",
                songs = tracks,
                trackCount = tracks.size,
            )
        }
    }

    // ─── Presence ─────────────────────────────────────────────────────

    private fun parseFollowedArtists(raw: Any?): List<FollowedArtist> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> {
                    val artistId = item["artistId"]?.toString()
                        ?: item["channelId"]?.toString()
                        ?: item["id"]?.toString()
                        ?: return@mapNotNull null
                    FollowedArtist(
                        artistId = artistId,
                        name = item["name"]?.toString() ?: "",
                        avatar = item["avatar"]?.toString()
                            ?: item["thumbnailUrl"]?.toString() ?: "",
                    )
                }
                is String -> FollowedArtist(artistId = item)
                else -> null
            }
        }
    }

    suspend fun updatePresence(isPlaying: Boolean, song: Song?) {
        val uid = currentUid ?: return
        patchDocument("presence/$uid", mapOf(
            "isOnline" to true,
            "isPlaying" to isPlaying,
            "trackInfo" to if (song != null) mapOf(
                "videoId" to song.videoId, "title" to song.title,
                "artist" to song.artistName, "thumbnail" to song.thumbnailUrl,
            ) else null,
            "lastSeen" to System.currentTimeMillis(),
        ))
    }

    suspend fun setOffline() {
        val uid = currentUid ?: return
        patchDocument("presence/$uid", mapOf("isOnline" to false))
    }
}

data class CloudState(
    val likedSongs: List<Song> = emptyList(),
    val recentTracks: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val followedArtists: List<FollowedArtist> = emptyList(),
    val updatedAt: Long = 0L,
)

data class FollowedArtist(
    val artistId: String,
    val name: String = "",
    val avatar: String = "",
)

