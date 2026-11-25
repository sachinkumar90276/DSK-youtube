package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.components.CompactVideoRow
import com.example.ui.theme.DskRed

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    videos: List<VideoWithChannel>,
    isOwner: Boolean,
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onEditPlaylist: (title: String, desc: String, visibility: String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveVideo: (String) -> Unit,
    onReorderVideo: (videoId: String, moveUp: Boolean) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(playlist.title, style = MaterialTheme.typography.titleLarge)
            }
            if (isOwner) {
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit playlist")
                    }
                    IconButton(onClick = {
                        onDeletePlaylist()
                        onBack()
                    }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete playlist", tint = DskRed)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(playlist.title, style = MaterialTheme.typography.headlineSmall)
                        if (playlist.description.isNotBlank()) {
                            Text(playlist.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "${videos.size} videos • ${playlist.visibility}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (videos.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { onPlayVideo(videos.first().video.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = DskRed)
                            ) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play All")
                            }
                        }
                    }
                }
            }

            itemsIndexed(videos, key = { _, item -> item.video.id }) { index, item ->
                CompactVideoRow(
                    item = item,
                    onClick = { onPlayVideo(item.video.id) },
                    trailingContent = if (isOwner) {
                        {
                            Row {
                                IconButton(
                                    onClick = { onReorderVideo(item.video.id, true) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = { onReorderVideo(item.video.id, false) },
                                    enabled = index < videos.lastIndex
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                                }
                                IconButton(onClick = { onRemoveVideo(item.video.id) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Remove from playlist")
                                }
                            }
                        }
                    } else null
                )
            }
        }
    }

    if (showEditDialog) {
        var title by remember { mutableStateOf(playlist.title) }
        var desc by remember { mutableStateOf(playlist.description) }
        var vis by remember { mutableStateOf(playlist.visibility) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = vis == "PUBLIC", onClick = { vis = "PUBLIC" }, label = { Text("Public") })
                        FilterChip(selected = vis == "PRIVATE", onClick = { vis = "PRIVATE" }, label = { Text("Private") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditPlaylist(title, desc, vis)
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}
