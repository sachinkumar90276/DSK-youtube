package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.PlaylistSummary
import com.example.data.local.entity.ShortWithChannel
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.util.formatCount
import com.example.ui.util.formatRelativeTime

@Composable
fun ChannelScreen(
    channel: ChannelEntity,
    channelVideos: List<VideoWithChannel>,
    channelShorts: List<ShortWithChannel>,
    channelPlaylists: List<PlaylistSummary>,
    isSubscribed: Boolean,
    onBack: () -> Unit,
    onRefreshChannel: () -> Unit = {},
    onToggleSubscribe: () -> Unit,
    onShareChannel: () -> Unit,
    onReportChannel: () -> Unit,
    onVideoClick: (String) -> Unit,
    onShortClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Videos", "Shorts", "Playlists", "About")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LiquidGlassTopBar(
            title = channel.name,
            subtitle = channel.handle,
            onBack = onBack
        ) {
            if (channel.sourceType == "YOUTUBE") {
                LiquidGlassIconButton(onClick = onRefreshChannel, size = 38.dp, intensity = GlassIntensity.SUBTLE) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh channel", modifier = Modifier.size(18.dp))
                }
            }
            LiquidGlassIconButton(onClick = onShareChannel, size = 38.dp, intensity = GlassIntensity.SUBTLE) {
                Icon(Icons.Outlined.Share, contentDescription = "Share channel", modifier = Modifier.size(18.dp))
            }
            LiquidGlassIconButton(onClick = onReportChannel, size = 38.dp, intensity = GlassIntensity.SUBTLE) {
                Icon(Icons.Outlined.Flag, contentDescription = "Report channel", modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner + Channel Identity Glass Card
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    intensity = GlassIntensity.REGULAR,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    if (!channel.bannerUri.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.bannerUri,
                            contentDescription = "${channel.name} banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_channel_banner),
                            contentDescription = "${channel.name} banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelAvatar(name = channel.name, avatarUri = channel.avatarUri, size = 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "${channel.handle} • ${formatCount(channel.subscriberCount)} subscribers • ${channelVideos.size} videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LiquidGlassButton(
                        onClick = onToggleSubscribe,
                        isPrimary = !isSubscribed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed (Free)" else "Subscribe — Free",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Glass Segmented Tabs
            item {
                LiquidGlassSegmentedTabs(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (channelVideos.isEmpty()) {
                        item { EmptyTabText("No videos uploaded by this channel yet.") }
                    } else {
                        items(channelVideos, key = { it.video.id }) { item ->
                            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                CompactVideoRow(item = item, onClick = { onVideoClick(item.video.id) })
                            }
                        }
                    }
                }
                1 -> {
                    if (channelShorts.isEmpty()) {
                        item { EmptyTabText("No Shorts uploaded by this channel yet.") }
                    } else {
                        items(channelShorts, key = { it.shortItem.id }) { s ->
                            LiquidGlassCard(
                                shape = RoundedCornerShape(16.dp),
                                intensity = GlassIntensity.SUBTLE,
                                onClick = { onShortClick(s.shortItem.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = DskRed)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(s.shortItem.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${formatCount(s.shortItem.viewCount)} views",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (channelPlaylists.isEmpty()) {
                        item { EmptyTabText("No public playlists on this channel.") }
                    } else {
                        items(channelPlaylists, key = { it.playlist.id }) { pl ->
                            LiquidGlassCard(
                                shape = RoundedCornerShape(16.dp),
                                intensity = GlassIntensity.SUBTLE,
                                onClick = { onPlaylistClick(pl.playlist.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.PlaylistPlay, contentDescription = null, tint = DskRed)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(pl.playlist.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${pl.videoCount} videos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    item {
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            intensity = GlassIntensity.REGULAR,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("About ${channel.name}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    channel.description.ifBlank { "Creator on DSK YouTube." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Text(
                                    "Total views: ${formatCount(channelVideos.sumOf { it.video.viewCount } + channelShorts.sumOf { it.shortItem.viewCount })}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Joined ${formatRelativeTime(channel.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTabText(msg: String) {
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        intensity = GlassIntensity.SUBTLE,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
