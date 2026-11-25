package com.example.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.util.formatCount

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
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
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No Shorts available yet. Upload one with Create (+)!",
                color = DskWhite,
                style = MaterialTheme.typography.titleMedium
            )
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
            val isVisible = pagerState.currentPage == page && commentSheetShortId == null
            ShortPageItem(
                item = item,
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

        // Top Shorts Title Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(99.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = DskRed, modifier = Modifier.size(18.dp))
                    Text("DSK Shorts", color = DskWhite, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // Comments Bottom Sheet
    commentSheetShortId?.let { sid ->
        val sheetComments = commentsMap["SHORT:$sid"].orEmpty()
        var draft by remember(sid) { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { commentSheetShortId = null }
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
                    style = MaterialTheme.typography.titleMedium
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (draft.isNotBlank()) {
                            onAddComment(sid, draft)
                            draft = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send comment", tint = DskRed)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortPageItem(
    item: ShortWithChannel,
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
    var userPaused by remember(s.id) { mutableStateOf(false) }

    val exoPlayer = remember(s.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(s.videoUrl))
        }
    }

    LaunchedEffect(isVisible, userPaused) {
        if (isVisible) {
            onRecordView()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { userPaused = !userPaused }
    ) {
        // 9:16 Centered Video Frame (responsive on both phone & tablet)
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
            if (isVisible) {
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

        // Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        if (userPaused) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Pause, contentDescription = "Paused", tint = DskWhite, modifier = Modifier.size(34.dp))
                }
            }
        }

        // Right Side Action Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShortRailButton(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(s.likeCount),
                tint = if (isLiked) DskRed else DskWhite,
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

        // Bottom Left Creator Info, Caption, Music Info
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
                    size = 36.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onChannelClick)
                )
                Text(
                    text = ch.handle,
                    color = DskWhite,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable(onClick = onChannelClick)
                )
                Button(
                    onClick = onSubscribe,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) Color.White.copy(alpha = 0.22f) else DskRed,
                        contentColor = DskWhite
                    ),
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (isSubscribed) "Subscribed" else "Subscribe",
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
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(99.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = DskWhite, style = MaterialTheme.typography.labelSmall)
    }
}
