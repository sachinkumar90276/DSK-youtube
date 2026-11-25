package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactVideoRow
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.util.formatCount

@Composable
fun SearchScreen(
    videos: List<VideoWithChannel>,
    shorts: List<ShortWithChannel>,
    channels: List<ChannelEntity>,
    categories: List<CategoryEntity>,
    searchHistory: List<SearchHistoryEntity>,
    onBack: () -> Unit,
    onRecordSearch: (String) -> Unit,
    onDeleteSearchQuery: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onVideoClick: (String) -> Unit,
    onShortClick: (String) -> Unit,
    onChannelClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("All") } // All, Videos, Shorts, Channels
    var categoryFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Relevance") } // Relevance, Newest, Views

    val suggestions = remember(query, videos, channels, categories) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else {
            (videos.map { it.video.title } +
                channels.map { it.name } +
                categories.map { it.name })
                .distinct()
                .filter { it.lowercase().contains(q) }
                .take(6)
        }
    }

    val activeQ = submittedQuery.trim().lowercase().ifEmpty { query.trim().lowercase() }

    val matchingVideos = remember(videos, activeQ, categoryFilter, sortBy) {
        val base = videos.filter { item ->
            val matchesQ = activeQ.isEmpty() ||
                item.video.title.lowercase().contains(activeQ) ||
                item.video.description.lowercase().contains(activeQ) ||
                item.video.category.lowercase().contains(activeQ) ||
                item.video.tags.lowercase().contains(activeQ) ||
                item.channel.name.lowercase().contains(activeQ)
            val matchesCat = categoryFilter == "All" || item.video.category.equals(categoryFilter, true)
            matchesQ && matchesCat
        }
        when (sortBy) {
            "Newest" -> base.sortedByDescending { it.video.createdAt }
            "Views" -> base.sortedByDescending { it.video.viewCount }
            else -> base
        }
    }

    val matchingShorts = remember(shorts, activeQ, sortBy) {
        val base = shorts.filter { item ->
            activeQ.isEmpty() ||
                item.shortItem.title.lowercase().contains(activeQ) ||
                item.shortItem.caption.lowercase().contains(activeQ) ||
                item.channel.name.lowercase().contains(activeQ)
        }
        when (sortBy) {
            "Newest" -> base.sortedByDescending { it.shortItem.createdAt }
            "Views" -> base.sortedByDescending { it.shortItem.viewCount }
            else -> base
        }
    }

    val matchingChannels = remember(channels, activeQ) {
        channels.filter { ch ->
            !ch.isSuspended && (activeQ.isEmpty() ||
                ch.name.lowercase().contains(activeQ) ||
                ch.handle.lowercase().contains(activeQ) ||
                ch.description.lowercase().contains(activeQ))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search videos, shorts, channels...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    submittedQuery = query
                    onRecordSearch(query)
                }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            submittedQuery = ""
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input")
            )
            IconButton(
                onClick = {
                    submittedQuery = query
                    onRecordSearch(query)
                },
                modifier = Modifier.testTag("search_submit_button")
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = DskRed)
            }
        }

        // Type Filters + Sort Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "Videos", "Shorts", "Channels")) { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { typeFilter = type },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DskRed,
                        selectedLabelColor = DskWhite
                    )
                )
            }
            items(listOf("Relevance", "Newest", "Views")) { sort ->
                FilterChip(
                    selected = sortBy == sort,
                    onClick = { sortBy = sort },
                    leadingIcon = { Icon(Icons.Outlined.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(sort) }
                )
            }
        }

        // Category filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = categoryFilter == "All",
                    onClick = { categoryFilter = "All" },
                    label = { Text("All Categories") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = categoryFilter == cat.name,
                    onClick = { categoryFilter = cat.name },
                    label = { Text(cat.name) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // History & Suggestions when query hasn't been submitted yet or is empty
            if (query.isBlank() && searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = onClearSearchHistory) {
                            Text("Clear history", color = DskRed)
                        }
                    }
                }
                items(searchHistory, key = { it.id }) { hist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = hist.query
                                submittedQuery = hist.query
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(hist.query, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { onDeleteSearchQuery(hist.id) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove search item")
                        }
                    }
                }
            }

            if (query.isNotBlank() && suggestions.isNotEmpty() && submittedQuery != query) {
                items(suggestions) { sug ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = sug
                                submittedQuery = sug
                                onRecordSearch(sug)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(sug, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (typeFilter == "All" || typeFilter == "Channels") {
                if (matchingChannels.isNotEmpty()) {
                    item {
                        Text("Channels (${matchingChannels.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(matchingChannels, key = { it.id }) { ch ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelClick(ch.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelAvatar(name = ch.name, avatarUri = ch.avatarUri, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${ch.handle} • ${formatCount(ch.subscriberCount)} subscribers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (typeFilter == "All" || typeFilter == "Shorts") {
                if (matchingShorts.isNotEmpty()) {
                    item {
                        Text("Shorts (${matchingShorts.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(matchingShorts, key = { it.shortItem.id }) { s ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShortClick(s.shortItem.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = DskRed)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.shortItem.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${s.channel.name} • ${formatCount(s.shortItem.viewCount)} views",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (typeFilter == "All" || typeFilter == "Videos") {
                if (matchingVideos.isNotEmpty()) {
                    item {
                        Text("Videos (${matchingVideos.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(matchingVideos, key = { it.video.id }) { item ->
                        CompactVideoRow(item = item, onClick = { onVideoClick(item.video.id) })
                    }
                }
            }

            if (matchingVideos.isEmpty() && matchingShorts.isEmpty() && matchingChannels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results found for \"$activeQ\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
