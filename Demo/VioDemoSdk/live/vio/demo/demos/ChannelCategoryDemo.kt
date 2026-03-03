package live.vio.demo.demos

import live.vio.demo.DemoConfig
import live.vio.demo.util.Logger
import live.vio.sdk.core.VioSdkClient
import live.vio.sdk.core.errors.SdkException

suspend fun runChannelCategoryDemo(config: DemoConfig) {
    val sdk = VioSdkClient(config.baseUrl, config.apiToken)
    val repo = sdk.channel.category

    try {
        Logger.section("ChannelCategory.get")
        val (categories, _) = Logger.measure("ChannelCategory.get") {
            repo.get()
        }
        Logger.json(categories, "Response (ChannelCategory.get)")

        Logger.section("Summary")
        Logger.info("Total categories: ${categories.size}")
        categories.firstOrNull()?.let {
            Logger.json(it, "Sample category")
        }

        Logger.section("Done")
        Logger.success("Channel category demo finished successfully.")
    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

