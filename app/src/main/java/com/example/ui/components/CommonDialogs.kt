package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.PlaylistSummary

fun shareText(context: Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share via DSK YouTube"))
}

@Composable
fun ReportDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reasons = listOf(
        "Spam",
        "Copyright",
        "Violence",
        "Misleading content",
        "Harassment",
        "Other"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    LiquidGlassDialog(
        onDismissRequest = onDismiss,
        title = "Report Content",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    LiquidGlassCard(
                        shape = RoundedCornerShape(14.dp),
                        intensity = if (isSelected) GlassIntensity.REGULAR else GlassIntensity.SUBTLE,
                        selected = isSelected,
                        elevation = 0.dp,
                        onClick = { selectedReason = reason },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                LiquidGlassTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = "Additional details (optional)",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            LiquidGlassButton(
                onClick = { onSubmit(selectedReason, details) },
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistSummary>,
    onSelectPlaylist: (String) -> Unit,
    onCreateNew: (title: String, visibility: String) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(playlists.isEmpty()) }
    var newTitle by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("PUBLIC") }

    LiquidGlassDialog(
        onDismissRequest = onDismiss,
        title = if (creating) "New Playlist" else "Save to Playlist",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!creating && playlists.isNotEmpty()) {
                    playlists.forEach { summary ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            elevation = 2.dp,
                            onClick = { onSelectPlaylist(summary.playlist.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(summary.playlist.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${summary.videoCount} videos • ${summary.playlist.visibility}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = { creating = true }) {
                        Text("+ Create new playlist")
                    }
                } else {
                    LiquidGlassTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = "Playlist title",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassChip(
                            selected = visibility == "PUBLIC",
                            onClick = { visibility = "PUBLIC" },
                            label = "Public"
                        )
                        LiquidGlassChip(
                            selected = visibility == "PRIVATE",
                            onClick = { visibility = "PRIVATE" },
                            label = "Private"
                        )
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
            if (creating) {
                Spacer(Modifier.width(8.dp))
                LiquidGlassButton(
                    onClick = {
                        if (newTitle.trim().length >= 2) {
                            onCreateNew(newTitle.trim(), visibility)
                            creating = false
                        }
                    }
                ) {
                    Text("Create")
                }
            }
        }
    )
}
