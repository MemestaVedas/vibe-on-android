package moe.memesta.vibeon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.delay
import moe.memesta.vibeon.ui.theme.VibeonTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)

        setContent {
            VibeonTheme {
                SpringyRotationSplash()
            }
        }
    }

    @Composable
    private fun SpringyRotationSplash() {
        val rotation = remember { Animatable(0f) }
        var scale by remember { mutableStateOf(0.72f) }
        val animatedScale by animateFloatAsState(
            targetValue = scale,
            animationSpec = spring(
                dampingRatio = 0.52f,
                stiffness = 210f
            ),
            label = "splash-scale"
        )

        LaunchedEffect(Unit) {
            scale = 1f
            rotation.animateTo(
                targetValue = 1080f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = 170f
                )
            )
            delay(180)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_vibe_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(130.dp)
                    .scale(animatedScale)
                    .rotate(rotation.value)
            )
        }
    }
}
