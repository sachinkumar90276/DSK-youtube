package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.data.security.AuthCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        AdminRoleEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        VideoEntity::class,
        ShortEntity::class,
        CommentEntity::class,
        CommentReplyEntity::class,
        LikeEntity::class,
        DislikeEntity::class,
        SubscriptionEntity::class,
        PlaylistEntity::class,
        PlaylistVideoEntity::class,
        WatchHistoryEntity::class,
        WatchLaterEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DskDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao
    abstract fun videoDao(): VideoDao
    abstract fun shortDao(): ShortDao
    abstract fun commentDao(): CommentDao
    abstract fun interactionDao(): InteractionDao
    abstract fun libraryDao(): LibraryDao
    abstract fun systemDao(): SystemDao

    companion object {
        @Volatile
        private var INSTANCE: DskDatabase? = null

        fun getInstance(context: Context): DskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DskDatabase::class.java,
                    "dsk_youtube.db"
                )
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = INSTANCE ?: return@launch
                seedInitialData(database)
            }
        }
    }
}

private suspend fun seedInitialData(db: DskDatabase) {
    val now = System.currentTimeMillis()
    val hour = 3_600_000L
    val day = 86_400_000L

    // Seed Categories
    val categories = listOf(
        "Music", "Gaming", "Movies", "News", "Live",
        "Comedy", "Technology", "Education", "Sports", "Travel"
    )
    categories.forEachIndexed { index, name ->
        db.systemDao().upsertCategory(
            CategoryEntity(
                id = "cat_${name.lowercase()}",
                name = name,
                sortOrder = index + 1
            )
        )
    }

    // Seed Demo Creator Accounts (never expose passwords; used purely as creator owners for demo streams)
    val demoCreators = listOf(
        Triple("creator_tech", "tech.studio@demo.dsk", "DSK Tech Lab"),
        Triple("creator_music", "pulse.stage@demo.dsk", "Pulse Stage Live"),
        Triple("creator_cinema", "horizon.cinema@demo.dsk", "Horizon Cinema")
    )
    demoCreators.forEach { (uid, email, displayName) ->
        val salt = AuthCrypto.generateSalt()
        val hash = AuthCrypto.hashPassword(AuthCrypto.generateSalt() + uid, salt)
        db.userDao().insertUser(
            UserEntity(id = uid, email = email, passwordHash = hash, salt = salt, createdAt = now - 30 * day)
        )
        db.userDao().upsertProfile(
            ProfileEntity(
                userId = uid,
                username = uid,
                displayName = displayName,
                bio = "Official demo creator channel showcasing free open video streams on DSK YouTube."
            )
        )
    }

    // Seed Channels
    db.channelDao().upsertChannel(
        ChannelEntity(
            id = "ch_tech",
            ownerUserId = "creator_tech",
            name = "DSK Tech Lab",
            handle = "@dsktechlab",
            description = "Deep dives into open hardware, optics, mobile engineering, and free creator tools.",
            subscriberCount = 148200L,
            createdAt = now - 90 * day
        )
    )
    db.channelDao().upsertChannel(
        ChannelEntity(
            id = "ch_music",
            ownerUserId = "creator_music",
            name = "Pulse Stage Live",
            handle = "@pulsestage",
            description = "Unfiltered live analog synth sessions, stage lighting design, and acoustic performances.",
            subscriberCount = 92400L,
            createdAt = now - 60 * day
        )
    )
    db.channelDao().upsertChannel(
        ChannelEntity(
            id = "ch_cinema",
            ownerUserId = "creator_cinema",
            name = "Horizon Cinema",
            handle = "@horizoncinema",
            description = "Open-source animated short films, aerial coastal documentaries, and 4K travel reels.",
            subscriberCount = 315800L,
            createdAt = now - 120 * day
        )
    )

    // Seed Real Playable Demo Videos (Public Sample Streams)
    val demoVideos = listOf(
        VideoEntity(
            id = "vid_1",
            channelId = "ch_tech",
            title = "Next-Gen Optical Sensors & Custom Circuit Architecture Explained [Demo]",
            description = "Demo Stream: Explore how modern optical lenses pair with high-speed signal buses for crystal-clear low-light capture. Watch completely free on DSK YouTube.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailResKey = "img_thumb_tech",
            durationSeconds = 15,
            category = "Technology",
            tags = "hardware,optics,engineering,demo",
            isFeatured = true,
            isTrending = true,
            isDemoContent = true,
            sectionTag = "Trending Videos",
            captionsText = "00:00 Welcome to DSK Tech Lab — Watch Everything, Pay Nothing.\n00:05 Exploring precision glass optics and crimson signal pathways.",
            viewCount = 482900L,
            likeCount = 34200L,
            dislikeCount = 310L,
            createdAt = now - 4 * hour
        ),
        VideoEntity(
            id = "vid_2",
            channelId = "ch_music",
            title = "Analog Synth Session: Midnight Crimson Wave (Live Stage Set) [Demo]",
            description = "Demo Stream: Live modular synthesizer showcase recorded in full stereo with laser choreography. Zero paywalls, zero ads.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            thumbnailResKey = "img_thumb_music",
            durationSeconds = 60,
            category = "Music",
            tags = "synth,electronic,live,music,demo",
            isFeatured = true,
            isTrending = true,
            isLive = true,
            isDemoContent = true,
            sectionTag = "Live Streams",
            captionsText = "00:00 Live from Pulse Stage.\n00:12 Analog oscillator sweep and sub-bass pulse.",
            viewCount = 219400L,
            likeCount = 19800L,
            dislikeCount = 120L,
            createdAt = now - 8 * hour
        ),
        VideoEntity(
            id = "vid_3",
            channelId = "ch_cinema",
            title = "Volcanic Cliffs at Golden Hour — Nordic Aerial Documentary [Demo]",
            description = "Demo Stream: Soar over mist-covered basalt sea stacks and crashing Atlantic swells in this open travel cinema reel.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailResKey = "img_thumb_cinema",
            durationSeconds = 15,
            category = "Travel",
            tags = "travel,aerial,nature,cinema,demo",
            isTrending = true,
            isDemoContent = true,
            sectionTag = "Recommended For You",
            captionsText = "00:00 Dawn breaks across the volcanic coastline.\n00:08 Filmic aerial pass above the sea mist.",
            viewCount = 875100L,
            likeCount = 64100L,
            dislikeCount = 420L,
            createdAt = now - 1 * day
        ),
        VideoEntity(
            id = "vid_4",
            channelId = "ch_cinema",
            title = "Big Buck Bunny — Open Source 3D Animated Feature Short [Demo]",
            description = "Demo Stream: Classic Blender Foundation open movie project streaming freely for everyone.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnailResKey = "img_thumb_cinema",
            durationSeconds = 596,
            category = "Movies",
            tags = "animation,movies,open-source,comedy,demo",
            isDemoContent = true,
            sectionTag = "Most Viewed",
            captionsText = "00:00 Big Buck Bunny — Open Movie Project.\n00:25 Peaceful morning in the meadow.",
            viewCount = 1420000L,
            likeCount = 112000L,
            dislikeCount = 1200L,
            createdAt = now - 3 * day
        ),
        VideoEntity(
            id = "vid_5",
            channelId = "ch_tech",
            title = "Elephant's Dream — Experimental Open Cinema & Sound Design [Demo]",
            description = "Demo Stream: Exploring surreal mechanical worlds and spatial audio mixing in open cinema.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            thumbnailResKey = "img_channel_banner",
            durationSeconds = 653,
            category = "Education",
            tags = "sound,education,cinema,demo",
            isDemoContent = true,
            sectionTag = "Latest Uploads",
            captionsText = "00:00 Exploring sound design and procedural animation.",
            viewCount = 156000L,
            likeCount = 9400L,
            dislikeCount = 190L,
            createdAt = now - 2 * day
        ),
        VideoEntity(
            id = "vid_6",
            channelId = "ch_music",
            title = "Midnight Encore — High Energy Festival Lighting & Beats [Demo]",
            description = "Demo Stream: Behind the scenes of stage lighting synchronization and electronic rhythms.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
            thumbnailResKey = "img_thumb_music",
            durationSeconds = 15,
            category = "Music",
            tags = "music,festival,beats,demo",
            isDemoContent = true,
            sectionTag = "Music Videos",
            captionsText = "00:00 Festival rhythm check and crimson stage wash.",
            viewCount = 310500L,
            likeCount = 24800L,
            dislikeCount = 180L,
            createdAt = now - 5 * day
        ),
        VideoEntity(
            id = "vid_7",
            channelId = "ch_tech",
            title = "Extreme Thermal Stress Test & Frame-Rate Telemetry Breakdown [Demo]",
            description = "Demo Stream: Benchmarking mobile GPU pipelines under sustained 60fps shader workloads.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            thumbnailResKey = "img_thumb_tech",
            durationSeconds = 15,
            category = "Gaming",
            tags = "gaming,benchmark,gpu,tech,demo",
            isDemoContent = true,
            sectionTag = "Recommended For You",
            captionsText = "00:00 Sustained GPU telemetry run at 60fps.",
            viewCount = 278400L,
            likeCount = 18900L,
            dislikeCount = 240L,
            createdAt = now - 6 * day
        ),
        VideoEntity(
            id = "vid_8",
            channelId = "ch_cinema",
            title = "Rally Trail Precision: Dust, Mud & Suspension Telemetry [Demo]",
            description = "Demo Stream: Tracking all-wheel traction and high-speed dirt cornering out in the backcountry.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            thumbnailResKey = "img_thumb_cinema",
            durationSeconds = 594,
            category = "Sports",
            tags = "sports,rally,cars,travel,demo",
            isDemoContent = true,
            sectionTag = "Latest Uploads",
            captionsText = "00:00 Backcountry dirt test run.",
            viewCount = 198300L,
            likeCount = 14200L,
            dislikeCount = 150L,
            createdAt = now - 7 * day
        )
    )
    demoVideos.forEach { db.videoDao().upsertVideo(it) }

    // Seed Demo Shorts
    val demoShorts = listOf(
        ShortEntity(
            id = "short_1",
            channelId = "ch_tech",
            title = "Crimson Optics Macro",
            caption = "Macro lens + neon PCB traces in 15 seconds [Demo Short]",
            musicInfo = "Pulse Synthwave - DSK Audio",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailResKey = "img_thumb_tech",
            durationSeconds = 15,
            isDemoContent = true,
            viewCount = 924000L,
            likeCount = 78200L,
            commentCount = 2L,
            createdAt = now - 2 * hour
        ),
        ShortEntity(
            id = "short_2",
            channelId = "ch_music",
            title = "Live Laser Drop",
            caption = "When the sub-bass hits the crimson stage lasers [Demo Short]",
            musicInfo = "Midnight Modular (Live) - Pulse Stage",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
            thumbnailResKey = "img_thumb_music",
            durationSeconds = 15,
            isDemoContent = true,
            viewCount = 651000L,
            likeCount = 54300L,
            commentCount = 1L,
            createdAt = now - 6 * hour
        ),
        ShortEntity(
            id = "short_3",
            channelId = "ch_cinema",
            title = "Atlantic Mist Drone Pass",
            caption = "Golden hour drone glide over volcanic sea cliffs [Demo Short]",
            musicInfo = "Horizon Echoes - Ambient Mix",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailResKey = "img_thumb_cinema",
            durationSeconds = 15,
            isDemoContent = true,
            viewCount = 1280000L,
            likeCount = 114000L,
            commentCount = 1L,
            createdAt = now - 12 * hour
        ),
        ShortEntity(
            id = "short_4",
            channelId = "ch_tech",
            title = "Thermal Meltdown Run",
            caption = "Testing peak frame pacing on mobile silicon [Demo Short]",
            musicInfo = "Cyber Grid - Original Sound",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            thumbnailResKey = "img_channel_banner",
            durationSeconds = 15,
            isDemoContent = true,
            viewCount = 412000L,
            likeCount = 31900L,
            commentCount = 0L,
            createdAt = now - 1 * day
        )
    )
    demoShorts.forEach { db.shortDao().upsertShort(it) }

    // Seed Initial Comments & Replies
    db.commentDao().insertComment(
        CommentEntity(
            id = "cmt_1",
            targetType = "VIDEO",
            targetId = "vid_1",
            userId = "creator_cinema",
            authorName = "Horizon Cinema",
            body = "The glass-to-sensor clarity in this demo looks razor sharp — and zero paywall to watch!",
            likeCount = 42,
            createdAt = now - 2 * hour
        )
    )
    db.commentDao().insertReply(
        CommentReplyEntity(
            id = "rep_1",
            commentId = "cmt_1",
            userId = "creator_tech",
            authorName = "DSK Tech Lab",
            body = "Appreciate it! Everything on DSK YouTube stays 100% free for creators and viewers.",
            likeCount = 18,
            createdAt = now - 1 * hour
        )
    )
    db.commentDao().insertComment(
        CommentEntity(
            id = "cmt_2",
            targetType = "SHORT",
            targetId = "short_1",
            userId = "creator_music",
            authorName = "Pulse Stage Live",
            body = "That crimson lighting matches the DSK aesthetic so well!",
            likeCount = 19,
            createdAt = now - 1 * hour
        )
    )
}
