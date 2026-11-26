package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWarning
import com.example.ui.theme.DskWhite
import com.example.ui.util.formatCount
import com.example.viewmodel.LiveSearchUiState

@Composable
fun SearchScreen(
    videos: List<VideoWithChannel>,
    shorts: List<ShortWithChannel>,
    channels: List<ChannelEntity>,
    categories: List<CategoryEntity>,
    searchHistory: List<SearchHistoryEntity>,
    liveSearchState: LiveSearchUiState,
    onBack: () -> Unit,
    onRecordSearch: (String) -> Unit,
    onSearchYouTube: (query: String, typeFilter: String, categoryFilter: String, sortBy: String) -> Unit,
    onClearLiveSearch: () -> Unit,
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

    fun triggerSearch(q: String, type: String = typeFilter, cat: String = categoryFilter, sort: String = sortBy) {
        val trimmed = q.trim()
        submittedQuery = trimmed
        if (trimmed.isNotEmpty()) {
            onRecordSearch(trimmed)
            onSearchYouTube(trimmed, type, cat, sort)
        } else {
            onClearLiveSearch()
        }
    }

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
    val activeLiveResults = (liveSearchState as? LiveSearchUiState.Results)
        ?.takeIf { it.query.equals(submittedQuery.trim(), ignoreCase = true) }
        ?.bundle

    val cachedVideos = remember(videos, activeQ, categoryFilter, sortBy) {
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

    val cachedShorts = remember(shorts, activeQ, sortBy) {
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

    val cachedChannels = remember(channels, activeQ) {
        channels.filter { ch ->
            !ch.isSuspended && (activeQ.isEmpty() ||
                ch.name.lowercase().contains(activeQ) ||
                ch.handle.lowercase().contains(activeQ) ||
                ch.description.lowercase().contains(activeQ))
        }
    }

    val matchingVideos = remember(activeLiveResults, cachedVideos) {
        if (activeLiveResults != null) {
            (activeLiveResults.videos + cachedVideos.filter { it.video.sourceType != "YOUTUBE" })
                .distinctBy { it.video.id }
        } else cachedVideos
    }

    val matchingShorts = remember(activeLiveResults, cachedShorts) {
        if (activeLiveResults != null) {
            (activeLiveResults.shorts + cachedShorts.filter { it.shortItem.sourceType != "YOUTUBE" })
                .distinctBy { it.shortItem.id }
        } else cachedShorts
    }

    val matchingChannels = remember(activeLiveResults, cachedChannels) {
        if (activeLiveResults != null) {
            (activeLiveResults.channels + cachedChannels.filter { it.sourceType != "YOUTUBE" })
                .distinctBy { it.id }
        } else cachedChannels
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Floating Liquid Glass Search Bar Header
        LiquidGlassCard(
            shape = RoundedCornerShape(26.dp),
            intensity = GlassIntensity.STRONG,
            elevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassIconButton(
                    onClick = onBack,
                    size = 40.dp,
                    intensity = GlassIntensity.SUBTLE
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
                LiquidGlassTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search YouTube videos, shorts, channels...",
                    singleLine = true,
                    shape = RoundedCornerShape(99.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { triggerSearch(query) }),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                submittedQuery = ""
                                onClearLiveSearch()
                            }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input")
                )
                LiquidGlassIconButton(
                    onClick = { triggerSearch(query) },
                    size = 42.dp,
                    selected = true,
                    intensity = GlassIntensity.STRONG,
                    modifier = Modifier.testTag("search_submit_button")
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = DskWhite, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (liveSearchState is LiveSearchUiState.Searching) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = DskRed
            )
        }

        // Type Filters + Sort Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "Videos", "Shorts", "Channels")) { type ->
                LiquidGlassChip(
                    selected = typeFilter == type,
                    onClick = {
                        typeFilter = type
                        if (submittedQuery.isNotBlank()) triggerSearch(submittedQuery, type = type)
                    },
                    label = type
                )
            }
            items(listOf("Relevance", "Newest", "Views")) { sort ->
                LiquidGlassChip(
                    selected = sortBy == sort,
                    onClick = {
                        sortBy = sort
                        if (submittedQuery.isNotBlank()) triggerSearch(submittedQuery, sort = sort)
                    },
                    leadingIcon = Icons.Outlined.Sort,
                    label = sort
                )
            }
        }

        // Category filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LiquidGlassChip(
                    selected = categoryFilter == "All",
                    onClick = {
                        categoryFilter = "All"
                        if (submittedQuery.isNotBlank()) triggerSearch(submittedQuery, cat = "All")
                    },
                    label = "All Categories"
                )
            }
            items(categories) { cat ->
                LiquidGlassChip(
                    selected = categoryFilter == cat.name,
                    onClick = {
                        categoryFilter = cat.name
                        if (submittedQuery.isNotBlank()) triggerSearch(submittedQuery, cat = cat.name)
                    },
                    label = cat.name
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (liveSearchState is LiveSearchUiState.Error) {
                item {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(16.dp),
                        accentTint = DskWarning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = liveSearchState.message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { triggerSearch(submittedQuery.ifBlank { query }) }) {
                                Text("Retry", color = DskRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // History & Suggestions
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
                    LiquidGlassCard(
                        shape = RoundedCornerShape(16.dp),
                        intensity = GlassIntensity.SUBTLE,
                        elevation = 2.dp,
                        onClick = {
                            query = hist.query
                            triggerSearch(hist.query)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
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
            }

            if (query.isNotBlank() && suggestions.isNotEmpty() && submittedQuery != query) {
                items(suggestions) { sug ->
                    LiquidGlassCard(
                        shape = RoundedCornerShape(16.dp),
                        intensity = GlassIntensity.SUBTLE,
                        elevation = 2.dp,
                        onClick = {
                            query = sug
                            triggerSearch(sug)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = DskRed)
                            Spacer(Modifier.width(12.dp))
                            Text(sug, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (typeFilter == "All" || typeFilter == "Channels") {
                if (matchingChannels.isNotEmpty()) {
                    item {
                        Text("Channels (${matchingChannels.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(matchingChannels, key = { it.id }) { ch ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            intensity = GlassIntensity.REGULAR,
                            onClick = { onChannelClick(ch.id) },
                            modifier = Modifier.fillMaxWidth()
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
                        LiquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            intensity = GlassIntensity.SUBTLE,
                            onClick = { onShortClick(s.shortItem.id) },
                            modifier = Modifier.fillMaxWidth()
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
                    LiquidGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        intensity = GlassIntensity.SUBTLE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
}
