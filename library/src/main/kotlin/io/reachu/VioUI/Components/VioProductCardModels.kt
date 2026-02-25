package io.reachu.VioUI.Components

import io.reachu.sdk.domain.models.PriceDto
import io.reachu.sdk.domain.models.ProductDto
import io.reachu.sdk.domain.models.ProductImageDto
import io.reachu.sdk.domain.models.VariantDto
import kotlin.math.roundToInt
import io.reachu.VioUI.Managers.Product as DomainProduct
import io.reachu.VioUI.Managers.Price as DomainPrice

data class VioProductCardConfig(
    val variant: Variant = Variant.GRID,
    val showBrand: Boolean = true,
    val showDescription: Boolean = true,
    val showProductDetail: Boolean = true,
    val currencySymbol: String = "",
) {
    enum class Variant {
        GRID,
        LIST,
        HERO,
        MINIMAL,
    }
}

data class VioProductCardState(
    val productId: Int,
    val title: String,
    val brand: String?,
    val description: String?,
    val primaryImage: String?,
    val imageGallery: List<String>,
    val priceLabel: String,
    val compareAtLabel: String?,
    val currencyCode: String,
    val discountPercentage: Int?,
    val isInStock: Boolean,
    val canShowDescription: Boolean,
    val canShowBrand: Boolean,
    val showProductDetail: Boolean,
) {
    val hasDiscount: Boolean get() = discountPercentage != null || compareAtLabel != null
}

fun ProductDto.toVioProductCardState(
    config: VioProductCardConfig = VioProductCardConfig(),
): VioProductCardState {
    val orderedImages = images.sortedBy { it.order ?: Int.MAX_VALUE }
    val gallery = orderedImages.map(ProductImageDto::url).filter { it.isNotBlank() }

    val isStockAvailable = when {
        quantity != null -> quantity > 0
        variants.isNotEmpty() -> variants.any { (it.quantity ?: 0) > 0 }
        else -> true
    }

    val currencyCode = price.currencyCode.ifBlank { config.currencySymbol }
    val priceLabel = price.formatAmount(currencyCode)
    val compareValue = price.compareAtInclTaxes ?: price.compareAt
    val compareLabel = compareValue?.takeIf { it > 0 }
        ?.let { compare -> buildPriceString(amount = compare, symbol = currencyCode) }
    val discountPercentage = calculateDiscountPercentage(
        current = price.amountInclTaxes ?: price.amount,
        original = compareValue,
    )

    return VioProductCardState(
        productId = id,
        title = title,
        brand = brand,
        description = description,
        primaryImage = gallery.firstOrNull(),
        imageGallery = gallery,
        priceLabel = priceLabel,
        compareAtLabel = compareLabel,
        currencyCode = currencyCode,
        discountPercentage = discountPercentage,
        isInStock = isStockAvailable,
        canShowDescription = config.showDescription && !description.isNullOrBlank(),
        canShowBrand = config.showBrand && !brand.isNullOrBlank(),
        showProductDetail = config.showProductDetail,
    )
}

/**
 * Overload for domain Product (used by UI managers), mirroring the DTO mapping.
 */
fun DomainProduct.toVioProductCardState(
    config: VioProductCardConfig = VioProductCardConfig(),
): VioProductCardState {
    val orderedImages = images.sortedBy { it.order }
    val gallery = orderedImages.map { it.url }.filter { it.isNotBlank() }

    val isStockAvailable = when {
        quantity != null -> quantity > 0
        variants.isNotEmpty() -> variants.any { (it.quantity ?: 0) > 0 }
        else -> true
    }

    val currencyCode = price.currencyCode.ifBlank { config.currencySymbol }
    val priceLabel = price.formatAmount(currencyCode)
    val compareValue = price.compareAtInclTaxes ?: price.compareAt
    val compareLabel = compareValue?.takeIf { it > 0f }
        ?.let { compare -> buildPriceString(amount = compare.toDouble(), symbol = currencyCode) }
    val discountPercentage = calculateDiscountPercentage(
        current = (price.amountInclTaxes ?: price.amount).toDouble(),
        original = compareValue?.toDouble(),
    )

    return VioProductCardState(
        productId = id,
        title = title,
        brand = brand,
        description = description,
        primaryImage = gallery.firstOrNull(),
        imageGallery = gallery,
        priceLabel = priceLabel,
        compareAtLabel = compareLabel,
        currencyCode = currencyCode,
        discountPercentage = discountPercentage,
        isInStock = isStockAvailable,
        canShowDescription = config.showDescription && !description.isNullOrBlank(),
        canShowBrand = config.showBrand && !brand.isNullOrBlank(),
        showProductDetail = config.showProductDetail,
    )
}

private fun DomainPrice.formatAmount(symbol: String): String {
    val resolvedSymbol = symbol.ifBlank { currencyCode }
    val value = amountInclTaxes ?: amount
    return buildPriceString(amount = value.toDouble(), symbol = resolvedSymbol)
}

private fun PriceDto.formatAmount(symbol: String): String {
    val resolvedSymbol = symbol.ifBlank { currencyCode }
    val value = amountInclTaxes ?: amount
    return buildPriceString(amount = value, symbol = resolvedSymbol)
}

private fun buildPriceString(amount: Double, symbol: String): String {
    val rounded = (amount * 100).roundToInt() / 100.0
    val separator = if (symbol.length > 1) " " else ""
    return "$symbol$separator${String.format("%.2f", rounded)}"
}

private fun calculateDiscountPercentage(current: Double?, original: Double?): Int? {
    val currentPrice = current ?: return null
    val originalPrice = original ?: return null
    if (originalPrice <= currentPrice || originalPrice <= 0.0) return null
    val discount = ((originalPrice - currentPrice) / originalPrice) * 100.0
    return discount.roundToInt().takeIf { it > 0 }
}

fun ProductDto.resolveGalleryWithFallback(): List<String> {
    val ordered = images.sortedBy { it.order ?: Int.MAX_VALUE }
    if (ordered.isNotEmpty()) return ordered.mapNotNull { it.url.takeIf(String::isNotBlank) }

    return variants
        .asSequence()
        .flatMap(VariantDto::images)
        .sortedBy { it.order ?: Int.MAX_VALUE }
        .mapNotNull { it.url.takeIf(String::isNotBlank) }
        .toList()
}
