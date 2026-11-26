package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.local.entity.CommentWithReplies
import com.example.data.local.entity.VideoEntity
import com.example.data.local.entity.VideoWithChannel
import com.example.data.remote.YouTubeParser
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskRedGlow
import com.example.ui.theme.DskWhite
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassRedBorder
import com.example.ui.util.formatCount
import com.example.ui.util.formatDuration
import com.example.ui.util.formatRelativeTime
import com.example.ui.util.formatTimeMs
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoItem: VideoWithChannel,
    relatedVideos: List<VideoWithChannel>,
    comments: List<CommentWithReplies>,
    isLiked: Boolean,
    isDisliked: Boolean,
    isSubscribed: Boolean,
    isWatchLater: Boolean,
    autoplayNext: Boolean,
    defaultQuality: String,
    currentUserId: String?,
    onBack: () -> Unit,
    onSelectVideo: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onReportVideo: () -> Unit,
    onToggleAutoplay: (Boolean) -> Unit,
    onAddComment: (String) -> Unit,
    onAddReply: (commentId: String, body: String) -> Unit,
    onLikeComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onReportComment: (commentId: String, label: String) -> Unit,
    onRecordView: (String) -> Unit,
    restrictionMessage: String? = null,
    onFullscreenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val video = videoItem.video
    val channel = videoItem.channel
    val youtubeVideoId = remember(video.id, video.videoUrl, video.sourceType) {
        if (video.sourceType == "YOUTUBE") YouTubeParser.extractVideoId(video.id) ?: video.id
        else YouTubeParser.extractVideoId(video.videoUrl)
    }
    val isYouTube = youtubeVideoId != null

    var isFullscreen by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var showDescription by remember { mutableStateOf(false) }
    var commentSort by remember { mutableStateOf("Top") }
    var commentDraft by remember { mutableStateOf("") }

    LaunchedEffect(isFullscreen) {
        onFullscreenChanged(isFullscreen)
    }

    LaunchedEffect(video.id) {
        onRecordView(video.id)
    }

    // System bars handling for Fullscreen
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.window?.let { w ->
                WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val sortedComments = remember(comments, commentSort) {
        if (commentSort == "Top") comments.sortedByDescending { it.comment.likeCount }
        else comments.sortedByDescending { it.comment.createdAt }
    }

    val nextRelatedVideoId = relatedVideos.firstOrNull()?.video?.id
    val playNextAction: (() -> Unit)? = if (nextRelatedVideoId != null) {
        { onSelectVideo(nextRelatedVideoId) }
    } else null

    val latestAutoplayNext by rememberUpdatedState(autoplayNext)
    val latestSpeed by rememberUpdatedState(speed)
    val latestRestrictionMessage by rememberUpdatedState(restrictionMessage)
    val latestOnBack by rememberUpdatedState(onBack)
    val latestOnToggleAutoplay by rememberUpdatedState(onToggleAutoplay)
    val latestOnSelectVideo by rememberUpdatedState(onSelectVideo)
    val latestNextVideoId by rememberUpdatedState(nextRelatedVideoId)
    val latestPlayNext by rememberUpdatedState(playNextAction)

    val playerContent = remember(video.id, youtubeVideoId, defaultQuality) {
        movableContentOf<Boolean> { fs ->
            if (youtubeVideoId != null) {
                YouTubePlayerSurface(
                    video = video,
                    youtubeVideoId = youtubeVideoId,
                    isFullscreen = fs,
                    autoplayNext = latestAutoplayNext,
                    speed = latestSpeed,
                    activity = activity,
                    restrictionMessage = latestRestrictionMessage,
                    onBackOrExitFullscreen = {
                        if (fs) isFullscreen = false else latestOnBack()
                    },
                    onToggleFullscreen = { isFullscreen = !fs },
                    onToggleAutoplay = { latestOnToggleAutoplay(it) },
                    onSpeedChange = { speed = it },
                    onEnded = {
                        val nextId = latestNextVideoId
                        if (latestAutoplayNext && nextId != null) latestOnSelectVideo(nextId)
                    },
                    onPlayNext = latestPlayNext,
                    modifier = if (fs) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
                )
            } else {
                DirectExoPlayerSurface(
                    video = video,
                    isFullscreen = fs,
                    autoplayNext = latestAutoplayNext,
                    defaultQuality = defaultQuality,
                    speed = latestSpeed,
                    activity = activity,
                    onBackOrExitFullscreen = {
                        if (fs) isFullscreen = false else latestOnBack()
                    },
                    onToggleFullscreen = { isFullscreen = !fs },
                    onToggleAutoplay = { latestOnToggleAutoplay(it) },
                    onSpeedChange = { speed = it },
                    onEnded = {
                        val nextId = latestNextVideoId
                        if (latestAutoplayNext && nextId != null) latestOnSelectVideo(nextId)
                    },
                    modifier = if (fs) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    }
                )
            }
        }
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            playerContent(true)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Keep AndroidView(WebView) rectangular & unclipped to avoid hardware RenderNode/clipping crashes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            playerContent(false)
        }

        // Scrollable Glass Details, Comments, Related Videos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title & Meta Glass Card
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    intensity = GlassIntensity.REGULAR,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${formatCount(video.viewCount)} views • ${formatRelativeTime(video.createdAt)} • ${video.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (isYouTube) DskRed.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f))
                                    .border(
                                        0.8.dp,
                                        if (isYouTube) GlassRedBorder else GlassHighlight.copy(alpha = 0.4f),
                                        RoundedCornerShape(99.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isYouTube) "YouTube" else "Creator Upload",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isYouTube) DskRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Channel Row + Free Subscribe Glass Card
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    intensity = GlassIntensity.REGULAR,
                    onClick = { onChannelClick(channel.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelAvatar(name = channel.name, avatarUri = channel.avatarUri, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatCount(channel.subscriberCount)} subscribers • Free Channel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LiquidGlassButton(
                            onClick = onToggleSubscribe,
                            isPrimary = !isSubscribed,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("player_subscribe_button")
                        ) {
                            Text(
                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Glass Action Pills Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ActionPill(
                            icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            label = formatCount(video.likeCount),
                            active = isLiked,
                            onClick = onToggleLike
                        )
                    }
                    item {
                        ActionPill(
                            icon = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            label = formatCount(video.dislikeCount),
                            active = isDisliked,
                            onClick = onToggleDislike
                        )
                    }
                    item {
                        ActionPill(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            active = false,
                            onClick = onShare
                        )
                    }
                    item {
                        ActionPill(
                            icon = if (isWatchLater) Icons.Outlined.BookmarkAdded else Icons.Outlined.WatchLater,
                            label = if (isWatchLater) "Saved" else "Watch Later",
                            active = isWatchLater,
                            onClick = onToggleWatchLater
                        )
                    }
                    item {
                        ActionPill(
                            icon = Icons.Outlined.PlaylistAdd,
                            label = "Playlist",
                            active = false,
                            onClick = onAddToPlaylist
                        )
                    }
                    item {
                        ActionPill(
                            icon = Icons.Outlined.Flag,
                            label = "Report",
                            active = false,
                            onClick = onReportVideo
                        )
                    }
                }
            }

            // Expandable Description Glass Card
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    intensity = GlassIntensity.SUBTLE,
                    onClick = { showDescription = !showDescription },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Description", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (showDescription) "Show less" else "Show more",
                                style = MaterialTheme.typography.labelMedium,
                                color = DskRed
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = video.description.ifBlank { "Watch freely on DSK YouTube." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (showDescription) 20 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showDescription && video.tags.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Tags: ${video.tags}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DskRed
                            )
                        }
                    }
                }
            }

            // Comments Header & Glass Input
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (${sortedComments.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidGlassChip(
                                selected = commentSort == "Top",
                                onClick = { commentSort = "Top" },
                                label = "Top"
                            )
                            LiquidGlassChip(
                                selected = commentSort == "Newest",
                                onClick = { commentSort = "Newest" },
                                label = "Newest"
                            )
                        }
                    }
                    if (video.commentsEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassTextField(
                                value = commentDraft,
                                onValueChange = { commentDraft = it },
                                placeholder = "Add a public comment...",
                                singleLine = true,
                                shape = RoundedCornerShape(99.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input")
                            )
                            LiquidGlassIconButton(
                                onClick = {
                                    if (commentDraft.isNotBlank()) {
                                        onAddComment(commentDraft)
                                        commentDraft = ""
                                    }
                                },
                                size = 46.dp,
                                selected = commentDraft.isNotBlank(),
                                modifier = Modifier.testTag("send_comment_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "Post comment",
                                    tint = if (commentDraft.isNotBlank()) DskWhite else DskRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Comments are turned off for this video.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Comments List
            items(sortedComments, key = { it.comment.id }) { item ->
                CommentCard(
                    item = item,
                    currentUserId = currentUserId,
                    onLike = { onLikeComment(item.comment.id) },
                    onReply = { body -> onAddReply(item.comment.id, body) },
                    onDelete = { onDeleteComment(item.comment.id) },
                    onReport = { onReportComment(item.comment.id, item.comment.body) }
                )
            }

            // Related Videos
            if (relatedVideos.isNotEmpty()) {
                item {
                    LiquidGlassSectionHeader(
                        title = "Up Next",
                        icon = Icons.Outlined.PlaylistPlay,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(relatedVideos, key = { it.video.id }) { rel ->
                    CompactVideoRow(
                        item = rel,
                        onClick = { onSelectVideo(rel.video.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun YouTubePlayerSurface(
    video: VideoEntity,
    youtubeVideoId: String,
    isFullscreen: Boolean,
    autoplayNext: Boolean,
    speed: Float,
    activity: Activity?,
    restrictionMessage: String?,
    onBackOrExitFullscreen: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleAutoplay: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onEnded: () -> Unit,
    onPlayNext: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var isPlaying by remember(youtubeVideoId) { mutableStateOf(true) }
    var isActive by remember(youtubeVideoId) { mutableStateOf(true) }
    var currentSec by remember(youtubeVideoId) { mutableIntStateOf(0) }
    var durationSec by remember(youtubeVideoId) { mutableIntStateOf(video.durationSeconds.coerceAtLeast(1)) }
    var volumePercent by remember { mutableIntStateOf(100) }
    var seekRequest by remember(youtubeVideoId) { mutableStateOf<Pair<Int, Long>?>(null) }

    val topControlBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0E1019).copy(alpha = if (isFullscreen) 0.72f else 0.90f),
                            Color(0xFF090B11).copy(alpha = if (isFullscreen) 0.45f else 0.78f)
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBackOrExitFullscreen,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Back",
                        tint = DskWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFullscreen) video.title else "DSK Watch",
                        style = MaterialTheme.typography.labelLarge,
                        color = DskWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isFullscreen) {
                        Text(
                            text = "/watch/${video.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto", color = DskWhite, style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = autoplayNext,
                    onCheckedChange = onToggleAutoplay,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Box {
                    TextButton(onClick = { showSpeedMenu = true }) {
                        Text("${speed}x", color = DskWhite, style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        containerColor = glassDropdownContainerColor(),
                        border = glassMenuBorder(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        listOf(0.5f, 1f, 1.25f, 1.5f, 2f).forEach { spd ->
                            DropdownMenuItem(
                                text = { Text("${spd}x") },
                                onClick = {
                                    onSpeedChange(spd)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                    IconButton(
                        onClick = {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity.enterPictureInPictureMode(params)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = DskWhite
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.testTag("player_fullscreen_toggle")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = DskWhite
                    )
                }
            }
        }
    }

    val bottomInAppControls: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF090B11).copy(alpha = if (isFullscreen) 0.45f else 0.78f),
                            Color(0xFF0E1019).copy(alpha = if (isFullscreen) 0.72f else 0.90f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (showVolumeSlider) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = if (volumePercent == 0) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = DskWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = volumePercent / 100f,
                        onValueChange = { volumePercent = (it * 100).toInt().coerceIn(0, 100) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = DskRed, activeTrackColor = DskRed)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val next = !isPlaying
                        isPlaying = next
                        isActive = next
                    },
                    modifier = Modifier.testTag("player_yt_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DskWhite
                    )
                }
                IconButton(
                    onClick = {
                        val target = (currentSec - 10).coerceAtLeast(0)
                        currentSec = target
                        seekRequest = target to System.nanoTime()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = DskWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val target = (currentSec + 10).coerceAtMost(durationSec.coerceAtLeast(1))
                        currentSec = target
                        seekRequest = target to System.nanoTime()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forward10,
                        contentDescription = "Forward 10s",
                        tint = DskWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "${formatDuration(currentSec)} / ${formatDuration(durationSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DskWhite
                )
                Slider(
                    value = if (durationSec > 0) (currentSec.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { frac ->
                        val target = (frac * durationSec).toInt().coerceAtLeast(0)
                        currentSec = target
                        seekRequest = target to System.nanoTime()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = DskRed,
                        activeTrackColor = DskRed,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                    )
                )
                IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                    Icon(
                        imageVector = if (volumePercent == 0) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                        contentDescription = "Volume",
                        tint = DskWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    val latestSpeed by rememberUpdatedState(speed)
    val latestVolume by rememberUpdatedState(volumePercent)
    val latestSeek by rememberUpdatedState(seekRequest)
    val latestIsActive by rememberUpdatedState(isActive)
    val latestRestriction by rememberUpdatedState(restrictionMessage)
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnPlayNext by rememberUpdatedState(onPlayNext)

    val embedSlot = remember(youtubeVideoId) {
        movableContentOf<Modifier> { embedMod ->
            YouTubeEmbedPlayer(
                videoId = youtubeVideoId,
                autoplay = true,
                loop = false,
                playbackRate = latestSpeed,
                volumePercent = latestVolume,
                seekToSecondsRequest = latestSeek,
                isActive = latestIsActive,
                restrictionMessage = latestRestriction,
                onProgress = { cur, dur ->
                    currentSec = cur
                    if (dur > 0) durationSec = dur
                },
                onPlaybackStateChanged = { playing ->
                    isPlaying = playing
                },
                onEnded = { latestOnEnded() },
                onPlayNext = latestOnPlayNext,
                modifier = embedMod
            )
        }
    }

    if (isFullscreen) {
        Box(modifier = modifier.background(Color.Black)) {
            embedSlot(Modifier.fillMaxSize())
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                topControlBar()
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                bottomInAppControls()
            }
        }
    } else {
        Column(modifier = modifier.background(Color.Black.copy(alpha = 0.85f))) {
            topControlBar()
            embedSlot(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            bottomInAppControls()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun DirectExoPlayerSurface(
    video: VideoEntity,
    isFullscreen: Boolean,
    autoplayNext: Boolean,
    defaultQuality: String,
    speed: Float,
    activity: Activity?,
    onBackOrExitFullscreen: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleAutoplay: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf((video.durationSeconds * 1000L).coerceAtLeast(1000L)) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }

    var volume by remember { mutableFloatStateOf(1f) }
    var selectedQuality by remember(defaultQuality) { mutableStateOf(defaultQuality) }
    var captionsEnabled by remember { mutableStateOf(false) }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(video.id, video.videoUrl) {
        playerError = null
        isBuffering = true
        exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(speed) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    LaunchedEffect(selectedQuality) {
        val maxHeight = when (selectedQuality) {
            "1080p" -> 1080
            "720p" -> 720
            "480p" -> 480
            "360p" -> 360
            else -> Int.MAX_VALUE
        }
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .build()
    }

    DisposableEffect(exoPlayer, autoplayNext) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
                } else if (playbackState == Player.STATE_ENDED && autoplayNext) {
                    onEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerError = "Playback error (${error.errorCodeName}). Check connection or stream URL."
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            delay(400)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { pv -> pv.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = DskRed
            )
        }

        if (playerError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playerError ?: "",
                    color = DskWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                LiquidGlassButton(onClick = {
                    playerError = null
                    exoPlayer.prepare()
                    exoPlayer.play()
                }) { Text("Retry") }
            }
        }

        if (captionsEnabled && video.captionsText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls) 64.dp else 14.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(0.8.dp, GlassHighlight.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = video.captionsText.lines().firstOrNull() ?: video.title,
                    color = DskWhite,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = onBackOrExitFullscreen,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = DskWhite
                            )
                        }
                        if (isFullscreen) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = DskWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto", color = DskWhite, style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = autoplayNext,
                                onCheckedChange = onToggleAutoplay,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        IconButton(onClick = { captionsEnabled = !captionsEnabled }) {
                            Icon(
                                imageVector = if (captionsEnabled) Icons.Filled.ClosedCaption else Icons.Outlined.ClosedCaption,
                                contentDescription = "Toggle Captions",
                                tint = if (captionsEnabled) DskRed else DskWhite
                            )
                        }
                        Box {
                            TextButton(onClick = { showSpeedMenu = true }) {
                                Text("${speed}x", color = DskWhite, style = MaterialTheme.typography.labelMedium)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                containerColor = glassDropdownContainerColor(),
                                border = glassMenuBorder(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                listOf(0.5f, 1f, 1.25f, 1.5f, 2f).forEach { spd ->
                                    DropdownMenuItem(
                                        text = { Text("${spd}x") },
                                        onClick = {
                                            onSpeedChange(spd)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Box {
                            TextButton(onClick = { showQualityMenu = true }) {
                                Text(selectedQuality, color = DskWhite, style = MaterialTheme.typography.labelMedium)
                            }
                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false },
                                containerColor = glassDropdownContainerColor(),
                                border = glassMenuBorder(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                listOf("Auto", "1080p", "720p", "480p", "360p").forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q) },
                                        onClick = {
                                            selectedQuality = q
                                            showQualityMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = DskWhite
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.Replay10, contentDescription = "Rewind 10s", tint = DskWhite, modifier = Modifier.size(32.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(DskRedGlow, DskRed)))
                            .border(1.2.dp, GlassRedBorder, CircleShape)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DskWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.Forward10, contentDescription = "Forward 10s", tint = DskWhite, modifier = Modifier.size(32.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (showVolumeSlider) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.8.dp, GlassHighlight.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = DskWhite, modifier = Modifier.size(18.dp))
                            Slider(
                                value = volume,
                                onValueChange = {
                                    volume = it
                                    exoPlayer.volume = it
                                },
                                colors = SliderDefaults.colors(thumbColor = DskRed, activeTrackColor = DskRed)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTimeMs(currentPosMs)} / ${formatTimeMs(durationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DskWhite
                        )
                        Slider(
                            value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { frac ->
                                val target = (frac * durationMs).toLong()
                                currentPosMs = target
                                exoPlayer.seekTo(target)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = DskRed,
                                activeTrackColor = DskRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                            Icon(
                                imageVector = if (volume == 0f) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                contentDescription = "Volume",
                                tint = DskWhite
                            )
                        }
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.testTag("player_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = DskWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassPill(
        intensity = if (active) GlassIntensity.STRONG else GlassIntensity.REGULAR,
        selected = active,
        elevation = if (active) 8.dp else 4.dp,
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) DskWhite else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (active) DskWhite else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CommentCard(
    item: CommentWithReplies,
    currentUserId: String?,
    onLike: () -> Unit,
    onReply: (String) -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var replying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val c = item.comment

    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        intensity = GlassIntensity.SUBTLE,
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelAvatar(name = c.authorName, avatarUri = c.authorAvatarUri, size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(c.authorName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Text(
                    formatRelativeTime(c.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(c.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onLike, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Icon(
                        imageVector = if (item.isLikedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like comment",
                        modifier = Modifier.size(16.dp),
                        tint = if (item.isLikedByMe) DskRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${c.likeCount}", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { replying = !replying }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Reply", style = MaterialTheme.typography.labelMedium)
                }
                if (currentUserId == c.userId) {
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Delete", style = MaterialTheme.typography.labelMedium, color = DskRed)
                    }
                } else {
                    TextButton(onClick = onReport, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Report", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (replying) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = "Write a reply...",
                        singleLine = true,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassIconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReply(replyText)
                                replyText = ""
                                replying = false
                            }
                        },
                        size = 42.dp
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Post reply", tint = DskRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (item.replies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.replies.forEach { rep ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(14.dp),
                            intensity = GlassIntensity.SUBTLE,
                            elevation = 0.dp
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "${rep.authorName} • ${formatRelativeTime(rep.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(rep.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
