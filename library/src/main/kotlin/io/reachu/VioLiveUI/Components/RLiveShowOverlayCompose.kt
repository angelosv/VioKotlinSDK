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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCore.models.Product
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.asProduct
import io.reachu.liveshow.models.formattedPrice
import io.reachu.liveui.configuration.VioLiveShowConfiguration

private fun String.toColor(): Color = toVioColor()

@Composable
fun VioLiveShowOverlay(
    configuration: VioLiveShowConfiguration = VioLiveShowConfiguration.default,
    controller: VioLiveShowOverlayController = remember(configuration) { VioLiveShowOverlayController(configuration = configuration) },
    modifier: Modifier = Modifier,
    videoContent: @Composable BoxScope.(LiveStream?) -> Unit = { stream ->
        DefaultVideoPlaceholder(stream)
    },
) {
    val state by controller.state.collectAsState()
    val chatController = remember(state.stream?.id) { VioLiveChatComponentController() }
    val colors = state.configuration.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.overlayBackground.toColor()),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            videoContent(state.stream)
        }

        AnimatedVisibility(
            visible = state.isVisible && state.stream != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LiveShowChrome(
                state = state,
                controller = controller,
                chatController = chatController,
                configuration = state.configuration,
            )
        }

        DynamicComponentLayer(
            components = state.activeComponents,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.stream != null && state.configuration.layout.showLikes) {
            VioLiveLikesComponent(
                manager = controller.likesManager,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoxScope.LiveShowChrome(
    state: LiveShowOverlayState,
    controller: VioLiveShowOverlayController,
    chatController: VioLiveChatComponentController,
    configuration: VioLiveShowConfiguration,
) {
    val stream = state.stream ?: return
    val colors = configuration.colors
    val layout = configuration.layout
    val spacing = configuration.spacing
    val typography = configuration.typography
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.contentPadding.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (layout.showControls) {
            LiveShowTopBar(
                stream = stream,
                isMuted = state.isMuted,
                controller = controller,
                colors = colors,
                typography = typography,
                spacing = spacing,
                layout = layout,
            )
            Spacer(Modifier.height(spacing.controlsSpacing.dp / 2))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (layout.showChat) {
                VioLiveChatComponent(
                    controller = chatController,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.chatBackground.toColor(), RoundedCornerShape(16.dp))
                        .padding(spacing.chatPadding.dp),
                )
            }
            if (layout.showProducts && stream.featuredProducts.isNotEmpty()) {
                ProductsSlider(
                    products = stream.featuredProducts,
                    colors = colors,
                    typography = typography,
                    onProductClick = {
                        controller.selectProduct(it.asProduct)
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.contentPadding.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = { controller.sendHeart(stream.isLive) }) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = "Send heart",
                        tint = colors.liveBadgeColor.toColor(),
                    )
                }
            }
            if (layout.showProducts) {
                state.selectedProduct?.let { selected ->
                    SelectedProductSheet(
                        product = selected,
                        onDismiss = controller::dismissProductDetail,
                        onAddToCart = controller::addSelectedProductToCart,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveShowTopBar(
    stream: LiveStream,
    isMuted: Boolean,
    controller: VioLiveShowOverlayController,
    colors: VioLiveShowConfiguration.Colors,
    typography: VioLiveShowConfiguration.Typography,
    spacing: VioLiveShowConfiguration.Spacing,
    layout: VioLiveShowConfiguration.Layout,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (layout.showCloseButton) {
                IconButton(onClick = { controller.dismissOverlay() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = colors.controlsTint.toColor(),
                    )
                }
            }
            Column {
                Text(
                    text = stream.title,
                    color = colors.controlsTint.toColor(),
                    fontSize = typography.streamTitleSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "by ${stream.streamer.name}",
                    color = colors.controlsTint.toColor().copy(alpha = 0.8f),
                    fontSize = typography.streamSubtitleSize.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LiveBadge(viewers = stream.viewerCount, badgeColor = colors.liveBadgeColor.toColor(), tint = colors.controlsTint.toColor())
            IconButton(onClick = { controller.toggleMute() }) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mute toggle",
                    tint = colors.controlsTint.toColor(),
                )
            }
        }
    }
}

@Composable
private fun LiveBadge(viewers: Int, badgeColor: Color, tint: Color) {
    Surface(
        color = badgeColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = tint,
            )
            Text(
                text = "LIVE · $viewers",
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        }
    }
}

@Composable
private fun ProductsSlider(
    products: List<LiveProduct>,
    colors: VioLiveShowConfiguration.Colors,
    typography: VioLiveShowConfiguration.Typography,
    onProductClick: (LiveProduct) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.productsBackground.toColor(), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Featured products",
            color = colors.controlsTint.toColor(),
            fontSize = typography.productTitleSize.sp,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                VioLiveProductCard(
                    product = product,
                    modifier = Modifier.width(220.dp),
                    onClick = { onProductClick(product) },
                )
            }
        }
    }
}

@Composable
internal fun DynamicComponentLayer(
    components: List<RenderedComponent>,
    modifier: Modifier = Modifier,
) {
    val dismissed = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(components) {
        val ids = components.map { it.id }.toSet()
        dismissed.keys.retainAll(ids)
    }
    Box(modifier = modifier.fillMaxSize()) {
        components.forEach { component ->
            if (dismissed[component.id] == true) return@forEach
            when (component) {
                is RenderedComponent.Banner -> BannerCard(
                    data = component,
                    modifier = Modifier
                        .align(component.position.alignment())
                        .padding(16.dp),
                    onClose = { dismissed[component.id] = true },
                )
                is RenderedComponent.FeaturedProduct -> FeaturedProductCard(
                    data = component,
                    modifier = Modifier
                        .align(component.position.alignment())
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun BannerCard(
    data: RenderedComponent.Banner,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                if (!data.text.isNullOrBlank()) {
                    Text(
                        text = data.text,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss banner",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(
    data: RenderedComponent.FeaturedProduct,
    modifier: Modifier = Modifier,
) {
    val imageUrl = data.product.images.firstOrNull()?.url
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.65f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VioImage(
                url = imageUrl,
                contentDescription = data.product.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                imageLoader = VioImageLoaderDefaults.current,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.product.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = data.product.price.formattedPrice(),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.DefaultVideoPlaceholder(stream: LiveStream?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1B1B), Color(0xFF000000)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (stream != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stream.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Waiting for video...",
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        } else {
            Text(
                text = "No live stream",
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

private fun DynamicComponentPosition?.alignment(): Alignment = when (this) {
    DynamicComponentPosition.TOP -> Alignment.TopCenter
    DynamicComponentPosition.BOTTOM -> Alignment.BottomCenter
    DynamicComponentPosition.TOP_CENTER -> Alignment.TopCenter
    DynamicComponentPosition.CENTER -> Alignment.Center
    DynamicComponentPosition.BOTTOM_CENTER -> Alignment.BottomCenter
    DynamicComponentPosition.CUSTOM, null -> Alignment.Center
}
