package io.reachu.liveui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlin.math.roundToInt

@Composable
fun BoxScope.VioLiveLikesComponent(
    manager: LiveLikesManager = LiveLikesManager.shared,
    modifier: Modifier = Modifier,
) {
    val hearts by manager.hearts.collectAsState()
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        hearts.forEach { heart ->
            FlyingHeartView(
                heart = heart,
                containerWidth = widthPx,
                containerHeight = heightPx,
            )
        }
    }
}

@Composable
private fun BoxScope.FlyingHeartView(
    heart: FlyingHeartModel,
    containerWidth: Float,
    containerHeight: Float,
) {
    val vertical = remember { Animatable(0f) }
    val horizontal = remember { Animatable(0f) }
    val maxLift = remember { Random.nextInt(220, 420).toFloat() }
    val horizontalSwing = remember { Random.nextInt(-80, 80).toFloat() }
    val startX = remember(heart.id, containerWidth) {
        val normalized = heart.startX / BASE_WIDTH
        containerWidth * normalized
    }
    val startY = remember(heart.id, containerHeight) {
        val normalized = heart.startY / BASE_HEIGHT
        containerHeight * normalized
    }

    LaunchedEffect(heart.id) {
        vertical.animateTo(
            targetValue = maxLift,
            animationSpec = tween(durationMillis = 2400),
        )
    }
    LaunchedEffect(heart.id) {
        horizontal.animateTo(
            targetValue = horizontalSwing,
            animationSpec = tween(durationMillis = 2000),
        )
    }
    Icon(
        imageVector = if (heart.isUserGenerated) Icons.Filled.Favorite else Icons.Outlined.Favorite,
        contentDescription = null,
        tint = if (heart.isUserGenerated) Color(0xFFFF4D73) else Color.White,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (startX + horizontal.value).roundToInt(),
                    y = (startY - vertical.value).roundToInt(),
                )
            }
            .size(heartSize(heart))
            .alpha(1f - vertical.value / maxLift),
    )
}

private fun heartSize(heart: FlyingHeartModel) =
    if (heart.isUserGenerated) 28.dp else 20.dp

private const val BASE_WIDTH = 390f
private const val BASE_HEIGHT = 844f
