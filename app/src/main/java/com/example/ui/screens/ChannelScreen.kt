package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlaylistPlay
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
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactVideoRow
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
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
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(channel.name, style = MaterialTheme.typography.titleLarge)
            }
            Row {
                IconButton(onClick = onShareChannel) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share channel")
                }
                IconButton(onClick = onReportChannel) {
                    Icon(Icons.Outlined.Flag, contentDescription = "Report channel")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner + Channel Identity
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!channel.bannerUri.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.bannerUri,
                            contentDescription = "${channel.name} banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_channel_banner),
                            contentDescription = "${channel.name} banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
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

                    Button(
                        onClick = onToggleSubscribe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else DskRed,
                            contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else DskWhite
                        ),
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed (Free)" else "Subscribe — Free",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tabs
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (channelVideos.isEmpty()) {
                        item { EmptyTabText("No videos uploaded by this channel yet.") }
                    } else {
                        items(channelVideos, key = { it.video.id }) { item ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onShortClick(s.shortItem.id) }
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
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { onPlaylistClick(pl.playlist.id) }
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
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
                                HorizontalDivider()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
