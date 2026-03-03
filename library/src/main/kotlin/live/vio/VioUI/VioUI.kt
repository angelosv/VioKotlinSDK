package live.vio.VioUI

import live.vio.VioCore.configuration.LocalizationConfiguration
import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.configuration.VioEnvironment
import live.vio.VioCore.configuration.VioLocalization

/**
 * Kotlin entry point mirroring the Swift `VioUI` struct.
 * Provides simple aliases for the migrated managers/components and initialization helpers.
 */
object VioUI {
    /**
     * Configure Vio UI defaults so that localization works even when the SDK
     * wasn't pre-configured by the host application. This is typically used internally
     * or for quick setup without full [VioConfiguration] customization.
     */
    fun configure(apiKey: String = "") {
        val configuration = VioConfiguration.shared
        if (!configuration.isValidConfiguration()) {
            VioConfiguration.configure(
                apiKey = apiKey,
                environment = VioEnvironment.SANDBOX,
            )
        }

        val localization = VioConfiguration.shared.state.value.localization
        if (localization.translations.isEmpty()) {
            VioLocalization.configure(LocalizationConfiguration.default())
        } else {
            VioLocalization.configure(localization)
        }
    }
}

// Public exports
typealias VioCartManager = live.vio.VioUI.Managers.CartManager
typealias VioProductCard = live.vio.VioUI.Components.VioProductCard
typealias VioProductSlider = live.vio.VioUI.Components.VioProductSlider
typealias VioProductCarousel = live.vio.VioUI.Components.VioProductCarousel
typealias VioProductBanner = live.vio.VioUI.Components.VioProductBanner
typealias VioProductStore = live.vio.VioUI.Components.VioProductStore
typealias VioCheckoutOverlayController = live.vio.VioUI.Components.VioCheckoutOverlayController
typealias VioProductDetailOverlay = live.vio.VioUI.Components.VioProductDetailOverlay
typealias VioFloatingCartIndicator = live.vio.VioUI.Components.VioFloatingCartIndicator
typealias VioOfferBanner = live.vio.VioUI.Components.VioOfferBanner
