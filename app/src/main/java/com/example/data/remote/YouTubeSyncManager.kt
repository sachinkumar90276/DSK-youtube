package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.DskDatabase
import com.example.data.local.entity.*
import com.example.data.security.AuthCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class VideoRestrictionInfo(
    val isPlayable: Boolean = true,
    val reasonMessage: String? = null
)

sealed interface YouTubeSyncState {
    data object Idle : YouTubeSyncState
    data object Refreshing : YouTubeSyncState
    data class Ready(val lastUpdatedMs: Long) : YouTubeSyncState
    data object MissingApiKey : YouTubeSyncState
    data class QuotaExceeded(val message: String, val retryAtMs: Long) : YouTubeSyncState
    data class Error(val message: String) : YouTubeSyncState
}

data class YouTubeSearchBundle(
    val videos: List<VideoWithChannel>,
    val shorts: List<ShortWithChannel>,
    val channels: List<ChannelEntity>
)

class YouTubeSyncManager(
    private val db: DskDatabase,
    private val api: YouTubeApiService
) {
    companion object {
        private const val YT_SYSTEM_USER_ID = "yt_public_api_owner"
        const val HOME_REFRESH_INTERVAL_MS = 10 * 60_000L // 10 minutes
        const val LIVE_REFRESH_INTERVAL_MS = 3 * 60_000L  // 3 minutes
        const val CHANNEL_REFRESH_INTERVAL_MS = 10 * 60_000L

        fun evaluateVideoRestrictions(
            res: YouTubeVideoResource,
            countryCode: String = Locale.getDefault().country
        ): VideoRestrictionInfo {
            val status = res.status
            if (status?.privacyStatus == "private") {
                return VideoRestrictionInfo(
                    isPlayable = false,
                    reasonMessage = "This video is private and cannot be played."
                )
            }
            if (status?.uploadStatus in setOf("deleted", "rejected", "failed")) {
                return VideoRestrictionInfo(
                    isPlayable = false,
                    reasonMessage = "This YouTube video has been removed or deleted."
                )
            }
            if (status?.embeddable == false) {
                return VideoRestrictionInfo(
                    isPlayable = false,
                    reasonMessage = "Embedding is disabled by the video owner. YouTube restricts this video from embedded playback."
                )
            }
            if (res.contentDetails?.contentRating?.ytRating == "ytAgeRestricted") {
                return VideoRestrictionInfo(
                    isPlayable = false,
                    reasonMessage = "This video is age-restricted on YouTube and cannot play in an embedded player."
                )
            }
            val restriction = res.contentDetails?.regionRestriction
            val cc = countryCode.uppercase(Locale.US)
            if (cc.length == 2) {
                if (restriction?.blocked?.any { it.equals(cc, ignoreCase = true) } == true ||
                    (restriction?.allowed != null && restriction.allowed.none { it.equals(cc, ignoreCase = true) })
                ) {
                    return VideoRestrictionInfo(
                        isPlayable = false,
                        reasonMessage = "This video is restricted in your region ($cc) by the content owner."
                    )
                }
            }
            return VideoRestrictionInfo(isPlayable = true, reasonMessage = null)
        }
    }

    private var lastHomeFetchMs: Long = 0L
    private var lastLiveFetchMs: Long = 0L
    private val lastCategoryFetchMs = ConcurrentHashMap<String, Long>()
    private val lastChannelFetchMs = ConcurrentHashMap<String, Long>()
    private val restrictionCache = ConcurrentHashMap<String, VideoRestrictionInfo>()

    fun hasValidApiKey(): Boolean {
        val key = BuildConfig.YOUTUBE_API_KEY.trim()
        return key.isNotEmpty() && key != "MY_YOUTUBE_API_KEY"
    }

    private fun apiKey(): String = BuildConfig.YOUTUBE_API_KEY.trim()

    private suspend fun ensureYouTubeSystemOwner() {
        if (db.userDao().getUserById(YT_SYSTEM_USER_ID) == null) {
            val salt = AuthCrypto.generateSalt()
            val hash = AuthCrypto.hashPassword(salt + YT_SYSTEM_USER_ID, salt)
            db.userDao().insertUser(
                UserEntity(
                    id = YT_SYSTEM_USER_ID,
                    email = "youtube-api@system.local",
                    passwordHash = hash,
                    salt = salt
                )
            )
        }
    }

    suspend fun refreshHomeFeeds(force: Boolean = false): YouTubeSyncState = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext YouTubeSyncState.MissingApiKey

        val now = System.currentTimeMillis()
        val homeStale = force || (now - lastHomeFetchMs >= HOME_REFRESH_INTERVAL_MS)
        val liveStale = force || (now - lastLiveFetchMs >= LIVE_REFRESH_INTERVAL_MS)

        if (!homeStale && !liveStale) {
            return@withContext YouTubeSyncState.Ready(lastHomeFetchMs)
        }

        try {
            ensureYouTubeSystemOwner()

            if (homeStale) {
                // 1. Trending / Most Popular videos (fetch 35 so filtering non-embeddable retains a rich feed)
                val popularResp = api.getMostPopularVideos(
                    maxResults = 35,
                    apiKey = apiKey()
                )
                val playablePopular = popularResp.items.filter { evaluateVideoRestrictions(it).isPlayable }
                val channelIds = mutableSetOf<String>()
                playablePopular.forEach { it.snippet?.channelId?.let(channelIds::add) }

                // 2. Music popular videos (filter non-embeddable tracks & supplement via videoEmbeddable=true)
                val musicResp = runCatching {
                    api.getMostPopularVideos(
                        videoCategoryId = "10",
                        maxResults = 20,
                        apiKey = apiKey()
                    )
                }.getOrNull()
                var playableMusic = musicResp?.items.orEmpty()
                    .filter { evaluateVideoRestrictions(it).isPlayable }
                if (playableMusic.size < 6) {
                    val musicSearch = runCatching {
                        api.search(
                            query = "live music performance acoustic",
                            type = "video",
                            videoEmbeddable = "true",
                            videoCategoryId = "10",
                            order = "viewCount",
                            maxResults = 15,
                            apiKey = apiKey()
                        )
                    }.getOrNull()
                    val mIds = musicSearch?.items.orEmpty().mapNotNull { it.id.videoId }.distinct()
                    if (mIds.isNotEmpty()) {
                        val hydratedMusic = runCatching {
                            api.getVideosByIds(ids = mIds.joinToString(","), apiKey = apiKey())
                        }.getOrNull()?.items.orEmpty().filter { evaluateVideoRestrictions(it).isPlayable }
                        playableMusic = (playableMusic + hydratedMusic).distinctBy { it.id }
                    }
                }
                playableMusic.forEach { it.snippet?.channelId?.let(channelIds::add) }

                // 3. Popular embeddable Shorts via official search + videos hydration
                val shortsSearch = runCatching {
                    api.search(
                        query = "#shorts",
                        type = "video",
                        videoEmbeddable = "true",
                        order = "viewCount",
                        maxResults = 16,
                        apiKey = apiKey()
                    )
                }.getOrNull()
                val shortVideoIds = shortsSearch?.items.orEmpty().mapNotNull { it.id.videoId }.distinct()
                val shortsHydrated = if (shortVideoIds.isNotEmpty()) {
                    runCatching {
                        api.getVideosByIds(ids = shortVideoIds.joinToString(","), apiKey = apiKey())
                    }.getOrNull()?.items.orEmpty().filter { evaluateVideoRestrictions(it).isPlayable }
                } else emptyList()
                shortsHydrated.forEach { it.snippet?.channelId?.let(channelIds::add) }

                // Hydrate all real YouTube channels in batches of 50
                val channelsMap = fetchAndCacheChannels(channelIds.toList())

                // Persist real trending & popular embeddable videos
                playablePopular.forEachIndexed { idx, res ->
                    upsertYouTubeVideo(
                        res = res,
                        channelsMap = channelsMap,
                        sectionTag = when {
                            idx < 8 -> "Trending Videos"
                            idx < 16 -> "Recommended For You"
                            else -> "Most Viewed"
                        },
                        isTrending = idx < 10,
                        isFeatured = idx < 3
                    )
                }

                // Persist real Music videos
                playableMusic.forEach { res ->
                    upsertYouTubeVideo(
                        res = res,
                        channelsMap = channelsMap,
                        sectionTag = "Music Videos",
                        isTrending = false,
                        isFeatured = false,
                        overrideCategory = "Music"
                    )
                }

                // Persist real YouTube Shorts
                shortsHydrated.forEach { res ->
                    upsertYouTubeShort(res, channelsMap)
                }

                lastHomeFetchMs = System.currentTimeMillis()
            }

            if (liveStale) {
                refreshLiveStreamsInternal()
                lastLiveFetchMs = System.currentTimeMillis()
            }

            YouTubeSyncState.Ready(System.currentTimeMillis())
        } catch (e: HttpException) {
            handleHttpException(e)
        } catch (e: IOException) {
            YouTubeSyncState.Error("Network unavailable. Showing cached YouTube content.")
        } catch (e: Exception) {
            YouTubeSyncState.Error(e.message ?: "Unable to refresh YouTube feed.")
        }
    }

    suspend fun refreshCategory(categoryName: String, force: Boolean = false): YouTubeSyncState = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext YouTubeSyncState.MissingApiKey
        if (categoryName == "All") return@withContext refreshHomeFeeds(force)

        val now = System.currentTimeMillis()
        if (categoryName == "Live") {
            if (!force && now - lastLiveFetchMs < LIVE_REFRESH_INTERVAL_MS) {
                return@withContext YouTubeSyncState.Ready(lastLiveFetchMs)
            }
            return@withContext try {
                ensureYouTubeSystemOwner()
                refreshLiveStreamsInternal()
                lastLiveFetchMs = System.currentTimeMillis()
                YouTubeSyncState.Ready(lastLiveFetchMs)
            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: IOException) {
                YouTubeSyncState.Error("Network unavailable. Showing cached live streams.")
            }
        }

        val lastCat = lastCategoryFetchMs[categoryName] ?: 0L
        if (!force && now - lastCat < HOME_REFRESH_INTERVAL_MS) {
            return@withContext YouTubeSyncState.Ready(lastCat)
        }

        try {
            ensureYouTubeSystemOwner()
            val catId = YouTubeParser.categoryIdForName(categoryName)
            var items = if (catId != null) {
                runCatching {
                    api.getMostPopularVideos(
                        videoCategoryId = catId,
                        maxResults = 25,
                        apiKey = apiKey()
                    ).items.filter { evaluateVideoRestrictions(it).isPlayable }
                }.getOrDefault(emptyList())
            } else emptyList()

            if (items.size < 6) {
                val searchRes = api.search(
                    query = categoryName,
                    type = "video",
                    videoEmbeddable = "true",
                    videoCategoryId = catId,
                    order = "viewCount",
                    maxResults = 18,
                    apiKey = apiKey()
                )
                val ids = searchRes.items.mapNotNull { it.id.videoId }.distinct()
                if (ids.isNotEmpty()) {
                    val searchHydrated = api.getVideosByIds(ids = ids.joinToString(","), apiKey = apiKey())
                        .items.filter { evaluateVideoRestrictions(it).isPlayable }
                    items = (items + searchHydrated).distinctBy { it.id }
                }
            }

            val channelIds = items.mapNotNull { it.snippet?.channelId }.distinct()
            val channelsMap = fetchAndCacheChannels(channelIds)

            items.forEach { res ->
                upsertYouTubeVideo(
                    res = res,
                    channelsMap = channelsMap,
                    sectionTag = categoryName,
                    isTrending = false,
                    isFeatured = false,
                    overrideCategory = categoryName
                )
            }
            val updated = System.currentTimeMillis()
            lastCategoryFetchMs[categoryName] = updated
            YouTubeSyncState.Ready(updated)
        } catch (e: HttpException) {
            handleHttpException(e)
        } catch (e: IOException) {
            YouTubeSyncState.Error("Network unavailable. Showing cached $categoryName videos.")
        } catch (e: Exception) {
            YouTubeSyncState.Error(e.message ?: "Failed to load $categoryName.")
        }
    }

    suspend fun refreshChannel(channelId: String, force: Boolean = false): YouTubeSyncState = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) return@withContext YouTubeSyncState.MissingApiKey
        if (!channelId.startsWith("UC")) return@withContext YouTubeSyncState.Ready(System.currentTimeMillis())

        val now = System.currentTimeMillis()
        val last = lastChannelFetchMs[channelId] ?: 0L
        if (!force && now - last < CHANNEL_REFRESH_INTERVAL_MS) {
            return@withContext YouTubeSyncState.Ready(last)
        }

        try {
            ensureYouTubeSystemOwner()
            val channelsMap = fetchAndCacheChannels(listOf(channelId))
            val searchResp = api.search(
                channelId = channelId,
                type = "video",
                videoEmbeddable = "true",
                order = "date",
                maxResults = 15,
                apiKey = apiKey()
            )
            val videoIds = searchResp.items.mapNotNull { it.id.videoId }.distinct()
            if (videoIds.isNotEmpty()) {
                val hydrated = api.getVideosByIds(ids = videoIds.joinToString(","), apiKey = apiKey())
                hydrated.items
                    .filter { evaluateVideoRestrictions(it).isPlayable }
                    .forEach { res ->
                        upsertYouTubeVideo(
                            res = res,
                            channelsMap = channelsMap,
                            sectionTag = "Latest Uploads",
                            isTrending = false,
                            isFeatured = false
                        )
                    }
            }
            val updated = System.currentTimeMillis()
            lastChannelFetchMs[channelId] = updated
            YouTubeSyncState.Ready(updated)
        } catch (e: HttpException) {
            handleHttpException(e)
        } catch (e: IOException) {
            YouTubeSyncState.Error("Network unavailable. Showing cached channel data.")
        } catch (e: Exception) {
            YouTubeSyncState.Error(e.message ?: "Could not refresh channel.")
        }
    }

    suspend fun fetchAndCacheVideoById(videoId: String): VideoWithChannel? = withContext(Dispatchers.IO) {
        val cleanId = YouTubeParser.extractVideoId(videoId) ?: videoId.trim()
        val existingVideo = db.videoDao().getVideoById(cleanId)
        if (existingVideo != null) {
            val ch = db.channelDao().getChannelById(existingVideo.channelId)
            if (ch != null) return@withContext VideoWithChannel(existingVideo, ch)
        }
        if (!hasValidApiKey()) return@withContext null
        return@withContext runCatching {
            ensureYouTubeSystemOwner()
            val resp = api.getVideosByIds(ids = cleanId, apiKey = apiKey())
            val res = resp.items.firstOrNull() ?: return@runCatching null
            val cid = res.snippet?.channelId.orEmpty()
            val channelsMap = if (cid.isNotBlank()) fetchAndCacheChannels(listOf(cid)) else emptyMap()
            val videoEntity = upsertYouTubeVideo(
                res = res,
                channelsMap = channelsMap,
                sectionTag = "Watch",
                isTrending = false,
                isFeatured = false
            )
            val ch = channelsMap[videoEntity.channelId] ?: db.channelDao().getChannelById(videoEntity.channelId)
            ch?.let { VideoWithChannel(videoEntity, it) }
        }.getOrNull()
    }

    suspend fun checkVideoRestrictions(videoId: String): VideoRestrictionInfo = withContext(Dispatchers.IO) {
        val cleanId = YouTubeParser.extractVideoId(videoId) ?: videoId.trim()
        if (!YouTubeParser.isValidVideoId(cleanId)) {
            return@withContext VideoRestrictionInfo(
                isPlayable = false,
                reasonMessage = "Invalid YouTube video ID."
            )
        }
        restrictionCache[cleanId]?.let { return@withContext it }
        if (!hasValidApiKey()) return@withContext VideoRestrictionInfo(isPlayable = true)
        return@withContext runCatching {
            val resp = api.getVideosByIds(ids = cleanId, apiKey = apiKey())
            val res = resp.items.firstOrNull()
            val info = if (res == null) {
                VideoRestrictionInfo(
                    isPlayable = false,
                    reasonMessage = "This YouTube video is deleted or unavailable."
                )
            } else {
                evaluateVideoRestrictions(res)
            }
            restrictionCache[cleanId] = info
            info
        }.getOrElse {
            VideoRestrictionInfo(isPlayable = true)
        }
    }

    suspend fun searchYouTubeLive(
        query: String,
        typeFilter: String,
        categoryFilter: String,
        sortBy: String
    ): Result<YouTubeSearchBundle> = withContext(Dispatchers.IO) {
        if (!hasValidApiKey()) {
            return@withContext Result.failure(IllegalStateException("YOUTUBE_API_KEY is not configured."))
        }
        val cleanQ = query.trim()
        if (cleanQ.isEmpty()) {
            return@withContext Result.success(YouTubeSearchBundle(emptyList(), emptyList(), emptyList()))
        }

        try {
            ensureYouTubeSystemOwner()
            val apiOrder = when (sortBy) {
                "Newest" -> "date"
                "Views" -> "viewCount"
                else -> "relevance"
            }
            val apiType = when (typeFilter) {
                "Channels" -> "channel"
                "Videos", "Shorts" -> "video"
                else -> "video,channel"
            }
            val catId = if (categoryFilter != "All" && categoryFilter != "Live") {
                YouTubeParser.categoryIdForName(categoryFilter)
            } else null

            val eventType = if (categoryFilter == "Live") "live" else null
            val effectiveType = if (catId != null || eventType != null) "video" else apiType
            val effectiveEmbeddable = if (effectiveType == "video") "true" else null
            val effectiveQuery = if (typeFilter == "Shorts") "$cleanQ #shorts" else cleanQ

            val searchResp = api.search(
                query = effectiveQuery,
                type = effectiveType,
                videoEmbeddable = effectiveEmbeddable,
                eventType = eventType,
                order = apiOrder,
                videoCategoryId = catId,
                maxResults = 20,
                apiKey = apiKey()
            )

            val videoIds = searchResp.items.mapNotNull { it.id.videoId }.distinct()
            val searchedChannelIds = searchResp.items.mapNotNull { it.id.channelId }.distinct()

            val hydratedVideos = if (videoIds.isNotEmpty()) {
                api.getVideosByIds(ids = videoIds.joinToString(","), apiKey = apiKey()).items
            } else emptyList()

            val allChannelIds = (searchedChannelIds + hydratedVideos.mapNotNull { it.snippet?.channelId }).distinct()
            val channelsMap = fetchAndCacheChannels(allChannelIds)

            val resultVideos = mutableListOf<VideoWithChannel>()
            val resultShorts = mutableListOf<ShortWithChannel>()

            // Sort officially embeddable videos first so playable embeds take priority
            val orderedHydrated = hydratedVideos.sortedByDescending { evaluateVideoRestrictions(it).isPlayable }

            orderedHydrated.forEach { res ->
                val playable = evaluateVideoRestrictions(res).isPlayable
                val durationSec = YouTubeParser.parseIsoDurationSeconds(res.contentDetails?.duration)
                val titleLower = res.snippet?.title?.lowercase().orEmpty()
                val isShortCandidate = typeFilter == "Shorts" ||
                    (durationSec in 1..60 && (titleLower.contains("#short") || res.snippet?.description?.lowercase()?.contains("#short") == true))

                val vidEntity = upsertYouTubeVideo(
                    res = res,
                    channelsMap = channelsMap,
                    sectionTag = "Search",
                    isTrending = false,
                    isFeatured = false
                )
                val chEntity = channelsMap[vidEntity.channelId]
                if (chEntity != null) {
                    resultVideos.add(VideoWithChannel(vidEntity, chEntity))
                    if (isShortCandidate && playable) {
                        val shEntity = upsertYouTubeShort(res, channelsMap)
                        resultShorts.add(ShortWithChannel(shEntity, chEntity))
                    }
                }
            }

            val resultChannels = searchedChannelIds.mapNotNull { channelsMap[it] }
            Result.success(YouTubeSearchBundle(resultVideos, resultShorts, resultChannels))
        } catch (e: HttpException) {
            val syncErr = handleHttpException(e)
            val msg = when (syncErr) {
                is YouTubeSyncState.QuotaExceeded -> syncErr.message
                is YouTubeSyncState.Error -> syncErr.message
                else -> "YouTube search failed."
            }
            Result.failure(IllegalStateException(msg))
        } catch (e: IOException) {
            Result.failure(IllegalStateException("Network unavailable during YouTube search."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshLiveStreamsInternal() {
        val liveSearch = api.search(
            query = "live",
            type = "video",
            videoEmbeddable = "true",
            eventType = "live",
            order = "viewCount",
            maxResults = 14,
            apiKey = apiKey()
        )
        val liveIds = liveSearch.items.mapNotNull { it.id.videoId }.distinct()
        if (liveIds.isEmpty()) return
        val hydrated = api.getVideosByIds(ids = liveIds.joinToString(","), apiKey = apiKey())
        val playableLive = hydrated.items.filter { evaluateVideoRestrictions(it).isPlayable }
        val channelIds = playableLive.mapNotNull { it.snippet?.channelId }.distinct()
        val channelsMap = fetchAndCacheChannels(channelIds)
        playableLive.forEach { res ->
            upsertYouTubeVideo(
                res = res,
                channelsMap = channelsMap,
                sectionTag = "Live Streams",
                isTrending = false,
                isFeatured = false,
                overrideCategory = "Live"
            )
        }
    }

    private suspend fun fetchAndCacheChannels(channelIds: List<String>): Map<String, ChannelEntity> {
        val distinct = channelIds.filter { it.isNotBlank() }.distinct()
        if (distinct.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, ChannelEntity>()
        distinct.chunked(50).forEach { chunk ->
            val resp = runCatching {
                api.getChannelsByIds(ids = chunk.joinToString(","), apiKey = apiKey())
            }.getOrNull()
            resp?.items?.forEach { ch ->
                val snippet = ch.snippet ?: return@forEach
                val entity = ChannelEntity(
                    id = ch.id,
                    ownerUserId = YT_SYSTEM_USER_ID,
                    name = snippet.title,
                    handle = snippet.customUrl ?: "@${snippet.title.replace(" ", "").lowercase()}",
                    description = snippet.description,
                    avatarUri = snippet.thumbnails?.bestUrl(),
                    bannerUri = ch.brandingSettings?.image?.bannerExternalUrl,
                    subscriberCount = ch.statistics?.subscriberCount?.toLongOrNull() ?: 0L,
                    isSuspended = false,
                    sourceType = "YOUTUBE",
                    fetchedAt = System.currentTimeMillis(),
                    createdAt = YouTubeParser.parsePublishedAtMillis(snippet.publishedAt)
                )
                db.channelDao().upsertChannel(entity)
                result[entity.id] = entity
            }
        }
        return result
    }

    private suspend fun upsertYouTubeVideo(
        res: YouTubeVideoResource,
        channelsMap: Map<String, ChannelEntity>,
        sectionTag: String,
        isTrending: Boolean,
        isFeatured: Boolean,
        overrideCategory: String? = null
    ): VideoEntity {
        val snippet = res.snippet
        val channelId = snippet?.channelId ?: "unknown"
        if (channelId !in channelsMap && db.channelDao().getChannelById(channelId) == null) {
            db.channelDao().upsertChannel(
                ChannelEntity(
                    id = channelId,
                    ownerUserId = YT_SYSTEM_USER_ID,
                    name = snippet?.channelTitle ?: "YouTube Channel",
                    handle = "@${(snippet?.channelTitle ?: "creator").replace(" ", "").lowercase()}",
                    sourceType = "YOUTUBE"
                )
            )
        }

        restrictionCache[res.id] = evaluateVideoRestrictions(res)
        val existing = db.videoDao().getVideoById(res.id)
        val isLive = snippet?.liveBroadcastContent == "live" || res.liveStreamingDetails?.concurrentViewers != null
        val category = overrideCategory ?: YouTubeParser.categoryNameForId(snippet?.categoryId)
        val entity = VideoEntity(
            id = res.id,
            channelId = channelId,
            title = snippet?.title ?: existing?.title ?: "Untitled Video",
            description = snippet?.description ?: existing?.description ?: "",
            videoUrl = "https://www.youtube.com/watch?v=${res.id}",
            thumbnailUrl = snippet?.thumbnails?.bestUrl() ?: existing?.thumbnailUrl,
            thumbnailResKey = null,
            durationSeconds = YouTubeParser.parseIsoDurationSeconds(res.contentDetails?.duration),
            category = category,
            tags = snippet?.tags?.joinToString(",") ?: existing?.tags ?: "",
            visibility = "PUBLIC",
            commentsEnabled = existing?.commentsEnabled ?: true,
            isPublished = existing?.isPublished ?: true,
            isFeatured = existing?.isFeatured ?: isFeatured,
            isTrending = existing?.isTrending ?: isTrending,
            isLive = isLive,
            isDemoContent = false,
            sourceType = "YOUTUBE",
            fetchedAt = System.currentTimeMillis(),
            sectionTag = sectionTag,
            captionsText = existing?.captionsText ?: "",
            viewCount = res.statistics?.viewCount?.toLongOrNull() ?: existing?.viewCount ?: 0L,
            likeCount = res.statistics?.likeCount?.toLongOrNull() ?: existing?.likeCount ?: 0L,
            dislikeCount = existing?.dislikeCount ?: 0L,
            createdAt = YouTubeParser.parsePublishedAtMillis(snippet?.publishedAt)
        )
        db.videoDao().upsertVideo(entity)
        return entity
    }

    private suspend fun upsertYouTubeShort(
        res: YouTubeVideoResource,
        channelsMap: Map<String, ChannelEntity>
    ): ShortEntity {
        val snippet = res.snippet
        val channelId = snippet?.channelId ?: "unknown"
        if (channelId !in channelsMap && db.channelDao().getChannelById(channelId) == null) {
            db.channelDao().upsertChannel(
                ChannelEntity(
                    id = channelId,
                    ownerUserId = YT_SYSTEM_USER_ID,
                    name = snippet?.channelTitle ?: "YouTube Creator",
                    handle = "@${(snippet?.channelTitle ?: "creator").replace(" ", "").lowercase()}",
                    sourceType = "YOUTUBE"
                )
            )
        }
        restrictionCache[res.id] = evaluateVideoRestrictions(res)
        val existing = db.shortDao().getShortById(res.id)
        val entity = ShortEntity(
            id = res.id,
            channelId = channelId,
            title = snippet?.title ?: "YouTube Short",
            caption = snippet?.title ?: "",
            musicInfo = snippet?.channelTitle ?: "YouTube Audio",
            videoUrl = "https://www.youtube.com/shorts/${res.id}",
            thumbnailUrl = snippet?.thumbnails?.bestUrl(),
            thumbnailResKey = null,
            durationSeconds = YouTubeParser.parseIsoDurationSeconds(res.contentDetails?.duration).coerceAtLeast(15),
            visibility = "PUBLIC",
            isPublished = existing?.isPublished ?: true,
            isDemoContent = false,
            sourceType = "YOUTUBE",
            fetchedAt = System.currentTimeMillis(),
            viewCount = res.statistics?.viewCount?.toLongOrNull() ?: existing?.viewCount ?: 0L,
            likeCount = res.statistics?.likeCount?.toLongOrNull() ?: existing?.likeCount ?: 0L,
            commentCount = res.statistics?.commentCount?.toLongOrNull() ?: existing?.commentCount ?: 0L,
            createdAt = YouTubeParser.parsePublishedAtMillis(snippet?.publishedAt)
        )
        db.shortDao().upsertShort(entity)
        return entity
    }

    private fun handleHttpException(e: HttpException): YouTubeSyncState {
        val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return if (e.code() == 403 || e.code() == 429 ||
            errorBody.contains("quotaExceeded", ignoreCase = true) ||
            errorBody.contains("rateLimitExceeded", ignoreCase = true)
        ) {
            YouTubeSyncState.QuotaExceeded(
                message = "YouTube API quota exhausted. Showing cached results; retrying automatically.",
                retryAtMs = System.currentTimeMillis() + 60_000L
            )
        } else {
            YouTubeSyncState.Error("YouTube API error (${e.code()}). Showing cached data.")
        }
    }
}
