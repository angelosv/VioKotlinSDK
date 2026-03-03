package live.vio.demo.demos

import live.vio.demo.DemoConfig
import live.vio.demo.util.Logger
import live.vio.sdk.core.VioSdkClient
import live.vio.sdk.core.errors.SdkException

suspend fun runMarketDemo(config: DemoConfig) {
    val sdk = VioSdkClient(config.baseUrl, config.apiToken)
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

