package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DskPrismEmblem
import com.example.ui.theme.DskBlack
import com.example.ui.theme.DskCharcoal
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "splash_alpha"
    )
    val progress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        DskRed.copy(alpha = 0.24f),
                        DskCharcoal,
                        DskBlack
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
                .padding(horizontal = 32.dp)
        ) {
            DskPrismEmblem(size = 92.dp)

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DSK",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = DskWhite
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = DskRed,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "YOUTUBE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = DskWhite
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Watch Everything. Pay Nothing.",
                style = MaterialTheme.typography.bodyLarge,
                color = DskWhite.copy(alpha = 0.78f)
            )

            Spacer(Modifier.height(28.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = DskRed,
                trackColor = Color.White.copy(alpha = 0.14f)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "100% FREE STREAMING",
                style = MaterialTheme.typography.labelSmall,
                color = DskWhite.copy(alpha = 0.55f)
            )
        }
    }
}
