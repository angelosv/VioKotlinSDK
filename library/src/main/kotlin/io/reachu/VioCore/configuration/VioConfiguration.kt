package io.reachu.VioCore.configuration

import io.reachu.VioCore.analytics.AnalyticsManager
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.sdk.domain.models.GetAvailableMarketsDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kotlin port of the Swift `VioConfiguration`.
 * Provides a singleton with module-level configuration settings for the Vio SDK.
 * Use [configure] to initialize the SDK with your API key and preferred environment
 * before using any Vio UI components or managers.
 */
class VioConfiguration private constructor() {

    // Observable state for Kotlin callers that want to react to configuration changes.
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    data class State(
        val apiKey: String = "",
        val environment: VioEnvironment = VioEnvironment.SANDBOX,
        val theme: VioTheme = VioTheme.defaultTheme(),
        val cart: CartConfiguration = CartConfiguration.default(),
        val network: NetworkConfiguration = NetworkConfiguration.default(),
        val ui: UIConfiguration = UIConfiguration.default(),
        val liveShow: LiveShowConfiguration = LiveShowConfiguration.default(),
        val market: MarketConfiguration = MarketConfiguration.default(),
        val productDetail: ProductDetailConfiguration = ProductDetailConfiguration.default(),
        val localization: LocalizationConfiguration = LocalizationConfiguration.default(),
        val campaign: CampaignConfiguration = CampaignConfiguration.default(),
        val analytics: AnalyticsConfiguration = AnalyticsConfiguration.default(),
        val demoData: DemoDataConfiguration = DemoDataConfiguration.default(),
        val isMarketAvailable: Boolean = true,
        val userCountryCode: String? = null,
        val availableMarkets: List<GetAvailableMarketsDto> = emptyList(),
        val isConfigured: Boolean = false,
    )

    companion object {
        val shared: VioConfiguration = VioConfiguration()

        fun configure(
            apiKey: String,
            environment: VioEnvironment = VioEnvironment.PRODUCTION,
            theme: VioTheme? = null,
            cartConfig: CartConfiguration? = null,
            networkConfig: NetworkConfiguration? = null,
            uiConfig: UIConfiguration? = null,
            liveShowConfig: LiveShowConfiguration? = null,
            marketConfig: MarketConfiguration? = null,
            productDetailConfig: ProductDetailConfiguration? = null,
            localizationConfig: LocalizationConfiguration? = null,
            campaignConfig: CampaignConfiguration? = null,
            analyticsConfig: AnalyticsConfiguration? = null,
            demoDataConfig: DemoDataConfiguration? = null,
        ) {
            val instance = shared
            instance._state.value = instance._state.value.copy(
                apiKey = apiKey,
                environment = environment,
                theme = theme ?: VioTheme.defaultTheme(),
                cart = cartConfig ?: CartConfiguration.default(),
                network = networkConfig ?: NetworkConfiguration.default(),
                ui = uiConfig ?: UIConfiguration.default(),
                liveShow = liveShowConfig ?: LiveShowConfiguration.default(),
                market = marketConfig ?: MarketConfiguration.default(),
                productDetail = productDetailConfig ?: ProductDetailConfiguration.default(),
                localization = localizationConfig ?: LocalizationConfiguration.default(),
                campaign = campaignConfig ?: CampaignConfiguration.default(),
                analytics = analyticsConfig ?: AnalyticsConfiguration.default(),
                demoData = demoDataConfig ?: DemoDataConfiguration.default(),
                isConfigured = true,
            )
            VioLocalization.configure(instance._state.value.localization)
            AnalyticsManager.configure(instance._state.value.analytics)
            CampaignManager.shared.reinitialize()

            println("🔧 Vio SDK configured successfully")
            println("   API Key: ${apiKey.take(8)}...")
            println("   Environment: $environment")
            println("   Theme: ${instance._state.value.theme.name}")
        }

        fun configure(apiKey: String) {
            configure(
                apiKey = apiKey,
                environment = VioEnvironment.PRODUCTION,
            )
        }

        fun updateTheme(theme: VioTheme) {
            shared._state.value = shared._state.value.copy(theme = theme)
        }

        fun updateCartConfiguration(config: CartConfiguration) {
            shared._state.value = shared._state.value.copy(cart = config)
        }

        fun setMarketAvailability(
            available: Boolean,
            userCountryCode: String? = null,
            availableMarkets: List<GetAvailableMarketsDto> = emptyList(),
        ) {
            val instance = shared
            val lang = languageCodeForCountry(userCountryCode)
            val localization = instance._state.value.localization
            val hasTranslation = localization.translations.containsKey(lang)
            VioLocalization.setLanguage(if (hasTranslation) lang else localization.defaultLanguage)
            instance._state.value = instance._state.value.copy(
                isMarketAvailable = available,
                userCountryCode = userCountryCode,
                availableMarkets = availableMarkets,
            )
        }

        fun setMarketAvailable(
            available: Boolean,
            userCountryCode: String? = null,
            availableMarkets: List<GetAvailableMarketsDto> = emptyList(),
        ) {
            setMarketAvailability(available, userCountryCode, availableMarkets)
        }

        private fun languageCodeForCountry(code: String?): String {
            val map = mapOf(
                "DE" to "de", "AT" to "de", "CH" to "de",
                "US" to "en", "GB" to "en", "CA" to "en", "AU" to "en",
                "NO" to "no", "SE" to "sv", "DK" to "da", "FI" to "fi",
                "ES" to "es", "FR" to "fr", "IT" to "it", "NL" to "nl",
                "PL" to "pl", "PT" to "pt", "BR" to "pt",
                "MX" to "es", "AR" to "es", "CL" to "es", "CO" to "es",
                "JP" to "ja", "CN" to "zh", "KR" to "ko",
            )
            val normalized = code?.uppercase() ?: return "en"
            return map[normalized] ?: "en"
        }
    }

    fun isValidConfiguration(): Boolean {
        val current = _state.value
        return current.isConfigured && current.apiKey.isNotBlank()
    }

    fun validateConfiguration() {
        val current = _state.value
        if (!current.isConfigured) throw ConfigurationError.NotConfigured
        if (current.apiKey.isBlank()) throw ConfigurationError.MissingApiKey
    }

    fun isMarketAvailableForCountry(countryCode: String): Boolean {
        val markets = _state.value.availableMarkets
        if (markets.isEmpty()) return true
        return markets.any { it.code.equals(countryCode, ignoreCase = true) }
    }

    fun getMarketInfo(countryCode: String): GetAvailableMarketsDto? {
        val markets = _state.value.availableMarkets
        if (markets.isEmpty()) return null
        val normalized = countryCode.uppercase()
        return markets.firstOrNull { it.code?.uppercase() == normalized }
    }

    val shouldUseSDK: Boolean
        get() {
            val current = _state.value
            return current.isConfigured && current.isMarketAvailable
        }
}

enum class VioEnvironment(val baseUrl: String) {
    DEVELOPMENT("https://graph-ql-dev.vio.live"),
    SANDBOX("https://graph-ql-dev.vio.live"),
    PRODUCTION("https://graph-ql.vio.live");

    val graphQLUrl: String get() = "$baseUrl/graphql"
}

sealed class ConfigurationError(message: String) : IllegalStateException(message) {
    object NotConfigured : ConfigurationError("Vio SDK is not configured. Call VioConfiguration.configure() first.")
    object MissingApiKey : ConfigurationError("API Key is required for Vio SDK configuration.")
    object InvalidEnvironment : ConfigurationError("Invalid environment specified.")
    object InvalidTheme : ConfigurationError("Invalid theme configuration.")
    class FileNotFound(val fileName: String) : ConfigurationError("Configuration file '$fileName' not found.")
    object InvalidJSON : ConfigurationError("Invalid JSON configuration format.")
    object InvalidPlist : ConfigurationError("Invalid Plist configuration format.")
}
