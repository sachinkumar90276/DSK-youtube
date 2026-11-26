package com.example

import com.example.data.remote.YouTubeContentDetails
import com.example.data.remote.YouTubeContentRating
import com.example.data.remote.YouTubeParser
import com.example.data.remote.YouTubeRegionRestriction
import com.example.data.remote.YouTubeRetrofitClient
import com.example.data.remote.YouTubeSyncManager
import com.example.data.remote.YouTubeVideoResource
import com.example.data.remote.YouTubeVideoStatus
import com.example.data.security.AuthCrypto
import com.example.ui.components.mapYouTubeErrorMessage
import com.example.ui.util.formatCount
import com.example.ui.util.formatDuration
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class DskUnitTest {

    @Test
    fun `password hashing and verification works securely`() {
        val salt = AuthCrypto.generateSalt()
        val hash = AuthCrypto.hashPassword("DskFree2026!", salt)
        assertTrue(AuthCrypto.verifyPassword("DskFree2026!", salt, hash))
        assertFalse(AuthCrypto.verifyPassword("WrongPassword", salt, hash))
    }

    @Test
    fun `media url validation accepts streaming and content uris`() {
        assertTrue(AuthCrypto.isValidMediaUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"))
        assertTrue(AuthCrypto.isValidMediaUrl("content://media/external/video/media/42"))
        assertFalse(AuthCrypto.isValidMediaUrl("javascript:alert(1)"))
    }

    @Test
    fun `count and duration formatting matches platform style`() {
        assertEquals("950", formatCount(950))
        assertEquals("1.5K", formatCount(1500))
        assertEquals("2M", formatCount(2_000_000))
        assertEquals("0:15", formatDuration(15))
        assertEquals("9:56", formatDuration(596))
        assertEquals("1:01:05", formatDuration(3665))
    }

    @Test
    fun `youtube parser decodes ISO durations, timestamps, and categories`() {
        assertEquals(3730, YouTubeParser.parseIsoDurationSeconds("PT1H2M10S"))
        assertEquals(45, YouTubeParser.parseIsoDurationSeconds("PT45S"))
        assertEquals(0, YouTubeParser.parseIsoDurationSeconds(null))
        assertEquals("Music", YouTubeParser.categoryNameForId("10"))
        assertEquals("28", YouTubeParser.categoryIdForName("Technology"))
        val ts = YouTubeParser.parsePublishedAtMillis("2025-05-01T12:00:00Z")
        assertTrue(ts > 1_700_000_000_000L)
    }

    @Test
    fun `youtube retrofit service parses video listings search and channels`() = runTest {
        val capturedUrls = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val path = req.url.encodedPath
                capturedUrls.add(req.url.toString())
                val json = when {
                    path.endsWith("/videos") -> """
                        {
                          "nextPageToken": "NEXT_VID",
                          "items": [
                            {
                              "id": "vid_123",
                              "snippet": {
                                "title": "Kotlin Multiplatform Overview",
                                "channelId": "UC_CHAN_1",
                                "channelTitle": "Android Developers",
                                "categoryId": "28"
                              },
                              "contentDetails": { "duration": "PT5M12S" },
                              "statistics": { "viewCount": "42000", "likeCount": "1500" }
                            }
                          ]
                        }
                    """.trimIndent()
                    path.endsWith("/search") -> """
                        {
                          "nextPageToken": "NEXT_SEARCH",
                          "items": [
                            {
                              "id": { "kind": "youtube#video", "videoId": "vid_search_9" },
                              "snippet": { "title": "Compose Tips", "channelId": "UC_CHAN_1", "channelTitle": "Android Developers" }
                            }
                          ]
                        }
                    """.trimIndent()
                    path.endsWith("/channels") -> """
                        {
                          "items": [
                            {
                              "id": "UC_CHAN_1",
                              "snippet": { "title": "Android Developers", "customUrl": "@androiddevelopers" },
                              "statistics": { "subscriberCount": "1200000", "videoCount": "950" }
                            }
                          ]
                        }
                    """.trimIndent()
                    else -> "{}"
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val api = YouTubeRetrofitClient.createService(client = client)

        val videosResp = api.getMostPopularVideos(videoCategoryId = "28", apiKey = "TEST_SECRET_KEY")
        assertEquals("NEXT_VID", videosResp.nextPageToken)
        assertEquals("vid_123", videosResp.items.first().id)
        assertEquals(312, YouTubeParser.parseIsoDurationSeconds(videosResp.items.first().contentDetails?.duration))

        val searchResp = api.search(
            query = "Compose",
            type = "video",
            videoEmbeddable = "true",
            apiKey = "TEST_SECRET_KEY"
        )
        assertEquals("NEXT_SEARCH", searchResp.nextPageToken)
        assertEquals("vid_search_9", searchResp.items.first().id.videoId)

        val channelResp = api.getChannelsByIds(ids = "UC_CHAN_1", apiKey = "TEST_SECRET_KEY")
        assertEquals("Android Developers", channelResp.items.first().snippet?.title)
        assertEquals("1200000", channelResp.items.first().statistics?.subscriberCount)

        assertTrue(capturedUrls[0].contains("/youtube/v3/videos"))
        assertTrue(capturedUrls[0].contains("videoCategoryId=28"))
        assertTrue(capturedUrls[1].contains("/youtube/v3/search"))
        assertTrue(capturedUrls[1].contains("videoEmbeddable=true"))
        assertTrue(capturedUrls[2].contains("/youtube/v3/channels"))
        assertFalse(YouTubeRetrofitClient.redactApiKey(capturedUrls[0]).contains("TEST_SECRET_KEY"))
    }

    @Test
    fun `youtube parser extracts video ids from all youtube url formats`() {
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s"))
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ?feature=share"))
        assertNull(YouTubeParser.extractVideoId("https://cdn.example.com/uploads/sample_video.mp4"))
        assertNull(YouTubeParser.extractVideoId("   "))
        assertTrue(YouTubeParser.isValidVideoId("dQw4w9WgXcQ"))
        assertFalse(YouTubeParser.isValidVideoId("invalid"))
    }

    @Test
    fun `video restriction evaluator detects embeddable, private, deleted, age, and region limits`() {
        val playable = YouTubeVideoResource(
            id = "dQw4w9WgXcQ",
            status = YouTubeVideoStatus(privacyStatus = "public", embeddable = true, uploadStatus = "processed")
        )
        assertTrue(YouTubeSyncManager.evaluateVideoRestrictions(playable, "US").isPlayable)

        val noEmbed = YouTubeVideoResource(
            id = "dQw4w9WgXcQ",
            status = YouTubeVideoStatus(privacyStatus = "public", embeddable = false)
        )
        assertFalse(YouTubeSyncManager.evaluateVideoRestrictions(noEmbed, "US").isPlayable)

        val priv = YouTubeVideoResource(
            id = "dQw4w9WgXcQ",
            status = YouTubeVideoStatus(privacyStatus = "private", embeddable = true)
        )
        assertFalse(YouTubeSyncManager.evaluateVideoRestrictions(priv, "US").isPlayable)

        val ageRestricted = YouTubeVideoResource(
            id = "dQw4w9WgXcQ",
            contentDetails = YouTubeContentDetails(contentRating = YouTubeContentRating(ytRating = "ytAgeRestricted")),
            status = YouTubeVideoStatus(privacyStatus = "public", embeddable = true)
        )
        assertFalse(YouTubeSyncManager.evaluateVideoRestrictions(ageRestricted, "US").isPlayable)

        val regionBlocked = YouTubeVideoResource(
            id = "dQw4w9WgXcQ",
            contentDetails = YouTubeContentDetails(regionRestriction = YouTubeRegionRestriction(blocked = listOf("US"))),
            status = YouTubeVideoStatus(privacyStatus = "public", embeddable = true)
        )
        assertFalse(YouTubeSyncManager.evaluateVideoRestrictions(regionBlocked, "US").isPlayable)
        assertTrue(YouTubeSyncManager.evaluateVideoRestrictions(regionBlocked, "IN").isPlayable)
    }

    @Test
    fun `youtube player error codes map to clear user facing messages`() {
        assertTrue(mapYouTubeErrorMessage(152).contains("152"))
        assertTrue(mapYouTubeErrorMessage(153).contains("153"))
        assertTrue(mapYouTubeErrorMessage(100).contains("unavailable", ignoreCase = true))
        assertTrue(mapYouTubeErrorMessage(101).contains("owner", ignoreCase = true))
        assertTrue(mapYouTubeErrorMessage(150).contains("owner", ignoreCase = true))
    }
}
