package moe.memesta.vibeon.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.memesta.vibeon.ui.HeartBurstEvent
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun HeartBurstOverlay(
    event: HeartBurstEvent?,
    reduceMotion: Boolean,
    onComplete: () -> Unit
) {
    if (event == null) return

    if (reduceMotion) {
        var visible by remember(event.timestampMs) { mutableStateOf(true) }
        LaunchedEffect(event.timestampMs) {
            delay(300)
            visible = false
            onComplete()
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxSize()
                .semantics { hideFromAccessibility() }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(84.dp)
                )
            }
        }
        return
    }

    data class Particle(val angle: Float, val distance: Float, val size: Float, val rotation: Float, val secondary: Boolean, val delayMs: Int)
    val particles = remember(event.timestampMs) {
        List(10) { index ->
            Particle(
                angle = Random.nextFloat() * (2f * PI.toFloat()),
                distance = Random.nextInt(80, 160).toFloat(),
                size = Random.nextInt(16, 40).toFloat(),
                rotation = Random.nextFloat() * 60f - 30f,
                secondary = index >= 7,
                delayMs = index * 40
            )
        }
    }
    val progressAnims = remember(event.timestampMs) { particles.map { Animatable(0f) } }
    LaunchedEffect(event.timestampMs) {
        progressAnims.forEachIndexed { index, anim ->
            launch {
                delay(particles[index].delayMs.toLong())
                anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
        }
        delay(980)
        onComplete()
    }

    Box(modifier = Modifier.fillMaxSize().semantics { hideFromAccessibility() }) {
        particles.forEachIndexed { index, particle ->
            val p = progressAnims[index].value
            val eased = 1f - (1f - p) * (1f - p) * (1f - p)
            val tx = particle.distance * eased * kotlin.math.cos(particle.angle)
            val ty = particle.distance * eased * kotlin.math.sin(particle.angle)
            val scale = when {
                p < 0.4f -> 1.2f * (p / 0.4f)
                p < 0.6f -> 1.2f - ((p - 0.4f) / 0.2f) * 0.2f
                else -> 1f
            }
            val alpha = if (p <= 0.6f) 1f else (1f - (p - 0.6f) / 0.4f).coerceIn(0f, 1f)
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = if (particle.secondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((event.x + tx).roundToInt(), (event.y + ty).roundToInt()) }
                    .size(particle.size.dp)
                    .rotate(particle.rotation)
                    .scale(scale)
                    .alpha(alpha)
            )
        }
    }
}
