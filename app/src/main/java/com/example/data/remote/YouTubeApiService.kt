package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface YouTubeApiService {

    /**
     * Fetches popular/trending YouTube videos or category-specific video listings.
     */
    @GET("videos")
    suspend fun getMostPopularVideos(
        @Query("part") part: String = "snippet,contentDetails,statistics,status,liveStreamingDetails",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "US",
        @Query("videoCategoryId") videoCategoryId: String? = null,
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String,
        @Query("pageToken") pageToken: String? = null
    ): YouTubeVideoListResponse

    /**
     * Hydrates full metadata (snippet, duration, statistics, status, liveStreamingDetails) for comma-separated video IDs.
     */
    @GET("videos")
    suspend fun getVideosByIds(
        @Query("part") part: String = "snippet,contentDetails,statistics,status,liveStreamingDetails",
        @Query("id") ids: String,
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse

    /**
     * Searches YouTube for videos, channels, live broadcasts, or Shorts.
     */
    @GET("search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("q") query: String? = null,
        @Query("type") type: String = "video",
        @Query("videoEmbeddable") videoEmbeddable: String? = null,
        @Query("eventType") eventType: String? = null,
        @Query("order") order: String = "relevance",
        @Query("videoCategoryId") videoCategoryId: String? = null,
        @Query("channelId") channelId: String? = null,
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String,
        @Query("pageToken") pageToken: String? = null
    ): YouTubeSearchResponse

    /**
     * Fetches full channel details (snippet, subscriber/video stats, banner branding) for comma-separated channel IDs.
     */
    @GET("channels")
    suspend fun getChannelsByIds(
        @Query("part") part: String = "snippet,statistics,brandingSettings",
        @Query("id") ids: String,
        @Query("key") apiKey: String
    ): YouTubeChannelListResponse

    /**
     * Fetches channel details by creator handle (e.g. "@GoogleDevelopers").
     */
    @GET("channels")
    suspend fun getChannelByHandle(
        @Query("part") part: String = "snippet,statistics,brandingSettings",
        @Query("forHandle") handle: String,
        @Query("key") apiKey: String
    ): YouTubeChannelListResponse

    companion object {
        fun create(
            baseUrl: String = YouTubeRetrofitClient.BASE_URL,
            client: OkHttpClient = YouTubeRetrofitClient.createOkHttpClient()
        ): YouTubeApiService = YouTubeRetrofitClient.createService(baseUrl = baseUrl, client = client)
    }
}

/**
 * Retrofit + OkHttp + Moshi client factory for the YouTube Data API v3.
 */
object YouTubeRetrofitClient {
    const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    private val keyQueryRegex = Regex("([?&]key=)[^&\\s]+")

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: YouTubeApiService by lazy {
        createService()
    }

    fun redactApiKey(message: String): String =
        keyQueryRegex.replace(message, "$1REDACTED")

    fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { msg ->
            HttpLoggingInterceptor.Logger.DEFAULT.log(redactApiKey(msg))
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun createService(
        baseUrl: String = BASE_URL,
        client: OkHttpClient = createOkHttpClient()
    ): YouTubeApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApiService::class.java)
    }
}
