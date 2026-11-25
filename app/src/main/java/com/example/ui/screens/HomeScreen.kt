package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.ShortWithChannel
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.DskBrandLogo
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.util.formatCount

@Composable
fun HomeScreen(
    videos: List<VideoWithChannel>,
    shorts: List<ShortWithChannel>,
    categories: List<CategoryEntity>,
    watchLaterIds: Set<String>,
    unreadNotifications: Int,
    currentProfile: ProfileEntity?,
    gridColumns: Int,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onShortClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onShareVideo: (VideoWithChannel) -> Unit,
    onReportVideo: (VideoWithChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val chipNames = remember(categories) {
        listOf("All") + categories.map { it.name }.distinct()
    }

    val filteredVideos = remember(videos, selectedCategory) {
        when (selectedCategory) {
            "All" -> videos
            "Live" -> videos.filter { it.video.isLive || it.video.category.equals("Live", true) }
            else -> videos.filter { it.video.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Brand Header
        Surface(
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DskBrandLogo()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_search_button")
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search")
                        }
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge(containerColor = DskRed) {
                                        Text(
                                            text = unreadNotifications.coerceAtMost(99).toString(),
                                            color = DskWhite
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = onNotificationsClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_notifications_button")
                            ) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }
                        }
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_profile_button")
                        ) {
                            if (currentProfile != null) {
                                ChannelAvatar(
                                    name = currentProfile.displayName,
                                    avatarUri = currentProfile.avatarUri,
                                    size = 30.dp
                                )
                            } else {
                                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile")
                            }
                        }
                    }
                }

                // Category Chips Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chipNames) { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DskRed,
                                selectedLabelColor = DskWhite
                            ),
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }
            }
        }

        // Feed Body
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 100% Free Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, DskRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(DskRed.copy(alpha = 0.16f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Watch Everything. Pay Nothing.",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No subscriptions, no paywalls, no hidden fees — free creator streaming.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = DskRed
                        )
                    }
                }
            }

            if (selectedCategory == "All") {
                val trending = videos.filter { it.video.isTrending || it.video.isFeatured }
                    .ifEmpty { videos.take(4) }
                val recommended = videos.filter { it.video.sectionTag == "Recommended For You" }
                    .ifEmpty { videos.take(4) }
                val latest = videos.sortedByDescending { it.video.createdAt }.take(6)
                val mostViewed = videos.sortedByDescending { it.video.viewCount }.take(4)
                val musicVideos = videos.filter { it.video.category.equals("Music", true) }
                val liveStreams = videos.filter { it.video.isLive }

                // Trending Section
                if (trending.isNotEmpty()) {
                    item { SectionHeader(title = "Trending Videos", icon = Icons.Outlined.Whatshot) }
                    item {
                        AdaptiveVideoGrid(
                            items = trending,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }

                // Popular Shorts Horizontal Strip
                if (shorts.isNotEmpty()) {
                    item { SectionHeader(title = "Popular Shorts", icon = Icons.Outlined.Bolt) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(shorts, key = { it.shortItem.id }) { s ->
                                ShortPreviewCard(
                                    item = s,
                                    onClick = { onShortClick(s.shortItem.id) }
                                )
                            }
                        }
                    }
                }

                // Recommended For You
                if (recommended.isNotEmpty()) {
                    item { SectionHeader(title = "Recommended For You", icon = Icons.Outlined.AutoAwesome) }
                    item {
                        AdaptiveVideoGrid(
                            items = recommended,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }

                // Live Streams
                if (liveStreams.isNotEmpty()) {
                    item { SectionHeader(title = "Live Streams", icon = Icons.Outlined.Sensors) }
                    item {
                        AdaptiveVideoGrid(
                            items = liveStreams,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }

                // Music Videos
                if (musicVideos.isNotEmpty()) {
                    item { SectionHeader(title = "Music Videos", icon = Icons.Outlined.MusicNote) }
                    item {
                        AdaptiveVideoGrid(
                            items = musicVideos,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }

                // Most Viewed
                if (mostViewed.isNotEmpty()) {
                    item { SectionHeader(title = "Most Viewed", icon = Icons.Outlined.TrendingUp) }
                    item {
                        AdaptiveVideoGrid(
                            items = mostViewed,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }

                // Latest Uploads
                if (latest.isNotEmpty()) {
                    item { SectionHeader(title = "Latest Uploads", icon = Icons.Outlined.Schedule) }
                    item {
                        AdaptiveVideoGrid(
                            items = latest,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }
            } else {
                item {
                    SectionHeader(title = "$selectedCategory Videos", icon = Icons.Outlined.VideoLibrary)
                }
                if (filteredVideos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "No videos in $selectedCategory yet.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Be the first creator to upload one using Create (+)!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        AdaptiveVideoGrid(
                            items = filteredVideos,
                            columns = gridColumns,
                            watchLaterIds = watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShareVideo = onShareVideo,
                            onReportVideo = onReportVideo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = DskRed, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AdaptiveVideoGrid(
    items: List<VideoWithChannel>,
    columns: Int,
    watchLaterIds: Set<String>,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onShareVideo: (VideoWithChannel) -> Unit,
    onReportVideo: (VideoWithChannel) -> Unit
) {
    val rows = items.chunked(columns.coerceAtLeast(1))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        VideoCard(
                            item = item,
                            isWatchLater = item.video.id in watchLaterIds,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onToggleWatchLater = onToggleWatchLater,
                            onAddToPlaylist = onAddToPlaylist,
                            onShare = onShareVideo,
                            onReport = onReportVideo
                        )
                    }
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShortPreviewCard(
    item: ShortWithChannel,
    onClick: () -> Unit
) {
    val s = item.shortItem
    Box(
        modifier = Modifier
            .width(148.dp)
            .height(248.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
            .testTag("home_short_card_${s.id}")
    ) {
        VideoThumbnailImage(
            thumbnailUrl = s.thumbnailUrl,
            thumbnailResKey = s.thumbnailResKey,
            contentDescription = s.title,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f)),
                        startY = 200f
                    )
                )
        )
        Surface(
            color = DskRed,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "SHORT",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DskWhite
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = s.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = DskWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatCount(s.viewCount)} views",
                style = MaterialTheme.typography.labelSmall,
                color = DskWhite.copy(alpha = 0.78f)
            )
        }
    }
}
