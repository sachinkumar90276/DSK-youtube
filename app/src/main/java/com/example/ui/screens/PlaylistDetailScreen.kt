package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.VideoWithChannel
import com.example.ui.components.*
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
        LiquidGlassTopBar(
            title = playlist.title,
            subtitle = "${videos.size} videos • ${playlist.visibility}",
            onBack = onBack
        ) {
            if (isOwner) {
                LiquidGlassIconButton(onClick = { showEditDialog = true }, size = 38.dp, intensity = GlassIntensity.SUBTLE) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit playlist", modifier = Modifier.size(18.dp))
                }
                LiquidGlassIconButton(
                    onClick = { onDeletePlaylist() },
                    size = 38.dp,
                    intensity = GlassIntensity.SUBTLE
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete playlist", tint = DskRed, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    intensity = GlassIntensity.REGULAR,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            LiquidGlassButton(onClick = { onPlayVideo(videos.first().video.id) }) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play All", fontWeight = FontWeight.Bold)
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
        LiquidGlassDialog(
            onDismissRequest = { showEditDialog = false },
            title = "Edit Playlist",
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
                        LiquidGlassChip(selected = vis == "PUBLIC", onClick = { vis = "PUBLIC" }, label = "Public")
                        LiquidGlassChip(selected = vis == "PRIVATE", onClick = { vis = "PRIVATE" }, label = "Private")
                    }
                }
            },
            actions = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                LiquidGlassButton(onClick = {
                    onEditPlaylist(title, desc, vis)
                    showEditDialog = false
                }) { Text("Save") }
            }
        )
    }
}
