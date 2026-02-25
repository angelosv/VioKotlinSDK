package io.reachu.liveui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.asProduct

@Composable
fun VioLiveShowFullScreenOverlay(
    controller: VioLiveShowOverlayController = remember { VioLiveShowOverlayController() },
    modifier: Modifier = Modifier,
    videoContent: @Composable BoxScope.(LiveStream?) -> Unit = { DefaultVideoPlaceholder(it) },
) {
    val state by controller.state.collectAsState()
    val chatController = remember(state.stream?.id) { VioLiveChatComponentController() }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize()) {
            videoContent(state.stream)
        }
        if (state.stream != null) {
            OverlayChrome(
                state = state,
                controller = controller,
                chatController = chatController,
            )
        }
        DynamicComponentLayer(state.activeComponents, modifier = Modifier.fillMaxSize())
        VioLiveLikesComponent(manager = controller.likesManager, modifier = Modifier.fillMaxSize())
        AnimatedVisibility(
            visible = state.showProductsGrid,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                VioLiveProductsGridOverlay(
                    products = state.stream?.featuredProducts ?: emptyList(),
                    controller = remember { VioLiveProductsComponentController() },
                    onClose = {
                        controller.toggleProductGrid()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
        AnimatedVisibility(visible = state.isLoading, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AnimatedVisibility(
            visible = state.selectedProduct != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                state.selectedProduct?.let { selected ->
                    SelectedProductSheet(
                        product = selected,
                        onAddToCart = controller::addSelectedProductToCart,
                        onDismiss = controller::dismissProductDetail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.OverlayChrome(
    state: LiveShowOverlayState,
    controller: VioLiveShowOverlayController,
    chatController: VioLiveChatComponentController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        state.stream?.let { stream ->
            FullScreenTopBar(stream = stream, controller = controller, isMuted = state.isMuted)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VioLiveChatComponent(
                controller = chatController,
                modifier = Modifier.fillMaxWidth(),
            )
            val products = state.stream?.featuredProducts.orEmpty()
            if (products.isNotEmpty()) {
                FeaturedProductsRail(
                    products = products,
                    onProductClick = { controller.selectProduct(it.asProduct) },
                )
            }
        }
    }
    RightSideControls(
        state = state,
        controller = controller,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(24.dp),
    )
}

@Composable
private fun FullScreenTopBar(
    stream: LiveStream,
    controller: VioLiveShowOverlayController,
    isMuted: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "by ${stream.streamer.name}",
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        IconButton(onClick = controller::dismissOverlay) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

@Composable
private fun RightSideControls(
    state: LiveShowOverlayState,
    controller: VioLiveShowOverlayController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = controller::togglePlayback) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Toggle playback",
                tint = Color.White,
            )
        }
        IconButton(onClick = controller::toggleMute) {
            Icon(
                imageVector = if (state.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = "Mute",
                tint = Color.White,
            )
        }
        IconButton(onClick = controller::toggleProductGrid) {
            Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = "Products",
                tint = Color.White,
            )
        }
        IconButton(onClick = { state.stream?.let { controller.sendHeart(it.isLive) } }) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Heart",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun FeaturedProductsRail(
    products: List<LiveProduct>,
    onProductClick: (LiveProduct) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Featured products",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                VioLiveProductCard(
                    product = product,
                    modifier = Modifier.width(280.dp),
                    onClick = { onProductClick(product) },
                )
            }
        }
    }
}
