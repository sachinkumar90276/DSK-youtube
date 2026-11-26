package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.NotificationEntity
import com.example.ui.components.GlassIntensity
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassTopBar
import com.example.ui.theme.DskRed
import com.example.ui.util.formatRelativeTime

@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LiquidGlassTopBar(
            title = "Notifications",
            onBack = onBack
        ) {
            if (notifications.any { !it.isRead }) {
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read", color = DskRed)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp), intensity = GlassIntensity.REGULAR) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = DskRed)
                        Text("No notifications yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Get alerts for subscribed uploads, comment replies, likes, and completed uploads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { n ->
                    val icon = when (n.type) {
                        "UPLOAD" -> Icons.Outlined.CloudDone
                        "REPLY" -> Icons.Outlined.Reply
                        "LIKE" -> Icons.Outlined.ThumbUp
                        else -> Icons.Outlined.Subscriptions
                    }
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        intensity = if (n.isRead) GlassIntensity.SUBTLE else GlassIntensity.REGULAR,
                        selected = !n.isRead,
                        onClick = {
                            if (!n.targetId.isNullOrBlank()) onOpenVideo(n.targetId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(icon, contentDescription = null, tint = DskRed)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(n.title, style = MaterialTheme.typography.titleSmall)
                                Text(n.message, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatRelativeTime(n.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
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
