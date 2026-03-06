package com.snowify.app.data.local.dao
import androidx.room.*
import com.snowify.app.data.local.entity.RecentSongEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface RecentSongDao {
    @Query("SELECT * FROM recent_songs ORDER BY playedAt DESC LIMIT 50")
    fun getRecentSongs(): Flow<List<RecentSongEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(song: RecentSongEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecentSongsBatch(songs: List<RecentSongEntity>)
    @Query("DELETE FROM recent_songs")
    suspend fun clearHistory()
    @Query("DELETE FROM recent_songs WHERE id NOT IN (SELECT id FROM recent_songs ORDER BY playedAt DESC LIMIT 50)")
    suspend fun trimHistory()
}
