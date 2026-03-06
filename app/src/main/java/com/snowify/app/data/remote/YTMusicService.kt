package com.snowify.app.data.remote

import com.google.gson.JsonObject
import com.snowify.app.util.Constants
import okhttp3.RequestBody
import retrofit2.http.*

interface YTMusicApiService {
    @POST("browse")
    suspend fun browse(@Body body: RequestBody, @Query("key") key: String = Constants.YTMUSIC_API_KEY): JsonObject
    @POST("search")
    suspend fun search(@Body body: RequestBody, @Query("key") key: String = Constants.YTMUSIC_API_KEY): JsonObject
    @POST("next")
    suspend fun next(@Body body: RequestBody, @Query("key") key: String = Constants.YTMUSIC_API_KEY): JsonObject
    @POST("player")
    suspend fun player(@Body body: RequestBody, @Query("key") key: String = Constants.YTMUSIC_API_KEY): JsonObject
}
