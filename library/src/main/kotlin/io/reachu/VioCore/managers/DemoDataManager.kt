package io.reachu.VioCore.managers

import io.reachu.VioCore.configuration.DemoDataConfiguration
import io.reachu.VioCore.configuration.VioConfiguration

/**
 * Singleton manager providing access to demo static data.
 * Mirrors the Swift `DemoDataManager`.
 */
class DemoDataManager private constructor() {

    private val configuration: DemoDataConfiguration
        get() = VioConfiguration.shared.state.value.demoData

    companion object {
        val shared = DemoDataManager()
    }

    // MARK: - Asset Access
    val defaultLogo: String get() = configuration.assets.defaultLogo
    val defaultAvatar: String get() = configuration.assets.defaultAvatar
    val backgroundImages get() = configuration.assets.backgroundImages
    val brandAssets get() = configuration.assets.brandAssets
    val contestAssets get() = configuration.assets.contestAssets

    // MARK: - Demo Users
    val defaultUsername: String get() = configuration.demoUsers.defaultUsername
    val chatUsernames get() = configuration.demoUsers.chatUsernames
    val socialAccounts get() = configuration.demoUsers.socialAccounts

    // MARK: - Product Mappings
    val productMappings get() = configuration.productMappings
    fun productMapping(forId: String) = configuration.productMappings[forId]

    // MARK: - Event IDs
    val eventIds get() = configuration.eventIds

    // MARK: - Match Defaults
    val matchDefaults get() = configuration.matchDefaults

    // MARK: - Offer Banner
    val offerBanner get() = configuration.offerBanner

    // MARK: - Carousel Cards
    val carouselCards get() = configuration.carouselCards

    // MARK: - Live Cards
    val liveCards get() = configuration.liveCards

    // MARK: - Sport Clips
    val sportClips get() = configuration.sportClips

    // MARK: - Matches
    val matches get() = configuration.matches
}
