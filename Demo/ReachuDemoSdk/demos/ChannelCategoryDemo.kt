package io.reachu.demo.demos

import io.reachu.demo.DemoConfig
import io.reachu.demo.util.Logger
import io.reachu.sdk.core.SdkClient
import io.reachu.sdk.core.errors.SdkException

suspend fun runChannelCategoryDemo(config: DemoConfig) {
    val sdk = SdkClient(config.baseUrl, config.apiToken)
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

