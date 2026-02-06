package moe.memesta.vibeon.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.memesta.vibeon.ui.ConnectionState

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    var showSuccess by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }
    
    // Success animation timing
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED && !showSuccess) {
            showSuccess = true
            delay(1500) // Show checkmark for 1.5s
            fadeOut = true
            delay(500) // Fade animation duration
            showSuccess = false
            fadeOut = false
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "FadeOut"
    )
    
    AnimatedVisibility(
        visible = connectionState == ConnectionState.CONNECTING || showSuccess,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .alpha(alpha)
                .background(
                    color = when {
                        showSuccess -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                showSuccess -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                connectionState == ConnectionState.CONNECTING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
