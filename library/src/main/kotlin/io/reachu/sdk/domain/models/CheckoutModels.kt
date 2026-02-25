package io.reachu.sdk.domain.models

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckoutTotalsDto(
    @JsonProperty("currency_code") val currencyCode: String = "",
    @JsonProperty("subtotal") val subtotal: Double = 0.0,
    @JsonProperty("total") val total: Double = 0.0,
    @JsonProperty("taxes") val taxes: Double = 0.0,
    @JsonProperty("tax_amount") val taxAmount: Double? = null,
    @JsonProperty("shipping") val shipping: Double = 0.0,
    @JsonProperty("discounts") val discounts: Double? = null,
)

data class CreateCheckoutDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("status") val status: String = "",
    @JsonProperty("checkout_url") val checkoutUrl: String? = null,
)

data class UpdateCheckoutDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("status") val status: String = "",
    @JsonProperty("checkout_url") val checkoutUrl: String? = null,
)

data class GetCheckoutDto(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("status") val status: String = "",
    @JsonProperty("checkout_url") val checkoutUrl: String? = null,
    @JsonProperty("totals") val totals: CheckoutTotalsDto? = null,
)

data class RemoveCheckoutDto(
    @JsonProperty("success") val success: Boolean = false,
    @JsonProperty("message") val message: String = "",
)
