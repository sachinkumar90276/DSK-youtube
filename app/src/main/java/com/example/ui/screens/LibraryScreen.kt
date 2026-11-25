package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactVideoRow
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite

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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Header Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(
                            name = currentProfile?.displayName ?: "Guest",
                            avatarUri = currentProfile?.avatarUri,
                            size = 56.dp
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
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("library_settings_button")
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentUser == null) {
                            Button(
                                onClick = onOpenAuth,
                                colors = ButtonDefaults.buttonColors(containerColor = DskRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("library_signin_button")
                            ) {
                                Text("Sign In / Register")
                            }
                        } else {
                            OutlinedButton(
                                onClick = onOpenEditProfile,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                            if (myChannel != null) {
                                OutlinedButton(
                                    onClick = { onChannelClick(myChannel.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("My Channel")
                                }
                            }
                        }
                    }

                    // Admin Panel Entry — ONLY rendered when user holds verified admin role
                    if (isAdmin) {
                        Button(
                            onClick = onOpenAdminPanel,
                            colors = ButtonDefaults.buttonColors(containerColor = DskRed, contentColor = DskWhite),
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

        // Section Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    "History", "Watch Later", "Liked", "My Videos", "My Shorts", "Subscriptions", "Playlists"
                )
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedSection == tab,
                        onClick = { selectedSection = tab },
                        label = { Text(tab) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DskRed,
                            selectedLabelColor = DskWhite
                        )
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShortClick(s.shortItem.id) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelClick(ch.id) }
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
                            Button(
                                onClick = { showCreatePlaylist = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DskRed)
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistClick(pl.playlist.id) }
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
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Create Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = visibility == "PUBLIC", onClick = { visibility = "PUBLIC" }, label = { Text("Public") })
                        FilterChip(selected = visibility == "PRIVATE", onClick = { visibility = "PRIVATE" }, label = { Text("Private") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreatePlaylist(title, desc, visibility)
                    showCreatePlaylist = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyLibraryNote(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
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
