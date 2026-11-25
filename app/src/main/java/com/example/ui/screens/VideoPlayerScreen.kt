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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactVideoRow
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.util.formatCount
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
    onRecordView: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val video = videoItem.video
    val channel = videoItem.channel

    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf((video.durationSeconds * 1000L).coerceAtLeast(1000L)) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }

    var volume by remember { mutableFloatStateOf(1f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var selectedQuality by remember(defaultQuality) { mutableStateOf(defaultQuality) }
    var captionsEnabled by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }
    var commentSort by remember { mutableStateOf("Top") }
    var commentDraft by remember { mutableStateOf("") }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Load new MediaItem when video changes
    LaunchedEffect(video.id, video.videoUrl) {
        playerError = null
        isBuffering = true
        onRecordView(video.id)
        exoPlayer.setMediaItem(MediaItem.fromUri(video.videoUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Apply quality max height constraint cleanly
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

    // Player listener
    DisposableEffect(exoPlayer, autoplayNext, relatedVideos) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
                } else if (playbackState == Player.STATE_ENDED && autoplayNext) {
                    relatedVideos.firstOrNull()?.let { next -> onSelectVideo(next.video.id) }
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

    // Poll playback progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            delay(400)
        }
    }

    // Auto-hide controls after 3.5s
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
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

    val playerSurface: @Composable (Modifier) -> Unit = { boxModifier ->
        Box(
            modifier = boxModifier
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
                    Button(onClick = {
                        playerError = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }) { Text("Retry") }
                }
            }

            // Captions Overlay
            if (captionsEnabled && video.captionsText.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (showControls) 64.dp else 14.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = video.captionsText.lines().firstOrNull() ?: video.title,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = DskWhite,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Custom Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    // Top Controls Row
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
                                onClick = { if (isFullscreen) isFullscreen = false else onBack() },
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
                            // Autoplay Next Toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Auto", color = DskWhite, style = MaterialTheme.typography.labelSmall)
                                Switch(
                                    checked = autoplayNext,
                                    onCheckedChange = onToggleAutoplay,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                            // Captions CC
                            IconButton(onClick = { captionsEnabled = !captionsEnabled }) {
                                Icon(
                                    imageVector = if (captionsEnabled) Icons.Filled.ClosedCaption else Icons.Outlined.ClosedCaption,
                                    contentDescription = "Toggle Captions",
                                    tint = if (captionsEnabled) DskRed else DskWhite
                                )
                            }
                            // Speed
                            Box {
                                TextButton(onClick = { showSpeedMenu = true }) {
                                    Text("${speed}x", color = DskWhite, style = MaterialTheme.typography.labelMedium)
                                }
                                DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                    listOf(0.5f, 1f, 1.25f, 1.5f, 2f).forEach { spd ->
                                        DropdownMenuItem(
                                            text = { Text("${spd}x") },
                                            onClick = {
                                                speed = spd
                                                exoPlayer.playbackParameters = PlaybackParameters(spd)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            // Quality
                            Box {
                                TextButton(onClick = { showQualityMenu = true }) {
                                    Text(selectedQuality, color = DskWhite, style = MaterialTheme.typography.labelMedium)
                                }
                                DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
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
                            // Picture-in-Picture
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

                    // Center Playback Controls
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
                        Surface(
                            color = DskRed.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                                .testTag("player_play_pause")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = DskWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
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

                    // Bottom Scrubber, Volume, Fullscreen
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
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
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
                                onClick = { isFullscreen = !isFullscreen },
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

    if (isFullscreen) {
        playerSurface(Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = modifierCompat()
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Video Player Surface (16:9)
        playerSurface(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        // Scrollable Details, Comments, Related Videos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title & Meta
            item {
                Column {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${formatCount(video.viewCount)} views • ${formatRelativeTime(video.createdAt)} • ${video.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (video.isDemoContent) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "DEMO",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Channel Row + Free Subscribe Button
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChannelClick(channel.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelAvatar(name = channel.name, avatarUri = channel.avatarUri, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatCount(channel.subscriberCount)} subscribers • Free Channel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onToggleSubscribe,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surface else DskRed,
                                contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else DskWhite
                            ),
                            shape = RoundedCornerShape(99.dp),
                            modifier = Modifier.testTag("player_subscribe_button")
                        ) {
                            Text(if (isSubscribed) "Subscribed" else "Subscribe")
                        }
                    }
                }
            }

            // Action Buttons Row (Like, Dislike, Share, Watch Later, Playlist, Report)
            item {
                androidx.compose.foundation.lazy.LazyRow(
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

            // Expandable Description
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDescription = !showDescription }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = video.description.ifBlank { "Watch freely on DSK YouTube." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (showDescription) 20 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showDescription && video.tags.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Tags: ${video.tags}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Comments Header & Input
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = commentSort == "Top",
                                onClick = { commentSort = "Top" },
                                label = { Text("Top") }
                            )
                            FilterChip(
                                selected = commentSort == "Newest",
                                onClick = { commentSort = "Newest" },
                                label = { Text("Newest") }
                            )
                        }
                    }
                    if (video.commentsEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentDraft,
                                onValueChange = { commentDraft = it },
                                placeholder = { Text("Add a public comment...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input"),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentDraft.isNotBlank()) {
                                        onAddComment(commentDraft)
                                        commentDraft = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_comment_button")
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Post comment", tint = DskRed)
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
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium,
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

private fun modifierCompat(): Modifier = Modifier

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = if (active) DskRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, DskRed) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) DskRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) DskRed else MaterialTheme.colorScheme.onSurface
            )
        }
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

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Write a reply...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(replyText)
                            replyText = ""
                            replying = false
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Post reply", tint = DskRed)
                    }
                }
            }

            if (item.replies.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.replies.forEach { rep ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
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
