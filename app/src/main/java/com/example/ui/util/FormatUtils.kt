package com.example.ui.util

import androidx.annotation.DrawableRes
import com.example.R
import java.util.Locale

fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0).replace(".0M", "M")
        count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
        else -> count.toString()
    }
}

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}

fun formatTimeMs(ms: Long): String {
    return formatDuration((ms.coerceAtLeast(0L) / 1000L).toInt())
}

fun formatRelativeTime(timestampMs: Long): String {
    val diff = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val mins = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        days < 365 -> "${days / 30}mo ago"
        else -> "${days / 365}y ago"
    }
}

@DrawableRes
fun resolveThumbnailDrawable(resKey: String?): Int {
    return when (resKey) {
        "img_thumb_tech" -> R.drawable.img_thumb_tech
        "img_thumb_music" -> R.drawable.img_thumb_music
        "img_thumb_cinema" -> R.drawable.img_thumb_cinema
        "img_channel_banner" -> R.drawable.img_channel_banner
        else -> R.drawable.img_thumb_tech
    }
}
