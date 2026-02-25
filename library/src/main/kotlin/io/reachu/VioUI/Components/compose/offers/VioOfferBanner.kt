package io.reachu.VioUI.Components.compose.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioCore.models.OfferBannerConfig
import io.reachu.VioUI.Components.VioOfferBanner
import io.reachu.VioUI.Components.TimeParts
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import io.reachu.VioUI.Components.compose.utils.toVioColor
import io.reachu.VioUI.Components.compose.common.SponsorBadgeContainer

private fun String.toColorOr(default: Color): Color = runCatching {
    toVioColor()
}.getOrElse { default }

fun interface ExternalOfferNavigator {
    fun open(link: String)
}

@Composable
fun VioOfferBanner(
    config: OfferBannerConfig,
    modifier: Modifier = Modifier,
    banner: VioOfferBanner = remember(config) { VioOfferBanner(config) },
    timePartsFlow: Flow<TimeParts>? = null,
    navigator: ExternalOfferNavigator? = null,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    deeplinkOverride: String? = null,
    height: Dp? = null,
    titleFontSize: TextUnit? = null,
    subtitleFontSize: TextUnit? = null,
    badgeFontSize: TextUnit? = null,
    buttonFontSize: TextUnit? = null,
    onNavigateToStore: (() -> Unit)? = null,
    onButtonClick: (() -> Unit)? = null,
    showSponsor: Boolean = false,
    sponsorPosition: String? = "topRight",
    sponsorLogoUrl: String? = null,
) {
    var timeRemaining by remember { mutableStateOf<TimeParts?>(null) }
    val countdownFlow = remember(timePartsFlow, config.countdownEndDate) {
        timePartsFlow ?: banner.countdownFlow()
    }
    LaunchedEffect(countdownFlow) {
        countdownFlow.collectLatest { timeRemaining = it }
    }

    val shape = RoundedCornerShape(12.dp)
    val resolvedHeight = height ?: 160.dp
    val backgroundUrl = banner.buildFullURL(config.backgroundImageUrl)
    val backgroundColor = config.backgroundColor?.toColorOr(Color(0xFF1B1B1F)) ?: Color(0xFF1B1B1F)
    val overlayAlpha = ((config.overlayOpacity ?: 0.4).coerceIn(0.0, 1.0)).toFloat()
    val campaignManager = remember { CampaignManager.shared }

    val titleSize = titleFontSize ?: 24.sp
    val subtitleSize = subtitleFontSize ?: 11.sp
    val badgeSize = badgeFontSize ?: 18.sp
    val buttonTextSize = buttonFontSize ?: 12.sp
    val hasCta = onNavigateToStore != null ||
        !deeplinkOverride.isNullOrBlank() ||
        !config.deeplinkUrl.isNullOrBlank() ||
        !config.ctaLink.isNullOrBlank()

    LaunchedEffect(config) {
        trackOfferBannerView(config, campaignManager)
    }

    SponsorBadgeContainer(
        showSponsor = showSponsor,
        sponsorPosition = sponsorPosition,
        sponsorLogoUrl = sponsorLogoUrl,
        imageLoader = imageLoader,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(resolvedHeight)
                .padding(bottom = 8.dp)
                .shadow(elevation = 8.dp, shape = shape)
                .clip(shape)
                .background(backgroundColor)
        ) {
            if (backgroundUrl != null) {
                VioImage(
                    url = backgroundUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    imageLoader = imageLoader,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = overlayAlpha),
                                    Color.Black.copy(alpha = overlayAlpha * 0.5f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!config.logoUrl.isNullOrBlank()) {
                        VioImage(
                            url = banner.buildFullURL(config.logoUrl),
                            contentDescription = null,
                            modifier = Modifier.height(16.dp),
                            imageLoader = imageLoader,
                        )
                    }

                    Text(text = config.title, fontSize = titleSize, fontWeight = FontWeight.Bold, color = Color.White)
                    config.subtitle?.let { sub ->
                        Text(text = sub, fontSize = subtitleSize, color = Color.White.copy(alpha = 0.9f))
                    }

                    timeRemaining?.let { AnalogCountdown(parts = it) }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = config.discountBadgeText,
                        color = Color.White,
                        fontSize = badgeSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    val bg = banner.resolvedButtonColorHex().toColorOr(Color(0xFF800080))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(bg)
                            .clickable(enabled = hasCta) {
                                handleOfferBannerCta(
                                    config = config,
                                    deeplinkOverride = deeplinkOverride,
                                    navigator = navigator,
                                    onNavigateToStore = onNavigateToStore,
                                    campaignManager = campaignManager,
                                )
                                onButtonClick?.invoke()
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = config.ctaText.ifBlank { "Shop now" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = buttonTextSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalogCountdown(parts: TimeParts) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CountdownPill("${parts.days}d")
        CountdownPill("${parts.hours}h")
        CountdownPill("${parts.minutes}m")
        CountdownPill("${parts.seconds}s")
    }
}

@Composable
private fun CountdownPill(label: String) {
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun trackOfferBannerView(config: OfferBannerConfig, campaignManager: CampaignManager) {
    val componentId = resolveOfferBannerComponentId(campaignManager)
    val campaignId = campaignManager.currentCampaign.value?.id
    val metadata = mapOf(
        "has_countdown" to config.countdownEndDate.isNotBlank(),
        "has_logo" to !config.logoUrl.isNullOrBlank(),
        "has_background_image" to !config.backgroundImageUrl.isNullOrBlank(),
        "has_background_color" to !config.backgroundColor.isNullOrBlank(),
    )
    AnalyticsManager.trackComponentView(
        componentId = componentId,
        componentType = "offer_banner",
        componentName = config.title,
        campaignId = campaignId,
        metadata = metadata,
    )
}

private fun handleOfferBannerCta(
    config: OfferBannerConfig,
    deeplinkOverride: String?,
    navigator: ExternalOfferNavigator?,
    onNavigateToStore: (() -> Unit)?,
    campaignManager: CampaignManager,
) {
    val componentId = resolveOfferBannerComponentId(campaignManager)
    val campaignId = campaignManager.currentCampaign.value?.id
    val (action, metadata, target) = when {
        onNavigateToStore != null -> Triple(
            "navigate_to_store",
            mapOf("deeplink_type" to "in_app"),
            null,
        )
        !deeplinkOverride.isNullOrBlank() -> Triple(
            "custom_deeplink",
            mapOf("deeplink" to deeplinkOverride),
            deeplinkOverride,
        )
        !config.deeplinkUrl.isNullOrBlank() -> Triple(
            "deeplink",
            mapOf(
                "deeplink" to config.deeplinkUrl,
                "deeplink_action" to config.deeplinkAction,
            ),
            config.deeplinkUrl,
        )
        !config.ctaLink.isNullOrBlank() -> Triple(
            "external_link",
            mapOf("link" to config.ctaLink),
            config.ctaLink,
        )
        else -> return
    }
    AnalyticsManager.trackComponentClick(
        componentId = componentId,
        componentType = "offer_banner",
        action = action,
        componentName = config.title,
        campaignId = campaignId,
        metadata = metadata,
    )
    when (action) {
        "navigate_to_store" -> onNavigateToStore?.invoke()
        else -> target?.let { navigator?.open(it) }
    }
}

private fun resolveOfferBannerComponentId(campaignManager: CampaignManager): String {
    return campaignManager.getActiveComponent(type = "offer_banner")?.id
        ?: campaignManager.getActiveComponent(type = "countdown")?.id
        ?: "unknown"
}
