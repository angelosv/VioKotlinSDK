package io.reachu.VioUI.Managers

/**
 * Kotlin port of the Swift `CartManager.Market` / `CartItem` structures.
 */
data class Market(
    val code: String,
    val name: String,
    val officialName: String? = null,
    val flagURL: String? = null,
    val phoneCode: String,
    val currencyCode: String,
    val currencySymbol: String,
) {
    val id: String = code
}

data class CartItem(
    val id: String,
    val productId: Int,
    val variantId: String? = null,
    val variantTitle: String? = null,
    val title: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val price: Double,
    val currency: String,
    val quantity: Int,
    val sku: String? = null,
    val supplier: String? = null,
    val shippingId: String? = null,
    val shippingName: String? = null,
    val shippingDescription: String? = null,
    val shippingAmount: Double? = null,
    val shippingCurrency: String? = null,
    val availableShippings: List<ShippingOption> = emptyList(),
) {
    data class ShippingOption(
        val id: String,
        val name: String,
        val description: String? = null,
        val amount: Double,
        val currency: String,
    )
}

sealed class CartError(message: String) : IllegalStateException(message) {
    object NoCartId : CartError("No cart ID available")
    object ProductNotFound : CartError("Product not found")
    object InvalidQuantity : CartError("Invalid quantity")
}
