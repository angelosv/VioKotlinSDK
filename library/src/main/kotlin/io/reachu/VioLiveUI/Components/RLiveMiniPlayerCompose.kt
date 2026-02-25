package io.reachu.liveui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.formattedPrice
import kotlin.math.roundToInt

@Composable
fun VioLiveMiniPlayer(
    stream: LiveStream,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    manager: LiveShowManager = LiveShowManager.shared,
) {
    BoxWithConstraints(modifier = modifier) {
        val scope = rememberCoroutineScope()
        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }
        var visible by remember { mutableStateOf(false) }
        val density = LocalDensity.current
        val widthPx = with(density) { 140.dp.toPx() }
        val heightPx = with(density) { 190.dp.toPx() }

        fun coerceOffset(dx: Float, dy: Float): Pair<Float, Float> {
            val maxX = with(density) { maxWidth.toPx() } - widthPx
            val maxY = with(density) { maxHeight.toPx() } - heightPx
            return dx.coerceIn(0f, maxX) to dy.coerceIn(0f, maxY)
        }

        LaunchedEffect(Unit) {
            visible = true
            val padding = with(density) { 32.dp.toPx() }
            val (cx, cy) = coerceOffset(with(density) { maxWidth.toPx() } - widthPx - padding, with(density) { maxHeight.toPx() } - heightPx - padding)
            offsetX.snapTo(cx)
            offsetY.snapTo(cy)
        }

        Card(
            modifier = Modifier
                .size(width = 140.dp, height = 190.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        val (nx, ny) = coerceOffset(offsetX.value + dragAmount.x, offsetY.value + dragAmount.y)
                        scope.launch {
                            offsetX.snapTo(nx)
                            offsetY.snapTo(ny)
                        }
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            onClick = { manager.expandFromMiniPlayer() },
        ) {
            Column {
                MiniPlayerThumbnail(
                    stream = stream,
                    onDismiss = onDismiss,
                )
                MiniPlayerDetails(stream = stream)
            }
        }
    }
}

@Composable
private fun MiniPlayerThumbnail(
    stream: LiveStream,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF202020),
                        Color(0xFF111111),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveBadge(stream.viewerCount)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close mini player", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                modifier = Modifier.align(Alignment.End),
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerDetails(stream: LiveStream) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
            Column {
                Text(
                    text = stream.streamer.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stream.viewerCount} watching",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        stream.featuredProducts.firstOrNull()?.let {
            MiniProductRow(product = it)
        }
    }
}

@Composable
private fun MiniProductRow(product: LiveProduct) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.price.formattedPrice(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun LiveBadge(viewers: Int) {
    Surface(
        color = Color.Red.copy(alpha = 0.85f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "LIVE · $viewers",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
