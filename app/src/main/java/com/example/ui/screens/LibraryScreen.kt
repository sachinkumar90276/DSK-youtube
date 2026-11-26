package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.ui.components.*
import com.example.ui.theme.DskRed

@Composable
fun LibraryScreen(
    currentUser: UserEntity?,
    currentProfile: ProfileEntity?,
    myChannel: ChannelEntity?,
    isAdmin: Boolean,
    watchHistory: List<VideoWithChannel>,
    watchLater: List<VideoWithChannel>,
    likedVideos: List<VideoWithChannel>,
    myVideos: List<VideoWithChannel>,
    myShorts: List<ShortWithChannel>,
    subscribedChannels: List<ChannelEntity>,
    playlists: List<PlaylistSummary>,
    onOpenAuth: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onVideoClick: (String) -> Unit,
    onShortClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylist: (title: String, desc: String, visibility: String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onClearWatchHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf("History") }
    var showCreatePlaylist by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Glass Profile Header Card
        item {
            LiquidGlassCard(
                shape = RoundedCornerShape(26.dp),
                intensity = GlassIntensity.STRONG,
                elevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(
                            name = currentProfile?.displayName ?: "Guest",
                            avatarUri = currentProfile?.avatarUri,
                            size = 58.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProfile?.displayName ?: "Guest Viewer",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = currentUser?.email ?: "Sign in to sync playlists, uploads & history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LiquidGlassIconButton(
                            onClick = onOpenSettings,
                            size = 42.dp,
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.testTag("library_settings_button")
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentUser == null) {
                            LiquidGlassButton(
                                onClick = onOpenAuth,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("library_signin_button")
                            ) {
                                Text("Sign In / Register", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            LiquidGlassButton(
                                onClick = onOpenEditProfile,
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                            if (myChannel != null) {
                                LiquidGlassButton(
                                    onClick = { onChannelClick(myChannel.id) },
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("My Channel")
                                }
                            }
                        }
                    }

                    // Admin Panel Entry — ONLY rendered when user holds verified admin role
                    if (isAdmin) {
                        LiquidGlassButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_admin_panel_button")
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Admin Moderation Panel", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section Glass Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    "History", "Watch Later", "Liked", "My Videos", "My Shorts", "Subscriptions", "Playlists"
                )
                items(tabs) { tab ->
                    LiquidGlassChip(
                        selected = selectedSection == tab,
                        onClick = { selectedSection = tab },
                        label = tab
                    )
                }
            }
        }

        when (selectedSection) {
            "History" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Watch History (${watchHistory.size})", style = MaterialTheme.typography.titleMedium)
                        if (watchHistory.isNotEmpty()) {
                            TextButton(onClick = onClearWatchHistory) {
                                Text("Clear history", color = DskRed)
                            }
                        }
                    }
                }
                if (watchHistory.isEmpty()) {
                    item { EmptyLibraryNote("Your watch history is empty.") }
                } else {
                    items(watchHistory, key = { it.video.id }) { item ->
                        CompactVideoRow(item = item, onClick = { onVideoClick(item.video.id) })
                    }
                }
            }
            "Watch Later" -> {
                item { Text("Watch Later (${watchLater.size})", style = MaterialTheme.typography.titleMedium) }
                if (watchLater.isEmpty()) {
                    item { EmptyLibraryNote("Save videos to Watch Later from any video card or player.") }
                } else {
                    items(watchLater, key = { it.video.id }) { item ->
                        CompactVideoRow(item = item, onClick = { onVideoClick(item.video.id) })
                    }
                }
            }
            "Liked" -> {
                item { Text("Liked Videos (${likedVideos.size})", style = MaterialTheme.typography.titleMedium) }
                if (likedVideos.isEmpty()) {
                    item { EmptyLibraryNote("Videos you like will appear here.") }
                } else {
                    items(likedVideos, key = { it.video.id }) { item ->
                        CompactVideoRow(item = item, onClick = { onVideoClick(item.video.id) })
                    }
                }
            }
            "My Videos" -> {
                item { Text("My Uploaded Videos (${myVideos.size})", style = MaterialTheme.typography.titleMedium) }
                if (myVideos.isEmpty()) {
                    item { EmptyLibraryNote("Use Create (+) in the bottom bar to upload your first free video.") }
                } else {
                    items(myVideos, key = { it.video.id }) { item ->
                        CompactVideoRow(item = item, onClick = { onVideoClick(item.video.id) })
                    }
                }
            }
            "My Shorts" -> {
                item { Text("My Uploaded Shorts (${myShorts.size})", style = MaterialTheme.typography.titleMedium) }
                if (myShorts.isEmpty()) {
                    item { EmptyLibraryNote("Use Create (+) to publish a 9:16 Short.") }
                } else {
                    items(myShorts, key = { it.shortItem.id }) { s ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            onClick = { onShortClick(s.shortItem.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = DskRed)
                                Spacer(Modifier.width(10.dp))
                                Text(s.shortItem.title, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
            "Subscriptions" -> {
                item { Text("My Subscriptions (${subscribedChannels.size})", style = MaterialTheme.typography.titleMedium) }
                if (subscribedChannels.isEmpty()) {
                    item { EmptyLibraryNote("No channel subscriptions yet.") }
                } else {
                    items(subscribedChannels, key = { it.id }) { ch ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            onClick = { onChannelClick(ch.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ChannelAvatar(name = ch.name, avatarUri = ch.avatarUri, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(ch.name, style = MaterialTheme.typography.titleSmall)
                                    Text(ch.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            "Playlists" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Playlists (${playlists.size})", style = MaterialTheme.typography.titleMedium)
                        if (currentUser != null) {
                            LiquidGlassButton(
                                onClick = { showCreatePlaylist = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New")
                            }
                        }
                    }
                }
                if (playlists.isEmpty()) {
                    item { EmptyLibraryNote("Create playlists to organize your favorite free videos.") }
                } else {
                    items(playlists, key = { it.playlist.id }) { pl ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            intensity = GlassIntensity.REGULAR,
                            onClick = { onPlaylistClick(pl.playlist.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.PlaylistPlay, contentDescription = null, tint = DskRed)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.playlist.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${pl.videoCount} videos • ${pl.playlist.visibility}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeletePlaylist(pl.playlist) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete playlist")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylist) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var visibility by remember { mutableStateOf("PUBLIC") }
        LiquidGlassDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = "Create Playlist",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LiquidGlassTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = "Description",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassChip(selected = visibility == "PUBLIC", onClick = { visibility = "PUBLIC" }, label = "Public")
                        LiquidGlassChip(selected = visibility == "PRIVATE", onClick = { visibility = "PRIVATE" }, label = "Private")
                    }
                }
            },
            actions = {
                TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                LiquidGlassButton(onClick = {
                    onCreatePlaylist(title, desc, visibility)
                    showCreatePlaylist = false
                }) { Text("Create") }
            }
        )
    }
}

@Composable
private fun EmptyLibraryNote(text: String) {
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        intensity = GlassIntensity.SUBTLE,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
