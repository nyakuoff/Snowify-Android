package com.snowify.app.data.repository
import com.google.gson.JsonObject
import com.snowify.app.data.local.dao.LyricsDao
import com.snowify.app.data.local.entity.CachedLyricsEntity
import com.snowify.app.data.model.LyricLine
import com.snowify.app.data.model.LyricsResult
import com.snowify.app.data.remote.LrcLibService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class LyricsRepository @Inject constructor(
    private val lrcLibService: LrcLibService,
    private val lyricsDao: LyricsDao,
    private val gson: Gson,
) {
    suspend fun getLyrics(songId: String, title: String, artist: String, durationMs: Long): LyricsResult {
        // Check cache first
        val cached = lyricsDao.getLyrics(songId)
        if (cached != null) return cached.toResult(gson)
        // Fetch from API
        return try {
            val durationSecs = (durationMs / 1000).toInt().takeIf { it > 0 }
            val response = lrcLibService.getLyrics(title, artist, durationSecs)
            val result = parseLyricsResponse(response)
            // Cache it
            lyricsDao.insertLyrics(CachedLyricsEntity(
                songId = songId,
                syncedJson = gson.toJson(result.synced),
                plainText = result.plain,
                isInstrumental = result.isInstrumental,
                hasSynced = result.hasSynced,
            ))
            result
        } catch (e: Exception) {
            LyricsResult()
        }
    }
    private fun parseLyricsResponse(response: JsonObject): LyricsResult {
        val instrumental = response.get("instrumental")?.asBoolean ?: false
        if (instrumental) return LyricsResult(isInstrumental = true)
        val syncedLyrics = response.get("syncedLyrics")?.asString
        val plainLyrics = response.get("plainLyrics")?.asString ?: ""
        if (!syncedLyrics.isNullOrBlank()) {
            val lines = parseLrc(syncedLyrics)
            return LyricsResult(synced = lines, plain = plainLyrics, hasSynced = true)
        }
        return LyricsResult(plain = plainLyrics, hasSynced = false)
    }
    private fun parseLrc(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val timePattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        val lrcLines = lrc.lines()
        for (i in lrcLines.indices) {
            val match = timePattern.matchEntire(lrcLines[i].trim()) ?: continue
            val (min, sec, ms, text) = match.destructured
            val startMs = min.toLong() * 60000 + sec.toLong() * 1000 + ms.padEnd(3, '0').take(3).toLong()
            val endMs = if (i + 1 < lrcLines.size) {
                val nextMatch = timePattern.matchEntire(lrcLines[i + 1].trim())
                if (nextMatch != null) {
                    val (nm, ns, nms) = nextMatch.destructured
                    nm.toLong() * 60000 + ns.toLong() * 1000 + nms.padEnd(3, '0').take(3).toLong()
                } else startMs + 5000
            } else startMs + 5000
            lines.add(LyricLine(startMs = startMs, endMs = endMs, text = text.trim()))
        }
        return lines
    }
}
fun CachedLyricsEntity.toResult(gson: Gson): LyricsResult {
    val listType = object : TypeToken<List<LyricLine>>() {}.type
    val synced: List<LyricLine> = try {
        gson.fromJson(syncedJson, listType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    return LyricsResult(synced = synced, plain = plainText, isInstrumental = isInstrumental, hasSynced = hasSynced)
}
