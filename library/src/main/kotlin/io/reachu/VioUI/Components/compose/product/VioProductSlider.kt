package io.reachu.VioUI.Components.compose.product

import io.reachu.VioUI.Components.compose.common.SponsorBadgeContainer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioLocalization
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioCore.configuration.TypographyToken
import io.reachu.VioUI.Components.VioProductCardConfig
import io.reachu.VioUI.Components.VioProductCardState
import io.reachu.VioUI.Components.toVioProductCardState
import io.reachu.VioUI.Components.VioProductSliderLayout
import io.reachu.VioUI.Components.slider.VioProductSliderViewModel
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Managers.addProduct
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Components.compose.utils.toVioColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL

private fun String.toColor(): Color = toVioColor()

@Composable
fun VioProductSlider(
    cartManager: CartManager,
    title: String? = null,
    products: List<Product>? = null,
    categoryId: Int? = null,
    layout: VioProductSliderLayout = VioProductSliderLayout.CARDS,
    showSeeAll: Boolean = false,
    maxItems: Int? = null,
    onProductTap: ((Product) -> Unit)? = null,
    onProductCardTap: ((VioProductCardState) -> Unit)? = null,
    onAddToCart: ((Product) -> Unit)? = null,
    onSeeAllTap: (() -> Unit)? = null,
    currency: String? = null,
    country: String? = null,
    imageSize: String = "large",
    showSponsor: Boolean = false,
    sponsorPosition: String? = "topRight",
    sponsorLogoUrl: String? = null,
    imageBackgroundColor: Color = Color.White,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    htmlRenderer: VioHtmlRenderer = VioPlainTextRenderer,
    isCampaignGated: Boolean = true,
    isLoading: Boolean? = null,
) {
    val cfg = VioConfiguration.shared.state.value
    val resolvedCurrency = (currency ?: cartManager.currency).ifBlank { cfg.market.currencyCode }
    val resolvedCountry = (country ?: cartManager.country).ifBlank { cfg.market.countryCode }

    if (!VioConfiguration.shared.shouldUseSDK) {
        return
    }

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
            type == "product_slider" || type == "recommended_products" || type == "products"
        }
    }
    if (!shouldShowComponent) {
        return
    }

    val controller = remember { VioProductSliderViewModel() }
    val sliderState by controller.state.collectAsState()
    val seeAllLabel = remember { VioLocalization.string("common.continue", defaultValue = "Continue") }
    val loadingLabel = remember { VioLocalization.string("common.loading", defaultValue = "Loading...") }
    val retryLabel = remember { VioLocalization.string("common.retry", defaultValue = "Retry") }
    val errorLabel = remember { VioLocalization.string("common.error", defaultValue = "Error") }

    val autoKey = remember(resolvedCurrency, resolvedCountry, categoryId, cartManager.isMarketReady) {
        "${resolvedCurrency}|${resolvedCountry}|${categoryId ?: -1}|${cartManager.isMarketReady}"
    }
    LaunchedEffect(autoKey) {
        if (!cartManager.isMarketReady) return@LaunchedEffect

        if (products == null) {
            controller.loadProducts(
                categoryId = categoryId,
                currency = resolvedCurrency,
                country = resolvedCountry,
                forceRefresh = true,
            )
        }
    }

    var sliderScaleState by remember { mutableStateOf(1f) }
    val sliderScale by animateFloatAsState(targetValue = sliderScaleState, label = "sliderScale")

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .scale(sliderScale)
    ) {
        if (title != null || showSeeAll) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title ?: "",
                    style = VioTypography.headline.toComposeTextStyle(),
                    color = VioColors.textPrimary.toColor()
                )
                if (showSeeAll && onSeeAllTap != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSeeAllTap() }) {
                        Text(
                            text = seeAllLabel,
                            color = VioColors.primary.toColor(),
                            style = VioTypography.callout.toComposeTextStyle(),
                        )
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = VioColors.primary.toColor(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (products == null && sliderState.isMarketUnavailable) {
            return
        }

        val resolvedProducts = products ?: sliderState.products
        val showLoading = isLoading ?: (products == null && sliderState.isLoading && resolvedProducts.isEmpty())
        val hasError = (products == null && sliderState.errorMessage != null && resolvedProducts.isEmpty())

        when {
            showLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = VioSpacing.xl.dp)) {
                    CircularProgressIndicator(color = VioColors.primary.toColor())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$loadingLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            hasError -> {
                val scope = rememberCoroutineScope()
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(sliderState.errorMessage ?: errorLabel, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            controller.reload(
                                categoryId = categoryId,
                                currency = resolvedCurrency,
                                country = resolvedCountry,
                            )
                        }
                    }) { Text(retryLabel) }
                }
            }
            else -> {
                val list: List<Product> = resolvedProducts.let { items ->
                    if (maxItems != null && items.size > maxItems) items.take(maxItems) else items
                }
                val space = layout.spacingDp.dp
                val scope = rememberCoroutineScope()
                val added: MutableState<Int?> = remember { mutableStateOf(null) }
                val detail: MutableState<Product?> = remember { mutableStateOf(null) }
                val cardConfig = remember(layout, cartManager.currencySymbol) {
                    VioProductCardConfig(
                        variant = layout.cardVariant,
                        showBrand = layout.showsBrand,
                        showDescription = layout.showsDescription,
                        currencySymbol = cartManager.currencySymbol,
                    )
                }

                SponsorBadgeContainer(
                    showSponsor = showSponsor,
                    sponsorPosition = sponsorPosition,
                    sponsorLogoUrl = sponsorLogoUrl,
                    imageLoader = imageLoader,
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(space)) {
                        items(list, key = { it.id }) { domain ->
                            val state = domain.toVioProductCardState(cardConfig)
                            val animatedScale by animateFloatAsState(targetValue = if (added.value == domain.id) 1.05f else 1.0f, label = "addScale")

                            val itemShape = RoundedCornerShape(VioBorderRadius.large.dp)
                            val highlight = (added.value == domain.id)
                            val cw = layout.cardWidth
                            val baseModifier = Modifier
                                .then(cw?.let { Modifier.width(it.dp) } ?: Modifier)
                                .scale(animatedScale)
                            Box(
                                modifier = baseModifier
                                    .clip(itemShape)
                                    .then(
                                        if (highlight) Modifier.background(Color.Transparent).border(
                                            width = 2.dp,
                                            color = VioColors.success.toColor(),
                                            shape = itemShape
                                        ) else Modifier
                                    )
                            ) {
                                VioProductCard(
                                    state = state,
                                    variant = layout.cardVariant,
                                    imageBackgroundColor = imageBackgroundColor,
                                    onTap = {
                                        onProductCardTap?.invoke(state)
                                        onProductTap?.invoke(domain)
                                    },
                                    onAddToCart = if (layout.allowsAddToCart) {
                                        { variant, quantity ->
                                            scope.launch {
                                                cartManager.addProduct(domain, variant, quantity)
                                                onAddToCart?.invoke(domain)
                                                added.value = domain.id
                                                sliderScaleState = 1.02f
                                                delay(200)
                                                sliderScaleState = 1.0f
                                                delay(1000)
                                                added.value = null
                                            }
                                        }
                                    } else null,
                                    onShowProductDetail = {
                                        onProductCardTap?.invoke(state)
                                        onProductTap?.invoke(domain)
                                    },
                                    imageLoader = imageLoader,
                                )
                            }
                        }
                    }
                }

                detail.value?.let { selected ->
                    VioProductDetailOverlay(
                        product = selected,
                        currencySymbol = cartManager.currencySymbol,
                        onAddToCart = { _, quantity ->
                            scope.launch { cartManager.addProduct(selected, quantity) }
                        },
                        onDismiss = { detail.value = null },
                        imageLoader = imageLoader,
                        htmlRenderer = htmlRenderer,
                    )
                }
            }
        }
    }
}

private fun TypographyToken.toComposeTextStyle(): TextStyle {
    val weight = when (fontWeight.lowercase()) {
        "bold" -> FontWeight.Bold
        "semibold" -> FontWeight.SemiBold
        "medium" -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    return TextStyle(
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
    )
}
