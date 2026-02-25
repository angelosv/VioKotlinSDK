package io.reachu.liveui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.liveshow.LiveShowCartManagerProvider
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.formattedPrice

@Composable
fun VioLiveStreamFullScreenOverlayLayout(
    stream: LiveStream,
    onDismiss: () -> Unit,
    manager: LiveShowManager = LiveShowManager.shared,
    cartManager: LiveShowCartManaging = LiveShowCartManagerProvider.default,
    modifier: Modifier = Modifier,
) {
    var showChat by remember { mutableStateOf(true) }
    var currentProductIndex by remember { mutableIntStateOf(0) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF222222), Color.Black),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TopControls(stream = stream, onDismiss = onDismiss, manager = manager)
            Spacer(modifier = Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (stream.featuredProducts.isNotEmpty()) {
                    LiveProductCarousel(
                        products = stream.featuredProducts,
                        currentIndex = currentProductIndex,
                        onProductSelected = { currentProductIndex = it },
                        cartManager = cartManager,
                        manager = manager,
                    )
                }
                if (showChat) {
                    VioLiveChatComponent(
                        controller = remember { VioLiveChatComponentController() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Live chat",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    IconButton(onClick = { showChat = !showChat }) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VioLiveStreamBottomSheet(
    stream: LiveStream,
    onDismiss: () -> Unit,
    manager: LiveShowManager = LiveShowManager.shared,
    cartManager: LiveShowCartManaging = LiveShowCartManagerProvider.default,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF121212), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(50)),
        )
        VideoPreview(stream = stream, onDismiss = onDismiss, manager = manager)
        StreamerInfoCompact(stream)
        if (stream.featuredProducts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Featured Products", color = Color.White, style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(stream.featuredProducts) { _, product ->
                        ProductCardCompact(product = product) {
                            manager.addProductToCart(product, cartManager)
                        }
                    }
                }
            }
        }
        VioLiveChatComponent(
            controller = remember { VioLiveChatComponentController() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun VioLiveStreamModal(
    stream: LiveStream,
    onDismiss: () -> Unit,
    manager: LiveShowManager = LiveShowManager.shared,
    cartManager: LiveShowCartManaging = LiveShowCartManagerProvider.default,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF141414),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stream.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                }
            }
            VideoPreview(stream = stream, onDismiss = {}, manager = manager)
            StreamerInfoCompact(stream)
            Button(
                onClick = {
                    stream.featuredProducts.firstOrNull()?.let {
                        manager.addProductToCart(it, cartManager)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add highlighted product")
            }
        }
    }
}

@Composable
private fun TopControls(
    stream: LiveStream,
    onDismiss: () -> Unit,
    manager: LiveShowManager,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = Color.Red, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = "${stream.viewerCount} viewers",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stream.streamer.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(onClick = { manager.showMiniPlayer() }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Mini player", tint = Color.White)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
private fun VideoPreview(
    stream: LiveStream,
    onDismiss: () -> Unit,
    manager: LiveShowManager,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2C2C2C), Color(0xFF141414)),
                ),
                RoundedCornerShape(20.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(50)) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                }
            }
            IconButton(
                onClick = { manager.showLiveStream(stream) },
                modifier = Modifier
                    .align(Alignment.End)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
            }
        }
    }
}

@Composable
private fun StreamerInfoCompact(stream: LiveStream) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
        ) {}
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stream.streamer.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (stream.streamer.isVerified) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF5CE1E6),
                        modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    )
                }
            }
            Text(
                text = stream.streamer.username,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LiveProductCarousel(
    products: List<LiveProduct>,
    currentIndex: Int,
    onProductSelected: (Int) -> Unit,
    cartManager: LiveShowCartManaging,
    manager: LiveShowManager,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Featured Products", color = Color.White)
            Text("${products.size} items", color = Color.White.copy(alpha = 0.6f))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(products) { index, product ->
                ProductCardCompact(product = product) {
                    onProductSelected(index)
                    manager.addProductToCart(product, cartManager)
                }
            }
        }
    }
}

@Composable
private fun ProductCardCompact(
    product: LiveProduct,
    onAdd: () -> Unit,
) {
    Card(
        onClick = onAdd,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(140.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
            ) {}
            Text(
                text = product.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.price.formattedPrice(),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}
