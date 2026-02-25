package io.reachu.VioUI.Components

import io.reachu.sdk.domain.models.ProductDto

class VioProductCard(
    private val product: ProductDto,
    private val config: VioProductCardConfig = VioProductCardConfig(),
    private val onTap: (() -> Unit)? = null,
    private val onAddToCart: ((io.reachu.VioUI.Managers.Variant?, Int) -> Unit)? = null,
) {

    val state: VioProductCardState by lazy { product.toVioProductCardState(config) }

    fun tap() {
        onTap?.invoke()
    }

    fun addToCart(variant: io.reachu.VioUI.Managers.Variant? = null, quantity: Int = 1) {
        onAddToCart?.invoke(variant, quantity)
    }

}
