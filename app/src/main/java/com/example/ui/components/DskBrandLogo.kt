package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskRedDark
import com.example.ui.theme.DskRedGlow
import com.example.ui.theme.DskWhite
import com.example.ui.util.resolveThumbnailDrawable

@Composable
fun DskPrismEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Crimson faceted play prism
        val prismPath = Path().apply {
            moveTo(w * 0.16f, h * 0.10f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w * 0.16f, h * 0.90f)
            close()
        }
        drawPath(
            path = prismPath,
            brush = Brush.linearGradient(
                colors = listOf(DskRedGlow, DskRed, DskRedDark),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Subtle inner facet highlight
        val topFacet = Path().apply {
            moveTo(w * 0.16f, h * 0.10f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w * 0.44f, h * 0.50f)
            close()
        }
        drawPath(
            path = topFacet,
            color = Color.White.copy(alpha = 0.14f)
        )

        // Interlocking geometric white ribbon forming DSK kinetic mark
        val ribbonPath = Path().apply {
            moveTo(w * 0.08f, h * 0.34f)
            lineTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.08f, h * 0.66f)
            moveTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.28f)
            moveTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.72f)
        }
        drawPath(
            path = ribbonPath,
            color = DskWhite,
            style = Stroke(
                width = w * 0.11f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun DskBrandLogo(
    modifier: Modifier = Modifier,
    showFreeBadge: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DskPrismEmblem(size = 28.dp)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DSK",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "YOUTUBE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = DskWhite
                    )
                }
            }
        }
        if (showFreeBadge) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(99.dp)
                )
            ) {
                Text(
                    text = "FREE",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ChannelAvatar(
    name: String,
    avatarUri: String?,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "D" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(DskRed, Color(0xFF3E1018))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "$name profile image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                ),
                color = DskWhite
            )
        }
    }
}

@Composable
fun VideoThumbnailImage(
    thumbnailUrl: String?,
    thumbnailResKey: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (!thumbnailUrl.isNullOrBlank()) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = resolveThumbnailDrawable(thumbnailResKey)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
