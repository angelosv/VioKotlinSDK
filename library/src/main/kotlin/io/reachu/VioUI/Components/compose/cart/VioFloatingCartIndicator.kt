package io.reachu.VioUI.Components.compose.cart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.reachu.VioCore.configuration.FloatingCartDisplayMode
import io.reachu.VioCore.configuration.FloatingCartPosition
import io.reachu.VioCore.configuration.FloatingCartSize
import io.reachu.VioCore.configuration.RLocalizedString
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioUI.Components.VioFloatingCartIndicator as IndicatorConfig
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.itemCount
import kotlin.math.max

private fun String.toColor(): Color = toVioColor()

@Composable
fun VioFloatingCartIndicator(
    cartManager: CartManager,
    position: IndicatorConfig.Position? = null,
    displayMode: IndicatorConfig.DisplayMode? = null,
    size: IndicatorConfig.Size? = null,
    customPadding: PaddingValues? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isCampaignGated: Boolean = true,
) {
    val cfgState = VioConfiguration.shared.state.value
    val cartCfg = cfgState.cart
    val showWhenEmpty = cartCfg.alwaysShowFloatingCart
    val campaignManager = remember { CampaignManager.shared }
    val campaignActive by campaignManager.isCampaignActive.collectAsState(initial = true)

    val shouldShow = if (isCampaignGated) {
        VioConfiguration.shared.shouldUseSDK && campaignActive
    } else {
        true
    }
    
    if (!shouldShow) return

    val itemCount = cartManager.itemCount
    if (!showWhenEmpty && itemCount <= 0) return

    val effectivePosition = position ?: cartCfg.floatingCartPosition.toIndicatorPosition()
    val effectiveDisplay = displayMode ?: cartCfg.floatingCartDisplayMode.toIndicatorDisplayMode()
    val effectiveSize = size ?: cartCfg.floatingCartSize.toIndicatorSize()
    val padding = customPadding ?: defaultPadding(effectivePosition, effectiveSize)

    Box(modifier = modifier) {
        CartIndicatorButton(
            cartManager = cartManager,
            displayMode = effectiveDisplay,
            size = effectiveSize,
            onTap = onTap ?: { cartManager.showCheckout() },
            modifier = Modifier
                .align(effectivePosition.toAlignment())
                .padding(padding)
                .zIndex(1f),
        )
    }
}

@Composable
private fun CartIndicatorButton(
    cartManager: CartManager,
    displayMode: IndicatorConfig.DisplayMode,
    size: IndicatorConfig.Size,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        label = "cartIndicatorPressScale",
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTap()
            },
    ) {
        when (displayMode) {
            IndicatorConfig.DisplayMode.full -> FullModeContent(cartManager, size)
            IndicatorConfig.DisplayMode.compact -> CompactModeContent(cartManager, size)
            IndicatorConfig.DisplayMode.minimal -> MinimalModeContent(cartManager, size)
            IndicatorConfig.DisplayMode.iconOnly -> IconOnlyContent(cartManager, size)
        }
    }
}

@Composable
private fun FullModeContent(cartManager: CartManager, size: IndicatorConfig.Size) {
    IndicatorSurface(size) {
        CartIconWithBadge(cartManager, size)
        Spacer(Modifier.width(if (size == IndicatorConfig.Size.small) VioSpacing.xs.dp else VioSpacing.sm.dp))
        ColumnTextPrice(cartManager)
        Spacer(Modifier.width(VioSpacing.sm.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CompactModeContent(cartManager: CartManager, size: IndicatorConfig.Size) {
    IndicatorSurface(size) {
        CartIconWithBadge(cartManager, size)
        Spacer(Modifier.width(VioSpacing.sm.dp))
        Text(
            text = "${cartManager.currency} ${String.format("%.0f", cartManager.cartTotal)}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MinimalModeContent(cartManager: CartManager, size: IndicatorConfig.Size) {
    IndicatorSurface(size) {
        CartIconWithBadge(cartManager, size)
        Spacer(Modifier.width(VioSpacing.xs.dp))
        Text(
            text = cartManager.itemCount.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IconOnlyContent(cartManager: CartManager, size: IndicatorConfig.Size) {
    val metrics = remember(size) { size.metrics() }
    Box(
        modifier = Modifier
            .size(metrics.circleSize)
            .shadow(metrics.shadowElevation, CircleShape, clip = false)
            .clip(CircleShape)
            .background(cartGradient()),
        contentAlignment = Alignment.Center,
    ) {
        CartIconWithBadge(cartManager, size)
    }
}

@Composable
private fun IndicatorSurface(
    size: IndicatorConfig.Size,
    content: @Composable () -> Unit,
) {
    val metrics = remember(size) { size.metrics() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(metrics.shadowElevation, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(cartGradient())
            .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding),
    ) {
        content()
    }
}

@Composable
private fun CartIconWithBadge(cartManager: CartManager, size: IndicatorConfig.Size) {
    val metrics = remember(size) { size.metrics() }
    val badgeScale = remember { Animatable(1f) }
    val clampedCount = cartManager.itemCount.coerceAtMost(99)
    LaunchedEffect(clampedCount) {
        badgeScale.snapTo(1.0f)
        badgeScale.animateTo(
            1.15f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        )
        badgeScale.animateTo(
            1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 260f),
        )
    }
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(metrics.iconSize),
        )
        Box(
            modifier = Modifier
                .padding(4.dp)
                .scale(badgeScale.value)
                .clip(RoundedCornerShape(50))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = max(clampedCount, 0).toString(),
                color = cartGradientStartColor(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ColumnTextPrice(cartManager: CartManager) {
    val cartLabel = remember { RLocalizedString("cart.title", defaultValue = "Cart") }
    Column {
        Text(
            text = cartLabel,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${cartManager.currency} ${String.format("%.0f", cartManager.cartTotal)}",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val cartGradientColors
    get() = listOf(
        VioColors.primary.toColor(),
        VioColors.primary.toColor().copy(alpha = 0.85f),
    )

private fun cartGradientStartColor(): Color = cartGradientColors.first()

private fun cartGradient(): Brush = Brush.horizontalGradient(cartGradientColors)

private fun defaultPadding(
    position: IndicatorConfig.Position,
    size: IndicatorConfig.Size,
): PaddingValues {
    val horizontal = if (size == IndicatorConfig.Size.small) 12.dp else VioSpacing.md.dp
    val bottom = if (size == IndicatorConfig.Size.small) 90.dp else VioSpacing.xl.dp
    val top = VioSpacing.xl.dp
    return when (position) {
        IndicatorConfig.Position.bottomRight -> PaddingValues(end = horizontal, bottom = bottom)
        IndicatorConfig.Position.bottomLeft -> PaddingValues(start = horizontal, bottom = bottom)
        IndicatorConfig.Position.bottomCenter -> PaddingValues(bottom = bottom, start = horizontal, end = horizontal)
        IndicatorConfig.Position.topRight -> PaddingValues(top = top, end = horizontal)
        IndicatorConfig.Position.topLeft -> PaddingValues(top = top, start = horizontal)
        IndicatorConfig.Position.topCenter -> PaddingValues(top = top, start = horizontal, end = horizontal)
        IndicatorConfig.Position.centerRight -> PaddingValues(end = horizontal)
        IndicatorConfig.Position.centerLeft -> PaddingValues(start = horizontal)
    }
}

private data class IndicatorSizeMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val circleSize: Dp,
    val shadowElevation: Dp,
    val iconSize: Dp,
)

private fun IndicatorConfig.Size.metrics(): IndicatorSizeMetrics = when (this) {
    IndicatorConfig.Size.small -> IndicatorSizeMetrics(
        horizontalPadding = 10.dp,
        verticalPadding = 8.dp,
        circleSize = 52.dp,
        shadowElevation = 2.dp,
        iconSize = 20.dp,
    )
    IndicatorConfig.Size.medium -> IndicatorSizeMetrics(
        horizontalPadding = VioSpacing.lg.dp,
        verticalPadding = VioSpacing.md.dp,
        circleSize = 68.dp,
        shadowElevation = 8.dp,
        iconSize = 26.dp,
    )
    IndicatorConfig.Size.large -> IndicatorSizeMetrics(
        horizontalPadding = VioSpacing.xl.dp,
        verticalPadding = VioSpacing.lg.dp,
        circleSize = 76.dp,
        shadowElevation = 12.dp,
        iconSize = 32.dp,
    )
}

private fun FloatingCartPosition.toIndicatorPosition(): IndicatorConfig.Position = when (this) {
    FloatingCartPosition.TOP_LEFT -> IndicatorConfig.Position.topLeft
    FloatingCartPosition.TOP_CENTER -> IndicatorConfig.Position.topCenter
    FloatingCartPosition.TOP_RIGHT -> IndicatorConfig.Position.topRight
    FloatingCartPosition.CENTER_LEFT -> IndicatorConfig.Position.centerLeft
    FloatingCartPosition.CENTER_RIGHT -> IndicatorConfig.Position.centerRight
    FloatingCartPosition.BOTTOM_LEFT -> IndicatorConfig.Position.bottomLeft
    FloatingCartPosition.BOTTOM_CENTER -> IndicatorConfig.Position.bottomCenter
    FloatingCartPosition.BOTTOM_RIGHT -> IndicatorConfig.Position.bottomRight
}

private fun FloatingCartDisplayMode.toIndicatorDisplayMode(): IndicatorConfig.DisplayMode = when (this) {
    FloatingCartDisplayMode.FULL -> IndicatorConfig.DisplayMode.full
    FloatingCartDisplayMode.COMPACT -> IndicatorConfig.DisplayMode.compact
    FloatingCartDisplayMode.MINIMAL -> IndicatorConfig.DisplayMode.minimal
    FloatingCartDisplayMode.ICON_ONLY -> IndicatorConfig.DisplayMode.iconOnly
}

private fun FloatingCartSize.toIndicatorSize(): IndicatorConfig.Size = when (this) {
    FloatingCartSize.SMALL -> IndicatorConfig.Size.small
    FloatingCartSize.MEDIUM -> IndicatorConfig.Size.medium
    FloatingCartSize.LARGE -> IndicatorConfig.Size.large
}

private fun IndicatorConfig.Position.toAlignment(): Alignment = when (this) {
    IndicatorConfig.Position.bottomRight -> Alignment.BottomEnd
    IndicatorConfig.Position.bottomLeft -> Alignment.BottomStart
    IndicatorConfig.Position.bottomCenter -> Alignment.BottomCenter
    IndicatorConfig.Position.topRight -> Alignment.TopEnd
    IndicatorConfig.Position.topLeft -> Alignment.TopStart
    IndicatorConfig.Position.topCenter -> Alignment.TopCenter
    IndicatorConfig.Position.centerRight -> Alignment.CenterEnd
    IndicatorConfig.Position.centerLeft -> Alignment.CenterStart
}
