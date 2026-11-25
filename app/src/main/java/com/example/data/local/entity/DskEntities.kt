package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val displayName: String,
    val avatarUri: String? = null,
    val bio: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admin_roles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AdminRoleEntity(
    @PrimaryKey val userId: String,
    val role: String = "SUPER_ADMIN",
    val grantedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerUserId"])]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val handle: String,
    val description: String = "",
    val avatarUri: String? = null,
    val bannerUri: String? = null,
    val subscriberCount: Long = 0L,
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["category"]),
        Index(value = ["visibility", "isPublished"])
    ]
)
data class VideoEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val thumbnailResKey: String? = null,
    val durationSeconds: Int = 0,
    val category: String = "Technology",
    val tags: String = "",
    val visibility: String = "PUBLIC", // PUBLIC, UNLISTED, PRIVATE
    val commentsEnabled: Boolean = true,
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isLive: Boolean = false,
    val isDemoContent: Boolean = false,
    val sectionTag: String = "Latest Uploads",
    val captionsText: String = "",
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val dislikeCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shorts",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["channelId"]), Index(value = ["visibility", "isPublished"])]
)
data class ShortEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val caption: String,
    val musicInfo: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val thumbnailResKey: String? = null,
    val durationSeconds: Int = 30,
    val visibility: String = "PUBLIC",
    val isPublished: Boolean = true,
    val isDemoContent: Boolean = false,
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["targetType", "targetId"]), Index(value = ["userId"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val targetType: String, // VIDEO or SHORT
    val targetId: String,
    val userId: String,
    val authorName: String,
    val authorAvatarUri: String? = null,
    val body: String,
    val likeCount: Long = 0L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comment_replies",
    foreignKeys = [
        ForeignKey(
            entity = CommentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["commentId"]), Index(value = ["userId"])]
)
data class CommentReplyEntity(
    @PrimaryKey val id: String,
    val commentId: String,
    val userId: String,
    val authorName: String,
    val body: String,
    val likeCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "likes",
    indices = [Index(value = ["userId", "targetType", "targetId"], unique = true)]
)
data class LikeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val targetType: String, // VIDEO, SHORT, COMMENT
    val targetId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dislikes",
    indices = [Index(value = ["userId", "targetType", "targetId"], unique = true)]
)
data class DislikeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val targetType: String, // VIDEO, SHORT
    val targetId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscriberUserId", "channelId"], unique = true), Index(value = ["channelId"])]
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val subscriberUserId: String,
    val channelId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["ownerUserId"])]
)
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val title: String,
    val description: String = "",
    val visibility: String = "PUBLIC", // PUBLIC or PRIVATE
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_videos",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["videoId"])]
)
data class PlaylistVideoEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val videoId: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_history",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "videoId"], unique = true), Index(value = ["videoId"])]
)
data class WatchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val videoId: String,
    val lastPositionMs: Long = 0L,
    val watchedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_later",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "videoId"], unique = true), Index(value = ["videoId"])]
)
data class WatchLaterEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val videoId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["recipientUserId"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val recipientUserId: String,
    val type: String, // UPLOAD, REPLY, LIKE, SUBSCRIPTION
    val title: String,
    val message: String,
    val targetId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reports",
    indices = [Index(value = ["status"]), Index(value = ["targetType", "targetId"])]
)
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterUserId: String,
    val targetType: String, // VIDEO, SHORT, COMMENT, CHANNEL
    val targetId: String,
    val targetLabel: String,
    val reason: String, // Spam, Copyright, Violence, Misleading content, Harassment, Other
    val details: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REMOVED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["userId", "query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

// Domain / UI Composite Models
data class VideoWithChannel(
    val video: VideoEntity,
    val channel: ChannelEntity
)

data class ShortWithChannel(
    val shortItem: ShortEntity,
    val channel: ChannelEntity
)

data class CommentWithReplies(
    val comment: CommentEntity,
    val replies: List<CommentReplyEntity>,
    val isLikedByMe: Boolean = false
)

data class PlaylistSummary(
    val playlist: PlaylistEntity,
    val videoCount: Int
)

data class UserWithProfile(
    val user: UserEntity,
    val profile: ProfileEntity?,
    val isAdmin: Boolean
)
