package io.reachu.VioUI.Managers

data class CartMarket(
    val code: String,
    val name: String,
    val officialName: String? = null,
    val flagURL: String? = null,
    val phoneCode: String,
    val currencyCode: String,
    val currencySymbol: String,
)
