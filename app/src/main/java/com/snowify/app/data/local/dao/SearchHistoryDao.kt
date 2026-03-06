package com.snowify.app.data.local.dao
import androidx.room.*
import com.snowify.app.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 5")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(entity: SearchHistoryEntity)
    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)
    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
