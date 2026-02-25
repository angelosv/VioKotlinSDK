package io.reachu.sdk.domain.models

import com.fasterxml.jackson.annotation.JsonProperty

data class GetCategoryDto(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("father_category_id") val fatherCategoryId: Int? = null,
    @JsonProperty("category_image") val categoryImage: String? = null,
)

data class SettingsChannelDto(
    @JsonProperty("stripe_payment_link") val stripePaymentLink: Boolean = false,
    @JsonProperty("stripe_payment_intent") val stripePaymentIntent: Boolean = false,
    @JsonProperty("klarna") val klarna: Boolean = false,
    @JsonProperty("markets") val markets: List<String> = emptyList(),
    @JsonProperty("purchase_conditions") val purchaseConditions: Boolean = false,
)

data class GetChannelsDto(
    @JsonProperty("channel") val channel: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("api_key") val apiKey: String? = null,
    @JsonProperty("settings") val settings: SettingsChannelDto = SettingsChannelDto(),
)

data class AttrContentElementTac(
    @JsonProperty("level") val level: Int? = null,
)

data class LinkMarkTac(
    @JsonProperty("href") val href: String? = null,
    @JsonProperty("linktype") val linktype: String? = null,
    @JsonProperty("target") val target: String? = null,
)

data class TextElementTac(
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("text") val text: String? = null,
    @JsonProperty("marks") val marks: List<LinkMarkTac>? = null,
)

data class ContentElementTac(
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("attrs") val attrs: AttrContentElementTac? = null,
    @JsonProperty("content") val content: List<TextElementTac>? = null,
)

data class GetTermsAndConditionsDto(
    @JsonProperty("headline") val headline: String? = null,
    @JsonProperty("lead") val lead: String? = null,
    @JsonProperty("updated") val updated: String? = null,
    @JsonProperty("content") val content: List<ContentElementTac>? = null,
)

data class CurrencyDto(
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("symbol") val symbol: String? = null,
)

data class GetAvailableMarketsDto(
    @JsonProperty("code") val code: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("official") val official: String? = null,
    @JsonProperty("flag") val flag: String? = null,
    @JsonProperty("phone_code") val phoneCode: String? = null,
    @JsonProperty("currency") val currency: CurrencyDto? = null,
)
