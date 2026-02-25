package io.reachu.VioUI.Managers

import io.reachu.VioCore.configuration.VioConfiguration

suspend fun CartManager.loadMarketsIfNeeded() {
    if (isMarketReady || isLoadingMarkets) return
    loadMarkets()
}

suspend fun CartManager.reloadMarkets() {
    isMarketReady = false
    loadMarkets()
}

suspend fun CartManager.selectMarket(market: Market) {
    val current = selectedMarket
    if (current != null &&
        current.code.equals(market.code, ignoreCase = true) &&
        current.currencyCode.equals(market.currencyCode, ignoreCase = true)
    ) {
        return
    }
    applyMarket(market, refreshData = true)
}

internal suspend fun CartManager.loadMarkets() {
    if (!VioConfiguration.shared.shouldUseSDK) {
        println("⚠️ [Markets] Skipping market load - SDK disabled (market not available)")
        return
    }

    val fallbackConfig = VioConfiguration.shared.state.value.market
    val fallbackMarket = Market(
        code = fallbackConfig.countryCode,
        name = fallbackConfig.countryName,
        officialName = fallbackConfig.countryName,
        flagURL = fallbackConfig.flagURL,
        phoneCode = fallbackConfig.phoneCode,
        currencyCode = fallbackConfig.currencyCode,
        currencySymbol = fallbackConfig.currencySymbol,
    )

    isLoadingMarkets = true
    try {
        logRequest("sdk.market.getAvailable")
        val start = System.currentTimeMillis()
        val dtos = ioCall { sdk.market.getAvailable() }
        val elapsed = System.currentTimeMillis() - start
        println("⏱️ [Markets] sdk.market.getAvailable took ${elapsed}ms")
        logResponse("sdk.market.getAvailable", mapOf("count" to dtos.size))
        println(
            "⬅️ [Markets] Raw GraphQL response: ${
                dtos.joinToString { dto -> "${dto.code}-${dto.currency?.code ?: "?"}" }
            }",
        )
        val mapped = dtos.mapNotNull { it.toMarket(fallbackConfig) }.toMutableList()

        if (mapped.isEmpty()) {
            mapped += fallbackMarket
        } else if (mapped.none { it.code == fallbackMarket.code }) {
            mapped.add(0, fallbackMarket)
        }

        markets = mapped
        isMarketReady = true

        val currentCode = selectedMarket?.code ?: fallbackMarket.code
        val target = mapped.firstOrNull { it.code == currentCode } ?: fallbackMarket
        val shouldRefresh = (country != target.code) || (currency != target.currencyCode)
        applyMarket(target, refreshData = shouldRefresh)
    } catch (t: Throwable) {
        val msg = t.message
        println("❌ [Markets] Failed to load markets: $msg")
        logError("sdk.market.getAvailable", t)
        markets = listOf(fallbackMarket)
        isMarketReady = false
        applyMarket(fallbackMarket, refreshData = false)
    } finally {
        isLoadingMarkets = false
    }
}

internal suspend fun CartManager.applyMarket(market: Market, refreshData: Boolean) {
    selectedMarket = market
    country = market.code
    currency = market.currencyCode
    currencySymbol = market.currencySymbol
    phoneCode = market.phoneCode
    flagURL = market.flagURL
    shippingCurrency = market.currencyCode
    pendingShippingSelections.clear()

    val needsInitialProducts = !didLoadInitialProducts

    if (refreshData) {
        resetForMarketChange(market.currencyCode)
        createCart(currency = market.currencyCode, country = market.code)
        loadProducts(
            currency = market.currencyCode,
            shippingCountryCode = market.code,
            useCache = false,
        )
        refreshShippingOptions()
    } else if (needsInitialProducts) {
        loadProducts(
            currency = market.currencyCode,
            shippingCountryCode = market.code,
            useCache = false,
        )
        refreshShippingOptions()
    } else {
        recalcShippingTotalsFromItems()
    }
}

internal fun CartManager.resetForMarketChange(defaultCurrency: String) {
    items = emptyList()
    products = emptyList()
    cartTotal = 0.0
    shippingTotal = 0.0
    shippingCurrency = defaultCurrency
    isProductsLoading = true
    currentCartId = null
    checkoutId = null
    lastDiscountCode = null
    lastDiscountId = null
    errorMessage = null
    lastLoadedProductCurrency = null
    lastLoadedProductCountry = null
    activeProductRequestID = null
    didLoadInitialProducts = false
}
