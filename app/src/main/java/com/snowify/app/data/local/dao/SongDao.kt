package com.snowify.app.data.local.dao
import androidx.room.*
import com.snowify.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY addedAt DESC")
    fun getAllSongs(): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY addedAt DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)
    @Update
    suspend fun updateSong(song: SongEntity)
    @Query("UPDATE songs SET isLiked = :liked WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean)
    @Delete
    suspend fun deleteSong(song: SongEntity)
    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: String)
}
