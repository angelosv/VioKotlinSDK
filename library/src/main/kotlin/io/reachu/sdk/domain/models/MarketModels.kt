package io.reachu.sdk.domain.models

import com.fasterxml.jackson.annotation.JsonProperty

data class MarketDto(
    @JsonProperty("code") val code: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("official") val official: String? = null,
    @JsonProperty("flag") val flag: String? = null,
    @JsonProperty("phone_code") val phoneCode: String? = null,
    @JsonProperty("currency") val currency: MarketCurrencyDto? = null,
)

data class MarketCurrencyDto(
    @JsonProperty("code") val code: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("symbol") val symbol: String? = null,
)

typealias GetAvailableGlobalMarketsDto = MarketDto
typealias CurrencyMarketsDto = MarketCurrencyDto
