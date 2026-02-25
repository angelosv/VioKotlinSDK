package io.reachu.VioUI.Components

import io.reachu.sdk.domain.models.ProductDto

class VioProductDetailOverlay(
    private val product: ProductDto,
    private val onDismiss: (() -> Unit)? = null,
    private val onAddToCart: ((ProductDto) -> Unit)? = null,
) {

    var selectedVariantId: String? = null
        private set
    var quantity: Int = 1
        private set

    fun selectVariant(variantId: String?) {
        selectedVariantId = variantId
    }

    fun setQuantity(value: Int) {
        quantity = value.coerceAtLeast(1)
    }

    fun addToCart() {
        onAddToCart?.invoke(product)
    }

    fun dismiss() {
        onDismiss?.invoke()
    }

}
