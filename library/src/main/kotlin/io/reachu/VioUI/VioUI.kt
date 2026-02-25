package io.reachu.VioUI

import io.reachu.VioCore.configuration.LocalizationConfiguration
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.VioEnvironment
import io.reachu.VioCore.configuration.VioLocalization

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
typealias VioCartManager = io.reachu.VioUI.Managers.CartManager
typealias VioProductCard = io.reachu.VioUI.Components.VioProductCard
typealias VioProductSlider = io.reachu.VioUI.Components.VioProductSlider
typealias VioProductCarousel = io.reachu.VioUI.Components.VioProductCarousel
typealias VioProductBanner = io.reachu.VioUI.Components.VioProductBanner
typealias VioProductStore = io.reachu.VioUI.Components.VioProductStore
typealias VioCheckoutOverlayController = io.reachu.VioUI.Components.VioCheckoutOverlayController
typealias VioProductDetailOverlay = io.reachu.VioUI.Components.VioProductDetailOverlay
typealias VioFloatingCartIndicator = io.reachu.VioUI.Components.VioFloatingCartIndicator
typealias VioOfferBanner = io.reachu.VioUI.Components.VioOfferBanner
