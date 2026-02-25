package io.reachu.VioUI.Components

import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Components.toVioProductCardState

enum class VioProductSliderLayout(
    val cardVariant: VioProductCardConfig.Variant,
    val cardWidth: Int?,
    val spacingDp: Int,
    val showsDescription: Boolean,
    val showsBrand: Boolean,
    val allowsAddToCart: Boolean,
) {
    COMPACT(
        cardVariant = VioProductCardConfig.Variant.MINIMAL,
        cardWidth = 120,
        spacingDp = 8,
        showsDescription = false,
        showsBrand = true,
        allowsAddToCart = false,
    ),
    CARDS(
        cardVariant = VioProductCardConfig.Variant.GRID,
        cardWidth = 180,
        spacingDp = 12,
        showsDescription = false,
        showsBrand = true,
        allowsAddToCart = true,
    ),
    FEATURED(
        cardVariant = VioProductCardConfig.Variant.HERO,
        cardWidth = 280,
        spacingDp = 16,
        showsDescription = true,
        showsBrand = true,
        allowsAddToCart = true,
    ),
    WIDE(
        cardVariant = VioProductCardConfig.Variant.LIST,
        cardWidth = 320,
        spacingDp = 12,
        showsDescription = true,
        showsBrand = true,
        allowsAddToCart = true,
    ),
    SHOWCASE(
        cardVariant = VioProductCardConfig.Variant.HERO,
        cardWidth = 360,
        spacingDp = 24,
        showsDescription = true,
        showsBrand = true,
        allowsAddToCart = true,
    ),
    MICRO(
        cardVariant = VioProductCardConfig.Variant.MINIMAL,
        cardWidth = 80,
        spacingDp = 4,
        showsDescription = false,
        showsBrand = false,
        allowsAddToCart = false,
    );
}

data class VioProductSliderState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isMarketUnavailable: Boolean = false,
) {
    val hasContent: Boolean get() = products.isNotEmpty()
    val isIdle: Boolean get() = !isLoading && errorMessage == null && !isMarketUnavailable
}

fun VioProductSliderState.toVioProductCardStates(
    layout: VioProductSliderLayout,
    currencySymbol: String = "",
): List<VioProductCardState> {
    val config = VioProductCardConfig(
        variant = layout.cardVariant,
        showBrand = layout.showsBrand,
        showDescription = layout.showsDescription,
        currencySymbol = currencySymbol,
    )
    return products.map { it.toVioProductCardState(config) }
}
