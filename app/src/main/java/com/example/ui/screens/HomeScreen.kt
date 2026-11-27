package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.data.remote.YouTubeSyncState
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWarning
import com.example.ui.theme.DskWhite
import com.example.ui.theme.GlassEdgeShadow
import com.example.ui.theme.GlassHighlight
import com.example.ui.util.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    videos: List<VideoWithChannel>,
    shorts: List<ShortWithChannel>,
    categories: List<CategoryEntity>,
    watchLaterIds: Set<String>,
    unreadNotifications: Int,
    currentProfile: ProfileEntity?,
    gridColumns: Int,
    syncState: YouTubeSyncState,
    onRefresh: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onCategoryRefresh: (String) -> Unit = { onCategorySelected(it) },
    onLoadMore: (String) -> Unit = {},
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

    val isRefreshing = syncState is YouTubeSyncState.Refreshing
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(selectedCategory, filteredVideos.size, videos.size) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 2 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore, selectedCategory, filteredVideos.size, isRefreshing) {
        if (shouldLoadMore && !isRefreshing && filteredVideos.isNotEmpty()) {
            onLoadMore(selectedCategory)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Floating Liquid Glass Header & Search Pill
        LiquidGlassCard(
            shape = RoundedCornerShape(26.dp),
            intensity = GlassIntensity.STRONG,
            elevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DskBrandLogo()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidGlassIconButton(
                            onClick = onRefresh,
                            size = 40.dp,
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh YouTube feed",
                                modifier = Modifier.size(20.dp)
                            )
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
                            LiquidGlassIconButton(
                                onClick = onNotificationsClick,
                                size = 40.dp,
                                intensity = GlassIntensity.SUBTLE,
                                modifier = Modifier.testTag("home_notifications_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        LiquidGlassIconButton(
                            onClick = onProfileClick,
                            size = 40.dp,
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.testTag("home_profile_button")
                        ) {
                            if (currentProfile != null) {
                                ChannelAvatar(
                                    name = currentProfile.displayName,
                                    avatarUri = currentProfile.avatarUri,
                                    size = 32.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.AccountCircle,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Interactive Glass Search Bar Pill
                LiquidGlassPill(
                    intensity = GlassIntensity.SUBTLE,
                    elevation = 2.dp,
                    onClick = onSearchClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .testTag("home_search_button")
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = DskRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Search live YouTube videos, Shorts, channels...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DskRed
                        )
                    }
                }
            }
        }

        // Translucent Category Glass Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chipNames) { cat ->
                val selected = selectedCategory == cat
                LiquidGlassChip(
                    selected = selected,
                    onClick = {
                        selectedCategory = cat
                        onCategorySelected(cat)
                    },
                    label = cat,
                    modifier = Modifier.testTag("category_chip_$cat")
                )
            }
        }

        // Pull-to-refresh Feed Body
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (selectedCategory == "All") onRefresh() else onCategoryRefresh(selectedCategory)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 100% Free Ad-Free Glass Banner
                item {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        intensity = GlassIntensity.REGULAR,
                        accentTint = DskRed,
                        elevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Watch Everything. Pay Nothing.",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "100% Free • Zero DSK Ads • Live YouTube Data API v3",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(12.dp),
                                        intensity = GlassIntensity.STRONG,
                                        selected = true
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayCircle,
                                    contentDescription = null,
                                    tint = DskWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Live YouTube Sync Status / Quota / Missing Key Banner
                when (syncState) {
                    YouTubeSyncState.MissingApiKey -> {
                        item {
                            SyncStatusBanner(
                                title = "YouTube API Key Not Configured",
                                message = "Add YOUTUBE_API_KEY in the AI Studio Secrets panel to stream live YouTube trending videos, categories, Shorts, live streams, and search results.",
                                accentColor = DskRed,
                                onRetry = onRefresh
                            )
                        }
                    }
                    is YouTubeSyncState.QuotaExceeded -> {
                        item {
                            SyncStatusBanner(
                                title = "YouTube API Quota Exhausted",
                                message = syncState.message,
                                accentColor = DskWarning,
                                onRetry = {
                                    if (selectedCategory == "All") onRefresh() else onCategoryRefresh(selectedCategory)
                                }
                            )
                        }
                    }
                    is YouTubeSyncState.Error -> {
                        item {
                            SyncStatusBanner(
                                title = "YouTube Refresh Notice",
                                message = syncState.message,
                                accentColor = DskWarning,
                                onRetry = {
                                    if (selectedCategory == "All") onRefresh() else onCategoryRefresh(selectedCategory)
                                }
                            )
                        }
                    }
                    else -> Unit
                }

                if (selectedCategory == "All") {
                    if (videos.isEmpty()) {
                        item {
                            EmptyFeedState(
                                title = if (isRefreshing) "Loading live YouTube videos..." else "No YouTube videos loaded yet",
                                subtitle = if (syncState is YouTubeSyncState.MissingApiKey) {
                                    "Configure YOUTUBE_API_KEY in AI Studio Secrets, or upload your own video with Create (+)."
                                } else {
                                    "Pull down or tap Refresh to fetch live trending videos from YouTube."
                                },
                                isLoading = isRefreshing,
                                onRetry = onRefresh
                            )
                        }
                    } else {
                        val trending = videos.filter { it.video.isTrending || it.video.isFeatured }
                            .ifEmpty { videos.take(4) }
                        val recommended = videos.filter { it.video.sectionTag == "Recommended For You" }
                            .ifEmpty { videos.drop(4).take(6).ifEmpty { videos.take(4) } }
                        val latest = videos.sortedByDescending { it.video.createdAt }.take(6)
                        val mostViewed = videos.sortedByDescending { it.video.viewCount }.take(4)
                        val musicVideos = videos.filter { it.video.category.equals("Music", true) }
                        val liveStreams = videos.filter { it.video.isLive }

                        if (trending.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Trending Videos", icon = Icons.Outlined.Whatshot) }
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

                        if (shorts.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Popular Shorts", icon = Icons.Outlined.Bolt) }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
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

                        if (recommended.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Recommended For You", icon = Icons.Outlined.AutoAwesome) }
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

                        if (liveStreams.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Live Streams", icon = Icons.Outlined.Sensors) }
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

                        if (musicVideos.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Music Videos", icon = Icons.Outlined.MusicNote) }
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

                        if (mostViewed.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Most Viewed", icon = Icons.Outlined.TrendingUp) }
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

                        if (latest.isNotEmpty()) {
                            item { LiquidGlassSectionHeader(title = "Latest Uploads", icon = Icons.Outlined.Schedule) }
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
                    }
                } else {
                    item {
                        LiquidGlassSectionHeader(title = "$selectedCategory Videos", icon = Icons.Outlined.VideoLibrary)
                    }
                    if (filteredVideos.isEmpty()) {
                        item {
                            EmptyFeedState(
                                title = if (isRefreshing) "Fetching $selectedCategory from YouTube..." else "No videos in $selectedCategory yet",
                                subtitle = "Pull down or tap Refresh to fetch live $selectedCategory videos from YouTube.",
                                isLoading = isRefreshing,
                                onRetry = { onCategoryRefresh(selectedCategory) }
                            )
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
}

@Composable
private fun SyncStatusBanner(
    title: String,
    message: String,
    accentColor: Color,
    onRetry: () -> Unit
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        intensity = GlassIntensity.REGULAR,
        accentTint = accentColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = accentColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRetry) {
                Text("Retry", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyFeedState(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    onRetry: () -> Unit
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(24.dp),
        intensity = GlassIntensity.REGULAR,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = DskRed)
            } else {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = DskRed
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isLoading) {
                Spacer(Modifier.height(16.dp))
                LiquidGlassButton(onClick = onRetry) {
                    Text("Refresh Feed", fontWeight = FontWeight.Bold)
                }
            }
        }
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
            .padding(horizontal = 14.dp),
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
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .width(152.dp)
            .height(254.dp)
            .shadow(10.dp, cardShape, ambientColor = GlassEdgeShadow, spotColor = GlassEdgeShadow)
            .clip(cardShape)
            .background(Color.Black)
            .border(1.dp, GlassHighlight.copy(alpha = 0.48f), cardShape)
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f)),
                        startY = 200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(DskRed.copy(alpha = 0.88f))
                .border(0.8.dp, Color(0xFFFFA3AA).copy(alpha = 0.65f), RoundedCornerShape(99.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "SHORT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DskWhite
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(11.dp)
        ) {
            Text(
                text = s.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = DskWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${formatCount(s.viewCount)} views",
                style = MaterialTheme.typography.labelSmall,
                color = DskWhite.copy(alpha = 0.8f)
            )
        }
    }
}
