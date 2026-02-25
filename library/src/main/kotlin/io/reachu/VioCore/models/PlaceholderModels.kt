package io.reachu.VioCore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Cart(
    val id: String,
    val items: List<CartItem> = emptyList(),
)

@Serializable
data class CartItem(
    val id: String,
    val productId: Int,
    val quantity: Int,
)

@Serializable
data class CartCost(
    val subtotal: Float,
    val tax: Float,
    val shipping: Float,
    val total: Float,
)

@Serializable
data class Checkout(
    val id: String,
    val cartId: String,
)

@Serializable
data class Address(
    val street: String,
    val city: String,
    val country: String,
)

@Serializable
data class ShippingRate(
    val id: String,
    val name: String,
    val price: Float,
)

@Serializable
data class CheckoutValidation(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
)

@Serializable
data class Order(
    val id: String,
    val status: String,
)

@Serializable
data class OrderTracking(
    val orderId: String,
    val status: String,
)

@Serializable
data class OrderLine(
    val id: String,
    val productId: Int,
    val quantity: Int,
)

@Serializable
enum class ReturnReason {
    @SerialName("defective")
    DEFECTIVE,

    @SerialName("wrong_item")
    WRONG_ITEM,

    @SerialName("not_as_described")
    NOT_AS_DESCRIBED,

    @SerialName("other")
    OTHER,
}

@Serializable
data class ReturnRequest(
    val id: String,
    val orderId: String,
    val reason: ReturnReason,
)

@Serializable
data class PaymentMethod(
    val id: String,
    val type: String,
    val name: String,
)

@Serializable
data class Payment(
    val id: String,
    val status: PaymentStatus,
    val amount: Float,
)

@Serializable
enum class PaymentStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("processing")
    PROCESSING,

    @SerialName("completed")
    COMPLETED,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,
}

@Serializable
data class ApplePayRequest(
    val amount: Float,
    val currency: String,
)

@Serializable
data class PaymentMethodValidation(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
)

@Serializable
data class ProductFilters(
    val categoryId: String? = null,
    val brandId: String? = null,
    val priceMin: Float? = null,
    val priceMax: Float? = null,
)
