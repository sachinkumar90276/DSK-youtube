package com.example.data.remote

import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoResource> = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoResource(
    val id: String = "",
    val snippet: YouTubeVideoSnippet? = null,
    val contentDetails: YouTubeContentDetails? = null,
    val statistics: YouTubeStatistics? = null,
    val status: YouTubeVideoStatus? = null,
    val liveStreamingDetails: YouTubeLiveDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoSnippet(
    val publishedAt: String? = null,
    val channelId: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnails: YouTubeThumbnails? = null,
    val channelTitle: String = "",
    val tags: List<String>? = null,
    val categoryId: String? = null,
    val liveBroadcastContent: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentDetails(
    val duration: String? = null,
    val regionRestriction: YouTubeRegionRestriction? = null,
    val contentRating: YouTubeContentRating? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeRegionRestriction(
    val allowed: List<String>? = null,
    val blocked: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentRating(
    val ytRating: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoStatus(
    val uploadStatus: String? = null,
    val privacyStatus: String? = null,
    val embeddable: Boolean? = null,
    val publicStatsViewable: Boolean? = null,
    val madeForKids: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveDetails(
    val actualStartTime: String? = null,
    val concurrentViewers: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchResource> = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResource(
    val id: YouTubeSearchId = YouTubeSearchId(),
    val snippet: YouTubeSearchSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchId(
    val kind: String? = null,
    val videoId: String? = null,
    val channelId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchSnippet(
    val publishedAt: String? = null,
    val channelId: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnails: YouTubeThumbnails? = null,
    val channelTitle: String = "",
    val liveBroadcastContent: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelListResponse(
    val items: List<YouTubeChannelResource> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelResource(
    val id: String = "",
    val snippet: YouTubeChannelSnippet? = null,
    val statistics: YouTubeChannelStatistics? = null,
    val brandingSettings: YouTubeBrandingSettings? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelSnippet(
    val title: String = "",
    val description: String = "",
    val customUrl: String? = null,
    val publishedAt: String? = null,
    val thumbnails: YouTubeThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelStatistics(
    val subscriberCount: String? = null,
    val videoCount: String? = null,
    val viewCount: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeBrandingSettings(
    val image: YouTubeBannerImage? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeBannerImage(
    val bannerExternalUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val default: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null,
    val standard: YouTubeThumbnail? = null,
    val maxres: YouTubeThumbnail? = null
) {
    fun bestUrl(): String? = maxres?.url ?: standard?.url ?: high?.url ?: medium?.url ?: default?.url
}

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null
)

object YouTubeParser {
    private val categoryMap = mapOf(
        "1" to "Movies",
        "2" to "Travel",
        "10" to "Music",
        "15" to "Entertainment",
        "17" to "Sports",
        "19" to "Travel",
        "20" to "Gaming",
        "22" to "Entertainment",
        "23" to "Comedy",
        "24" to "Entertainment",
        "25" to "News",
        "26" to "Education",
        "27" to "Education",
        "28" to "Technology"
    )

    private val nameToCategoryId = mapOf(
        "Music" to "10",
        "Gaming" to "20",
        "Movies" to "1",
        "Entertainment" to "24",
        "News" to "25",
        "Comedy" to "23",
        "Technology" to "28",
        "Education" to "27",
        "Sports" to "17",
        "Travel" to "19"
    )

    fun categoryNameForId(categoryId: String?): String {
        return categoryMap[categoryId] ?: "Entertainment"
    }

    fun categoryIdForName(name: String): String? = nameToCategoryId[name]

    fun parseIsoDurationSeconds(iso: String?): Int {
        if (iso.isNullOrBlank()) return 0
        val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
        val match = regex.matchEntire(iso.trim()) ?: return 0
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val mins = match.groupValues[2].toIntOrNull() ?: 0
        val secs = match.groupValues[3].toIntOrNull() ?: 0
        return hours * 3600 + mins * 60 + secs
    }

    fun parsePublishedAtMillis(publishedAt: String?): Long {
        if (publishedAt.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(publishedAt)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private val rawIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")
    private val watchParamRegex = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
    private val shortPathRegex = Regex("(?:youtu\\.be/|youtube\\.com/(?:embed|shorts|live|v)/)([a-zA-Z0-9_-]{11})")

    fun isValidVideoId(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        return rawIdRegex.matches(id.trim())
    }

    fun extractVideoId(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        if (rawIdRegex.matches(trimmed)) return trimmed
        watchParamRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        shortPathRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }
}
