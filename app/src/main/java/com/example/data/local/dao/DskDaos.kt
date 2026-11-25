package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): ProfileEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM profiles")
    fun observeAllProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("UPDATE users SET isBlocked = :blocked WHERE id = :userId")
    suspend fun setUserBlocked(userId: String, blocked: Boolean)

    @Query("SELECT * FROM admin_roles WHERE userId = :userId LIMIT 1")
    fun observeAdminRole(userId: String): Flow<AdminRoleEntity?>

    @Query("SELECT * FROM admin_roles WHERE userId = :userId LIMIT 1")
    suspend fun getAdminRole(userId: String): AdminRoleEntity?

    @Query("SELECT COUNT(*) FROM admin_roles")
    suspend fun getAdminCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminRole(role: AdminRoleEntity)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY subscriberCount DESC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun observeChannel(channelId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE ownerUserId = :ownerUserId LIMIT 1")
    fun observeChannelByOwner(ownerUserId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE ownerUserId = :ownerUserId LIMIT 1")
    suspend fun getChannelByOwner(ownerUserId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET subscriberCount = MAX(0, subscriberCount + :delta) WHERE id = :channelId")
    suspend fun adjustSubscribers(channelId: String, delta: Long)

    @Query("UPDATE channels SET isSuspended = :suspended WHERE id = :channelId")
    suspend fun setChannelSuspended(channelId: String, suspended: Boolean)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE visibility = 'PUBLIC' AND isPublished = 1 ORDER BY createdAt DESC")
    fun observePublicVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun observeAllVideosAdmin(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    fun observeVideo(videoId: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY createdAt DESC")
    fun observeChannelVideos(channelId: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideo(videoId: String)

    @Query("UPDATE videos SET viewCount = viewCount + 1 WHERE id = :videoId")
    suspend fun incrementView(videoId: String)

    @Query("UPDATE videos SET likeCount = MAX(0, likeCount + :delta) WHERE id = :videoId")
    suspend fun adjustLikes(videoId: String, delta: Long)

    @Query("UPDATE videos SET dislikeCount = MAX(0, dislikeCount + :delta) WHERE id = :videoId")
    suspend fun adjustDislikes(videoId: String, delta: Long)

    @Query("UPDATE videos SET isPublished = :published WHERE id = :videoId")
    suspend fun setPublished(videoId: String, published: Boolean)

    @Query("UPDATE videos SET isFeatured = :featured WHERE id = :videoId")
    suspend fun setFeatured(videoId: String, featured: Boolean)

    @Query("UPDATE videos SET isTrending = :trending WHERE id = :videoId")
    suspend fun setTrending(videoId: String, trending: Boolean)
}

@Dao
interface ShortDao {
    @Query("SELECT * FROM shorts WHERE visibility = 'PUBLIC' AND isPublished = 1 ORDER BY createdAt DESC")
    fun observePublicShorts(): Flow<List<ShortEntity>>

    @Query("SELECT * FROM shorts ORDER BY createdAt DESC")
    fun observeAllShortsAdmin(): Flow<List<ShortEntity>>

    @Query("SELECT * FROM shorts WHERE channelId = :channelId ORDER BY createdAt DESC")
    fun observeChannelShorts(channelId: String): Flow<List<ShortEntity>>

    @Query("SELECT * FROM shorts WHERE id = :shortId LIMIT 1")
    suspend fun getShortById(shortId: String): ShortEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShort(shortItem: ShortEntity)

    @Query("DELETE FROM shorts WHERE id = :shortId")
    suspend fun deleteShort(shortId: String)

    @Query("UPDATE shorts SET viewCount = viewCount + 1 WHERE id = :shortId")
    suspend fun incrementView(shortId: String)

    @Query("UPDATE shorts SET likeCount = MAX(0, likeCount + :delta) WHERE id = :shortId")
    suspend fun adjustLikes(shortId: String, delta: Long)

    @Query("UPDATE shorts SET commentCount = MAX(0, commentCount + :delta) WHERE id = :shortId")
    suspend fun adjustComments(shortId: String, delta: Long)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE targetType = :targetType AND targetId = :targetId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeComments(targetType: String, targetId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comment_replies ORDER BY createdAt ASC")
    fun observeAllReplies(): Flow<List<CommentReplyEntity>>

    @Query("SELECT * FROM comments WHERE id = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): CommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: CommentReplyEntity)

    @Query("UPDATE comments SET isDeleted = 1 WHERE id = :commentId")
    suspend fun softDeleteComment(commentId: String)

    @Query("UPDATE comments SET likeCount = MAX(0, likeCount + :delta) WHERE id = :commentId")
    suspend fun adjustCommentLikes(commentId: String, delta: Long)
}

@Dao
interface InteractionDao {
    @Query("SELECT * FROM likes WHERE userId = :userId")
    fun observeUserLikes(userId: String): Flow<List<LikeEntity>>

    @Query("SELECT * FROM likes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getLike(userId: String, targetType: String, targetId: String): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId")
    suspend fun deleteLike(userId: String, targetType: String, targetId: String)

    @Query("SELECT * FROM dislikes WHERE userId = :userId")
    fun observeUserDislikes(userId: String): Flow<List<DislikeEntity>>

    @Query("SELECT * FROM dislikes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getDislike(userId: String, targetType: String, targetId: String): DislikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislike(dislike: DislikeEntity)

    @Query("DELETE FROM dislikes WHERE userId = :userId AND targetType = :targetType AND targetId = :targetId")
    suspend fun deleteDislike(userId: String, targetType: String, targetId: String)

    @Query("SELECT * FROM subscriptions WHERE subscriberUserId = :userId")
    fun observeUserSubscriptions(userId: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT subscriberUserId FROM subscriptions WHERE channelId = :channelId")
    suspend fun getSubscriberUserIds(channelId: String): List<String>

    @Query("SELECT * FROM subscriptions WHERE subscriberUserId = :userId AND channelId = :channelId LIMIT 1")
    suspend fun getSubscription(userId: String, channelId: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE subscriberUserId = :userId AND channelId = :channelId")
    suspend fun deleteSubscription(userId: String, channelId: String)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM playlists WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun observeUserPlaylists(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE visibility = 'PUBLIC' AND ownerUserId = :userId ORDER BY createdAt DESC")
    fun observePublicPlaylistsByOwner(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_videos ORDER BY position ASC, addedAt ASC")
    fun observeAllPlaylistVideos(): Flow<List<PlaylistVideoEntity>>

    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    suspend fun getPlaylistVideos(playlistId: String): List<PlaylistVideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistVideo(item: PlaylistVideoEntity)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removePlaylistVideo(playlistId: String, videoId: String)

    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY watchedAt DESC")
    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearWatchHistory(userId: String)

    @Query("SELECT * FROM watch_later WHERE userId = :userId ORDER BY addedAt DESC")
    fun observeWatchLater(userId: String): Flow<List<WatchLaterEntity>>

    @Query("SELECT * FROM watch_later WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    suspend fun getWatchLater(userId: String, videoId: String): WatchLaterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchLater(entry: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE userId = :userId AND videoId = :videoId")
    suspend fun removeWatchLater(userId: String, videoId: String)

    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY searchedAt DESC LIMIT 15")
    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchQuery(id: String)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearSearchHistory(userId: String)
}

@Dao
interface SystemDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("SELECT * FROM notifications WHERE recipientUserId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientUserId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)
}
