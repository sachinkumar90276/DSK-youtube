package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.remote.YouTubeParser
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import kotlinx.coroutines.delay

internal object WebViewRendererCompat {
    @Volatile
    var preferSoftwareLayer: Boolean = isAndroidEmulator()
}

private fun isAndroidEmulator(): Boolean {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
        Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
        Build.HARDWARE.contains("cutf_cvm", ignoreCase = true) ||
        Build.MODEL.contains("google_sdk", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
        Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk_gphone", ignoreCase = true) ||
        Build.PRODUCT.contains("google_sdk", ignoreCase = true) ||
        Build.PRODUCT.contains("vbox86p", ignoreCase = true) ||
        Build.PRODUCT.contains("emulator", ignoreCase = true) ||
        Build.PRODUCT.contains("simulator", ignoreCase = true)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    autoplay: Boolean = true,
    loop: Boolean = false,
    playbackRate: Float = 1f,
    volumePercent: Int = 100,
    seekToSecondsRequest: Pair<Int, Long>? = null,
    playbackToggleRequest: Pair<Boolean, Long>? = null,
    isActive: Boolean = true,
    restrictionMessage: String? = null,
    onProgress: (currentSec: Int, durationSec: Int) -> Unit = { _, _ -> },
    onPlaybackStateChanged: (isPlaying: Boolean) -> Unit = {},
    onEnded: () -> Unit = {},
    onPlayNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeVideoId = remember(videoId) {
        YouTubeParser.extractVideoId(videoId) ?: videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    }
    val hasValidId = remember(safeVideoId) { YouTubeParser.isValidVideoId(safeVideoId) }
    val appOrigin = remember(context) {
        val pkg = context.packageName.trim().ifBlank { "com.aistudio.dskyoutube.xqkmvp" }
        "https://$pkg"
    }

    var embedErrorCode by remember(safeVideoId) { mutableStateOf<Int?>(null) }
    var networkError by remember(safeVideoId) { mutableStateOf<String?>(null) }
    var isLoading by remember(safeVideoId) { mutableStateOf(true) }
    var isPlayerReady by remember(safeVideoId) { mutableStateOf(false) }
    var reloadNonce by remember(safeVideoId) { mutableIntStateOf(0) }
    var webViewInstanceEpoch by remember(safeVideoId) { mutableIntStateOf(0) }
    var autoRecoveryAttempts by remember(safeVideoId) { mutableIntStateOf(0) }
    var pendingRendererRecovery by remember(safeVideoId) { mutableStateOf(false) }
    var useSoftwareLayer by remember(safeVideoId) {
        mutableStateOf(WebViewRendererCompat.preferSoftwareLayer)
    }
    var webViewRef by remember(safeVideoId) { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember(safeVideoId) { mutableStateOf<View?>(null) }
    var customViewCallback by remember(safeVideoId) { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnPlaybackStateChanged by rememberUpdatedState(onPlaybackStateChanged)

    val lifecycleOwner = LocalLifecycleOwner.current

    // Loading timeout safety so user never gets stuck on an infinite spinner or loop
    LaunchedEffect(safeVideoId, webViewInstanceEpoch, reloadNonce, isLoading, restrictionMessage, hasValidId) {
        if (hasValidId && restrictionMessage == null && isLoading && embedErrorCode == null && networkError == null) {
            delay(18_000L)
            if (isLoading && !isPlayerReady) {
                isLoading = false
                networkError = "YouTube player took too long to respond. Check your internet connection and tap Retry."
            }
        }
    }

    DisposableEffect(lifecycleOwner, safeVideoId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val wv = webViewRef ?: return@LifecycleEventObserver
                    if (wv.tag != "DESTROYED") {
                        runCatching { wv.onPause() }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (pendingRendererRecovery) {
                        pendingRendererRecovery = false
                        embedErrorCode = null
                        networkError = null
                        isPlayerReady = false
                        isLoading = true
                        webViewInstanceEpoch++
                    } else {
                        val wv = webViewRef ?: return@LifecycleEventObserver
                        if (wv.tag != "DESTROYED") {
                            runCatching { wv.onResume() }
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { customViewCallback?.onCustomViewHidden() }
            customFullscreenView = null
            customViewCallback = null
            webViewRef?.let { destroyWebViewSafely(it, notifyJs = true, removeFromParent = false) }
            webViewRef = null
        }
    }

    LaunchedEffect(isActive, isPlayerReady, safeVideoId, webViewInstanceEpoch) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPlayerReady || wv.tag == "DESTROYED") return@LaunchedEffect
        if (isActive) {
            wv.evaluateJavascript("if (window.dskPlay) window.dskPlay();", null)
        } else {
            wv.evaluateJavascript("if (window.dskPause) window.dskPause();", null)
        }
    }

    LaunchedEffect(playbackToggleRequest, isPlayerReady, webViewInstanceEpoch) {
        val req = playbackToggleRequest ?: return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPlayerReady || wv.tag == "DESTROYED") return@LaunchedEffect
        if (req.first) {
            wv.evaluateJavascript("if (window.dskPlay) window.dskPlay();", null)
        } else {
            wv.evaluateJavascript("if (window.dskPause) window.dskPause();", null)
        }
    }

    LaunchedEffect(playbackRate, isPlayerReady, webViewInstanceEpoch) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isPlayerReady && wv.tag != "DESTROYED") {
            wv.evaluateJavascript("if (window.dskSetRate) window.dskSetRate($playbackRate);", null)
        }
    }

    LaunchedEffect(volumePercent, isPlayerReady, webViewInstanceEpoch) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isPlayerReady && wv.tag != "DESTROYED") {
            wv.evaluateJavascript("if (window.dskSetVolume) window.dskSetVolume(${volumePercent.coerceIn(0, 100)});", null)
        }
    }

    LaunchedEffect(seekToSecondsRequest, isPlayerReady, webViewInstanceEpoch) {
        val wv = webViewRef ?: return@LaunchedEffect
        val targetSec = seekToSecondsRequest?.first ?: return@LaunchedEffect
        if (isPlayerReady && wv.tag != "DESTROYED") {
            wv.evaluateJavascript("if (window.dskSeekTo) window.dskSeekTo($targetSec);", null)
        }
    }

    val activeErrorMessage = when {
        !hasValidId -> "Invalid YouTube video ID."
        restrictionMessage != null -> restrictionMessage
        networkError != null -> networkError
        embedErrorCode != null -> mapYouTubeErrorMessage(embedErrorCode!!)
        else -> null
    }

    Box(modifier = modifier.background(Color.Black)) {
        if (activeErrorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = DskRed,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = activeErrorMessage,
                    color = DskWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasValidId) {
                        Button(
                            onClick = {
                                embedErrorCode = null
                                networkError = null
                                pendingRendererRecovery = false
                                isLoading = true
                                isPlayerReady = false
                                webViewInstanceEpoch++
                                reloadNonce++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DskRed)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                    if (onPlayNext != null) {
                        OutlinedButton(onClick = onPlayNext) {
                            Icon(Icons.Outlined.SkipNext, contentDescription = null, tint = DskWhite, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Next Video", color = DskWhite)
                        }
                    }
                }
            }
        } else {
            key(safeVideoId, webViewInstanceEpoch) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                // Do not waive priority while temporarily covered (overlays/sheets/spinners)
                                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                            }
                            if (useSoftwareLayer) {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            }
                            val wv = this
                            runCatching {
                                CookieManager.getInstance().apply {
                                    setAcceptCookie(true)
                                    setAcceptThirdPartyCookies(wv, true)
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = false
                                allowFileAccess = false
                                allowContentAccess = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                setNeedInitialFocus(false)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    safeBrowsingEnabled = false
                                }
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message?
                                ): Boolean = false

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    runCatching { customViewCallback?.onCustomViewHidden() }
                                    customFullscreenView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    runCatching { customViewCallback?.onCustomViewHidden() }
                                    customFullscreenView = null
                                    customViewCallback = null
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    val isExternalAppScheme = url.startsWith("intent:") ||
                                        url.startsWith("vnd.youtube:") ||
                                        url.startsWith("market:")
                                    if (isExternalAppScheme) return true

                                    // Prevent top-level WebView from navigating away from our player page
                                    if (request?.isForMainFrame == true) return true

                                    val host = request?.url?.host?.lowercase().orEmpty()
                                    val path = request?.url?.path?.lowercase().orEmpty()
                                    val isWatchRedirect = (host.endsWith("youtube.com") || host == "youtu.be") && (
                                        host == "youtu.be" ||
                                            path == "/" ||
                                            path.startsWith("/watch") ||
                                            path.startsWith("/shorts") ||
                                            path.startsWith("/channel") ||
                                            path.startsWith("/c/") ||
                                            path.startsWith("/@")
                                        )
                                    return isWatchRedirect
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        post {
                                            isLoading = false
                                            networkError = "Unable to connect to YouTube embedded player. Check network connectivity and tap Retry."
                                        }
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val deadView = view ?: webViewRef
                                        if (deadView != null) {
                                            destroyWebViewSafely(
                                                deadView,
                                                notifyJs = false,
                                                removeFromParent = true
                                            )
                                            if (webViewRef === deadView) {
                                                webViewRef = null
                                            }
                                        }
                                        runCatching { customViewCallback?.onCustomViewHidden() }
                                        customFullscreenView = null
                                        customViewCallback = null
                                        isPlayerReady = false

                                        // Persist software fallback across videos if HW compositor crashed renderer
                                        WebViewRendererCompat.preferSoftwareLayer = true
                                        useSoftwareLayer = true

                                        val isForeground = lifecycleOwner.lifecycle.currentState
                                            .isAtLeast(Lifecycle.State.RESUMED)
                                        if (!isForeground) {
                                            pendingRendererRecovery = true
                                            isLoading = false
                                        } else if (autoRecoveryAttempts < 1) {
                                            autoRecoveryAttempts++
                                            embedErrorCode = null
                                            networkError = null
                                            isLoading = true
                                            webViewInstanceEpoch++
                                        } else {
                                            isLoading = false
                                            networkError = "Video renderer process terminated by the system or emulator. Tap Retry to reload."
                                        }
                                    }
                                    return true
                                }
                            }
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onIframeLoaded() {
                                        post { isLoading = false }
                                    }

                                    @JavascriptInterface
                                    fun onReady() {
                                        post {
                                            isLoading = false
                                            isPlayerReady = true
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onState(state: Int) {
                                        // YT.PlayerState: ENDED = 0, PLAYING = 1, PAUSED = 2
                                        post {
                                            if (state == 1) {
                                                isLoading = false
                                                currentOnPlaybackStateChanged(true)
                                            } else if (state == 2 || state == 0) {
                                                currentOnPlaybackStateChanged(false)
                                            }
                                            if (state == 0) {
                                                currentOnEnded()
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onProgress(currentSec: Int, durationSec: Int) {
                                        post { currentOnProgress(currentSec, durationSec) }
                                    }

                                    @JavascriptInterface
                                    fun onError(code: Int) {
                                        post {
                                            isLoading = false
                                            embedErrorCode = code
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onApiLoadFailed() {
                                        post {
                                            isLoading = false
                                            networkError = "Unable to load YouTube IFrame Player API script. Check network access and tap Retry."
                                        }
                                    }
                                },
                                "DskYouTubeBridge"
                            )
                        }.also { webViewRef = it }
                    },
                    update = { wv ->
                        if (wv.tag == "DESTROYED") return@AndroidView
                        val loadKey = "$safeVideoId#$webViewInstanceEpoch#$reloadNonce"
                        if (wv.tag != loadKey) {
                            wv.tag = loadKey
                            isLoading = true
                            isPlayerReady = false
                            embedErrorCode = null
                            networkError = null
                            val html = buildYouTubeIframeHtml(
                                videoId = safeVideoId,
                                appOrigin = appOrigin,
                                autoplay = if (autoplay && isActive) 1 else 0,
                                loop = if (loop) 1 else 0
                            )
                            wv.loadDataWithBaseURL(
                                "$appOrigin/",
                                html,
                                "text/html",
                                "utf-8",
                                "$appOrigin/"
                            )
                        }
                    },
                    onRelease = { wv ->
                        destroyWebViewSafely(wv, notifyJs = true, removeFromParent = false)
                        if (webViewRef === wv) {
                            webViewRef = null
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            customFullscreenView?.let { fsView ->
                AndroidView(
                    factory = {
                        (fsView.parent as? ViewGroup)?.removeView(fsView)
                        fsView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DskRed)
                }
            }
        }
    }
}

internal fun mapYouTubeErrorMessage(code: Int): String = when (code) {
    2 -> "Invalid YouTube video ID or player parameters (Error 2)."
    5 -> "This video cannot be played in the Android WebView HTML5 player (Error 5)."
    100 -> "This YouTube video is unavailable, private, or has been deleted (Error 100)."
    101, 150 -> "Embedding is disabled by the video owner (Error $code)."
    152 -> "YouTube restricted embedded playback for this video (Error 152). This occurs when the video restricts embedding or when the preview environment blocks YouTube player verification."
    153 -> "YouTube rejected the embedder Referer/Origin identity configuration (Error 153)."
    else -> "Unable to load YouTube embedded player (Error $code)."
}

private fun destroyWebViewSafely(
    wv: WebView,
    notifyJs: Boolean = true,
    removeFromParent: Boolean = true
) {
    if (wv.tag == "DESTROYED") return
    wv.tag = "DESTROYED"
    runCatching {
        if (notifyJs) {
            runCatching { wv.stopLoading() }
            runCatching { wv.onPause() }
        }
        runCatching { wv.removeJavascriptInterface("DskYouTubeBridge") }
        runCatching { wv.clearHistory() }
        runCatching { wv.removeAllViews() }
        if (removeFromParent) {
            runCatching { (wv.parent as? ViewGroup)?.removeView(wv) }
        }
        wv.destroy()
    }
}

private fun buildYouTubeIframeHtml(
    videoId: String,
    appOrigin: String,
    autoplay: Int,
    loop: Int
): String {
    val safeId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    val safeOrigin = appOrigin.trimEnd('/')
    val playlistJsonEntry = if (loop == 1) "playlist: '$safeId'," else ""
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="referrer" content="strict-origin-when-cross-origin">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
            #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <script
            src="https://www.youtube.com/iframe_api"
            onerror="if (window.DskYouTubeBridge) window.DskYouTubeBridge.onApiLoadFailed();">
          </script>
          <script>
            var player = null;
            var progressTimer = null;
            var playerInitialized = false;
            function startProgressLoop() {
              if (progressTimer) clearInterval(progressTimer);
              progressTimer = setInterval(function() {
                if (player && player.getCurrentTime && player.getDuration) {
                  var c = Math.floor(player.getCurrentTime() || 0);
                  var d = Math.floor(player.getDuration() || 0);
                  if (window.DskYouTubeBridge) window.DskYouTubeBridge.onProgress(c, d);
                }
              }, 800);
            }
            function onYouTubeIframeAPIReady() {
              if (playerInitialized) return;
              playerInitialized = true;
              player = new YT.Player('player', {
                width: '100%',
                height: '100%',
                host: 'https://www.youtube.com',
                videoId: '$safeId',
                playerVars: {
                  autoplay: $autoplay,
                  controls: 1,
                  playsinline: 1,
                  rel: 0,
                  modestbranding: 1,
                  fs: 1,
                  enablejsapi: 1,
                  loop: $loop,
                  $playlistJsonEntry
                  origin: '$safeOrigin',
                  widget_referrer: '$safeOrigin/'
                },
                events: {
                  'onReady': function(e) {
                    if ($autoplay === 1 && e && e.target && e.target.playVideo) {
                      try { e.target.playVideo(); } catch (err) {}
                    }
                    if (window.DskYouTubeBridge) window.DskYouTubeBridge.onReady();
                    startProgressLoop();
                  },
                  'onStateChange': function(e) {
                    if (window.DskYouTubeBridge) window.DskYouTubeBridge.onState(e.data);
                  },
                  'onError': function(e) {
                    if (window.DskYouTubeBridge) window.DskYouTubeBridge.onError(e.data);
                  }
                }
              });
              try {
                var iframeEl = player.getIframe ? player.getIframe() : document.getElementById('player');
                if (iframeEl && iframeEl.tagName === 'IFRAME') {
                  iframeEl.setAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
                  iframeEl.onload = function() {
                    if (window.DskYouTubeBridge) window.DskYouTubeBridge.onIframeLoaded();
                  };
                }
              } catch (err) {}
            }
            window.dskDispose = function() {
              try {
                if (progressTimer) {
                  clearInterval(progressTimer);
                  progressTimer = null;
                }
                if (player && player.stopVideo) player.stopVideo();
                if (player && player.destroy) player.destroy();
                player = null;
              } catch (e) {}
            };
            window.addEventListener('pagehide', window.dskDispose);
            window.dskPlay = function() { if (player && player.playVideo) player.playVideo(); };
            window.dskPause = function() { if (player && player.pauseVideo) player.pauseVideo(); };
            window.dskSeekTo = function(sec) { if (player && player.seekTo) player.seekTo(sec, true); };
            window.dskSetRate = function(r) { if (player && player.setPlaybackRate) player.setPlaybackRate(r); };
            window.dskSetVolume = function(v) {
              if (!player) return;
              if (v <= 0 && player.mute) { player.mute(); }
              else {
                if (player.unMute) player.unMute();
                if (player.setVolume) player.setVolume(v);
              }
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}
