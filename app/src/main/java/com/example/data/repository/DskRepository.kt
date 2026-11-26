package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.DskDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.UserSettings
import com.example.data.local.entity.*
import com.example.data.remote.VideoRestrictionInfo
import com.example.data.remote.YouTubeSearchBundle
import com.example.data.remote.YouTubeSyncManager
import com.example.data.remote.YouTubeSyncState
import com.example.data.security.AuthCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DskRepository(
    private val context: Context,
    private val db: DskDatabase,
    private val settingsStore: SettingsDataStore,
    private val youtubeSync: YouTubeSyncManager? = null
) {
    companion object {
        const val MAX_FREE_UPLOAD_BYTES = 100L * 1024L * 1024L // 100 MB free local/tier limit
    }

    private val userDao = db.userDao()
    private val channelDao = db.channelDao()
    private val videoDao = db.videoDao()
    private val shortDao = db.shortDao()
    private val commentDao = db.commentDao()
    private val interactionDao = db.interactionDao()
    private val libraryDao = db.libraryDao()
    private val systemDao = db.systemDao()

    val settingsFlow: Flow<UserSettings> = settingsStore.settingsFlow

    val categoriesFlow: Flow<List<CategoryEntity>> = systemDao.observeCategories()
    val channelsFlow: Flow<List<ChannelEntity>> = channelDao.observeAllChannels()
    val publicVideosFlow: Flow<List<VideoEntity>> = videoDao.observePublicVideos()
    val publicShortsFlow: Flow<List<ShortEntity>> = shortDao.observePublicShorts()
    val allCommentsFlow: Flow<List<CommentEntity>> = commentDao.observeAllComments()
    val allRepliesFlow: Flow<List<CommentReplyEntity>> = commentDao.observeAllReplies()

    // Admin streams (UI only renders when isAdmin == true; mutations enforce requireAdmin)
    val allUsersFlow: Flow<List<UserEntity>> = userDao.observeAllUsers()
    val allProfilesFlow: Flow<List<ProfileEntity>> = userDao.observeAllProfiles()
    val allVideosAdminFlow: Flow<List<VideoEntity>> = videoDao.observeAllVideosAdmin()
    val allShortsAdminFlow: Flow<List<ShortEntity>> = shortDao.observeAllShortsAdmin()
    val allReportsFlow: Flow<List<ReportEntity>> = systemDao.observeAllReports()

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUser(userId)
    fun observeProfile(userId: String): Flow<ProfileEntity?> = userDao.observeProfile(userId)
    fun observeIsAdmin(userId: String): Flow<Boolean> = userDao.observeAdminRole(userId).map { it != null }
    fun observeMyChannel(userId: String): Flow<ChannelEntity?> = channelDao.observeChannelByOwner(userId)
    fun observeUserLikes(userId: String): Flow<List<LikeEntity>> = interactionDao.observeUserLikes(userId)
    fun observeUserDislikes(userId: String): Flow<List<DislikeEntity>> = interactionDao.observeUserDislikes(userId)
    fun observeSubscriptions(userId: String): Flow<List<SubscriptionEntity>> = interactionDao.observeUserSubscriptions(userId)
    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntity>> = libraryDao.observeWatchHistory(userId)
    fun observeWatchLater(userId: String): Flow<List<WatchLaterEntity>> = libraryDao.observeWatchLater(userId)
    fun observePlaylists(userId: String): Flow<List<PlaylistEntity>> = libraryDao.observeUserPlaylists(userId)
    val allPlaylistVideosFlow: Flow<List<PlaylistVideoEntity>> = libraryDao.observeAllPlaylistVideos()
    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntity>> = libraryDao.observeSearchHistory(userId)
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> = systemDao.observeNotifications(userId)

    // --- Live YouTube Data API v3 Sync ---
    fun hasYouTubeApiKey(): Boolean = youtubeSync?.hasValidApiKey() == true

    suspend fun syncYouTubeHome(force: Boolean = false): YouTubeSyncState {
        return youtubeSync?.refreshHomeFeeds(force) ?: YouTubeSyncState.MissingApiKey
    }

    suspend fun syncYouTubeCategory(category: String, force: Boolean = false): YouTubeSyncState {
        return youtubeSync?.refreshCategory(category, force) ?: YouTubeSyncState.MissingApiKey
    }

    suspend fun syncYouTubeChannel(channelId: String, force: Boolean = false): YouTubeSyncState {
        return youtubeSync?.refreshChannel(channelId, force) ?: YouTubeSyncState.MissingApiKey
    }

    suspend fun fetchVideoById(videoId: String): VideoWithChannel? {
        val cached = videoDao.getVideoById(videoId)
        if (cached != null) {
            val ch = channelDao.getChannelById(cached.channelId)
            if (ch != null) return VideoWithChannel(cached, ch)
        }
        return youtubeSync?.fetchAndCacheVideoById(videoId)
    }

    suspend fun checkVideoRestrictions(videoId: String): VideoRestrictionInfo {
        return youtubeSync?.checkVideoRestrictions(videoId) ?: VideoRestrictionInfo(isPlayable = true)
    }

    suspend fun searchYouTubeLive(
        query: String,
        typeFilter: String,
        categoryFilter: String,
        sortBy: String
    ): Result<YouTubeSearchBundle> {
        return youtubeSync?.searchYouTubeLive(query, typeFilter, categoryFilter, sortBy)
            ?: Result.failure(IllegalStateException("YouTube API sync unavailable."))
    }

    // --- Authentication ---
    suspend fun register(email: String, username: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            val cleanUser = username.trim()
            require(AuthCrypto.isValidEmail(cleanEmail)) { "Please enter a valid email address." }
            require(cleanUser.length >= 3) { "Username must be at least 3 characters." }
            require(AuthCrypto.isValidPassword(password)) { "Password must be at least 6 characters." }
            if (userDao.getUserByEmail(cleanEmail) != null) {
                throw IllegalStateException("An account with this email already exists.")
            }

            val userId = "usr_${UUID.randomUUID().toString().take(12)}"
            val salt = AuthCrypto.generateSalt()
            val hash = AuthCrypto.hashPassword(password, salt)
            val user = UserEntity(
                id = userId,
                email = cleanEmail,
                passwordHash = hash,
                salt = salt
            )
            userDao.insertUser(user)
            userDao.upsertProfile(
                ProfileEntity(
                    userId = userId,
                    username = cleanUser.lowercase().replace(" ", "_"),
                    displayName = cleanUser,
                    bio = "Creator on DSK YouTube — 100% Free Video Platform."
                )
            )
            // Automatically provision creator channel
            val channelId = "ch_${UUID.randomUUID().toString().take(10)}"
            channelDao.upsertChannel(
                ChannelEntity(
                    id = channelId,
                    ownerUserId = userId,
                    name = cleanUser,
                    handle = "@${cleanUser.lowercase().replace(Regex("[^a-z0-9_]"), "")}",
                    description = "Welcome to $cleanUser's official DSK YouTube channel."
                )
            )
            // First registered user becomes platform owner admin (zero hardcoded credentials)
            if (userDao.getAdminCount() == 0) {
                userDao.insertAdminRole(AdminRoleEntity(userId = userId, role = "SUPER_ADMIN"))
            }
            settingsStore.setCurrentUserId(userId)
            user
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            require(AuthCrypto.isValidEmail(cleanEmail)) { "Please enter a valid email address." }
            require(password.isNotEmpty()) { "Password is required." }
            val user = userDao.getUserByEmail(cleanEmail)
                ?: throw IllegalArgumentException("Invalid email or password.")
            if (user.isBlocked) {
                throw SecurityException("This account has been suspended by moderation.")
            }
            if (!AuthCrypto.verifyPassword(password, user.salt, user.passwordHash)) {
                throw IllegalArgumentException("Invalid email or password.")
            }
            settingsStore.setCurrentUserId(user.id)
            user
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            require(AuthCrypto.isValidEmail(cleanEmail)) { "Please enter a valid email address." }
            require(AuthCrypto.isValidPassword(newPassword)) { "New password must be at least 6 characters." }
            val user = userDao.getUserByEmail(cleanEmail)
                ?: throw IllegalArgumentException("No account found for $cleanEmail.")
            val newSalt = AuthCrypto.generateSalt()
            val newHash = AuthCrypto.hashPassword(newPassword, newSalt)
            userDao.updateUser(user.copy(salt = newSalt, passwordHash = newHash))
        }
    }

    suspend fun updateProfileAndChannel(
        userId: String,
        displayName: String,
        username: String,
        bio: String,
        avatarUri: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(displayName.trim().length >= 2) { "Display name is too short." }
            require(username.trim().length >= 3) { "Username must be at least 3 characters." }
            val existingProfile = userDao.getProfile(userId)
            val savedAvatarPath = if (avatarUri != null) {
                copySmallAssetToInternal(avatarUri, "avatars", "avatar_$userId.jpg")
            } else {
                existingProfile?.avatarUri
            }
            val updatedProfile = ProfileEntity(
                userId = userId,
                username = username.trim().lowercase().replace(" ", "_"),
                displayName = displayName.trim(),
                avatarUri = savedAvatarPath,
                bio = bio.trim(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.upsertProfile(updatedProfile)
            channelDao.getChannelByOwner(userId)?.let { ch ->
                channelDao.upsertChannel(
                    ch.copy(
                        name = displayName.trim(),
                        handle = "@${updatedProfile.username}",
                        description = bio.trim().ifBlank { ch.description },
                        avatarUri = savedAvatarPath ?: ch.avatarUri
                    )
                )
            }
            Unit
        }
    }

    suspend fun changePassword(userId: String, currentPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(AuthCrypto.isValidPassword(newPass)) { "New password must be at least 6 characters." }
            val user = userDao.getUserById(userId) ?: throw IllegalStateException("User not found.")
            if (!AuthCrypto.verifyPassword(currentPass, user.salt, user.passwordHash)) {
                throw IllegalArgumentException("Current password is incorrect.")
            }
            val newSalt = AuthCrypto.generateSalt()
            val newHash = AuthCrypto.hashPassword(newPass, newSalt)
            userDao.updateUser(user.copy(salt = newSalt, passwordHash = newHash))
        }
    }

    suspend fun logout() {
        settingsStore.setCurrentUserId(null)
    }

    // --- Media Upload (Real local storage with size limits & chunked progress) ---
    suspend fun uploadMedia(
        userId: String,
        isShort: Boolean,
        videoUri: Uri?,
        directVideoUrl: String?,
        thumbnailUri: Uri?,
        title: String,
        description: String,
        category: String,
        tags: String,
        visibility: String,
        commentsEnabled: Boolean,
        musicInfo: String,
        onProgress: (Int, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val user = userDao.getUserById(userId) ?: throw IllegalStateException("Please sign in to upload.")
            if (user.isBlocked) throw SecurityException("Your account is restricted from uploading.")
            val channel = channelDao.getChannelByOwner(userId)
                ?: throw IllegalStateException("Creator channel not found.")
            if (channel.isSuspended) throw SecurityException("Your channel is currently suspended.")

            val cleanTitle = title.trim()
            require(cleanTitle.length >= 3) { "Title must be at least 3 characters." }

            val storedVideoPath: String
            if (videoUri != null) {
                onProgress(5, "Verifying video file size & format...")
                val size = queryUriSize(videoUri)
                if (size > MAX_FREE_UPLOAD_BYTES) {
                    throw IllegalStateException("Free storage limit exceeded: max 100 MB per upload. Try a smaller clip or direct MP4 URL.")
                }
                val fileId = UUID.randomUUID().toString().take(10)
                val destDir = File(context.filesDir, "uploads/videos").apply { mkdirs() }
                val destFile = File(destDir, "${if (isShort) "short" else "vid"}_$fileId.mp4")
                copyUriWithProgress(videoUri, destFile, size) { pct ->
                    onProgress(pct, "Storing video ($pct%)...")
                }
                if (!destFile.exists() || destFile.length() == 0L) {
                    throw IllegalStateException("Video storage failed. File could not be written.")
                }
                storedVideoPath = Uri.fromFile(destFile).toString()
            } else if (!directVideoUrl.isNullOrBlank()) {
                val url = directVideoUrl.trim()
                require(AuthCrypto.isValidMediaUrl(url)) {
                    "Enter a valid http(s):// or content:// MP4/WebM video URL."
                }
                onProgress(50, "Validating stream URL...")
                storedVideoPath = url
                onProgress(90, "Linking stream metadata...")
            } else {
                throw IllegalArgumentException("Please select a video file or provide a direct MP4/WebM URL.")
            }

            val storedThumbPath = thumbnailUri?.let {
                onProgress(95, "Saving thumbnail...")
                copySmallAssetToInternal(it, "uploads/thumbs", "thumb_${UUID.randomUUID().toString().take(10)}.jpg")
            }

            val contentId = if (isShort) {
                val sid = "short_${UUID.randomUUID().toString().take(10)}"
                shortDao.upsertShort(
                    ShortEntity(
                        id = sid,
                        channelId = channel.id,
                        title = cleanTitle,
                        caption = description.trim().ifBlank { cleanTitle },
                        musicInfo = musicInfo.trim().ifBlank { "Original Sound - ${channel.name}" },
                        videoUrl = storedVideoPath,
                        thumbnailUrl = storedThumbPath,
                        thumbnailResKey = if (storedThumbPath == null) "img_thumb_tech" else null,
                        durationSeconds = 30,
                        visibility = visibility,
                        isPublished = true,
                        isDemoContent = false,
                        sourceType = "USER_UPLOAD"
                    )
                )
                sid
            } else {
                val vid = "vid_${UUID.randomUUID().toString().take(10)}"
                videoDao.upsertVideo(
                    VideoEntity(
                        id = vid,
                        channelId = channel.id,
                        title = cleanTitle,
                        description = description.trim(),
                        videoUrl = storedVideoPath,
                        thumbnailUrl = storedThumbPath,
                        thumbnailResKey = if (storedThumbPath == null) "img_thumb_tech" else null,
                        durationSeconds = 60,
                        category = category,
                        tags = tags.trim(),
                        visibility = visibility,
                        commentsEnabled = commentsEnabled,
                        isPublished = true,
                        isDemoContent = false,
                        sourceType = "USER_UPLOAD",
                        sectionTag = "Latest Uploads"
                    )
                )
                vid
            }

            // Notify uploader & subscribers when public
            systemDao.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(10)}",
                    recipientUserId = userId,
                    type = "UPLOAD",
                    title = "Upload complete",
                    message = "\"$cleanTitle\" is now stored and ready on DSK YouTube.",
                    targetId = if (!isShort) contentId else null
                )
            )
            if (visibility == "PUBLIC") {
                val subs = interactionDao.getSubscriberUserIds(channel.id)
                subs.filter { it != userId }.forEach { subUid ->
                    systemDao.insertNotification(
                        NotificationEntity(
                            id = "notif_${UUID.randomUUID().toString().take(10)}",
                            recipientUserId = subUid,
                            type = "SUBSCRIPTION",
                            title = "${channel.name} uploaded a new ${if (isShort) "Short" else "video"}",
                            message = cleanTitle,
                            targetId = if (!isShort) contentId else null
                        )
                    )
                }
            }
            onProgress(100, "Upload complete!")
            contentId
        }
    }

    private fun queryUriSize(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
            }
        }
        return -1L
    }

    private fun copyUriWithProgress(uri: Uri, dest: File, knownSize: Long, onProgress: (Int) -> Unit) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open selected video file.")
        input.use { stream ->
            FileOutputStream(dest).use { out ->
                val buffer = ByteArray(64 * 1024)
                var totalRead = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    totalRead += read
                    if (totalRead > MAX_FREE_UPLOAD_BYTES) {
                        dest.delete()
                        throw IllegalStateException("Free storage limit exceeded (max 100 MB per file).")
                    }
                    out.write(buffer, 0, read)
                    val pct = if (knownSize > 0) {
                        ((totalRead * 90) / knownSize).toInt().coerceIn(10, 92)
                    } else {
                        ((totalRead / (1024 * 1024)).toInt() * 4).coerceIn(15, 90)
                    }
                    onProgress(pct)
                }
            }
        }
    }

    private fun copySmallAssetToInternal(uri: Uri, folder: String, filename: String): String {
        val dir = File(context.filesDir, folder).apply { mkdirs() }
        val file = File(dir, filename)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to read selected image.")
        input.use { stream ->
            FileOutputStream(file).use { out ->
                stream.copyTo(out)
            }
        }
        return Uri.fromFile(file).toString()
    }

    // --- Playback & Engagement ---
    suspend fun recordVideoView(videoId: String, userId: String?, privateHistory: Boolean) = withContext(Dispatchers.IO) {
        videoDao.incrementView(videoId)
        if (!userId.isNullOrBlank() && !privateHistory) {
            libraryDao.upsertWatchHistory(
                WatchHistoryEntity(
                    id = "wh_${userId}_$videoId",
                    userId = userId,
                    videoId = videoId,
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun recordShortView(shortId: String) = withContext(Dispatchers.IO) {
        shortDao.incrementView(shortId)
    }

    suspend fun toggleLike(userId: String, targetType: String, targetId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val existingLike = interactionDao.getLike(userId, targetType, targetId)
            if (existingLike != null) {
                interactionDao.deleteLike(userId, targetType, targetId)
                when (targetType) {
                    "VIDEO" -> videoDao.adjustLikes(targetId, -1)
                    "SHORT" -> shortDao.adjustLikes(targetId, -1)
                    "COMMENT" -> commentDao.adjustCommentLikes(targetId, -1)
                }
                false
            } else {
                interactionDao.insertLike(
                    LikeEntity(
                        id = "lk_${userId}_${targetType}_$targetId",
                        userId = userId,
                        targetType = targetType,
                        targetId = targetId
                    )
                )
                when (targetType) {
                    "VIDEO" -> {
                        videoDao.adjustLikes(targetId, 1)
                        if (interactionDao.getDislike(userId, targetType, targetId) != null) {
                            interactionDao.deleteDislike(userId, targetType, targetId)
                            videoDao.adjustDislikes(targetId, -1)
                        }
                        videoDao.getVideoById(targetId)?.let { v ->
                            channelDao.getChannelById(v.channelId)?.let { ch ->
                                if (ch.ownerUserId != userId) {
                                    systemDao.insertNotification(
                                        NotificationEntity(
                                            id = "notif_${UUID.randomUUID().toString().take(10)}",
                                            recipientUserId = ch.ownerUserId,
                                            type = "LIKE",
                                            title = "New like on your video",
                                            message = "Someone liked \"${v.title}\"",
                                            targetId = v.id
                                        )
                                    )
                                }
                            }
                        }
                    }
                    "SHORT" -> {
                        shortDao.adjustLikes(targetId, 1)
                        if (interactionDao.getDislike(userId, targetType, targetId) != null) {
                            interactionDao.deleteDislike(userId, targetType, targetId)
                        }
                    }
                    "COMMENT" -> commentDao.adjustCommentLikes(targetId, 1)
                }
                true
            }
        }
    }

    suspend fun toggleDislike(userId: String, targetType: String, targetId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val existingDislike = interactionDao.getDislike(userId, targetType, targetId)
            if (existingDislike != null) {
                interactionDao.deleteDislike(userId, targetType, targetId)
                if (targetType == "VIDEO") videoDao.adjustDislikes(targetId, -1)
                false
            } else {
                interactionDao.insertDislike(
                    DislikeEntity(
                        id = "dl_${userId}_${targetType}_$targetId",
                        userId = userId,
                        targetType = targetType,
                        targetId = targetId
                    )
                )
                if (targetType == "VIDEO") videoDao.adjustDislikes(targetId, 1)
                if (interactionDao.getLike(userId, targetType, targetId) != null) {
                    interactionDao.deleteLike(userId, targetType, targetId)
                    when (targetType) {
                        "VIDEO" -> videoDao.adjustLikes(targetId, -1)
                        "SHORT" -> shortDao.adjustLikes(targetId, -1)
                    }
                }
                true
            }
        }
    }

    suspend fun toggleSubscription(userId: String, channelId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = interactionDao.getSubscription(userId, channelId)
            if (existing != null) {
                interactionDao.deleteSubscription(userId, channelId)
                channelDao.adjustSubscribers(channelId, -1)
                false
            } else {
                interactionDao.insertSubscription(
                    SubscriptionEntity(
                        id = "sub_${userId}_$channelId",
                        subscriberUserId = userId,
                        channelId = channelId
                    )
                )
                channelDao.adjustSubscribers(channelId, 1)
                true
            }
        }
    }

    suspend fun toggleWatchLater(userId: String, videoId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = libraryDao.getWatchLater(userId, videoId)
            if (existing != null) {
                libraryDao.removeWatchLater(userId, videoId)
                false
            } else {
                libraryDao.insertWatchLater(
                    WatchLaterEntity(
                        id = "wl_${userId}_$videoId",
                        userId = userId,
                        videoId = videoId
                    )
                )
                true
            }
        }
    }

    suspend fun addComment(userId: String, targetType: String, targetId: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = body.trim()
            require(clean.isNotEmpty()) { "Comment cannot be empty." }
            val profile = userDao.getProfile(userId)
            val authorName = profile?.displayName ?: "Viewer"
            val commentId = "cmt_${UUID.randomUUID().toString().take(10)}"
            commentDao.insertComment(
                CommentEntity(
                    id = commentId,
                    targetType = targetType,
                    targetId = targetId,
                    userId = userId,
                    authorName = authorName,
                    authorAvatarUri = profile?.avatarUri,
                    body = clean
                )
            )
            if (targetType == "SHORT") {
                shortDao.adjustComments(targetId, 1)
            }
        }
    }

    suspend fun addReply(userId: String, commentId: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = body.trim()
            require(clean.isNotEmpty()) { "Reply cannot be empty." }
            val profile = userDao.getProfile(userId)
            val parent = commentDao.getCommentById(commentId)
            commentDao.insertReply(
                CommentReplyEntity(
                    id = "rep_${UUID.randomUUID().toString().take(10)}",
                    commentId = commentId,
                    userId = userId,
                    authorName = profile?.displayName ?: "Viewer",
                    body = clean
                )
            )
            if (parent != null && parent.userId != userId) {
                systemDao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(10)}",
                        recipientUserId = parent.userId,
                        type = "REPLY",
                        title = "${profile?.displayName ?: "Someone"} replied to your comment",
                        message = clean,
                        targetId = if (parent.targetType == "VIDEO") parent.targetId else null
                    )
                )
            }
        }
    }

    suspend fun deleteOwnComment(userId: String, commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val comment = commentDao.getCommentById(commentId) ?: throw IllegalStateException("Comment not found.")
            val isAdmin = userDao.getAdminRole(userId) != null
            if (comment.userId != userId && !isAdmin) {
                throw SecurityException("You can only delete your own comments.")
            }
            commentDao.softDeleteComment(commentId)
            if (comment.targetType == "SHORT") {
                shortDao.adjustComments(comment.targetId, -1)
            }
        }
    }

    // --- Playlists ---
    suspend fun createPlaylist(userId: String, title: String, description: String, visibility: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(title.trim().length >= 2) { "Playlist title must be at least 2 characters." }
            val id = "pl_${UUID.randomUUID().toString().take(10)}"
            libraryDao.upsertPlaylist(
                PlaylistEntity(
                    id = id,
                    ownerUserId = userId,
                    title = title.trim(),
                    description = description.trim(),
                    visibility = visibility
                )
            )
            id
        }
    }

    suspend fun updatePlaylist(userId: String, playlist: PlaylistEntity, title: String, description: String, visibility: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (playlist.ownerUserId != userId) throw SecurityException("Unauthorized playlist edit.")
            require(title.trim().length >= 2) { "Playlist title must be at least 2 characters." }
            libraryDao.upsertPlaylist(playlist.copy(title = title.trim(), description = description.trim(), visibility = visibility))
        }
    }

    suspend fun deletePlaylist(userId: String, playlist: PlaylistEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (playlist.ownerUserId != userId) throw SecurityException("Unauthorized playlist delete.")
            libraryDao.deletePlaylist(playlist.id)
        }
    }

    suspend fun addVideoToPlaylist(playlistId: String, videoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = libraryDao.getPlaylistVideos(playlistId)
            if (existing.any { it.videoId == videoId }) return@runCatching
            libraryDao.upsertPlaylistVideo(
                PlaylistVideoEntity(
                    id = "plv_${playlistId}_$videoId",
                    playlistId = playlistId,
                    videoId = videoId,
                    position = existing.size
                )
            )
        }
    }

    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String) = withContext(Dispatchers.IO) {
        libraryDao.removePlaylistVideo(playlistId, videoId)
    }

    suspend fun reorderPlaylistVideo(playlistId: String, videoId: String, moveUp: Boolean) = withContext(Dispatchers.IO) {
        val items = libraryDao.getPlaylistVideos(playlistId).toMutableList()
        val idx = items.indexOfFirst { it.videoId == videoId }
        if (idx < 0) return@withContext
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in items.indices) return@withContext
        val swapped = items[idx]
        items[idx] = items[targetIdx]
        items[targetIdx] = swapped
        items.forEachIndexed { pos, item ->
            libraryDao.upsertPlaylistVideo(item.copy(position = pos))
        }
    }

    // --- Search & History ---
    suspend fun recordSearchQuery(userId: String?, query: String) = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (userId.isNullOrBlank() || clean.isEmpty()) return@withContext
        libraryDao.upsertSearchHistory(
            SearchHistoryEntity(
                id = "sh_${userId}_${clean.lowercase()}",
                userId = userId,
                query = clean
            )
        )
    }

    suspend fun deleteSearchQuery(id: String) = withContext(Dispatchers.IO) {
        libraryDao.deleteSearchQuery(id)
    }

    suspend fun clearSearchHistory(userId: String) = withContext(Dispatchers.IO) {
        libraryDao.clearSearchHistory(userId)
    }

    suspend fun clearWatchHistory(userId: String) = withContext(Dispatchers.IO) {
        libraryDao.clearWatchHistory(userId)
    }

    suspend fun markNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        systemDao.markAllNotificationsRead(userId)
    }

    // --- Reports ---
    suspend fun submitReport(
        reporterUserId: String,
        targetType: String,
        targetId: String,
        targetLabel: String,
        reason: String,
        details: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            systemDao.insertReport(
                ReportEntity(
                    id = "rep_${UUID.randomUUID().toString().take(10)}",
                    reporterUserId = reporterUserId,
                    targetType = targetType,
                    targetId = targetId,
                    targetLabel = targetLabel,
                    reason = reason,
                    details = details.trim(),
                    status = "PENDING"
                )
            )
        }
    }

    // --- Admin Role-Protected Backend Operations ---
    private suspend fun requireAdmin(actingUserId: String) {
        val role = userDao.getAdminRole(actingUserId)
            ?: throw SecurityException("Forbidden: Administrator privileges required.")
        if (role.role.isBlank()) {
            throw SecurityException("Forbidden: Invalid administrator role.")
        }
    }

    suspend fun adminToggleVideoPublished(actingUserId: String, videoId: String, published: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            videoDao.setPublished(videoId, published)
        }
    }

    suspend fun adminToggleVideoFeatured(actingUserId: String, videoId: String, featured: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            videoDao.setFeatured(videoId, featured)
        }
    }

    suspend fun adminToggleVideoTrending(actingUserId: String, videoId: String, trending: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            videoDao.setTrending(videoId, trending)
        }
    }

    suspend fun adminEditVideo(actingUserId: String, video: VideoEntity, newTitle: String, newCategory: String, newSection: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            require(newTitle.trim().length >= 3) { "Title too short." }
            videoDao.upsertVideo(video.copy(title = newTitle.trim(), category = newCategory, sectionTag = newSection))
        }
    }

    suspend fun adminDeleteVideo(actingUserId: String, videoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            videoDao.deleteVideo(videoId)
        }
    }

    suspend fun adminDeleteShort(actingUserId: String, shortId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            shortDao.deleteShort(shortId)
        }
    }

    suspend fun adminAddCategory(actingUserId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            val clean = name.trim()
            require(clean.length >= 2) { "Category name too short." }
            systemDao.upsertCategory(CategoryEntity(id = "cat_${clean.lowercase()}", name = clean, sortOrder = 99))
        }
    }

    suspend fun adminDeleteCategory(actingUserId: String, categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            systemDao.deleteCategory(categoryId)
        }
    }

    suspend fun adminSetUserBlocked(actingUserId: String, targetUserId: String, blocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            if (actingUserId == targetUserId) throw IllegalArgumentException("Cannot block your own active admin account.")
            userDao.setUserBlocked(targetUserId, blocked)
        }
    }

    suspend fun adminSetChannelSuspended(actingUserId: String, channelId: String, suspended: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            channelDao.setChannelSuspended(channelId, suspended)
        }
    }

    suspend fun adminResolveReport(actingUserId: String, report: ReportEntity, action: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireAdmin(actingUserId)
            when (action) {
                "REMOVE" -> {
                    when (report.targetType) {
                        "VIDEO" -> videoDao.deleteVideo(report.targetId)
                        "SHORT" -> shortDao.deleteShort(report.targetId)
                        "COMMENT" -> commentDao.softDeleteComment(report.targetId)
                        "CHANNEL" -> channelDao.setChannelSuspended(report.targetId, true)
                    }
                    systemDao.updateReportStatus(report.id, "REMOVED")
                }
                "APPROVE" -> systemDao.updateReportStatus(report.id, "APPROVED")
                "REJECT" -> systemDao.updateReportStatus(report.id, "REJECTED")
                "SUSPEND_ACCOUNT" -> {
                    when (report.targetType) {
                        "VIDEO" -> videoDao.getVideoById(report.targetId)?.let { v ->
                            channelDao.getChannelById(v.channelId)?.let { ch ->
                                userDao.setUserBlocked(ch.ownerUserId, true)
                                channelDao.setChannelSuspended(ch.id, true)
                            }
                        }
                        "SHORT" -> shortDao.getShortById(report.targetId)?.let { s ->
                            channelDao.getChannelById(s.channelId)?.let { ch ->
                                userDao.setUserBlocked(ch.ownerUserId, true)
                                channelDao.setChannelSuspended(ch.id, true)
                            }
                        }
                        "COMMENT" -> commentDao.getCommentById(report.targetId)?.let { c ->
                            userDao.setUserBlocked(c.userId, true)
                            commentDao.softDeleteComment(c.id)
                        }
                        "CHANNEL" -> channelDao.getChannelById(report.targetId)?.let { ch ->
                            userDao.setUserBlocked(ch.ownerUserId, true)
                            channelDao.setChannelSuspended(ch.id, true)
                        }
                    }
                    systemDao.updateReportStatus(report.id, "REMOVED")
                }
            }
        }
    }

    // --- Settings ---
    suspend fun setDarkMode(enabled: Boolean) = settingsStore.setDarkMode(enabled)
    suspend fun setNotificationsEnabled(enabled: Boolean) = settingsStore.setNotificationsEnabled(enabled)
    suspend fun setAutoplayNext(enabled: Boolean) = settingsStore.setAutoplayNext(enabled)
    suspend fun setDefaultQuality(quality: String) = settingsStore.setDefaultQuality(quality)
    suspend fun setRestrictedMode(enabled: Boolean) = settingsStore.setRestrictedMode(enabled)
    suspend fun setLanguage(language: String) = settingsStore.setLanguage(language)
    suspend fun setPrivateWatchHistory(enabled: Boolean) = settingsStore.setPrivateWatchHistory(enabled)
}
