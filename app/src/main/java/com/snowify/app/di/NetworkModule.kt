package com.snowify.app.di
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.snowify.app.data.remote.LrcLibService
import com.snowify.app.data.remote.NewPipeHelper
import com.snowify.app.data.remote.YTMusicApiService
import com.snowify.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            android.util.Log.d("YTMusicHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Content-Type", "application/json")
                    .header("X-Origin", "https://music.youtube.com")
                    .header("Origin", "https://music.youtube.com")
                    .header("Referer", "https://music.youtube.com/")
                    .header("X-Goog-AuthUser", "0")
                    .header("X-YouTube-Client-Name", "67")
                    .header("X-YouTube-Client-Version", "1.20241015.01.00")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }
    @Provides
    @Singleton
    @Named("ytmusic")
    fun provideYTMusicRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.YTMUSIC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    @Provides
    @Singleton
    @Named("lrclib")
    fun provideLrcLibRetrofit(gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.LRCLIB_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    @Provides
    @Singleton
    fun provideYTMusicService(@Named("ytmusic") retrofit: Retrofit): YTMusicApiService =
        retrofit.create(YTMusicApiService::class.java)
    @Provides
    @Singleton
    fun provideLrcLibService(@Named("lrclib") retrofit: Retrofit): LrcLibService =
        retrofit.create(LrcLibService::class.java)
    @Provides
    @Singleton
    fun provideNewPipeHelper(): NewPipeHelper {
        // Plain OkHttpClient for Piped API calls (no YTMusic-specific headers)
        val plainClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        return NewPipeHelper(plainClient)
    }
}
