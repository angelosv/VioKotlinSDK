package io.reachu.sdk.domain.models

import com.fasterxml.jackson.annotation.JsonProperty

data class PriceDto(
    @JsonProperty("amount") val amount: Double = 0.0,
    @JsonProperty("currency_code") val currencyCode: String = "",
    @JsonProperty("compare_at") val compareAt: Double? = null,
    @JsonProperty("amount_incl_taxes") val amountInclTaxes: Double? = null,
    @JsonProperty("compare_at_incl_taxes") val compareAtInclTaxes: Double? = null,
    @JsonProperty("tax_amount") val taxAmount: Double? = null,
    @JsonProperty("tax_rate") val taxRate: Double? = null,
)

data class ProductImageDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("url") val url: String = "",
    @JsonProperty("width") val width: Int? = null,
    @JsonProperty("height") val height: Int? = null,
    @JsonProperty("order") val order: Int? = null,
)

data class OptionDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("order") val order: Int = 0,
    @JsonProperty("values") val values: String = "",
)

data class CategoryDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("name") val name: String = "",
)

data class VariantDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("barcode") val barcode: String? = null,
    @JsonProperty("quantity") val quantity: Int? = null,
    @JsonProperty("sku") val sku: String = "",
    @JsonProperty("title") val title: String = "",
    @JsonProperty("price") val price: PriceDto = PriceDto(),
    @JsonProperty("images") val images: List<ProductImageDto> = emptyList(),
)

data class ShippingCountryDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("country") val country: String = "",
    @JsonProperty("price") val price: PriceDto = PriceDto(),
)

data class ProductShippingDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("custom_price_enabled") val customPriceEnabled: Boolean = false,
    @JsonProperty("default") val defaultOption: Boolean = false,
    @JsonProperty("shipping_country") val shippingCountry: List<ShippingCountryDto>? = null,
)

data class ReturnAddressDto(
    @JsonProperty("address") val address: String? = null,
    @JsonProperty("address_2") val address2: String? = null,
    @JsonProperty("country") val country: String? = null,
    @JsonProperty("post_code") val postCode: String? = null,
    @JsonProperty("return_city") val returnCity: String? = null,
    @JsonProperty("same_as_business") val sameAsBusiness: Boolean? = null,
    @JsonProperty("same_as_warehouse") val sameAsWarehouse: Boolean? = null,
    @JsonProperty("timezone") val timezone: String? = null,
)

data class ReturnInfoDto(
    @JsonProperty("return_address") val returnAddress: ReturnAddressDto? = null,
    @JsonProperty("return_cost") val returnCost: Double? = null,
    @JsonProperty("return_label") val returnLabel: String? = null,
    @JsonProperty("return_right") val returnRight: Boolean? = null,
    @JsonProperty("supplier_policy") val supplierPolicy: String? = null,
)

data class ProductDto(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("title") val title: String = "",
    @JsonProperty("sku") val sku: String = "",
    @JsonProperty("supplier") val supplier: String = "",
    @JsonProperty("brand") val brand: String? = null,
    @JsonProperty("barcode") val barcode: String? = null,
    @JsonProperty("origin") val origin: String = "",
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("digital") val digital: Boolean = false,
    @JsonProperty("options_enabled") val optionsEnabled: Boolean = false,
    @JsonProperty("quantity") val quantity: Int? = null,
    @JsonProperty("referral_fee") val referralFee: Int? = null,
    @JsonProperty("imported_product") val importedProduct: Boolean? = null,
    @JsonProperty("tags") val tags: String? = null,
    @JsonProperty("supplier_id") val supplierId: Int? = null,
    @JsonProperty("price") val price: PriceDto = PriceDto(),
    @JsonProperty("images") val images: List<ProductImageDto> = emptyList(),
    @JsonProperty("variants") val variants: List<VariantDto> = emptyList(),
    @JsonProperty("options") val options: List<OptionDto> = emptyList(),
    @JsonProperty("categories") val categories: List<CategoryDto>? = null,
    @JsonProperty("product_shipping") val productShipping: List<ProductShippingDto>? = null,
    @JsonProperty("return") val returnInfo: ReturnInfoDto? = null,
)
