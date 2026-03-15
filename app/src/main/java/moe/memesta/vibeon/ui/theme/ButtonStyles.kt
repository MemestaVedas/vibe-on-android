package moe.memesta.vibeon.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Shared private shapes ───────────────────────────────────────────────────

private val PillShape  = RoundedCornerShape(percent = 50)

/**
 * Asymmetric prism shape: top-end and bottom-start corners are larger than
 * their diagonal counterparts, giving a kinetic "rotated" visual energy.
 */
private val PrismShape = RoundedCornerShape(
    topStart = 10.dp, topEnd = 18.dp,
    bottomStart = 18.dp, bottomEnd = 10.dp
)

// ─── OrbitButton ─────────────────────────────────────────────────────────────

/**
 * Futuristic primary CTA.
 *
 * - Pill-shaped, primary-colored fill.
 * - When [isActive] is true an arc sweeps around the perimeter in a
 *   continuous orbit, communicating live/playing state.
 * - Spring-based press scale with reduced-motion respect.
 */
@Composable
fun OrbitButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val ixSource = remember { MutableInteractionSource() }
    val isPressed by ixSource.collectIsPressedAsState()
    val prefersReducedMotion = rememberPrefersReducedMotion()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = if (prefersReducedMotion) snap() else MotionTokens.Spatial.fast(),
        label = "orbitScale"
    )

    // Orbit arc — only runs when isActive and motion enabled
    val infiniteTransition = rememberInfiniteTransition(label = "orbitArc")
    val arcDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arcDeg"
    )
    val arcAlpha by animateFloatAsState(
        targetValue = if (isActive && !prefersReducedMotion) 0.75f else 0f,
        animationSpec = tween(MotionTokens.Duration.Standard),
        label = "arcAlpha"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = PillShape,
        enabled = enabled,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = ixSource,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
            // Orbit arc drawn on top of the button surface
            if (arcAlpha > 0f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val strokePx = 2.dp.toPx()
                    rotate(degrees = arcDeg, pivot = center) {
                        drawArc(
                            color = Color.White.copy(alpha = arcAlpha),
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(strokePx, strokePx),
                            size = Size(
                                width = size.width - strokePx * 2,
                                height = size.height - strokePx * 2
                            ),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

// ─── FluxPill ────────────────────────────────────────────────────────────────

/**
 * Futuristic toggle / state chip.
 *
 * - Filled with primaryContainer when [selected]; outlined otherwise.
 * - Color and border transitions use MotionTokens.Duration.Fast tween.
 * - Pops up slightly on selection via spring scale.
 */
@Composable
fun FluxPill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    label: String? = null
) {
    val prefersReducedMotion = rememberPrefersReducedMotion()

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(MotionTokens.Duration.Fast),
        label = "fluxBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(MotionTokens.Duration.Fast),
        label = "fluxFg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(MotionTokens.Duration.Fast),
        label = "fluxBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = if (prefersReducedMotion) snap() else MotionTokens.Spatial.fast(),
        label = "fluxScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(40.dp),
        shape = PillShape,
        enabled = enabled,
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

// ─── PrismIconButton ─────────────────────────────────────────────────────────

/**
 * Futuristic secondary icon button with an asymmetric prism shape.
 *
 * - Prismatic gradient border brightens on press, communicating depth.
 * - Spring-based scale with reduced-motion gating.
 */
@Composable
fun PrismIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified
) {
    val ixSource = remember { MutableInteractionSource() }
    val isPressed by ixSource.collectIsPressedAsState()
    val prefersReducedMotion = rememberPrefersReducedMotion()

    val resolvedContainer = if (containerColor == Color.Unspecified)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else containerColor

    val resolvedTint = if (tint == Color.Unspecified)
        MaterialTheme.colorScheme.onSurfaceVariant
    else tint

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = if (prefersReducedMotion) snap() else MotionTokens.Spatial.fast(),
        label = "prismScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 0.25f,
        animationSpec = tween(MotionTokens.Duration.Fast),
        label = "prismBorderAlpha"
    )

    val primary = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = PrismShape,
        enabled = enabled,
        color = resolvedContainer,
        interactionSource = ixSource,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = borderAlpha),
                    primary.copy(alpha = borderAlpha * 0.35f),
                    Color.Transparent
                )
            )
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

    // ─── OrbitPlayButton ─────────────────────────────────────────────────────────

    /**
     * Compact circular play/pause button for mini-players and toolbars.
     *
     * Shares the same orbit arc animation as [OrbitButton] but fits in a fixed [size] circle.
     */
    @Composable
    fun OrbitPlayButton(
        isPlaying: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        size: Dp = 44.dp,
        enabled: Boolean = true,
        playIcon: ImageVector,
        pauseIcon: ImageVector
    ) {
        val ixSource = remember { MutableInteractionSource() }
        val isPressed by ixSource.collectIsPressedAsState()
        val prefersReducedMotion = rememberPrefersReducedMotion()

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = if (prefersReducedMotion) snap() else MotionTokens.Spatial.fast(),
            label = "orbitPlayScale"
        )
        val infiniteTransition = rememberInfiniteTransition(label = "orbitPlayArc")
        val arcDeg by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbitPlayArcDeg"
        )
        val arcAlpha by animateFloatAsState(
            targetValue = if (isPlaying && !prefersReducedMotion) 0.7f else 0f,
            animationSpec = tween(MotionTokens.Duration.Standard),
            label = "orbitPlayArcAlpha"
        )

        Surface(
            onClick = onClick,
            modifier = modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            shape = CircleShape,
            enabled = enabled,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            interactionSource = ixSource
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) pauseIcon else playIcon,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.5f)
                )
                if (arcAlpha > 0f) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val strokePx = 1.5.dp.toPx()
                        rotate(degrees = arcDeg, pivot = center) {
                            drawArc(
                                color = Color.White.copy(alpha = arcAlpha),
                                startAngle = -60f,
                                sweepAngle = 80f,
                                useCenter = false,
                                topLeft = Offset(strokePx, strokePx),
                                size = Size(
                                    width = this.size.width - strokePx * 2,
                                    height = this.size.height - strokePx * 2
                                ),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
