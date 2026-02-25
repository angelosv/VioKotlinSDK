package io.reachu.VioCore.configuration

import kotlinx.serialization.Serializable

/**
 * Configuration for static demo data used in demos and development.
 * Mirrors the Swift `DemoDataConfiguration`.
 */
@Serializable
data class DemoDataConfiguration(
    val assets: AssetConfiguration = AssetConfiguration(),
    val demoUsers: DemoUserConfiguration = DemoUserConfiguration(),
    val productMappings: Map<String, ProductMapping> = emptyMap(),
    val eventIds: EventIdConfiguration = EventIdConfiguration(),
    val matchDefaults: MatchDefaultConfiguration = MatchDefaultConfiguration(),
    val offerBanner: OfferBannerConfiguration = OfferBannerConfiguration(),
    val carouselCards: List<CarouselCardItem> = emptyList(),
    val liveCards: List<LiveCardItem> = emptyList(),
    val sportClips: List<SportClipItem> = emptyList(),
    val matches: List<MatchItem> = emptyList()
) {
    companion object {
        fun default() = DemoDataConfiguration()
    }

    @Serializable
    data class AssetConfiguration(
        val defaultLogo: String = "logo1",
        val defaultAvatar: String = "avatar_el",
        val backgroundImages: BackgroundImageAssets = BackgroundImageAssets(),
        val brandAssets: BrandImageAssets = BrandImageAssets(),
        val contestAssets: ContestImageAssets = ContestImageAssets()
    )

    @Serializable
    data class BackgroundImageAssets(
        val footballField: String = "football_field_bg",
        val mainBackground: String = "bg-main",
        val sportDetail: String = "bg",
        val sportDetailImage: String = "img1"
    )

    @Serializable
    data class BrandImageAssets(
        val icon: String = "icon ",
        val logo: String = "logo"
    )

    @Serializable
    data class ContestImageAssets(
        val giftCard: String = "elkjop_konk",
        val championsLeagueTickets: String = "billeter_power"
    )

    @Serializable
    data class DemoUserConfiguration(
        val defaultUsername: String = "Usuario",
        val chatUsernames: List<ChatUsername> = emptyList(),
        val socialAccounts: List<SocialAccount> = emptyList()
    )

    @Serializable
    data class ChatUsername(
        val name: String,
        val color: String
    )

    @Serializable
    data class SocialAccount(
        val name: String,
        val handle: String,
        val verified: Boolean
    )

    @Serializable
    data class ProductMapping(
        val name: String,
        val productUrl: String,
        val checkoutUrl: String
    )

    @Serializable
    data class EventIdConfiguration(
        val contestQuiz: String = "casting-contest-quiz",
        val contestGiveaway: String = "casting-contest-giveaway",
        val productCombo: String = "casting-product-combo",
        val tweetHalftime1: String = "tweet-halftime-1",
        val tweetHalftime2: String = "tweet-halftime-2"
    )

    @Serializable
    data class MatchDefaultConfiguration(
        val broadcastIdMappings: Map<String, String> = mapOf("barcelona-psg" to "barcelona-psg-2025-01-23"),
        val defaultScore: Int = 3
    )

    @Serializable
    data class CarouselCardItem(
        val imageUrl: String = "img1",
        val time: String = "",
        val logo: String = "",
        val title: String = "",
        val subtitle: String = ""
    )

    @Serializable
    data class LiveCardItem(
        val logo: String = "",
        val logoIcon: String = "star.fill",
        val title: String = "",
        val subtitle: String = "",
        val time: String = "",
        val backgroundImage: String? = null
    )

    @Serializable
    data class SportClipItem(
        val imageUrl: String = "img1",
        val time: String = "",
        val title: String = "",
        val subtitle: String = "",
        val isLarge: Boolean = false
    )

    @Serializable
    data class MatchItem(
        val broadcastId: String,
        val title: String,
        val subtitle: String,
        val imageUrl: String,
        val isLive: Boolean
    )

    @Serializable
    data class OfferBannerConfiguration(
        val countdown: CountdownConfiguration = CountdownConfiguration(),
        val title: String = "Ukens tilbud",
        val subtitle: String = "Se denne ukes beste tilbud",
        val discountText: String = "Opp til 30%",
        val buttonText: String = "Se alle tilbud"
    )

    @Serializable
    data class CountdownConfiguration(
        val days: Int = 2,
        val hours: Int = 1,
        val minutes: Int = 59,
        val seconds: Int = 47
    )
}
