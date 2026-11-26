package com.example.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class GlassIntensity {
    SUBTLE,
    REGULAR,
    STRONG
}

@Composable
fun LiquidGlassBackground(
    darkTheme: Boolean = LocalIsDarkGlass.current,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val animatorsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val driftPhase by if (animatorsEnabled && animate) {
        val transition = rememberInfiniteTransition(label = "liquid_glass_ambient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 22_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambient_phase"
        )
    } else {
        remember { mutableFloatStateOf(0.8f) }
    }

    val baseTop = if (darkTheme) GlassVoid else Color(0xFFE7ECF6)
    val baseMid = if (darkTheme) GlassAbyss else Color(0xFFF2F5FB)
    val baseBottom = if (darkTheme) Color(0xFF07080D) else Color(0xFFE9EEF7)

    val redOrbAlpha = if (darkTheme) 0.24f else 0.14f
    val crimsonOrbAlpha = if (darkTheme) 0.20f else 0.10f
    val iceOrbAlpha = if (darkTheme) 0.12f else 0.16f
    val particleAlpha = if (darkTheme) 0.08f else 0.12f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(baseTop, baseMid, baseBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val dx1 = cos(driftPhase.toDouble()).toFloat() * w * 0.08f
            val dy1 = sin(driftPhase.toDouble()).toFloat() * h * 0.05f
            val dx2 = sin((driftPhase * 0.7f).toDouble()).toFloat() * w * 0.09f
            val dy2 = cos((driftPhase * 0.7f).toDouble()).toFloat() * h * 0.06f

            // Top-left soft DSK Red ambient glow
            val orb1Center = Offset(w * 0.18f + dx1, h * 0.14f + dy1)
            val orb1Radius = (w.coerceAtLeast(h)) * 0.56f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DskRed.copy(alpha = redOrbAlpha),
                        DskRedDark.copy(alpha = redOrbAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = orb1Center
            )

            // Mid-right cool sapphire reflection for glass refraction depth
            val orb2Center = Offset(w * 0.86f - dx2, h * 0.42f + dy2)
            val orb2Radius = (w.coerceAtLeast(h)) * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DskInfo.copy(alpha = iceOrbAlpha),
                        Color(0xFF6366F1).copy(alpha = iceOrbAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = orb2Center
            )

            // Bottom-left deep crimson ambient anchor
            val orb3Center = Offset(w * 0.28f + dx2 * 0.6f, h * 0.86f - dy1 * 0.7f)
            val orb3Radius = (w.coerceAtLeast(h)) * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DskRedGlow.copy(alpha = crimsonOrbAlpha),
                        DskRedDark.copy(alpha = crimsonOrbAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = orb3Center,
                    radius = orb3Radius
                ),
                radius = orb3Radius,
                center = orb3Center
            )

            // Subtle drifting micro-reflections
            val particleColor = if (darkTheme) Color.White else DskRed
            for (i in 0 until 10) {
                val angle = driftPhase + i * 0.63f
                val px = ((0.12f + (i * 0.085f)) % 0.92f) * w + sin(angle.toDouble()).toFloat() * 14f
                val py = ((0.09f + (i * 0.11f)) % 0.90f) * h + cos(angle.toDouble()).toFloat() * 18f
                val r = (1.6f + (i % 3) * 0.9f) * density
                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha * (0.4f + (i % 3) * 0.25f)),
                    radius = r,
                    center = Offset(px, py)
                )
            }
        }

        content()
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(22.dp),
    intensity: GlassIntensity = GlassIntensity.REGULAR,
    selected: Boolean = false,
    accentTint: Color? = null,
    darkTheme: Boolean = LocalIsDarkGlass.current
): Modifier {
    val topAlpha = when (intensity) {
        GlassIntensity.SUBTLE -> if (darkTheme) 0.09f else 0.66f
        GlassIntensity.REGULAR -> if (darkTheme) 0.14f else 0.76f
        GlassIntensity.STRONG -> if (darkTheme) 0.20f else 0.86f
    }
    val bottomAlpha = when (intensity) {
        GlassIntensity.SUBTLE -> if (darkTheme) 0.04f else 0.44f
        GlassIntensity.REGULAR -> if (darkTheme) 0.06f else 0.54f
        GlassIntensity.STRONG -> if (darkTheme) 0.10f else 0.66f
    }

    val fillBrush = when {
        selected -> Brush.verticalGradient(
            colors = listOf(
                DskRedGlow.copy(alpha = if (darkTheme) 0.34f else 0.24f),
                DskRed.copy(alpha = if (darkTheme) 0.22f else 0.16f),
                DskRedDark.copy(alpha = if (darkTheme) 0.18f else 0.10f)
            )
        )
        accentTint != null -> Brush.verticalGradient(
            colors = listOf(
                accentTint.copy(alpha = if (darkTheme) 0.20f else 0.16f),
                (if (darkTheme) Color.White else Color.White).copy(alpha = bottomAlpha)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = (topAlpha + bottomAlpha) * 0.45f),
                if (darkTheme) Color(0xFF0A0C14).copy(alpha = 0.24f) else Color.White.copy(alpha = bottomAlpha)
            )
        )
    }

    val borderBrush = when {
        selected -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF9AA2).copy(alpha = 0.72f),
                GlassRedBorder,
                DskRed.copy(alpha = 0.28f)
            )
        )
        accentTint != null -> Brush.linearGradient(
            colors = listOf(
                accentTint.copy(alpha = 0.55f),
                if (darkTheme) GlassBorder else Color.White.copy(alpha = 0.7f),
                accentTint.copy(alpha = 0.22f)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                if (darkTheme) GlassHighlight else Color.White.copy(alpha = 0.92f),
                if (darkTheme) GlassBorder else Color(0xFFC5CEE0).copy(alpha = 0.65f),
                if (darkTheme) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.45f)
            )
        )
    }

    return this
        .clip(shape)
        .background(fillBrush, shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .drawWithContent {
            drawContent()
            // Top subtle specular reflection highlight
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (darkTheme) 0.24f else 0.65f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.12f, 1.dp.toPx()),
                end = Offset(size.width * 0.88f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    intensity: GlassIntensity = GlassIntensity.REGULAR,
    selected: Boolean = false,
    accentTint: Color? = null,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_card_scale"
    )
    val shadowColor = if (selected) DskRed.copy(alpha = 0.28f) else GlassEdgeShadow

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .liquidGlass(
            shape = shape,
            intensity = intensity,
            selected = selected,
            accentTint = accentTint
        )
        .let {
            if (onClick != null) {
                it.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color.White.copy(alpha = 0.22f)),
                    onClick = onClick
                )
            } else it
        }

    Column(modifier = baseModifier, content = content)
}

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.REGULAR,
    selected: Boolean = false,
    accentTint: Color? = null,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(99.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_pill_scale"
    )

    val rowModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (selected) DskRed.copy(alpha = 0.34f) else GlassEdgeShadow,
            spotColor = if (selected) DskRed.copy(alpha = 0.34f) else GlassEdgeShadow
        )
        .liquidGlass(
            shape = shape,
            intensity = intensity,
            selected = selected,
            accentTint = accentTint
        )
        .let {
            if (onClick != null) {
                it.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color.White.copy(alpha = 0.22f)),
                    onClick = onClick
                )
            } else it
        }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun LiquidGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val darkTheme = LocalIsDarkGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chip_scale"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected -> DskWhite
            darkTheme -> DskWhite.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "chip_text_color"
    )

    val shape = RoundedCornerShape(99.dp)
    val containerModifier = modifier
        .heightIn(min = 38.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = if (selected) 8.dp else 2.dp,
            shape = shape,
            ambientColor = if (selected) DskRed.copy(alpha = 0.35f) else GlassEdgeShadow,
            spotColor = if (selected) DskRed.copy(alpha = 0.35f) else GlassEdgeShadow
        )
        .liquidGlass(
            shape = shape,
            intensity = if (selected) GlassIntensity.STRONG else GlassIntensity.REGULAR,
            selected = selected
        )
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White.copy(alpha = 0.24f)),
            onClick = onClick
        )
        .padding(horizontal = 14.dp, vertical = 8.dp)

    Row(
        modifier = containerModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) DskWhite else DskRed,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    shape: Shape = RoundedCornerShape(99.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val darkTheme = LocalIsDarkGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_btn_scale"
    )

    val fillBrush = when {
        !enabled -> Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.05f))
        )
        isPrimary -> Brush.linearGradient(
            colors = listOf(
                DskRedGlow.copy(alpha = 0.92f),
                DskRed.copy(alpha = 0.88f),
                DskRedDark.copy(alpha = 0.92f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (darkTheme) 0.14f else 0.84f),
                Color.White.copy(alpha = if (darkTheme) 0.07f else 0.62f)
            )
        )
    }

    val borderBrush = if (isPrimary && enabled) {
        Brush.linearGradient(listOf(Color(0xFFFFB2B7).copy(alpha = 0.72f), GlassRedBorder))
    } else {
        Brush.linearGradient(
            listOf(
                if (darkTheme) GlassHighlight else Color.White.copy(alpha = 0.92f),
                if (darkTheme) GlassBorder else Color(0xFFC5CEE0).copy(alpha = 0.7f)
            )
        )
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        isPrimary -> DskWhite
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPrimary && enabled) 10.dp else 4.dp,
                shape = shape,
                ambientColor = if (isPrimary) DskRed.copy(alpha = 0.36f) else GlassEdgeShadow,
                spotColor = if (isPrimary) DskRed.copy(alpha = 0.36f) else GlassEdgeShadow
            )
            .clip(shape)
            .background(fillBrush, shape)
            .border(1.dp, borderBrush, shape)
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    selected: Boolean = false,
    intensity: GlassIntensity = GlassIntensity.REGULAR,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "icon_btn_scale"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlass(
                shape = CircleShape,
                intensity = intensity,
                selected = selected
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun LiquidGlassTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backButtonTag: String? = null,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    LiquidGlassPill(
        intensity = GlassIntensity.STRONG,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        LiquidGlassIconButton(
            onClick = onBack,
            size = 40.dp,
            intensity = GlassIntensity.SUBTLE,
            modifier = if (backButtonTag != null) Modifier.testTag(backButtonTag) else Modifier
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = trailingContent
        )
    }
}

@Composable
fun LiquidGlassSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val containerModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .liquidGlass(shape = RoundedCornerShape(99.dp), intensity = GlassIntensity.SUBTLE)
        .padding(4.dp)

    if (scrollable) {
        Row(
            modifier = containerModifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                SegmentTabPill(
                    label = label,
                    selected = selectedIndex == idx,
                    onClick = { onSelect(idx) }
                )
            }
        }
    } else {
        Row(
            modifier = containerModifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                SegmentTabPill(
                    label = label,
                    selected = selectedIndex == idx,
                    onClick = { onSelect(idx) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SegmentTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(99.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) Modifier.liquidGlass(
                    shape = shape,
                    intensity = GlassIntensity.STRONG,
                    selected = true
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) DskWhite else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    val darkTheme = LocalIsDarkGlass.current
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            intensity = GlassIntensity.SUBTLE
        )
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            label = if (label != null) { { Text(label) } } else null,
            placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = DskRed,
                focusedLabelColor = DskRed,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = if (darkTheme) DskWhite else MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (darkTheme) DskWhite else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val darkTheme = LocalIsDarkGlass.current
    val dialogBackdropTint = if (darkTheme) Color(0xFF0E1018).copy(alpha = 0.80f) else Color(0xFFF6F8FD).copy(alpha = 0.90f)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                shape = RoundedCornerShape(28.dp),
                intensity = GlassIntensity.STRONG,
                elevation = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .background(dialogBackdropTint, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    content()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(10.dp),
                    intensity = GlassIntensity.REGULAR,
                    accentTint = DskRed
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DskRed, modifier = Modifier.size(18.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun glassDropdownContainerColor(): Color {
    val dark = LocalIsDarkGlass.current
    return if (dark) Color(0xFF171A26).copy(alpha = 0.96f) else Color(0xFFF7F9FD).copy(alpha = 0.96f)
}

@Composable
fun glassMenuBorder(): BorderStroke {
    val dark = LocalIsDarkGlass.current
    return BorderStroke(1.dp, if (dark) GlassBorder else Color(0xFFC8D0E0))
}
