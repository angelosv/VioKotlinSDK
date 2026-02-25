package io.reachu.VioCore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Int,
    val title: String,
    val brand: String? = null,
    val description: String? = null,
    val tags: String? = null,
    val sku: String,
    val quantity: Int? = null,
    val price: Price,
    val variants: List<Variant> = emptyList(),
    val barcode: String? = null,
    val options: List<Option>? = null,
    val categories: List<Category>? = null,
    val images: List<ProductImage> = emptyList(),
    @SerialName("product_shipping") val productShipping: List<ProductShipping>? = null,
    val supplier: String,
    @SerialName("supplier_id") val supplierId: Int? = null,
    @SerialName("imported_product") val importedProduct: Boolean? = null,
    @SerialName("referral_fee") val referralFee: Int? = null,
    @SerialName("options_enabled") val optionsEnabled: Boolean = false,
    val digital: Boolean = false,
    val origin: String = "",
    @SerialName("return") val returns: ReturnInfo? = null,
)

@Serializable
data class Price(
    val amount: Float,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("amount_incl_taxes") val amountInclTaxes: Float? = null,
    @SerialName("tax_amount") val taxAmount: Float? = null,
    @SerialName("tax_rate") val taxRate: Float? = null,
    @SerialName("compare_at") val compareAt: Float? = null,
    @SerialName("compare_at_incl_taxes") val compareAtInclTaxes: Float? = null,
) {
    val displayAmount: String
        get() {
            val priceToShow = amountInclTaxes ?: amount
            return "$currencyCode ${"%.2f".format(priceToShow)}"
        }

    val displayCompareAtAmount: String?
        get() = when {
            compareAtInclTaxes != null -> "$currencyCode ${"%.2f".format(compareAtInclTaxes)}"
            compareAt != null -> "$currencyCode ${"%.2f".format(compareAt)}"
            else -> null
        }
}

@Serializable
data class Variant(
    val id: String,
    val barcode: String? = null,
    val price: Price,
    val quantity: Int? = null,
    val sku: String,
    val title: String,
    val images: List<ProductImage> = emptyList(),
)

@Serializable
data class ProductImage(
    val id: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val order: Int = 0,
)

@Serializable
data class Option(
    val id: String,
    val name: String,
    val order: Int,
    val values: String,
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
)

@Serializable
data class ProductShipping(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("custom_price_enabled") val customPriceEnabled: Boolean = false,
    @SerialName("default") val isDefault: Boolean = false,
    @SerialName("shipping_country") val shippingCountry: List<ShippingCountry>? = null,
)

@Serializable
data class ShippingCountry(
    val id: String,
    val country: String,
    val price: BasePrice,
)

@Serializable
data class BasePrice(
    val amount: Float,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("amount_incl_taxes") val amountInclTaxes: Float? = null,
    @SerialName("tax_amount") val taxAmount: Float? = null,
    @SerialName("tax_rate") val taxRate: Float? = null,
)

@Serializable
data class ReturnInfo(
    @SerialName("return_right") val returnRight: Boolean? = null,
    @SerialName("return_label") val returnLabel: String? = null,
    @SerialName("return_cost") val returnCost: Float? = null,
    @SerialName("supplier_policy") val supplierPolicy: String? = null,
    @SerialName("return_address") val returnAddress: ReturnAddress? = null,
)

@Serializable
data class ReturnAddress(
    @SerialName("same_as_business") val sameAsBusiness: Boolean? = null,
    @SerialName("same_as_warehouse") val sameAsWarehouse: Boolean? = null,
    val country: String? = null,
    val timezone: String? = null,
    val address: String? = null,
    @SerialName("address_2") val address2: String? = null,
    @SerialName("post_code") val postCode: String? = null,
    @SerialName("return_city") val returnCity: String? = null,
)
