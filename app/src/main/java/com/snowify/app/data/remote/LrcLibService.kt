package com.snowify.app.data.remote
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query
interface LrcLibService {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("duration") durationSeconds: Int? = null,
    ): JsonObject
    @GET("search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
    ): List<JsonObject>
}
