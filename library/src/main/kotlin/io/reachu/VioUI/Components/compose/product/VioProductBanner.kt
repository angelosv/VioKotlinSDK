package io.reachu.VioUI.Components.compose.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioUI.Components.VioProductBanner
import io.reachu.VioUI.Components.VioProductBannerState
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Helpers.LoadedImage
import io.reachu.VioUI.Components.compose.common.SponsorBadgeContainer
import java.net.URI
import io.reachu.VioCore.configuration.VioConfiguration

@Composable
fun VioProductBanner(
    componentId: String? = null,
    modifier: Modifier = Modifier,
    controller: VioProductBanner = remember(componentId) { VioProductBanner(componentId) },
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    onBannerClick: (VioProductBannerState) -> Unit = {},
    onCtaClick: (VioProductBannerState) -> Unit = {},
    showSponsor: Boolean = false,
    sponsorPosition: String? = "topRight",
    sponsorLogoUrl: String? = null,
    imageBackgroundColor: Color = Color.White,
    isCampaignGated: Boolean = true,
) {
    if (!VioConfiguration.shared.shouldUseSDK) return

    val campaignManager = remember { CampaignManager.shared }
    val isCampaignActive by campaignManager.isCampaignActive.collectAsState(initial = true)
    val activeComponents by campaignManager.activeComponents.collectAsState(initial = emptyList())
    val currentCampaign by campaignManager.currentCampaign.collectAsState(initial = null)
    
    val shouldShowComponent = remember(isCampaignActive, activeComponents, currentCampaign, isCampaignGated) {
        if (!isCampaignGated) return@remember true
        val state = VioConfiguration.shared.state.value
        val campaignId = state.liveShow.campaignId
        if (campaignId <= 0) return@remember true
        if (!isCampaignActive || currentCampaign?.isPaused == true) return@remember false
        if (activeComponents.isEmpty()) return@remember true
        activeComponents.any { component ->
            val type = component.type?.lowercase()
            type == "product_banner" || type == "banner"
        }
    }
    
    if (!shouldShowComponent) return

    val state by controller.state.collectAsState()
    if (!state.isVisible && isCampaignGated) return

    val titleColor = state.titleColor.toComposeColor(default = Color.White)
    val subtitleColor = state.subtitleColor.toComposeColor(default = Color.White.copy(alpha = 0.85f))
    val buttonBackground = state.buttonBackgroundColor.toComposeColor(default = Color.White)
    val buttonTextColor = state.buttonTextColor.toComposeColor(default = Color.Black)
    val overlayStart = Color.Black.copy(alpha = state.overlayOpacity.toFloat().coerceIn(0f, 1f))
    val overlayBrush = Brush.verticalGradient(
        colors = listOf(overlayStart, Color.Transparent),
    )
    val contentAlignment = resolveAlignment(
        horizontal = state.textAlignment,
        vertical = state.contentVerticalAlignment,
    )
    val textAlign = resolveTextAlign(state.textAlignment)

    val height = state.bannerHeightDp.coerceIn(150, 400).dp
    SponsorBadgeContainer(
        showSponsor = showSponsor,
        sponsorPosition = sponsorPosition,
        sponsorLogoUrl = sponsorLogoUrl,
        imageLoader = imageLoader,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                trackBannerClick(state)
                onBannerClick(state)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(VioBorderRadius.extraLarge.dp))
                .background(imageBackgroundColor),
        ) {
            LoadedImage(
                url = state.backgroundImageUrl,
                contentDescription = state.title,
                modifier = Modifier.fillMaxSize(),
                imageLoader = imageLoader,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush),
            )
            Column(
                horizontalAlignment = when (textAlign) {
                    TextAlign.Center -> Alignment.CenterHorizontally
                    TextAlign.Right -> Alignment.End
                    else -> Alignment.Start
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(contentAlignment)
                    .padding(24.dp),
            ) {
                Text(
                    text = state.title,
                    color = titleColor,
                    textAlign = textAlign,
                    fontSize = state.titleFontSizeSp.dp.value.sp,
                    style = MaterialTheme.typography.headlineSmall,
                )
                state.subtitle?.let {
                    Text(
                        text = it,
                        fontSize = state.subtitleFontSizeSp.dp.value.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor,
                        textAlign = textAlign,
                    )
                }
                Spacer(Modifier.height(VioSpacing.sm.dp))
                Button(
                    onClick = {
                        trackBannerClick(state)
                        onCtaClick(state)
                    },
                    modifier = Modifier.align(Alignment.Start),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBackground,
                        contentColor = buttonTextColor,
                    ),
                ) {
                    Text(state.buttonText, fontSize = state.buttonFontSizeSp.dp.value.sp)
                }
            }
        }
    }
}

private fun resolveAlignment(horizontal: String, vertical: String): Alignment {
    val h = horizontal.lowercase()
    val v = vertical.lowercase()
    return when {
        v == "top" && h == "center" -> Alignment.TopCenter
        v == "top" && h == "trailing" -> Alignment.TopEnd
        v == "center" && h == "center" -> Alignment.Center
        v == "center" && h == "trailing" -> Alignment.CenterEnd
        v == "bottom" && h == "center" -> Alignment.BottomCenter
        v == "bottom" && h == "trailing" -> Alignment.BottomEnd
        v == "center" && h == "leading" -> Alignment.CenterStart
        v == "top" -> Alignment.TopStart
        v == "bottom" -> Alignment.BottomStart
        else -> Alignment.CenterStart
    }
}

private fun resolveTextAlign(alignment: String): TextAlign {
    return when (alignment.lowercase()) {
        "center" -> TextAlign.Center
        "right", "trailing" -> TextAlign.Right
        else -> TextAlign.Left
    }
}

private fun String?.toComposeColor(default: Color): Color {
    if (this == null) return default
    val trimmed = trim()
    return when {
        trimmed.startsWith("rgba", ignoreCase = true) -> parseRgba(trimmed) ?: default
        else -> this.toVioColor().takeIf { it != Color.Unspecified } ?: default
    }
}

private fun parseRgba(value: String): Color? {
    val content = value.substringAfter("(").substringBefore(")")
    val parts = content.split(",").map { it.trim() }
    if (parts.size < 3) return null
    val r = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
    val g = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
    val b = parts.getOrNull(2)?.toDoubleOrNull() ?: return null
    val a = parts.getOrNull(3)?.toDoubleOrNull() ?: 1.0
    val normalize: (Double) -> Float = { valueComponent ->
        (if (valueComponent > 1) valueComponent / 255.0 else valueComponent).toFloat()
    }
    return Color(
        red = normalize(r),
        green = normalize(g),
        blue = normalize(b),
        alpha = a.toFloat().coerceIn(0f, 1f),
    )
}

private fun trackBannerClick(state: VioProductBannerState) {
    val componentId = state.componentId ?: return
    val actionType = resolveBannerAction(state)
    AnalyticsManager.trackComponentClick(
        componentId = componentId,
        componentType = "product_banner",
        action = actionType,
        componentName = state.componentName,
        campaignId = state.campaignId,
        metadata = mapOf("product_id" to state.productId),
    )
    if (actionType == "product_detail" && !state.productId.isNullOrBlank()) {
        AnalyticsManager.trackProductViewed(
            productId = state.productId,
            productName = state.title,
            productPrice = null,
            productCurrency = null,
            source = "product_banner",
            componentId = componentId,
            componentType = "product_banner",
        )
    }
}

private fun resolveBannerAction(state: VioProductBannerState): String {
    return when {
        state.deeplinkUrl.isValidUrl() -> "deeplink"
        state.buttonLink.isValidUrl() -> "cta_link"
        else -> "product_detail"
    }
}

private fun String?.isValidUrl(): Boolean {
    if (this.isNullOrBlank()) return false
    return runCatching { URI(this) }.getOrNull()?.scheme?.isNotBlank() == true
}
