package io.reachu.sdk.domain.models

import com.fasterxml.jackson.annotation.JsonProperty

data class PriceDataDto(
    @JsonProperty("amount") val amount: Double = 0.0,
    @JsonProperty("currency_code") val currencyCode: String = "",
    @JsonProperty("compare_at") val compareAt: Double? = null,
    @JsonProperty("discount") val discount: Double? = null,
    @JsonProperty("amount_incl_taxes") val amountInclTaxes: Double? = null,
    @JsonProperty("compare_at_incl_taxes") val compareAtInclTaxes: Double? = null,
    @JsonProperty("tax_amount") val taxAmount: Double? = null,
    @JsonProperty("tax_rate") val taxRate: Double? = null,
)

data class ShippingPriceDto(
    @JsonProperty("amount") val amount: Double = 0.0,
    @JsonProperty("currency_code") val currencyCode: String = "",
    @JsonProperty("amount_incl_taxes") val amountInclTaxes: Double? = null,
    @JsonProperty("tax_amount") val taxAmount: Double? = null,
    @JsonProperty("tax_rate") val taxRate: Double? = null,
)

data class ShippingDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("price") val price: ShippingPriceDto = ShippingPriceDto(),
)

data class VariantOptionDto(
    @JsonProperty("option") val option: String = "",
    @JsonProperty("value") val value: String = "",
)

data class PriceLineItemAvailableShippingDto(
    @JsonProperty("amount") val amount: Double? = null,
    @JsonProperty("currency_code") val currencyCode: String? = null,
    @JsonProperty("amount_incl_taxes") val amountInclTaxes: Double? = null,
    @JsonProperty("tax_amount") val taxAmount: Double? = null,
    @JsonProperty("tax_rate") val taxRate: Double? = null,
)

data class LineItemAvailableShippingDto(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("country_code") val countryCode: String? = null,
    @JsonProperty("price") val price: PriceLineItemAvailableShippingDto = PriceLineItemAvailableShippingDto(),
)

data class LineItemDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("supplier") val supplier: String = "",
    @JsonProperty("image") val image: List<ProductImageDto>? = null,
    @JsonProperty("sku") val sku: String? = null,
    @JsonProperty("barcode") val barcode: String? = null,
    @JsonProperty("brand") val brand: String? = null,
    @JsonProperty("product_id") val productId: Int = 0,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("variant_id") val variantId: Int? = null,
    @JsonProperty("variant_title") val variantTitle: String? = null,
    @JsonProperty("variant") val variant: List<VariantOptionDto> = emptyList(),
    @JsonProperty("quantity") val quantity: Int = 0,
    @JsonProperty("price") val price: PriceDataDto = PriceDataDto(),
    @JsonProperty("shipping") val shipping: ShippingDto? = null,
    @JsonProperty("available_shippings") val availableShippings: List<LineItemAvailableShippingDto>? = null,
)

data class CartDto(
    @JsonProperty("available_shipping_countries") val availableShippingCountries: List<String> = emptyList(),
    @JsonProperty("cart_id") val cartId: String = "",
    @JsonProperty("currency") val currency: String = "",
    @JsonProperty("customer_session_id") val customerSessionId: String = "",
    @JsonProperty("line_items") val lineItems: List<LineItemDto> = emptyList(),
    @JsonProperty("shipping_country") val shippingCountry: String? = null,
    @JsonProperty("subtotal") val subtotal: Double = 0.0,
    @JsonProperty("shipping") val shipping: Double = 0.0,
)

data class RemoveCartDto(
    @JsonProperty("success") val success: Boolean = false,
    @JsonProperty("message") val message: String = "",
)

data class SupplierLineItemsBySupplierDto(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("name") val name: String? = null,
)

data class GetLineItemsBySupplierDto(
    @JsonProperty("supplier") val supplier: SupplierLineItemsBySupplierDto? = null,
    @JsonProperty("available_shippings") val availableShippings: List<LineItemAvailableShippingDto>? = null,
    @JsonProperty("line_items") val lineItems: List<LineItemDto> = emptyList(),
)

data class PriceDataInput(
    val currency: String,
    val tax: Double? = null,
    val unitPrice: Double? = null,
) {
    fun toJson(): Map<String, Any> = buildMap {
        put("currency", currency)
        tax?.let { put("tax", it) }
        unitPrice?.let { put("unit_price", it) }
    }
}

data class LineItemInput(
    val productId: Int? = null,
    val variantId: Int? = null,
    val quantity: Int? = null,
    val priceData: PriceDataInput? = null,
) {
    fun toJson(): Map<String, Any> = buildMap {
        productId?.let { put("product_id", it) }
        variantId?.let { put("variant_id", it) }
        quantity?.let { put("quantity", it) }
        priceData?.let { put("price_data", it.toJson()) }
    }
}

typealias GetCartDto = CartDto
typealias CreateCartDto = CartDto
typealias UpdateCartDto = CartDto
typealias CreateItemToCartDto = CartDto
typealias UpdateItemToCartDto = CartDto
typealias RemoveItemToCartDto = CartDto
