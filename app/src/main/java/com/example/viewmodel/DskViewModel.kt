package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserSettings
import com.example.data.local.entity.*
import com.example.data.remote.VideoRestrictionInfo
import com.example.data.remote.YouTubeSearchBundle
import com.example.data.remote.YouTubeSyncState
import com.example.data.repository.DskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UploadUiState {
    data object Idle : UploadUiState
    data class Uploading(val progress: Int, val status: String) : UploadUiState
    data class Success(val contentId: String, val isShort: Boolean) : UploadUiState
    data class Error(val message: String) : UploadUiState
}

sealed interface LiveSearchUiState {
    data object Idle : LiveSearchUiState
    data object Searching : LiveSearchUiState
    data class Results(val query: String, val bundle: YouTubeSearchBundle) : LiveSearchUiState
    data class Error(val message: String) : LiveSearchUiState
}

data class AdminDashboardState(
    val totalUsers: Int = 0,
    val totalChannels: Int = 0,
    val totalVideos: Int = 0,
    val totalShorts: Int = 0,
    val totalComments: Int = 0,
    val totalViews: Long = 0L,
    val users: List<UserWithProfile> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val videos: List<VideoWithChannel> = emptyList(),
    val shorts: List<ShortWithChannel> = emptyList(),
    val comments: List<CommentEntity> = emptyList(),
    val reports: List<ReportEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class DskViewModel(private val repo: DskRepository) : ViewModel() {

    val settings: StateFlow<UserSettings> = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserSettings()
    )

    private val currentUserIdFlow: Flow<String?> = settings.map { it.currentUserId }.distinctUntilChanged()

    val currentUser: StateFlow<UserEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.observeUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentProfile: StateFlow<ProfileEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.observeProfile(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAdmin: StateFlow<Boolean> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(false) else repo.observeIsAdmin(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val myChannel: StateFlow<ChannelEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.observeMyChannel(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categories: StateFlow<List<CategoryEntity>> = repo.categoriesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val channels: StateFlow<List<ChannelEntity>> = repo.channelsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val videos: StateFlow<List<VideoWithChannel>> = combine(
        repo.publicVideosFlow,
        repo.channelsFlow,
        settings
    ) { vids, chans, prefs ->
        val chanMap = chans.associateBy { it.id }
        vids.mapNotNull { v ->
            val ch = chanMap[v.channelId] ?: return@mapNotNull null
            if (ch.isSuspended) return@mapNotNull null
            if (prefs.restrictedMode && v.tags.contains("mature", ignoreCase = true)) return@mapNotNull null
            VideoWithChannel(v, ch)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shorts: StateFlow<List<ShortWithChannel>> = combine(
        repo.publicShortsFlow,
        repo.channelsFlow
    ) { shs, chans ->
        val chanMap = chans.associateBy { it.id }
        shs.mapNotNull { s ->
            val ch = chanMap[s.channelId] ?: return@mapNotNull null
            if (ch.isSuspended) return@mapNotNull null
            ShortWithChannel(s, ch)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userLikesFlow = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeUserLikes(uid)
    }

    private val userDislikesFlow = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeUserDislikes(uid)
    }

    val likedKeys: StateFlow<Set<String>> = userLikesFlow.map { list ->
        list.map { "${it.targetType}:${it.targetId}" }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val dislikedKeys: StateFlow<Set<String>> = userDislikesFlow.map { list ->
        list.map { "${it.targetType}:${it.targetId}" }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val subscribedChannelIds: StateFlow<Set<String>> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptySet())
        else repo.observeSubscriptions(uid).map { list -> list.map { it.channelId }.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val watchLaterIds: StateFlow<Set<String>> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptySet())
        else repo.observeWatchLater(uid).map { list -> list.map { it.videoId }.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val watchHistory: StateFlow<List<VideoWithChannel>> = combine(
        currentUserIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.observeWatchHistory(uid)
        },
        videos
    ) { history, allVideos ->
        val vidMap = allVideos.associateBy { it.video.id }
        history.mapNotNull { vidMap[it.videoId] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchLaterVideos: StateFlow<List<VideoWithChannel>> = combine(
        watchLaterIds,
        videos
    ) { ids, allVideos ->
        allVideos.filter { it.video.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedVideos: StateFlow<List<VideoWithChannel>> = combine(
        likedKeys,
        videos
    ) { keys, allVideos ->
        allVideos.filter { "VIDEO:${it.video.id}" in keys }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = combine(
        currentUserIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.observePlaylists(uid)
        },
        repo.allPlaylistVideosFlow
    ) { lists, plVideos ->
        val counts = plVideos.groupingBy { it.playlistId }.eachCount()
        lists.map { PlaylistSummary(it, counts[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistVideosMap: StateFlow<Map<String, List<VideoWithChannel>>> = combine(
        repo.allPlaylistVideosFlow,
        videos
    ) { plVideos, allVideos ->
        val vidMap = allVideos.associateBy { it.video.id }
        plVideos.groupBy { it.playlistId }.mapValues { (_, items) ->
            items.sortedBy { it.position }.mapNotNull { vidMap[it.videoId] }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val notifications: StateFlow<List<NotificationEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeNotifications(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeSearchHistory(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commentsMap: StateFlow<Map<String, List<CommentWithReplies>>> = combine(
        repo.allCommentsFlow,
        repo.allRepliesFlow,
        likedKeys
    ) { comments, replies, likes ->
        val repliesByComment = replies.groupBy { it.commentId }
        comments.groupBy { "${it.targetType}:${it.targetId}" }.mapValues { (_, list) ->
            list.map { c ->
                CommentWithReplies(
                    comment = c,
                    replies = repliesByComment[c.id].orEmpty(),
                    isLikedByMe = "COMMENT:${c.id}" in likes
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val adminDashboard: StateFlow<AdminDashboardState> = combine(
        isAdmin,
        repo.allUsersFlow,
        repo.allProfilesFlow,
        repo.channelsFlow,
        repo.allVideosAdminFlow,
        repo.allShortsAdminFlow,
        repo.allCommentsFlow,
        repo.allReportsFlow
    ) { values ->
        val admin = values[0] as Boolean
        if (!admin) return@combine AdminDashboardState()
        @Suppress("UNCHECKED_CAST")
        val users = values[1] as List<UserEntity>
        @Suppress("UNCHECKED_CAST")
        val profiles = (values[2] as List<ProfileEntity>).associateBy { it.userId }
        @Suppress("UNCHECKED_CAST")
        val chans = values[3] as List<ChannelEntity>
        val chanMap = chans.associateBy { it.id }
        @Suppress("UNCHECKED_CAST")
        val allVids = values[4] as List<VideoEntity>
        @Suppress("UNCHECKED_CAST")
        val allShs = values[5] as List<ShortEntity>
        @Suppress("UNCHECKED_CAST")
        val allCmts = values[6] as List<CommentEntity>
        @Suppress("UNCHECKED_CAST")
        val allReps = values[7] as List<ReportEntity>

        val totalViews = allVids.sumOf { it.viewCount } + allShs.sumOf { it.viewCount }
        AdminDashboardState(
            totalUsers = users.size,
            totalChannels = chans.size,
            totalVideos = allVids.size,
            totalShorts = allShs.size,
            totalComments = allCmts.size,
            totalViews = totalViews,
            users = users.map { u -> UserWithProfile(u, profiles[u.id], false) },
            channels = chans,
            videos = allVids.mapNotNull { v -> chanMap[v.channelId]?.let { VideoWithChannel(v, it) } },
            shorts = allShs.mapNotNull { s -> chanMap[s.channelId]?.let { ShortWithChannel(s, it) } },
            comments = allCmts,
            reports = allReps
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardState())

    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    private val _youtubeSyncState = MutableStateFlow<YouTubeSyncState>(YouTubeSyncState.Idle)
    val youtubeSyncState: StateFlow<YouTubeSyncState> = _youtubeSyncState.asStateFlow()

    private val _liveSearchState = MutableStateFlow<LiveSearchUiState>(LiveSearchUiState.Idle)
    val liveSearchState: StateFlow<LiveSearchUiState> = _liveSearchState.asStateFlow()

    private val _loadedWatchVideo = MutableStateFlow<VideoWithChannel?>(null)
    val loadedWatchVideo: StateFlow<VideoWithChannel?> = _loadedWatchVideo.asStateFlow()

    private val _watchVideoRestriction = MutableStateFlow<Pair<String, VideoRestrictionInfo>?>(null)
    val watchVideoRestriction: StateFlow<Pair<String, VideoRestrictionInfo>?> = _watchVideoRestriction.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        refreshYouTubeHome(force = false)
        // Background periodic refresh: respects 3-min Live / 10-min Home caching & auto-retries after quota backoff
        viewModelScope.launch {
            while (true) {
                delay(180_000L)
                if (_youtubeSyncState.value !is YouTubeSyncState.Refreshing) {
                    val result = repo.syncYouTubeHome(force = false)
                    _youtubeSyncState.value = result
                }
            }
        }
    }

    fun refreshYouTubeHome(force: Boolean = true) {
        viewModelScope.launch {
            _youtubeSyncState.value = YouTubeSyncState.Refreshing
            val result = repo.syncYouTubeHome(force = force)
            _youtubeSyncState.value = result
            if (result is YouTubeSyncState.QuotaExceeded) {
                viewModelScope.launch {
                    delay(60_000L)
                    if (_youtubeSyncState.value is YouTubeSyncState.QuotaExceeded) {
                        refreshYouTubeHome(force = true)
                    }
                }
            }
        }
    }

    fun refreshCategory(category: String) {
        viewModelScope.launch {
            _youtubeSyncState.value = YouTubeSyncState.Refreshing
            _youtubeSyncState.value = repo.syncYouTubeCategory(category, force = false)
        }
    }

    fun refreshChannel(channelId: String) {
        viewModelScope.launch {
            val state = repo.syncYouTubeChannel(channelId, force = false)
            if (state !is YouTubeSyncState.MissingApiKey) {
                _youtubeSyncState.value = state
            }
        }
    }

    fun ensureVideoLoaded(videoId: String) {
        viewModelScope.launch {
            val restriction = repo.checkVideoRestrictions(videoId)
            _watchVideoRestriction.value = videoId to restriction
            val loaded = repo.fetchVideoById(videoId)
            if (loaded != null) {
                _loadedWatchVideo.value = loaded
                _watchVideoRestriction.value = videoId to repo.checkVideoRestrictions(videoId)
            }
        }
    }

    fun searchYouTubeLive(query: String, typeFilter: String, categoryFilter: String, sortBy: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _liveSearchState.value = LiveSearchUiState.Idle
            return
        }
        viewModelScope.launch {
            _liveSearchState.value = LiveSearchUiState.Searching
            repo.searchYouTubeLive(clean, typeFilter, categoryFilter, sortBy)
                .onSuccess { bundle ->
                    _liveSearchState.value = LiveSearchUiState.Results(clean, bundle)
                }
                .onFailure { err ->
                    _liveSearchState.value = LiveSearchUiState.Error(err.message ?: "YouTube search failed.")
                }
        }
    }

    fun clearLiveSearch() {
        _liveSearchState.value = LiveSearchUiState.Idle
    }

    fun consumeMessage() {
        _uiMessage.value = null
    }

    private fun postMessage(msg: String) {
        _uiMessage.value = msg
    }

    // --- Auth ---
    fun register(email: String, username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.register(email, username, password)
                .onSuccess {
                    postMessage("Welcome to DSK YouTube, $username!")
                    onSuccess()
                }
                .onFailure { postMessage(it.message ?: "Registration failed.") }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.login(email, password)
                .onSuccess {
                    postMessage("Signed in successfully.")
                    onSuccess()
                }
                .onFailure { postMessage(it.message ?: "Sign in failed.") }
        }
    }

    fun resetPassword(email: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.resetPassword(email, newPassword)
                .onSuccess {
                    postMessage("Password updated. You can now sign in.")
                    onSuccess()
                }
                .onFailure { postMessage(it.message ?: "Password reset failed.") }
        }
    }

    fun updateProfile(displayName: String, username: String, bio: String, avatarUri: Uri?, onSuccess: () -> Unit) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.updateProfileAndChannel(uid, displayName, username, bio, avatarUri)
                .onSuccess {
                    postMessage("Profile & channel updated.")
                    onSuccess()
                }
                .onFailure { postMessage(it.message ?: "Update failed.") }
        }
    }

    fun changePassword(currentPass: String, newPass: String, onSuccess: () -> Unit) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.changePassword(uid, currentPass, newPass)
                .onSuccess {
                    postMessage("Password changed.")
                    onSuccess()
                }
                .onFailure { postMessage(it.message ?: "Could not change password.") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            postMessage("Signed out.")
        }
    }

    // --- Upload ---
    fun resetUploadState() {
        _uploadState.value = UploadUiState.Idle
    }

    fun uploadMedia(
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
        musicInfo: String
    ) {
        val uid = currentUser.value?.id
        if (uid == null) {
            _uploadState.value = UploadUiState.Error("Please sign in to upload content.")
            return
        }
        viewModelScope.launch {
            _uploadState.value = UploadUiState.Uploading(2, "Preparing upload...")
            repo.uploadMedia(
                userId = uid,
                isShort = isShort,
                videoUri = videoUri,
                directVideoUrl = directVideoUrl,
                thumbnailUri = thumbnailUri,
                title = title,
                description = description,
                category = category,
                tags = tags,
                visibility = visibility,
                commentsEnabled = commentsEnabled,
                musicInfo = musicInfo
            ) { pct, status ->
                _uploadState.value = UploadUiState.Uploading(pct, status)
            }.onSuccess { contentId ->
                _uploadState.value = UploadUiState.Success(contentId, isShort)
            }.onFailure { err ->
                _uploadState.value = UploadUiState.Error(err.message ?: "Upload failed.")
            }
        }
    }

    // --- Playback & Engagement ---
    fun recordVideoView(videoId: String) {
        viewModelScope.launch {
            repo.recordVideoView(videoId, currentUser.value?.id, settings.value.privateWatchHistory)
        }
    }

    fun recordShortView(shortId: String) {
        viewModelScope.launch { repo.recordShortView(shortId) }
    }

    fun toggleLike(targetType: String, targetId: String, onRequireAuth: () -> Unit) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.toggleLike(uid, targetType, targetId)
                .onFailure { postMessage(it.message ?: "Action failed.") }
        }
    }

    fun toggleDislike(targetType: String, targetId: String, onRequireAuth: () -> Unit) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.toggleDislike(uid, targetType, targetId)
                .onFailure { postMessage(it.message ?: "Action failed.") }
        }
    }

    fun toggleSubscription(channelId: String, onRequireAuth: () -> Unit) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.toggleSubscription(uid, channelId)
                .onSuccess { subscribed ->
                    postMessage(if (subscribed) "Subscribed for free!" else "Unsubscribed.")
                }
                .onFailure { postMessage(it.message ?: "Subscription update failed.") }
        }
    }

    fun toggleWatchLater(videoId: String, onRequireAuth: () -> Unit) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.toggleWatchLater(uid, videoId)
                .onSuccess { added ->
                    postMessage(if (added) "Saved to Watch Later" else "Removed from Watch Later")
                }
        }
    }

    fun addComment(targetType: String, targetId: String, body: String, onRequireAuth: () -> Unit) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.addComment(uid, targetType, targetId, body)
                .onFailure { postMessage(it.message ?: "Could not post comment.") }
        }
    }

    fun addReply(commentId: String, body: String, onRequireAuth: () -> Unit) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.addReply(uid, commentId, body)
                .onFailure { postMessage(it.message ?: "Could not post reply.") }
        }
    }

    fun deleteComment(commentId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.deleteOwnComment(uid, commentId)
                .onSuccess { postMessage("Comment deleted.") }
                .onFailure { postMessage(it.message ?: "Delete failed.") }
        }
    }

    // --- Playlists ---
    fun createPlaylist(title: String, description: String, visibility: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.createPlaylist(uid, title, description, visibility)
                .onSuccess { postMessage("Playlist created.") }
                .onFailure { postMessage(it.message ?: "Could not create playlist.") }
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity, title: String, description: String, visibility: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.updatePlaylist(uid, playlist, title, description, visibility)
                .onSuccess { postMessage("Playlist updated.") }
                .onFailure { postMessage(it.message ?: "Update failed.") }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.deletePlaylist(uid, playlist)
                .onSuccess { postMessage("Playlist deleted.") }
                .onFailure { postMessage(it.message ?: "Delete failed.") }
        }
    }

    fun addVideoToPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch {
            repo.addVideoToPlaylist(playlistId, videoId)
                .onSuccess { postMessage("Added to playlist.") }
        }
    }

    fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        viewModelScope.launch {
            repo.removeVideoFromPlaylist(playlistId, videoId)
            postMessage("Removed from playlist.")
        }
    }

    fun reorderPlaylistVideo(playlistId: String, videoId: String, moveUp: Boolean) {
        viewModelScope.launch { repo.reorderPlaylistVideo(playlistId, videoId, moveUp) }
    }

    // --- Search & History & Notifications ---
    fun recordSearch(query: String) {
        viewModelScope.launch { repo.recordSearchQuery(currentUser.value?.id, query) }
    }

    fun deleteSearchQuery(id: String) {
        viewModelScope.launch { repo.deleteSearchQuery(id) }
    }

    fun clearSearchHistory() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repo.clearSearchHistory(uid) }
    }

    fun clearWatchHistory() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.clearWatchHistory(uid)
            postMessage("Watch history cleared.")
        }
    }

    fun markNotificationsRead() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repo.markNotificationsRead(uid) }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        targetLabel: String,
        reason: String,
        details: String,
        onRequireAuth: () -> Unit
    ) {
        val uid = currentUser.value?.id ?: return onRequireAuth()
        viewModelScope.launch {
            repo.submitReport(uid, targetType, targetId, targetLabel, reason, details)
                .onSuccess { postMessage("Report submitted for moderation.") }
                .onFailure { postMessage(it.message ?: "Could not submit report.") }
        }
    }

    // --- Admin Actions ---
    fun adminTogglePublished(videoId: String, published: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminToggleVideoPublished(uid, videoId, published)
                .onFailure { postMessage(it.message ?: "Admin action failed.") }
        }
    }

    fun adminToggleFeatured(videoId: String, featured: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminToggleVideoFeatured(uid, videoId, featured)
                .onFailure { postMessage(it.message ?: "Admin action failed.") }
        }
    }

    fun adminToggleTrending(videoId: String, trending: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminToggleVideoTrending(uid, videoId, trending)
                .onFailure { postMessage(it.message ?: "Admin action failed.") }
        }
    }

    fun adminEditVideo(video: VideoEntity, title: String, category: String, section: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminEditVideo(uid, video, title, category, section)
                .onSuccess { postMessage("Video updated.") }
                .onFailure { postMessage(it.message ?: "Update failed.") }
        }
    }

    fun adminDeleteVideo(videoId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminDeleteVideo(uid, videoId)
                .onSuccess { postMessage("Video deleted.") }
                .onFailure { postMessage(it.message ?: "Delete failed.") }
        }
    }

    fun adminDeleteShort(shortId: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminDeleteShort(uid, shortId)
                .onSuccess { postMessage("Short deleted.") }
                .onFailure { postMessage(it.message ?: "Delete failed.") }
        }
    }

    fun adminAddCategory(name: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminAddCategory(uid, name)
                .onSuccess { postMessage("Category added.") }
                .onFailure { postMessage(it.message ?: "Failed.") }
        }
    }

    fun adminDeleteCategory(id: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminDeleteCategory(uid, id)
                .onSuccess { postMessage("Category deleted.") }
                .onFailure { postMessage(it.message ?: "Failed.") }
        }
    }

    fun adminSetUserBlocked(targetUserId: String, blocked: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminSetUserBlocked(uid, targetUserId, blocked)
                .onSuccess { postMessage(if (blocked) "User blocked." else "User unblocked.") }
                .onFailure { postMessage(it.message ?: "Action failed.") }
        }
    }

    fun adminSetChannelSuspended(channelId: String, suspended: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminSetChannelSuspended(uid, channelId, suspended)
                .onSuccess { postMessage(if (suspended) "Channel suspended." else "Channel restored.") }
                .onFailure { postMessage(it.message ?: "Action failed.") }
        }
    }

    fun adminResolveReport(report: ReportEntity, action: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repo.adminResolveReport(uid, report, action)
                .onSuccess { postMessage("Moderation action applied.") }
                .onFailure { postMessage(it.message ?: "Moderation failed.") }
        }
    }

    // --- Settings ---
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { repo.setDarkMode(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    fun setAutoplayNext(enabled: Boolean) = viewModelScope.launch { repo.setAutoplayNext(enabled) }
    fun setDefaultQuality(quality: String) = viewModelScope.launch { repo.setDefaultQuality(quality) }
    fun setRestrictedMode(enabled: Boolean) = viewModelScope.launch { repo.setRestrictedMode(enabled) }
    fun setLanguage(language: String) = viewModelScope.launch { repo.setLanguage(language) }
    fun setPrivateWatchHistory(enabled: Boolean) = viewModelScope.launch { repo.setPrivateWatchHistory(enabled) }

    class Factory(private val repo: DskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DskViewModel(repo) as T
        }
    }
}
