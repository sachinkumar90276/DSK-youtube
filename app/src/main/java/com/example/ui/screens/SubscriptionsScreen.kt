package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.util.formatCount

@Composable
fun SubscriptionsScreen(
    isSignedIn: Boolean,
    channels: List<ChannelEntity>,
    subscribedIds: Set<String>,
    videos: List<VideoWithChannel>,
    watchLaterIds: Set<String>,
    onRequireSignIn: () -> Unit,
    onChannelClick: (String) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onToggleWatchLater: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onShareVideo: (VideoWithChannel) -> Unit,
    onReportVideo: (VideoWithChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val subscribedChannels = remember(channels, subscribedIds) {
        channels.filter { it.id in subscribedIds && !it.isSuspended }
    }
    val subVideos = remember(videos, subscribedIds) {
        videos.filter { it.channel.id in subscribedIds }.sortedByDescending { it.video.createdAt }
    }
    val discoverChannels = remember(channels, subscribedIds) {
        channels.filter { it.id !in subscribedIds && !it.isSuspended }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LiquidGlassCard(
                shape = RoundedCornerShape(24.dp),
                intensity = GlassIntensity.STRONG,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Subscriptions", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "100% free creator subscriptions — never any fee or membership charge.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!isSignedIn) {
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    intensity = GlassIntensity.REGULAR,
                    accentTint = DskRed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Subscriptions, contentDescription = null, modifier = Modifier.size(46.dp), tint = DskRed)
                        Text("Sign in to follow your favorite creators", style = MaterialTheme.typography.titleMedium)
                        LiquidGlassButton(onClick = onRequireSignIn) {
                            Text("Sign In", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (subscribedChannels.isNotEmpty()) {
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    intensity = GlassIntensity.SUBTLE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(subscribedChannels, key = { it.id }) { ch ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clickable { onChannelClick(ch.id) }
                            ) {
                                ChannelAvatar(name = ch.name, avatarUri = ch.avatarUri, size = 56.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (subVideos.isNotEmpty()) {
            item {
                Text("Latest From Your Subscriptions", style = MaterialTheme.typography.titleLarge)
            }
            items(subVideos, key = { it.video.id }) { item ->
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

        if (discoverChannels.isNotEmpty()) {
            item {
                Text("Discover Free Channels", style = MaterialTheme.typography.titleLarge)
            }
            items(discoverChannels, key = { it.id }) { ch ->
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    intensity = GlassIntensity.REGULAR,
                    onClick = { onChannelClick(ch.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelAvatar(name = ch.name, avatarUri = ch.avatarUri, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${ch.handle} • ${formatCount(ch.subscriberCount)} subscribers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LiquidGlassButton(
                            onClick = { onToggleSubscribe(ch.id) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Subscribe", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
