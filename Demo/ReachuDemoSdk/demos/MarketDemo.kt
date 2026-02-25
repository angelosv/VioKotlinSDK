package io.reachu.demo.demos

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.SdkClient
import io.reachu.sdk.core.errors.SdkException

suspend fun runMarketDemo(config: DemoConfig) {
    val sdk = SdkClient(config.baseUrl, config.apiToken)
    try {
        Logger.section("GetAvailableMarkets")
        val (markets, _) = Logger.measure("GetAvailableMarkets") {
            sdk.market.getAvailable()
        }
        Logger.json(markets, "Response (GetAvailableMarkets)")
        Logger.section("Summary")
        Logger.info("Total markets: ${markets.size}")
        markets.firstOrNull()?.let { sample ->
            Logger.json(sample, "First market sample")
        }
        Logger.section("Done")
        Logger.success("Market demo finished successfully.")
    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

