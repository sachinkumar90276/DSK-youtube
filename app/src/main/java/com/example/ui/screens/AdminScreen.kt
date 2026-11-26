package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ReportEntity
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskSuccess
import com.example.ui.theme.DskWarning
import com.example.ui.util.formatCount
import com.example.viewmodel.AdminDashboardState

@Composable
fun AdminScreen(
    isAdmin: Boolean,
    dashboard: AdminDashboardState,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddDirectUrlVideo: (title: String, url: String, category: String, isShort: Boolean) -> Unit,
    onEditVideo: (VideoEntity, String, String, String) -> Unit,
    onTogglePublished: (String, Boolean) -> Unit,
    onToggleFeatured: (String, Boolean) -> Unit,
    onToggleTrending: (String, Boolean) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onDeleteShort: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSetUserBlocked: (String, Boolean) -> Unit,
    onSetChannelSuspended: (String, Boolean) -> Unit,
    onResolveReport: (ReportEntity, String) -> Unit
) {
    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(shape = RoundedCornerShape(26.dp), intensity = GlassIntensity.REGULAR) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(54.dp), tint = DskRed)
                    Text("Administrator Access Required", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "This Admin Panel is strictly protected. Standard user accounts cannot access moderation tools.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LiquidGlassButton(onClick = onBack) { Text("Return") }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Content", "Categories", "Users", "Reports")
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<VideoEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LiquidGlassTopBar(
            title = "DSK Admin Panel",
            onBack = onBack
        ) {
            LiquidGlassButton(
                onClick = { showAddUrlDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add URL")
            }
        }

        LiquidGlassSegmentedTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            scrollable = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile("Users", dashboard.totalUsers.toString(), Modifier.weight(1f))
                                StatTile("Channels", dashboard.totalChannels.toString(), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile("Videos", dashboard.totalVideos.toString(), Modifier.weight(1f))
                                StatTile("Shorts", dashboard.totalShorts.toString(), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile("Comments", dashboard.totalComments.toString(), Modifier.weight(1f))
                                StatTile("Total Views", formatCount(dashboard.totalViews), Modifier.weight(1f))
                            }
                            StatTile(
                                "Pending Reports",
                                dashboard.reports.count { it.status == "PENDING" }.toString(),
                                Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                1 -> {
                    item { Text("Videos (${dashboard.videos.size})", style = MaterialTheme.typography.titleMedium) }
                    items(dashboard.videos, key = { it.video.id }) { item ->
                        val v = item.video
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            intensity = GlassIntensity.REGULAR,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(v.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.channel.name} • ${v.category} • ${v.sectionTag} • ${formatCount(v.viewCount)} views",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LiquidGlassChip(
                                        selected = v.isPublished,
                                        onClick = { onTogglePublished(v.id, !v.isPublished) },
                                        label = if (v.isPublished) "Published" else "Hidden"
                                    )
                                    LiquidGlassChip(
                                        selected = v.isFeatured,
                                        onClick = { onToggleFeatured(v.id, !v.isFeatured) },
                                        label = "Featured"
                                    )
                                    LiquidGlassChip(
                                        selected = v.isTrending,
                                        onClick = { onToggleTrending(v.id, !v.isTrending) },
                                        label = "Trending"
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LiquidGlassButton(
                                        onClick = { editingVideo = v },
                                        isPrimary = false,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) { Text("Edit") }
                                    LiquidGlassButton(
                                        onClick = { onDeleteVideo(v.id) },
                                        isPrimary = false,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Delete", color = DskRed)
                                    }
                                }
                            }
                        }
                    }
                    item { Text("Shorts (${dashboard.shorts.size})", style = MaterialTheme.typography.titleMedium) }
                    items(dashboard.shorts, key = { it.shortItem.id }) { s ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.shortItem.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${s.channel.name} • ${formatCount(s.shortItem.viewCount)} views", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onDeleteShort(s.shortItem.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete short", tint = DskRed)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        var newCat by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassTextField(
                                value = newCat,
                                onValueChange = { newCat = it },
                                label = "New Category",
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassButton(
                                onClick = {
                                    if (newCat.isNotBlank()) {
                                        onAddCategory(newCat)
                                        newCat = ""
                                    }
                                }
                            ) { Text("Add") }
                        }
                    }
                    items(categories, key = { it.id }) { cat ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete category")
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item { Text("Users (${dashboard.users.size})", style = MaterialTheme.typography.titleMedium) }
                    items(dashboard.users, key = { it.user.id }) { uwp ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(uwp.profile?.displayName ?: uwp.user.email, style = MaterialTheme.typography.titleSmall)
                                    Text(uwp.user.email, style = MaterialTheme.typography.bodySmall)
                                }
                                LiquidGlassButton(
                                    onClick = { onSetUserBlocked(uwp.user.id, !uwp.user.isBlocked) },
                                    isPrimary = false,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(if (uwp.user.isBlocked) "Unblock" else "Block")
                                }
                            }
                        }
                    }
                    item { Text("Channels (${dashboard.channels.size})", style = MaterialTheme.typography.titleMedium) }
                    items(dashboard.channels, key = { it.id }) { ch ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, style = MaterialTheme.typography.titleSmall)
                                    Text("${ch.handle} • ${formatCount(ch.subscriberCount)} subs", style = MaterialTheme.typography.bodySmall)
                                }
                                LiquidGlassButton(
                                    onClick = { onSetChannelSuspended(ch.id, !ch.isSuspended) },
                                    isPrimary = false,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(if (ch.isSuspended) "Restore" else "Suspend")
                                }
                            }
                        }
                    }
                }

                4 -> {
                    if (dashboard.reports.isEmpty()) {
                        item {
                            LiquidGlassCard(shape = RoundedCornerShape(18.dp), intensity = GlassIntensity.SUBTLE) {
                                Text(
                                    "No moderation reports submitted.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(dashboard.reports, key = { it.id }) { rep ->
                            LiquidGlassCard(
                                shape = RoundedCornerShape(18.dp),
                                intensity = GlassIntensity.REGULAR,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_report_${rep.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${rep.targetType} • ${rep.reason}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            rep.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (rep.status) {
                                                "PENDING" -> DskWarning
                                                "APPROVED" -> DskSuccess
                                                else -> DskRed
                                            }
                                        )
                                    }
                                    Text(rep.targetLabel, style = MaterialTheme.typography.bodySmall)
                                    if (rep.details.isNotBlank()) {
                                        Text("Details: ${rep.details}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (rep.status == "PENDING") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            LiquidGlassButton(
                                                onClick = { onResolveReport(rep, "REMOVE") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) { Text("Remove") }
                                            LiquidGlassButton(
                                                onClick = { onResolveReport(rep, "APPROVE") },
                                                isPrimary = false,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) { Text("Approve") }
                                            LiquidGlassButton(
                                                onClick = { onResolveReport(rep, "REJECT") },
                                                isPrimary = false,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) { Text("Reject") }
                                            LiquidGlassButton(
                                                onClick = { onResolveReport(rep, "SUSPEND_ACCOUNT") },
                                                isPrimary = false,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text("Suspend", color = DskRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUrlDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Technology") }
        var isShort by remember { mutableStateOf(false) }

        LiquidGlassDialog(
            onDismissRequest = { showAddUrlDialog = false },
            title = "Add Video / Short by Direct URL",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassChip(selected = !isShort, onClick = { isShort = false }, label = "Video")
                        LiquidGlassChip(selected = isShort, onClick = { isShort = true }, label = "Short")
                    }
                    LiquidGlassTextField(value = title, onValueChange = { title = it }, label = "Title", modifier = Modifier.fillMaxWidth())
                    LiquidGlassTextField(value = url, onValueChange = { url = it }, label = "Direct MP4/WebM URL", modifier = Modifier.fillMaxWidth())
                    LiquidGlassTextField(value = category, onValueChange = { category = it }, label = "Category", modifier = Modifier.fillMaxWidth())
                }
            },
            actions = {
                TextButton(onClick = { showAddUrlDialog = false }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                LiquidGlassButton(onClick = {
                    onAddDirectUrlVideo(title, url, category, isShort)
                    showAddUrlDialog = false
                }) { Text("Publish") }
            }
        )
    }

    editingVideo?.let { vid ->
        var title by remember { mutableStateOf(vid.title) }
        var category by remember { mutableStateOf(vid.category) }
        var section by remember { mutableStateOf(vid.sectionTag) }

        LiquidGlassDialog(
            onDismissRequest = { editingVideo = null },
            title = "Edit Video Metadata",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassTextField(value = title, onValueChange = { title = it }, label = "Title", modifier = Modifier.fillMaxWidth())
                    LiquidGlassTextField(value = category, onValueChange = { category = it }, label = "Category", modifier = Modifier.fillMaxWidth())
                    LiquidGlassTextField(value = section, onValueChange = { section = it }, label = "Homepage Section", modifier = Modifier.fillMaxWidth())
                }
            },
            actions = {
                TextButton(onClick = { editingVideo = null }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                LiquidGlassButton(onClick = {
                    onEditVideo(vid, title, category, section)
                    editingVideo = null
                }) { Text("Save") }
            }
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    LiquidGlassCard(
        shape = RoundedCornerShape(20.dp),
        intensity = GlassIntensity.REGULAR,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}
