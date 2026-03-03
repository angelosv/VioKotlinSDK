package live.vio.demo.demos

import live.vio.demo.DemoConfig
import live.vio.demo.util.Logger
import live.vio.sdk.core.VioSdkClient
import live.vio.sdk.core.errors.SdkException

suspend fun runChannelInfoDemo(config: DemoConfig) {
    val sdk = VioSdkClient(config.baseUrl, config.apiToken)
    val infoRepo = sdk.channel.info

    try {
        Logger.section("ChannelInfo.getChannels")
        val (channels, _) = Logger.measure("ChannelInfo.getChannels") {
            infoRepo.getChannels()
        }
        Logger.json(channels, "Response (ChannelInfo.getChannels)")

        Logger.section("ChannelInfo.getPurchaseConditions")
        val (purchaseConditions, _) = Logger.measure("ChannelInfo.getPurchaseConditions") {
            infoRepo.getPurchaseConditions()
        }
        Logger.json(purchaseConditions, "Response (ChannelInfo.getPurchaseConditions)")

        Logger.section("ChannelInfo.getTermsAndConditions")
        val (terms, _) = Logger.measure("ChannelInfo.getTermsAndConditions") {
            infoRepo.getTermsAndConditions()
        }
        Logger.json(terms, "Response (ChannelInfo.getTermsAndConditions)")

        Logger.section("Done")
        Logger.success("Channel info demo finished successfully.")
    } catch (ex: Exception) {
        Logger.section("Error")
        val message = (ex as? SdkException)?.toString() ?: ex.localizedMessage
        Logger.error(message)
    }
}

