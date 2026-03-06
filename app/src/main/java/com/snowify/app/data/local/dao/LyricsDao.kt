package com.snowify.app.data.local.dao
import androidx.room.*
import com.snowify.app.data.local.entity.CachedLyricsEntity
@Dao
interface LyricsDao {
    @Query("SELECT * FROM cached_lyrics WHERE songId = :songId")
    suspend fun getLyrics(songId: String): CachedLyricsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: CachedLyricsEntity)
}
