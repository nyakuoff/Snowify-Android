package com.snowify.app.data.remote

import android.content.Context
import android.util.Log
import com.snowify.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Handles audio stream URL extraction from YouTube Music.
 *
 * Uses the ANDROID_VR innertube client with a visitorData token obtained
 * from youtube.com. This is the same approach used by yt-dlp and SimpMusic.
 * The visitorData prevents "LOGIN_REQUIRED" / bot detection responses.
 */
class NewPipeHelper(private val okHttpClient: OkHttpClient) {
    companion object {
        private const val TAG = "StreamHelper"
        private const val YT_BASE = "https://www.youtube.com"
        private const val PLAYER_ENDPOINT = "$YT_BASE/youtubei/v1/player"
        private const val PLAYER_API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
        private const val VR_USER_AGENT =
            "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; eureka-user Build/SQ3A.220605.009.A1) gzip"
        private const val VR_VERSION = "1.61.48"
        private const val KIDS_USER_AGENT =
            "com.google.android.apps.youtube.kids/9.22.2 (Linux; U; Android 12) gzip"
        private const val KIDS_VERSION = "9.22.2"
        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private val VISITOR_DATA_PATTERN = Pattern.compile("\"VISITOR_DATA\":\"([^\"]+)\"")
        private const val VISITOR_DATA_TTL_MS = 30 * 60 * 1000L
    }

    private val streamUrlCache = mutableMapOf<String, Pair<String, Long>>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Volatile
    private var cachedVisitorData: String? = null
    @Volatile
    private var visitorDataTimestamp: Long = 0

    suspend fun getStreamUrl(videoId: String, preferBest: Boolean = true): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Check stream URL cache
                val cached = streamUrlCache[videoId]
                if (cached != null && System.currentTimeMillis() - cached.second < Constants.STREAM_URL_CACHE_TTL_MS) {
                    Log.d(TAG, "Stream cache hit for $videoId")
                    return@withContext Result.success(cached.first)
                }

                // Get visitorData (required to avoid LOGIN_REQUIRED)
                val visitorData = getOrFetchVisitorData()
                if (visitorData == null) {
                    Log.w(TAG, "Failed to obtain visitorData — trying without it")
                }

                // Strategy 1: ANDROID_VR + visitorData (primary)
                val vrResult = try {
                    fetchPlayerStream(
                        videoId, "ANDROID_VR", VR_VERSION, VR_USER_AGENT,
                        30, visitorData, preferBest
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "ANDROID_VR failed for $videoId: ${t.message}")
                    null
                }
                if (vrResult != null) {
                    streamUrlCache[videoId] = Pair(vrResult, System.currentTimeMillis())
                    return@withContext Result.success(vrResult)
                }

                // Strategy 2: ANDROID_KIDS + visitorData
                val kidsResult = try {
                    fetchPlayerStream(
                        videoId, "ANDROID_KIDS", KIDS_VERSION, KIDS_USER_AGENT,
                        30, visitorData, preferBest
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "ANDROID_KIDS failed for $videoId: ${t.message}")
                    null
                }
                if (kidsResult != null) {
                    streamUrlCache[videoId] = Pair(kidsResult, System.currentTimeMillis())
                    return@withContext Result.success(kidsResult)
                }

                // Strategy 3: Force-refresh visitorData and retry ANDROID_VR
                if (visitorData != null) {
                    val freshVisitorData = fetchVisitorData()
                    if (freshVisitorData != null && freshVisitorData != visitorData) {
                        val retryResult = try {
                            fetchPlayerStream(
                                videoId, "ANDROID_VR", VR_VERSION, VR_USER_AGENT,
                                30, freshVisitorData, preferBest
                            )
                        } catch (t: Throwable) {
                            Log.w(TAG, "ANDROID_VR retry failed: ${t.message}")
                            null
                        }
                        if (retryResult != null) {
                            streamUrlCache[videoId] = Pair(retryResult, System.currentTimeMillis())
                            return@withContext Result.success(retryResult)
                        }
                    }
                }

                Result.failure(Exception("No audio stream found for $videoId"))
            } catch (t: Throwable) {
                Log.e(TAG, "getStreamUrl failed for $videoId", t)
                Result.failure(Exception(t.message ?: "Stream extraction failed", t))
            }
        }
    }

    private fun getOrFetchVisitorData(): String? {
        val now = System.currentTimeMillis()
        val existing = cachedVisitorData
        if (existing != null && now - visitorDataTimestamp < VISITOR_DATA_TTL_MS) {
            return existing
        }
        return fetchVisitorData()
    }

    /**
     * Fetches a fresh visitorData token from youtube.com.
     * This token is required to avoid LOGIN_REQUIRED / bot detection.
     */
    private fun fetchVisitorData(): String? {
        return try {
            val request = OkRequest.Builder()
                .url(YT_BASE)
                .header("User-Agent", WEB_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string() ?: return null

            val matcher = VISITOR_DATA_PATTERN.matcher(html)
            if (matcher.find()) {
                val visitorData = matcher.group(1)
                if (!visitorData.isNullOrBlank()) {
                    cachedVisitorData = visitorData
                    visitorDataTimestamp = System.currentTimeMillis()
                    Log.d(TAG, "Got visitorData: ${visitorData.take(30)}...")
                    return visitorData
                }
            }

            Log.w(TAG, "Could not extract visitorData from YouTube page")
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchVisitorData failed: ${e.message}")
            null
        }
    }

    /**
     * Calls innertube /player endpoint with the specified client and visitorData.
     * Returns a direct audio stream URL or null.
     */
    private fun fetchPlayerStream(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String,
        androidSdkVersion: Int?,
        visitorData: String?,
        preferBest: Boolean
    ): String? {
        val clientObj = buildString {
            append("{")
            append("\"clientName\":\"$clientName\",")
            append("\"clientVersion\":\"$clientVersion\",")
            append("\"hl\":\"en\",\"gl\":\"US\"")
            if (androidSdkVersion != null) append(",\"androidSdkVersion\":$androidSdkVersion")
            if (visitorData != null) append(",\"visitorData\":\"$visitorData\"")
            append("}")
        }

        val body = """
        {
            "videoId": "$videoId",
            "context": {
                "client": $clientObj
            },
            "playbackContext": {
                "contentPlaybackContext": {
                    "signatureTimestamp": 20073
                }
            },
            "contentCheckOk": true,
            "racyCheckOk": true
        }
        """.trimIndent()

        val requestBuilder = OkRequest.Builder()
            .url("$PLAYER_ENDPOINT?key=$PLAYER_API_KEY&prettyPrint=false")
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))

        if (visitorData != null) {
            requestBuilder.header("X-Goog-Visitor-Id", visitorData)
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            Log.w(TAG, "$clientName response ${response.code}: ${responseBody.take(200)}")
            return null
        }

        return extractBestAudioUrl(responseBody, preferBest, clientName)
    }

    /**
     * Parses innertube /player response and extracts the best audio stream URL.
     */
    private fun extractBestAudioUrl(responseBody: String, preferBest: Boolean, tag: String): String? {
        return try {
            val json = JSONObject(responseBody)

            val status = json.optJSONObject("playabilityStatus")
            val playabilityStatus = status?.optString("status", "")
            if (playabilityStatus != "OK") {
                val reason = status?.optString("reason", "Unknown")
                Log.w(TAG, "$tag: $playabilityStatus — $reason")
                return null
            }

            val streamingData = json.optJSONObject("streamingData") ?: run {
                Log.w(TAG, "$tag: no streamingData")
                return null
            }

            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            val formats = streamingData.optJSONArray("formats")

            var bestUrl: String? = null
            var bestBitrate = if (preferBest) 0 else Int.MAX_VALUE

            // Prefer adaptive formats (audio-only, higher quality)
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.getJSONObject(i)
                    val mimeType = format.optString("mimeType", "")
                    if (!mimeType.startsWith("audio/")) continue
                    val url = format.optString("url", "")
                    if (url.isBlank()) continue
                    val bitrate = format.optInt("bitrate", 0)
                    if (preferBest && bitrate > bestBitrate) {
                        bestBitrate = bitrate; bestUrl = url
                    } else if (!preferBest && bitrate in 1 until bestBitrate) {
                        bestBitrate = bitrate; bestUrl = url
                    } else if (bestUrl == null) {
                        bestBitrate = bitrate; bestUrl = url
                    }
                }
            }

            // Fallback to regular formats
            if (bestUrl == null && formats != null) {
                for (i in 0 until formats.length()) {
                    val format = formats.getJSONObject(i)
                    val url = format.optString("url", "")
                    if (url.isBlank()) continue
                    val mimeType = format.optString("mimeType", "")
                    if (mimeType.contains("audio") || mimeType.contains("mp4")) {
                        bestUrl = url; break
                    }
                }
            }

            if (bestUrl != null) {
                Log.d(TAG, "$tag: stream OK (bitrate=$bestBitrate)")
            } else {
                Log.w(TAG, "$tag: no usable audio URL")
            }
            bestUrl
        } catch (e: Exception) {
            Log.e(TAG, "$tag parse error: ${e.message}")
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun initialize(context: Context) {
        Log.d(TAG, "Stream helper initialized (ANDROID_VR + visitorData)")
    }
}

