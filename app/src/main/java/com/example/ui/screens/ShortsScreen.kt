package com.example.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.entity.CommentWithReplies
import com.example.data.local.entity.ShortWithChannel
import com.example.data.remote.YouTubeParser
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassRedBorder
import com.example.ui.util.formatCount

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    shorts: List<ShortWithChannel>,
    initialShortId: String?,
    likedKeys: Set<String>,
    subscribedChannelIds: Set<String>,
    commentsMap: Map<String, List<CommentWithReplies>>,
    currentUserId: String?,
    onChannelClick: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onShareShort: (ShortWithChannel) -> Unit,
    onReportShort: (ShortWithChannel) -> Unit,
    onAddComment: (shortId: String, body: String) -> Unit,
    onAddReply: (commentId: String, body: String) -> Unit,
    onLikeComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onReportComment: (commentId: String, label: String) -> Unit,
    onRecordShortView: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shorts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(shape = RoundedCornerShape(24.dp), intensity = GlassIntensity.REGULAR) {
                Text(
                    text = "No Shorts loaded yet. Refresh Home to sync YouTube Shorts, or upload one with Create (+)!",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        return
    }

    val initialPage = remember(initialShortId, shorts) {
        shorts.indexOfFirst { it.shortItem.id == initialShortId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { shorts.size })
    var commentSheetShortId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 0, // Efficient bandwidth: only prepare visible Short
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = shorts[page]
            val isCurrentPage = pagerState.currentPage == page
            val isVisible = isCurrentPage && commentSheetShortId == null
            ShortPageItem(
                item = item,
                isCurrentPage = isCurrentPage,
                isVisible = isVisible,
                isLiked = "SHORT:${item.shortItem.id}" in likedKeys,
                isSubscribed = item.channel.id in subscribedChannelIds,
                commentCount = commentsMap["SHORT:${item.shortItem.id}"]?.size ?: item.shortItem.commentCount.toInt(),
                onChannelClick = { onChannelClick(item.channel.id) },
                onToggleLike = { onToggleLike(item.shortItem.id) },
                onOpenComments = { commentSheetShortId = item.shortItem.id },
                onShare = { onShareShort(item) },
                onSubscribe = { onToggleSubscribe(item.channel.id) },
                onReport = { onReportShort(item) },
                onRecordView = { onRecordShortView(item.shortItem.id) }
            )
        }

        // Top Floating Liquid Glass Shorts Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF0E1018).copy(alpha = 0.56f))
                    .border(1.dp, GlassHighlight.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = DskRed, modifier = Modifier.size(18.dp))
                    Text("DSK Shorts", color = DskWhite, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // Translucent Liquid Glass Comments Sheet
    commentSheetShortId?.let { sid ->
        val sheetComments = commentsMap["SHORT:$sid"].orEmpty()
        var draft by remember(sid) { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { commentSheetShortId = null },
            containerColor = Color(0xFF10131E).copy(alpha = 0.94f),
            contentColor = DskWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Comments (${sheetComments.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sheetComments, key = { it.comment.id }) { c ->
                        CommentCard(
                            item = c,
                            currentUserId = currentUserId,
                            onLike = { onLikeComment(c.comment.id) },
                            onReply = { body -> onAddReply(c.comment.id, body) },
                            onDelete = { onDeleteComment(c.comment.id) },
                            onReport = { onReportComment(c.comment.id, c.comment.body) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = "Add a comment...",
                        singleLine = true,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.weight(1f)
                    )
                    LiquidGlassIconButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onAddComment(sid, draft)
                                draft = ""
                            }
                        },
                        size = 44.dp
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send comment", tint = DskRed)
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ShortPageItem(
    item: ShortWithChannel,
    isCurrentPage: Boolean,
    isVisible: Boolean,
    isLiked: Boolean,
    isSubscribed: Boolean,
    commentCount: Int,
    onChannelClick: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onSubscribe: () -> Unit,
    onReport: () -> Unit,
    onRecordView: () -> Unit
) {
    val context = LocalContext.current
    val s = item.shortItem
    val ch = item.channel
    val ytShortId = remember(s.id, s.videoUrl, s.sourceType) {
        if (s.sourceType == "YOUTUBE") YouTubeParser.extractVideoId(s.id) ?: s.id
        else YouTubeParser.extractVideoId(s.videoUrl)
    }
    val isYouTube = ytShortId != null
    var userPaused by remember(s.id) { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage, s.id) {
        if (isCurrentPage) onRecordView()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { userPaused = !userPaused }
    ) {
        // 9:16 Centered Video Frame
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .align(Alignment.Center)
                .background(Color.Black)
        ) {
            VideoThumbnailImage(
                thumbnailUrl = s.thumbnailUrl,
                thumbnailResKey = s.thumbnailResKey,
                contentDescription = s.title,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrentPage) {
                if (isYouTube && ytShortId != null) {
                    YouTubeEmbedPlayer(
                        videoId = ytShortId,
                        autoplay = true,
                        loop = true,
                        isActive = isVisible && !userPaused,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val exoPlayer = remember(s.id) {
                        ExoPlayer.Builder(context).build().apply {
                            repeatMode = Player.REPEAT_MODE_ONE
                            setMediaItem(MediaItem.fromUri(s.videoUrl))
                        }
                    }

                    LaunchedEffect(isVisible, userPaused) {
                        if (isVisible) {
                            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                                exoPlayer.prepare()
                            }
                            exoPlayer.playWhenReady = !userPaused
                        } else {
                            exoPlayer.playWhenReady = false
                            exoPlayer.pause()
                        }
                    }

                    DisposableEffect(s.id) {
                        onDispose { exoPlayer.release() }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = exoPlayer
                            }
                        },
                        update = { pv -> pv.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        if (userPaused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.56f))
                    .border(1.2.dp, GlassHighlight.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Pause, contentDescription = "Paused", tint = DskWhite, modifier = Modifier.size(34.dp))
            }
        }

        // Right Side Floating Glass Action Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShortRailButton(
                icon = if (userPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                label = if (userPaused) "Play" else "Pause",
                tint = DskWhite,
                onClick = { userPaused = !userPaused },
                tag = "short_play_pause_${s.id}"
            )
            ShortRailButton(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(s.likeCount),
                tint = if (isLiked) DskRed else DskWhite,
                highlighted = isLiked,
                onClick = onToggleLike,
                tag = "short_like_${s.id}"
            )
            ShortRailButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = commentCount.toString(),
                tint = DskWhite,
                onClick = onOpenComments,
                tag = "short_comment_${s.id}"
            )
            ShortRailButton(
                icon = Icons.Outlined.Share,
                label = "Share",
                tint = DskWhite,
                onClick = onShare,
                tag = "short_share_${s.id}"
            )
            ShortRailButton(
                icon = Icons.Outlined.Flag,
                label = "Report",
                tint = DskWhite,
                onClick = onReport,
                tag = "short_report_${s.id}"
            )
        }

        // Bottom Left Glass Creator Pill, Caption, Music Pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChannelAvatar(
                    name = ch.name,
                    avatarUri = ch.avatarUri,
                    size = 38.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onChannelClick)
                )
                Text(
                    text = ch.handle,
                    color = DskWhite,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable(onClick = onChannelClick)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (isSubscribed) Color.White.copy(alpha = 0.18f)
                            else DskRed.copy(alpha = 0.90f)
                        )
                        .border(
                            0.8.dp,
                            if (isSubscribed) GlassHighlight else GlassRedBorder,
                            RoundedCornerShape(99.dp)
                        )
                        .clickable(onClick = onSubscribe)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                        color = DskWhite,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Text(
                text = s.caption,
                color = DskWhite,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF0E1018).copy(alpha = 0.56f))
                    .border(0.8.dp, GlassHighlight.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = DskWhite, modifier = Modifier.size(14.dp))
                    Text(
                        text = s.musicInfo,
                        color = DskWhite,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) DskRed.copy(alpha = 0.28f)
                    else Color(0xFF0E1018).copy(alpha = 0.56f)
                )
                .border(
                    width = 1.dp,
                    color = if (highlighted) GlassRedBorder else GlassHighlight.copy(alpha = 0.45f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = DskWhite, style = MaterialTheme.typography.labelSmall)
    }
}
