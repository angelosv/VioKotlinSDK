package live.vio.VioCore.managers

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.models.DynamicConfig
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.helpers.JsonUtils

/**
 * Manager responsible for parsing and managing dynamic technical configurations
 * like [SponsorConfig] and [CommerceConfig].
 * Mirrors the Swift `DynamicConfigManager`.
 */
class DynamicConfigManager private constructor() {

    companion object {
        private const val TAG = "DynamicConfig"
        val shared = DynamicConfigManager()
    }

    /**
     * Updates the dynamic configuration from a JSON string.
     * Usually called when the campaign config is fetched.
     */
    fun updateFromConfig(json: String) {
        try {
            val config = JsonUtils.mapper.readValue(json, DynamicConfig::class.java)
            update(config)
        } catch (e: Exception) {
            VioLogger.error("Failed to parse dynamic config: ${e.message}", TAG)
        }
    }

    /**
     * Updates [VioConfiguration] with the parsed dynamic config.
     */
    fun update(config: DynamicConfig) {
        VioLogger.debug("Updating dynamic configuration...", TAG)
        
        // Update Sponsor Assets
        config.sponsor?.let {
            VioLogger.success("Updating sponsor: ${it.name}", TAG)
            live.vio.VioCore.models.SponsorAssets.update(it)
            VioConfiguration.updateSponsor(it)
        }

        // Update Commerce Config
        config.commerce?.let {
            VioLogger.success("Updating commerce config: ${it.apiKey}", TAG)
            VioConfiguration.updateDynamicCommerceConfig(it)
        }

        // Update Checkout Config
        config.checkout?.let {
            VioLogger.success("Updating checkout config: ${it.paymentMethods.size} methods", TAG)
            VioConfiguration.updateCheckoutConfig(it)
        }
    }
}
