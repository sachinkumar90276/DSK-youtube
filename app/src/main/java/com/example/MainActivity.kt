package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.ReportDialog
import com.example.ui.components.shareText
import com.example.ui.screens.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import com.example.ui.theme.DskYouTubeTheme
import com.example.viewmodel.DskViewModel

sealed interface AppRoute {
    data object Splash : AppRoute
    data object Home : AppRoute
    data class Shorts(val initialShortId: String? = null) : AppRoute
    data object Create : AppRoute
    data object Subscriptions : AppRoute
    data object You : AppRoute
    data class VideoPlayer(val videoId: String) : AppRoute
    data object Search : AppRoute
    data class Channel(val channelId: String) : AppRoute
    data class PlaylistDetail(val playlistId: String) : AppRoute
    data class Auth(val mode: AuthMode = AuthMode.SIGN_IN) : AppRoute
    data object Notifications : AppRoute
    data object Settings : AppRoute
    data object Admin : AppRoute
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

    val isSecondaryScreen = routeStack.size > 1 ||
        currentRoute !in listOf(AppRoute.Home, AppRoute.Create, AppRoute.Subscriptions, AppRoute.You) &&
        currentRoute !is AppRoute.Shorts &&
        currentRoute !is AppRoute.Splash

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
        currentRoute == AppRoute.You

    fun isTabSelected(tab: NavTab): Boolean = when (tab.route) {
        is AppRoute.Home -> currentRoute == AppRoute.Home
        is AppRoute.Shorts -> currentRoute is AppRoute.Shorts
        is AppRoute.Create -> currentRoute == AppRoute.Create
        is AppRoute.Subscriptions -> currentRoute == AppRoute.Subscriptions
        is AppRoute.You -> currentRoute == AppRoute.You
        else -> false
    }

    DskYouTubeTheme(darkTheme = settings.darkMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showMainNav && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navTabs.forEach { tab ->
                            val selected = isTabSelected(tab)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { switchRootTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(if (tab.route == AppRoute.Create) 28.dp else 24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DskRed,
                                    selectedTextColor = DskRed,
                                    indicatorColor = DskRed.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainNav && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        navTabs.forEach { tab ->
                            val selected = isTabSelected(tab)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { switchRootTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DskRed,
                                    selectedTextColor = DskRed,
                                    indicatorColor = DskRed.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
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
                                onSearchClick = { navigateTo(AppRoute.Search) },
                                onNotificationsClick = { navigateTo(AppRoute.Notifications) },
                                onProfileClick = {
                                    if (currentUser == null) navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                                    else switchRootTab(AppRoute.You)
                                },
                                onVideoClick = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) },
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
                                        "Watch \"${item.video.title}\" free on DSK YouTube: ${item.video.videoUrl}"
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
                                        "Watch Short \"${item.shortItem.title}\" free on DSK YouTube: ${item.shortItem.videoUrl}"
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
                                    else navigateTo(AppRoute.VideoPlayer(id))
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
                                onVideoClick = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) },
                                onToggleWatchLater = { vid ->
                                    viewModel.toggleWatchLater(vid) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                },
                                onAddToPlaylist = { vid ->
                                    if (currentUser == null) navigateTo(AppRoute.Auth(AuthMode.SIGN_IN))
                                    else playlistTargetVideoId = vid
                                },
                                onShareVideo = { item ->
                                    shareText(context, item.video.title, "Watch free on DSK YouTube: ${item.video.videoUrl}")
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
                                onVideoClick = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) },
                                onShortClick = { sid -> switchRootTab(AppRoute.Shorts(sid)) },
                                onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) },
                                onPlaylistClick = { pid -> navigateTo(AppRoute.PlaylistDetail(pid)) },
                                onCreatePlaylist = { title, desc, vis -> viewModel.createPlaylist(title, desc, vis) },
                                onDeletePlaylist = { pl -> viewModel.deletePlaylist(pl) },
                                onClearWatchHistory = { viewModel.clearWatchHistory() }
                            )
                        }

                        is AppRoute.VideoPlayer -> {
                            val target = videos.firstOrNull { it.video.id == route.videoId }
                            if (target == null) {
                                popRoute()
                            } else {
                                val related = remember(videos, target.video.id) {
                                    videos.filter { it.video.id != target.video.id }
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
                                        routeStack = routeStack.dropLast(1) + AppRoute.VideoPlayer(vid)
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
                                            "Watch \"${target.video.title}\" free on DSK YouTube: ${target.video.videoUrl}"
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
                                    onRecordView = { vid -> viewModel.recordVideoView(vid) }
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
                                onBack = { popRoute() },
                                onRecordSearch = { q -> viewModel.recordSearch(q) },
                                onDeleteSearchQuery = { id -> viewModel.deleteSearchQuery(id) },
                                onClearSearchHistory = { viewModel.clearSearchHistory() },
                                onVideoClick = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) },
                                onShortClick = { sid -> switchRootTab(AppRoute.Shorts(sid)) },
                                onChannelClick = { cid -> navigateTo(AppRoute.Channel(cid)) }
                            )
                        }

                        is AppRoute.Channel -> {
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
                                    onToggleSubscribe = {
                                        viewModel.toggleSubscription(ch.id) { navigateTo(AppRoute.Auth(AuthMode.SIGN_IN)) }
                                    },
                                    onShareChannel = {
                                        shareText(context, ch.name, "Check out ${ch.name} (${ch.handle}) free on DSK YouTube!")
                                    },
                                    onReportChannel = {
                                        reportTarget = ReportTarget("CHANNEL", ch.id, ch.name)
                                    },
                                    onVideoClick = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) },
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
                                    onPlayVideo = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) },
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
                                onOpenVideo = { vid -> navigateTo(AppRoute.VideoPlayer(vid)) }
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
