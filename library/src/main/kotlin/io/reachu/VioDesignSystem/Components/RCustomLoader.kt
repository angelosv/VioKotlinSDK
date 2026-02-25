package io.reachu.VioDesignSystem.Components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioColors

enum class LoaderStyle {
    Rotate, Pulse, Bounce
}

/**
 * Animated loaders mirroring Swift's `VioLoader`.
 */
@Composable
fun VioLoader(
    modifier: Modifier = Modifier,
    style: LoaderStyle = LoaderStyle.Rotate,
    size: Dp = 24.dp,
    color: Color = Color(android.graphics.Color.parseColor(VioColors.primary)),
    speed: Float = 1f
) {
    // Current simple implementation ignores speed for now to avoid complexity, 
    // but accepts it to maintain API compatibility.
    when (style) {
        LoaderStyle.Rotate -> RotatingLogoLoader(modifier, size, color)
        LoaderStyle.Pulse -> VioPulseLoader(modifier, size, color)
        LoaderStyle.Bounce -> VioBounceLoader(modifier, size, color)
    }
}

@Composable
private fun RotatingLogoLoader(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Unspecified
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VioLoaderTransition")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VioLogo(modifier = Modifier.size(size).rotate(rotation), color = color)
    }
}

/**
 * Pulse loader implementation
 */
@Composable
fun VioPulseLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color(android.graphics.Color.parseColor(VioColors.primary))
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VioPulseTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VioLogo(modifier = Modifier.size(size * scale).alpha(alpha), color = color)
    }
}

/**
 * Bounce loader implementation
 */
@Composable
fun VioBounceLoader(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color(android.graphics.Color.parseColor(VioColors.primary))
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VioBounceTransition")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VioLogo(modifier = Modifier.size(size).offset(y = offset.dp), color = color)
    }
}

@Composable
fun VioLogo(modifier: Modifier, color: Color) {
    // Simplified Vio Logo using Canvas
    Canvas(modifier = modifier) {
        drawCircle(color = color, radius = size.minDimension / 2)
    }
}
