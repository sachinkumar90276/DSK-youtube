package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskRedGlow
import com.example.ui.theme.DskWhite
import com.example.ui.theme.GlassHighlight
import com.example.ui.util.formatCount
import com.example.ui.util.formatDuration
import com.example.ui.util.formatRelativeTime

@Composable
fun VideoCard(
    item: VideoWithChannel,
    isWatchLater: Boolean,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onShare: (VideoWithChannel) -> Unit,
    onReport: (VideoWithChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val v = item.video
    val ch = item.channel

    LiquidGlassCard(
        shape = RoundedCornerShape(22.dp),
        intensity = GlassIntensity.REGULAR,
        elevation = 8.dp,
        onClick = { onVideoClick(v.id) },
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${v.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.Black)
        ) {
            VideoThumbnailImage(
                thumbnailUrl = v.thumbnailUrl,
                thumbnailResKey = v.thumbnailResKey,
                contentDescription = v.title,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle bottom contrast vignette
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )

            // Frosted glass source attribution badge top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.Black.copy(alpha = 0.48f))
                    .border(0.8.dp, GlassHighlight.copy(alpha = 0.65f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (v.sourceType == "YOUTUBE") "YouTube" else "Creator Upload",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DskWhite
                )
            }

            // Duration or LIVE glass pill bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (v.isLive) Brush.horizontalGradient(listOf(DskRedGlow, DskRed))
                        else Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Black.copy(alpha = 0.52f)))
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (v.isLive) Color(0xFFFFA5AB).copy(alpha = 0.7f) else GlassHighlight.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(99.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (v.isLive) "LIVE" else formatDuration(v.durationSeconds),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = DskWhite
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            ChannelAvatar(
                name = ch.name,
                avatarUri = ch.avatarUri,
                size = 40.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable { onChannelClick(ch.id) }
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = v.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${ch.name} • ${formatCount(v.viewCount)} views • ${formatRelativeTime(v.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("video_menu_${v.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options for ${v.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = glassDropdownContainerColor(),
                    border = glassMenuBorder(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isWatchLater) "Remove from Watch Later" else "Save to Watch Later") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isWatchLater) Icons.Outlined.BookmarkAdded else Icons.Outlined.WatchLater,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleWatchLater(v.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist(v.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View channel") },
                        leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onChannelClick(ch.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShare(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Report video") },
                        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onReport(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactVideoRow(
    item: VideoWithChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val v = item.video
    val ch = item.channel
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        intensity = GlassIntensity.SUBTLE,
        elevation = 4.dp,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(142.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                VideoThumbnailImage(
                    thumbnailUrl = v.thumbnailUrl,
                    thumbnailResKey = v.thumbnailResKey,
                    contentDescription = v.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (v.isLive) DskRed else Color.Black.copy(alpha = 0.72f))
                        .border(0.6.dp, GlassHighlight.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (v.isLive) "LIVE" else formatDuration(v.durationSeconds),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DskWhite
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = v.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ch.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatCount(v.viewCount)} views • ${formatRelativeTime(v.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
