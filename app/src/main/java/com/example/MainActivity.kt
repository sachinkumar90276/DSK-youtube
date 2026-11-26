package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.theme.DskYouTubeTheme
import com.example.ui.theme.GlassEdgeShadow
import com.example.viewmodel.DskViewModel
import com.example.viewmodel.LiveSearchUiState

sealed interface AppRoute {
    val routePath: String

    data object Splash : AppRoute {
        override val routePath = "/splash"
    }
    data object Home : AppRoute {
        override val routePath = "/"
    }
    data class Shorts(val initialShortId: String? = null) : AppRoute {
        override val routePath = if (initialShortId == null) "/shorts" else "/shorts/$initialShortId"
    }
    data object Create : AppRoute {
        override val routePath = "/create"
    }
    data object Subscriptions : AppRoute {
        override val routePath = "/subscriptions"
    }
    data object You : AppRoute {
        override val routePath = "/you"
    }
    data class Watch(val videoId: String) : AppRoute {
        override val routePath = "/watch/$videoId"
    }
    data object Search : AppRoute {
        override val routePath = "/search"
    }
    data class Channel(val channelId: String) : AppRoute {
        override val routePath = "/channel/$channelId"
    }
    data class PlaylistDetail(val playlistId: String) : AppRoute {
        override val routePath = "/playlist/$playlistId"
    }
    data class Auth(val mode: AuthMode = AuthMode.SIGN_IN) : AppRoute {
        override val routePath = "/auth"
    }
    data object Notifications : AppRoute {
        override val routePath = "/notifications"
    }
    data object Settings : AppRoute {
        override val routePath = "/settings"
    }
    data object Admin : AppRoute {
        override val routePath = "/admin"
    }
}

private data class NavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: AppRoute,
    val tag: String
)

private data class ReportTarget(
    val targetType: String,
    val targetId: String,
    val targetLabel: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: DskViewModel by viewModels {
        DskViewModel.Factory((application as DskYouTubeApp).repository)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            DskYouTubeRoot(
                viewModel = viewModel,
                widthSizeClass = widthSizeClass
            )
        }
    }
}

@Composable
private fun DskYouTubeRoot(
    viewModel: DskViewModel,
    widthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val myChannel by viewModel.myChannel.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val shorts by viewModel.shorts.collectAsStateWithLifecycle()
    val likedKeys by viewModel.likedKeys.collectAsStateWithLifecycle()
    val dislikedKeys by viewModel.dislikedKeys.collectAsStateWithLifecycle()
    val subscribedIds by viewModel.subscribedChannelIds.collectAsStateWithLifecycle()
    val watchLaterIds by viewModel.watchLaterIds.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val watchLaterVideos by viewModel.watchLaterVideos.collectAsStateWithLifecycle()
    val likedVideos by viewModel.likedVideos.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistVideosMap by viewModel.playlistVideosMap.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val commentsMap by viewModel.commentsMap.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val youtubeSyncState by viewModel.youtubeSyncState.collectAsStateWithLifecycle()
    val liveSearchState by viewModel.liveSearchState.collectAsStateWithLifecycle()
    val loadedWatchVideo by viewModel.loadedWatchVideo.collectAsStateWithLifecycle()
    val watchVideoRestriction by viewModel.watchVideoRestriction.collectAsStateWithLifecycle()
    val adminDashboard by viewModel.adminDashboard.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        val msg = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    var routeStack by remember { mutableStateOf<List<AppRoute>>(listOf(AppRoute.Splash)) }
    val currentRoute = routeStack.lastOrNull() ?: AppRoute.Home
    var isWatchFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        if (currentRoute !is AppRoute.Watch) {
            isWatchFullscreen = false
        }
    }

    fun navigateTo(route: AppRoute) {
        routeStack = routeStack + route
    }

    fun switchRootTab(route: AppRoute) {
        routeStack = listOf(route)
    }

    fun popRoute() {
        if (routeStack.size > 1) {
            routeStack = routeStack.dropLast(1)
        } else if (currentRoute != AppRoute.Home && currentRoute != AppRoute.Splash) {
            routeStack = listOf(AppRoute.Home)
        }
    }

    BackHandler(enabled = currentRoute != AppRoute.Home && currentRoute != AppRoute.Splash) {
        popRoute()
    }

    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var playlistTargetVideoId by remember { mutableStateOf<String?>(null) }

    val gridColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 3
        WindowWidthSizeClass.Medium -> 2
        else -> 1
    }
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact

    val navTabs = remember {
        listOf(
            NavTab("Home", Icons.Filled.Home, Icons.Outlined.Home, AppRoute.Home, "nav_home"),
            NavTab("Shorts", Icons.Filled.Bolt, Icons.Outlined.Bolt, AppRoute.Shorts(), "nav_shorts"),
            NavTab("Create", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, AppRoute.Create, "nav_create"),
            NavTab("Subscriptions", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions, AppRoute.Subscriptions, "nav_subscriptions"),
            NavTab("You", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, AppRoute.You, "nav_you")
        )
    }

    val showMainNav = currentRoute == AppRoute.Home ||
        currentRoute is AppRoute.Shorts ||
        currentRoute == AppRoute.Create ||
        currentRoute == AppRoute.Subscriptions ||
        currentRoute == AppRoute.You ||
        (currentRoute is AppRoute.Watch && !isWatchFullscreen)

    fun isTabSelected(tab: NavTab): Boolean = when (tab.route) {
        is AppRoute.Home -> currentRoute == AppRoute.Home
        is AppRoute.Shorts -> currentRoute is AppRoute.Shorts
        is AppRoute.Create -> currentRoute == AppRoute.Create
        is AppRoute.Subscriptions -> currentRoute == AppRoute.Subscriptions
        is AppRoute.You -> currentRoute == AppRoute.You
        else -> false
    }

    DskYouTubeTheme(darkTheme = settings.darkMode) {
        LiquidGlassBackground(
            darkTheme = settings.darkMode,
            animate = currentRoute !is AppRoute.Watch && currentRoute !is AppRoute.Shorts
        ) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        LiquidGlassPill(
                            intensity = GlassIntensity.STRONG,
                            elevation = 14.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showMainNav && !useRail) {
                        LiquidGlassBottomDock(
                            tabs = navTabs,
                            isSelected = ::isTabSelected,
                            onSelect = ::switchRootTab
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showMainNav && useRail) {
                        LiquidGlassNavigationRail(
                            tabs = navTabs,
                            isSelected = ::isTabSelected,
                            onSelect = ::switchRootTab
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (val route = currentRoute) {
                            AppRoute.Splash -> {
                                SplashScreen(
                                    onFinished = { routeStack = listOf(AppRoute.Home) }
                                )
                            }

                            AppRoute.Home -> {
                                HomeScreen(
                                    videos = videos,
                                    shorts = shorts,
                                    categories = categories,
                                    watchLaterIds = watchLaterIds,
                                    unreadNotifications = notifications.count { !it.isRead },
                                    currentProfile = currentProfile,
                                    gridColumns = gridColumns,
                                    syncState = youtubeSyncState,
                                    onRefresh = { viewModel.refreshYouTubeHome(force = true) },
                                    onCategorySelected = { cat -> viewModel.refreshCategory(cat) },
                                    onSearchClick = { navigateTo(AppRoute.Search) },
                                    onNotificationsClick = { navigateTo(AppRoute.Notifications) },
                                    onProfileClick = {
                                        if (currentUser == null) navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                                        else switchRootTab(AppRoute.You)
                                    },
                                    onVideoClick = { vid -> navigateTo(AppRoute.Watch(vid)) },
                                    onShortClick = { sid -> switchRootTab(AppRoute.Shorts(sid)) },
                                    onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) },
                                    onToggleWatchLater = { vid ->
                                        viewModel.toggleWatchLater(vid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onAddToPlaylist = { vid ->
                                        if (currentUser == null) navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                                        else playlistTargetVideoId = vid
                                    },
                                    onShareVideo = { item ->
                                        shareText(
                                            context,
                                            item.video.title,
                                            "Watch \"${item.video.title}\" free on DSK YouTube (/watch/${item.video.id})"
                                        )
                                    },
                                    onReportVideo = { item ->
                                        reportTarget = ReportTarget("VIDEO", item.video.id, item.video.title)
                                    }
                                )
                            }

                            is AppRoute.Shorts -> {
                                ShortsScreen(
                                    shorts = shorts,
                                    initialShortId = route.initialShortId,
                                    likedKeys = likedKeys,
                                    subscribedChannelIds = subscribedIds,
                                    commentsMap = commentsMap,
                                    currentUserId = currentUser?.id,
                                    onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) },
                                    onToggleLike = { sid ->
                                        viewModel.toggleLike("SHORT", sid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onToggleSubscribe = { cid ->
                                        viewModel.toggleSubscription(cid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onShareShort = { item ->
                                        shareText(
                                            context,
                                            item.shortItem.title,
                                            "Watch Short \"${item.shortItem.title}\" free on DSK YouTube (/shorts/${item.shortItem.id})"
                                        )
                                    },
                                    onReportShort = { item ->
                                        reportTarget = ReportTarget("SHORT", item.shortItem.id, item.shortItem.title)
                                    },
                                    onAddComment = { sid, body ->
                                        viewModel.addComment("SHORT", sid, body) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onAddReply = { cid, body ->
                                        viewModel.addReply(cid, body) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onLikeComment = { cid ->
                                        viewModel.toggleLike("COMMENT", cid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onDeleteComment = { cid -> viewModel.deleteComment(cid) },
                                    onReportComment = { cid, label ->
                                        reportTarget = ReportTarget("COMMENT", cid, label)
                                    },
                                    onRecordShortView = { sid -> viewModel.recordShortView(sid) }
                                )
                            }

                            AppRoute.Create -> {
                                UploadScreen(
                                    isSignedIn = currentUser != null,
                                    categories = categories,
                                    uploadState = uploadState,
                                    onRequireSignIn = { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) },
                                    onUpload = { isShort, vUri, url, tUri, title, desc, cat, tags, vis, cmt, music ->
                                        viewModel.uploadMedia(isShort, vUri, url, tUri, title, desc, cat, tags, vis, cmt, music)
                                    },
                                    onResetUploadState = { viewModel.resetUploadState() },
                                    onOpenUploadedVideo = { id, isShort ->
                                        if (isShort) switchRootTab(AppRoute.Shorts(id))
                                        else navigateTo(AppRoute.Watch(id))
                                    }
                                )
                            }

                            AppRoute.Subscriptions -> {
                                SubscriptionsScreen(
                                    isSignedIn = currentUser != null,
                                    channels = channels,
                                    subscribedIds = subscribedIds,
                                    videos = videos,
                                    watchLaterIds = watchLaterIds,
                                    onRequireSignIn = { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) },
                                    onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) },
                                    onToggleSubscribe = { cid ->
                                        viewModel.toggleSubscription(cid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onVideoClick = { vid -> navigateTo(AppRoute.Watch(vid)) },
                                    onToggleWatchLater = { vid ->
                                        viewModel.toggleWatchLater(vid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onAddToPlaylist = { vid ->
                                        if (currentUser == null) navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                                        else playlistTargetVideoId = vid
                                    },
                                    onShareVideo = { item ->
                                        shareText(context, item.video.title, "Watch free on DSK YouTube (/watch/${item.video.id})")
                                    },
                                    onReportVideo = { item ->
                                        reportTarget = ReportTarget("VIDEO", item.video.id, item.video.title)
                                    }
                                )
                            }

                            AppRoute.You -> {
                                val myVids = remember(videos, myChannel) {
                                    if (myChannel == null) emptyList() else videos.filter { it.channel.id == myChannel?.id }
                                }
                                val myShs = remember(shorts, myChannel) {
                                    if (myChannel == null) emptyList() else shorts.filter { it.channel.id == myChannel?.id }
                                }
                                val subChans = remember(channels, subscribedIds) {
                                    channels.filter { it.id in subscribedIds }
                                }
                                LibraryScreen(
                                    currentUser = currentUser,
                                    currentProfile = currentProfile,
                                    myChannel = myChannel,
                                    isAdmin = isAdmin,
                                    watchHistory = watchHistory,
                                    watchLater = watchLaterVideos,
                                    likedVideos = likedVideos,
                                    myVideos = myVids,
                                    myShorts = myShs,
                                    subscribedChannels = subChans,
                                    playlists = playlists,
                                    onOpenAuth = { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) },
                                    onOpenEditProfile = { navigateTo(AppRoute.Auth(AuthMode.EDIT_PROFILE)) },
                                    onOpenSettings = { navigateTo(AppRoute.Settings) },
                                    onOpenAdminPanel = { navigateTo(AppRoute.Admin) },
                                    onVideoClick = { vid -> navigateTo(AppRoute.Watch(vid)) },
                                    onShortClick = { sid -> switchRootTab(AppRoute.Shorts(sid)) },
                                    onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) },
                                    onPlaylistClick = { pid -> navigateTo(AppRoute.PlaylistDetail(pid)) },
                                    onCreatePlaylist = { title, desc, vis -> viewModel.createPlaylist(title, desc, vis) },
                                    onDeletePlaylist = { pl -> viewModel.deletePlaylist(pl) },
                                    onClearWatchHistory = { viewModel.clearWatchHistory() }
                                )
                            }

                            is AppRoute.Watch -> {
                                LaunchedEffect(route.videoId) {
                                    viewModel.ensureVideoLoaded(route.videoId)
                                }
                                val searchVideos = (liveSearchState as? LiveSearchUiState.Results)?.bundle?.videos.orEmpty()
                                val target = videos.firstOrNull { it.video.id == route.videoId }
                                    ?: searchVideos.firstOrNull { it.video.id == route.videoId }
                                    ?: loadedWatchVideo?.takeIf { it.video.id == route.videoId }

                                if (target == null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .statusBarsPadding()
                                    ) {
                                        LiquidGlassTopBar(
                                            title = "Loading Stream",
                                            subtitle = route.routePath,
                                            onBack = { popRoute() }
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LiquidGlassCard(
                                                shape = RoundedCornerShape(24.dp),
                                                intensity = GlassIntensity.STRONG
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(28.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    CircularProgressIndicator(color = DskRed)
                                                    Text(
                                                        "Preparing In-App Player...",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val related = remember(videos, searchVideos, target.video.id) {
                                        (videos + searchVideos)
                                            .distinctBy { it.video.id }
                                            .filter { it.video.id != target.video.id }
                                    }
                                    VideoPlayerScreen(
                                        videoItem = target,
                                        relatedVideos = related,
                                        comments = commentsMap["VIDEO:${target.video.id}"].orEmpty(),
                                        isLiked = "VIDEO:${target.video.id}" in likedKeys,
                                        isDisliked = "VIDEO:${target.video.id}" in dislikedKeys,
                                        isSubscribed = target.channel.id in subscribedIds,
                                        isWatchLater = target.video.id in watchLaterIds,
                                        autoplayNext = settings.autoplayNext,
                                        defaultQuality = settings.defaultQuality,
                                        currentUserId = currentUser?.id,
                                        onBack = { popRoute() },
                                        onSelectVideo = { vid ->
                                            navigateTo(AppRoute.Watch(vid))
                                        },
                                        onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) },
                                        onToggleLike = {
                                            viewModel.toggleLike("VIDEO", target.video.id) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onToggleDislike = {
                                            viewModel.toggleDislike("VIDEO", target.video.id) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onToggleSubscribe = {
                                            viewModel.toggleSubscription(target.channel.id) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onToggleWatchLater = {
                                            viewModel.toggleWatchLater(target.video.id) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onAddToPlaylist = {
                                            if (currentUser == null) navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                                            else playlistTargetVideoId = target.video.id
                                        },
                                        onShare = {
                                            shareText(
                                                context,
                                                target.video.title,
                                                "Watch \"${target.video.title}\" free on DSK YouTube (${route.routePath})"
                                            )
                                        },
                                        onReportVideo = {
                                            reportTarget = ReportTarget("VIDEO", target.video.id, target.video.title)
                                        },
                                        onToggleAutoplay = { enabled -> viewModel.setAutoplayNext(enabled) },
                                        onAddComment = { body ->
                                            viewModel.addComment("VIDEO", target.video.id, body) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onAddReply = { cid, body ->
                                            viewModel.addReply(cid, body) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onLikeComment = { cid ->
                                            viewModel.toggleLike("COMMENT", cid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onDeleteComment = { cid -> viewModel.deleteComment(cid) },
                                        onReportComment = { cid, label ->
                                            reportTarget = ReportTarget("COMMENT", cid, label)
                                        },
                                        onRecordView = { vid -> viewModel.recordVideoView(vid) },
                                        restrictionMessage = watchVideoRestriction
                                            ?.takeIf { it.first == target.video.id && !it.second.isPlayable }
                                            ?.second?.reasonMessage,
                                        onFullscreenChanged = { fs -> isWatchFullscreen = fs }
                                    )
                                }
                            }

                            AppRoute.Search -> {
                                SearchScreen(
                                    videos = videos,
                                    shorts = shorts,
                                    channels = channels,
                                    categories = categories,
                                    searchHistory = searchHistory,
                                    liveSearchState = liveSearchState,
                                    onBack = { popRoute() },
                                    onRecordSearch = { q -> viewModel.recordSearch(q) },
                                    onSearchYouTube = { q, t, c, s -> viewModel.searchYouTubeLive(q, t, c, s) },
                                    onClearLiveSearch = { viewModel.clearLiveSearch() },
                                    onDeleteSearchQuery = { id -> viewModel.deleteSearchQuery(id) },
                                    onClearSearchHistory = { viewModel.clearSearchHistory() },
                                    onVideoClick = { vid -> navigateTo(AppRoute.Watch(vid)) },
                                    onShortClick = { sid -> switchRootTab(AppRoute.Shorts(sid)) },
                                    onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) }
                                )
                            }

                            is AppRoute.Channel -> {
                                LaunchedEffect(route.channelId) {
                                    viewModel.refreshChannel(route.channelId)
                                }
                                val ch = channels.firstOrNull { it.id == route.channelId }
                                if (ch == null) {
                                    popRoute()
                                } else {
                                    ChannelScreen(
                                        channel = ch,
                                        channelVideos = videos.filter { it.channel.id == ch.id },
                                        channelShorts = shorts.filter { it.channel.id == ch.id },
                                        channelPlaylists = playlists.filter { it.playlist.ownerUserId == ch.ownerUserId && it.playlist.visibility == "PUBLIC" },
                                        isSubscribed = ch.id in subscribedIds,
                                        onBack = { popRoute() },
                                        onRefreshChannel = { viewModel.refreshChannel(ch.id) },
                                        onToggleSubscribe = {
                                            viewModel.toggleSubscription(ch.id) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                        },
                                        onShareChannel = {
                                            shareText(context, ch.name, "Check out ${ch.name} (${ch.handle}) free on DSK YouTube!")
                                        },
                                        onReportChannel = {
                                            reportTarget = ReportTarget("CHANNEL", ch.id, ch.name)
                                        },
                                        onVideoClick = { vid -> navigateTo(AppRoute.Watch(vid)) },
                                        onShortClick = { sid -> switchRootTab(AppRoute.Shorts(sid)) },
                                        onPlaylistClick = { pid -> navigateTo(AppRoute.PlaylistDetail(pid)) }
                                    )
                                }
                            }

                            is AppRoute.PlaylistDetail -> {
                                val plSummary = playlists.firstOrNull { it.playlist.id == route.playlistId }
                                if (plSummary == null) {
                                    popRoute()
                                } else {
                                    PlaylistDetailScreen(
                                        playlist = plSummary.playlist,
                                        videos = playlistVideosMap[plSummary.playlist.id].orEmpty(),
                                        isOwner = plSummary.playlist.ownerUserId == currentUser?.id,
                                        onBack = { popRoute() },
                                        onPlayVideo = { vid -> navigateTo(AppRoute.Watch(vid)) },
                                        onEditPlaylist = { t, d, v -> viewModel.updatePlaylist(plSummary.playlist, t, d, v) },
                                        onDeletePlaylist = { viewModel.deletePlaylist(plSummary.playlist) },
                                        onRemoveVideo = { vid -> viewModel.removeVideoFromPlaylist(plSummary.playlist.id, vid) },
                                        onReorderVideo = { vid, up -> viewModel.reorderPlaylistVideo(plSummary.playlist.id, vid, up) }
                                    )
                                }
                            }

                            is AppRoute.Auth -> {
                                AuthScreen(
                                    initialMode = route.mode,
                                    currentUser = currentUser,
                                    currentProfile = currentProfile,
                                    onBack = { popRoute() },
                                    onLogin = { e, p -> viewModel.login(e, p) { popRoute() } },
                                    onRegister = { e, u, p -> viewModel.register(e, u, p) { popRoute() } },
                                    onResetPassword = { e, np -> viewModel.resetPassword(e, np) { } },
                                    onUpdateProfile = { dn, un, bio, uri ->
                                        viewModel.updateProfile(dn, un, bio, uri) { popRoute() }
                                    },
                                    onChangePassword = { cur, np -> viewModel.changePassword(cur, np) { } },
                                    onLogout = {
                                        viewModel.logout()
                                        popRoute()
                                    }
                                )
                            }

                            AppRoute.Notifications -> {
                                NotificationsScreen(
                                    notifications = notifications,
                                    onBack = { popRoute() },
                                    onMarkAllRead = { viewModel.markNotificationsRead() },
                                    onOpenVideo = { vid -> navigateTo(AppRoute.Watch(vid)) }
                                )
                            }

                            AppRoute.Settings -> {
                                SettingsScreen(
                                    settings = settings,
                                    isSignedIn = currentUser != null,
                                    onBack = { popRoute() },
                                    onSetDarkMode = { viewModel.setDarkMode(it) },
                                    onSetNotifications = { viewModel.setNotificationsEnabled(it) },
                                    onSetAutoplay = { viewModel.setAutoplayNext(it) },
                                    onSetQuality = { viewModel.setDefaultQuality(it) },
                                    onSetRestrictedMode = { viewModel.setRestrictedMode(it) },
                                    onSetLanguage = { viewModel.setLanguage(it) },
                                    onSetPrivateHistory = { viewModel.setPrivateWatchHistory(it) },
                                    onClearHistory = { viewModel.clearWatchHistory() },
                                    onLogout = { viewModel.logout() }
                                )
                            }

                            AppRoute.Admin -> {
                                AdminScreen(
                                    isAdmin = isAdmin,
                                    dashboard = adminDashboard,
                                    categories = categories,
                                    onBack = { popRoute() },
                                    onAddDirectUrlVideo = { title, url, cat, isShort ->
                                        viewModel.uploadMedia(
                                            isShort = isShort,
                                            videoUri = null,
                                            directVideoUrl = url,
                                            thumbnailUri = null,
                                            title = title,
                                            description = "Published via DSK Admin Panel",
                                            category = cat,
                                            tags = cat.lowercase(),
                                            visibility = "PUBLIC",
                                            commentsEnabled = true,
                                            musicInfo = "Admin Sound"
                                        )
                                    },
                                    onEditVideo = { v, t, c, s -> viewModel.adminEditVideo(v, t, c, s) },
                                    onTogglePublished = { id, pub -> viewModel.adminTogglePublished(id, pub) },
                                    onToggleFeatured = { id, feat -> viewModel.adminToggleFeatured(id, feat) },
                                    onToggleTrending = { id, tr -> viewModel.adminToggleTrending(id, tr) },
                                    onDeleteVideo = { id -> viewModel.adminDeleteVideo(id) },
                                    onDeleteShort = { id -> viewModel.adminDeleteShort(id) },
                                    onAddCategory = { name -> viewModel.adminAddCategory(name) },
                                    onDeleteCategory = { id -> viewModel.adminDeleteCategory(id) },
                                    onSetUserBlocked = { uid, b -> viewModel.adminSetUserBlocked(uid, b) },
                                    onSetChannelSuspended = { cid, s -> viewModel.adminSetChannelSuspended(cid, s) },
                                    onResolveReport = { rep, act -> viewModel.adminResolveReport(rep, act) }
                                )
                            }
                        }
                    }
                }
            }

            // Report Modal Dialog
            reportTarget?.let { target ->
                ReportDialog(
                    targetLabel = target.targetLabel,
                    onDismiss = { reportTarget = null },
                    onSubmit = { reason, details ->
                        viewModel.submitReport(
                            targetType = target.targetType,
                            targetId = target.targetId,
                            targetLabel = target.targetLabel,
                            reason = reason,
                            details = details,
                            onRequireAuth = {
                                reportTarget = null
                                navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                            }
                        )
                        reportTarget = null
                    }
                )
            }

            // Add to Playlist Modal Dialog
            playlistTargetVideoId?.let { vid ->
                AddToPlaylistDialog(
                    playlists = playlists,
                    onSelectPlaylist = { pid ->
                        viewModel.addVideoToPlaylist(pid, vid)
                        playlistTargetVideoId = null
                    },
                    onCreateNew = { title, vis ->
                        viewModel.createPlaylist(title, "", vis)
                    },
                    onDismiss = { playlistTargetVideoId = null }
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassBottomDock(
    tabs: List<NavTab>,
    isSelected: (NavTab) -> Boolean,
    onSelect: (AppRoute) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassPill(
            intensity = GlassIntensity.STRONG,
            elevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            tabs.forEach { tab ->
                val selected = isSelected(tab)
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.02f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dock_scale_${tab.tag}"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) DskWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "dock_tint_${tab.tag}"
                )
                val capsuleShape = RoundedCornerShape(99.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(capsuleShape)
                        .then(
                            if (selected) {
                                Modifier.liquidGlass(
                                    shape = capsuleShape,
                                    intensity = GlassIntensity.STRONG,
                                    selected = true
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.22f)),
                            onClick = { onSelect(tab.route) }
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag(tab.tag)
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = if (selected) DskRed else iconTint,
                        modifier = Modifier.size(if (tab.route == AppRoute.Create) 24.dp else 21.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassNavigationRail(
    tabs: List<NavTab>,
    isSelected: (NavTab) -> Boolean,
    onSelect: (AppRoute) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
    ) {
        LiquidGlassCard(
            shape = RoundedCornerShape(28.dp),
            intensity = GlassIntensity.STRONG,
            elevation = 14.dp,
            modifier = Modifier
                .fillMaxHeight()
                .width(86.dp)
                .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = GlassEdgeShadow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DskPrismEmblem(size = 30.dp)
                Spacer(Modifier.height(6.dp))
                tabs.forEach { tab ->
                    val selected = isSelected(tab)
                    val itemShape = RoundedCornerShape(20.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
                            .clip(itemShape)
                            .then(
                                if (selected) Modifier.liquidGlass(
                                    shape = itemShape,
                                    intensity = GlassIntensity.STRONG,
                                    selected = true
                                ) else Modifier
                            )
                            .clickable { onSelect(tab.route) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag(tab.tag)
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (selected) DskRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
