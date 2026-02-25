package io.reachu.VioUI.Components

import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioCore.models.Component
import io.reachu.VioCore.models.ProductBannerConfig
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class VioProductBannerState(
    val productId: String? = null,
    val title: String = "",
    val subtitle: String? = null,
    val buttonText: String = "",
    val buttonLink: String? = null,
    val deeplinkUrl: String? = null,
    val backgroundImageUrl: String? = null,
    val titleColor: String? = null,
    val subtitleColor: String? = null,
    val buttonBackgroundColor: String? = null,
    val buttonTextColor: String? = null,
    val backgroundColor: String? = null,
    val overlayOpacity: Double = 0.5,
    val textAlignment: String = "leading",
    val contentVerticalAlignment: String = "bottom",
    val isVisible: Boolean = false,
    val bannerHeightDp: Int = 200,
    val titleFontSizeSp: Int = 14,
    val subtitleFontSizeSp: Int = 10,
    val buttonFontSizeSp: Int = 14,
    val componentId: String? = null,
    val componentName: String? = null,
    val campaignId: Int? = null,
    val hasBackgroundImage: Boolean = false,
)

class VioProductBanner(
    private val componentId: String? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val campaignManager: CampaignManager = CampaignManager.shared,
) {

    private val _state = MutableStateFlow(VioProductBannerState())
    val state: StateFlow<VioProductBannerState> = _state.asStateFlow()

    private val lastConfigId = AtomicReference<String?>(null)
    private val lastTrackedViewId = AtomicReference<String?>(null)
    private var latestConfig: ProductBannerConfig? = null
    private var currentComponent: Component? = null

    init {
        scope.launch {
            campaignManager.activeComponents.collectLatest { components ->
                val component = components.findComponent("product_banner", componentId)
                if (component == null || !VioConfiguration.shared.shouldUseSDK) {
                    hide()
                    return@collectLatest
                }
                val config = component.decodeConfig<ProductBannerConfig>()
                if (config == null) {
                    hide()
                    return@collectLatest
                }
                handleConfig(component, config)
            }
        }
    }

    private fun handleConfig(component: Component, config: ProductBannerConfig) {
        val configId = buildConfigId(config)
        if (lastConfigId.get() == configId) return
        lastConfigId.set(configId)
        latestConfig = config
        currentComponent = component
        val imageUrl = resolveAssetUrl(config.backgroundImageUrl)
        val bannerHeight = config.bannerHeight?.coerceIn(150, 400) ?: 200
        val titleFont = config.titleFontSize?.coerceIn(10, 18) ?: 14
        val subtitleFont = config.subtitleFontSize?.coerceIn(8, 12) ?: 10
        val buttonFont = config.buttonFontSize?.coerceIn(12, 16) ?: 14
        val state = VioProductBannerState(
            productId = config.productId,
            title = config.title,
            subtitle = config.subtitle,
            buttonText = config.ctaText,
            buttonLink = config.ctaLink,
            deeplinkUrl = config.deeplink,
            backgroundImageUrl = imageUrl,
            titleColor = config.titleColor,
            subtitleColor = config.subtitleColor,
            buttonBackgroundColor = config.buttonBackgroundColor,
            buttonTextColor = config.buttonTextColor,
            backgroundColor = config.backgroundColor,
            overlayOpacity = config.overlayOpacity ?: 0.5,
            textAlignment = config.textAlignment ?: "leading",
            contentVerticalAlignment = config.contentVerticalAlignment ?: "bottom",
            isVisible = true,
            bannerHeightDp = bannerHeight,
            titleFontSizeSp = titleFont,
            subtitleFontSizeSp = subtitleFont,
            buttonFontSizeSp = buttonFont,
            componentId = component.id,
            componentName = component.name,
            campaignId = campaignManager.currentCampaign.value?.id,
            hasBackgroundImage = !imageUrl.isNullOrBlank(),
        )
        _state.value = state
        trackComponentView(component, config, configId, imageUrl)
    }

    private fun hide() {
        _state.value = VioProductBannerState(isVisible = false)
        lastConfigId.set(null)
        latestConfig = null
        currentComponent = null
        lastTrackedViewId.set(null)
    }

    private fun buildConfigId(config: ProductBannerConfig): String {
        return listOf(
            config.productId,
            config.backgroundImageUrl,
            config.title,
            config.subtitle,
            config.buttonBackgroundColor,
            config.buttonTextColor,
        ).joinToString("|")
    }

    private fun resolveAssetUrl(path: String?): String? = 
        io.reachu.VioCore.utils.UrlUtils.resolveAssetUrl(path)

    fun refresh() {
        val config = latestConfig
        val component = currentComponent
        if (config != null && component != null) {
            handleConfig(component, config)
        }
    }

    private fun trackComponentView(
        component: Component,
        config: ProductBannerConfig,
        configId: String,
        backgroundUrl: String?,
    ) {
        if (lastTrackedViewId.get() == configId) return
        lastTrackedViewId.set(configId)
        AnalyticsManager.trackComponentView(
            componentId = component.id,
            componentType = "product_banner",
            componentName = config.title,
            campaignId = campaignManager.currentCampaign.value?.id,
            metadata = mapOf(
                "product_id" to config.productId,
                "has_background_image" to (!backgroundUrl.isNullOrBlank()),
            ),
        )
    }
}
