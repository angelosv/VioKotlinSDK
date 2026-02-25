package io.reachu.VioCore.configuration

import io.reachu.VioCore.utils.VioLogger
import io.reachu.sdk.core.SdkClient
import io.reachu.sdk.core.errors.NotFoundException
import io.reachu.sdk.core.errors.SdkException
import java.io.File
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ConfigurationLoader {

    private const val COMPONENT = "Config"
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun loadConfiguration(
        fileName: String? = null,
        basePath: String,
        userCountryCode: String? = null,
    ) {
        try {
            val targetName = fileName ?: detectConfigName(basePath)
            val resolvedCountry = userCountryCode ?: System.getenv("REACHU_USER_COUNTRY")
            if (targetName != null) {
                VioLogger.debug("Loading configuration: $targetName.json", COMPONENT)
                val data = readFile("$basePath$targetName.json")
                val config = resolveConfigurationFromString(data)
                applyConfiguration(config, basePath, resolvedCountry)
            } else {
                VioLogger.warning("No config file found, using defaults", COMPONENT)
                applyDefaultConfiguration(resolvedCountry)
            }
        } catch (t: Throwable) {
            VioLogger.error("Error loading configuration: ${t.message}", COMPONENT)
            applyDefaultConfiguration(userCountryCode)
        }
    }

    suspend fun loadFromRemote(
        url: URL,
        basePath: String = "",
        userCountryCode: String? = null,
    ) {
        val data = withContext(Dispatchers.IO) { url.readText() }
        val config = json.decodeFromString<JsonConfiguration>(data)
        applyConfiguration(config, basePath, userCountryCode ?: System.getenv("REACHU_USER_COUNTRY"))
    }

    private fun applyConfiguration(
        config: JsonConfiguration,
        basePath: String,
        userCountryCode: String?,
    ) {
        val envApi = System.getenv("REACHU_API_TOKEN")?.takeIf { it.isNotBlank() }
        val envEnv = System.getenv("REACHU_ENVIRONMENT")?.takeIf { it.isNotBlank() }

        val environment = runCatching {
            (envEnv ?: config.environment)?.let { VioEnvironment.valueOf(it.uppercase()) }
        }.getOrNull() ?: VioEnvironment.PRODUCTION

        val theme = config.theme?.toDomain()
        val cart = config.cart?.toDomain()
        val network = config.network?.toDomain()
        val ui = config.ui?.toDomain()
        val liveShow = when {
            config.liveShow != null -> config.liveShow.toDomain(config.campaignId)
            config.campaignId != null -> LiveShowConfiguration.default().copy(campaignId = config.campaignId)
            else -> null
        }
        val marketConfig = config.marketFallback?.toDomain()
        val localization = config.localization?.toDomain(basePath) ?: LocalizationConfiguration.default()
        val campaign = config.campaigns?.toDomain()
        val analyticsConfig = config.analytics?.toDomain()
        val productDetail = config.productDetail?.toDomain()
        val demoData = loadDemoDataConfiguration(basePath = basePath)

        VioConfiguration.configure(
            apiKey = (envApi ?: config.apiKey).orEmpty(),
            environment = environment,
            theme = theme,
            cartConfig = cart,
            networkConfig = network,
            uiConfig = ui,
            liveShowConfig = liveShow,
            marketConfig = marketConfig,
            productDetailConfig = productDetail,
            localizationConfig = localization,
            campaignConfig = campaign,
            analyticsConfig = analyticsConfig,
            demoDataConfig = demoData,
        )

        val preferredCountry = userCountryCode
            ?: marketConfig?.countryCode
            ?: MarketConfiguration.default().countryCode
        setLanguageForCountry(localization, preferredCountry)

        if (!userCountryCode.isNullOrBlank()) {
            checkMarketAvailability(userCountryCode)
        } else {
            VioConfiguration.setMarketAvailability(true, availableMarkets = emptyList())
        }
    }

    private fun applyDefaultConfiguration(userCountryCode: String?) {
        VioConfiguration.configure(
            apiKey = "",
            environment = VioEnvironment.SANDBOX,
            theme = VioTheme.defaultTheme(),
            marketConfig = MarketConfiguration.default(),
            localizationConfig = LocalizationConfiguration.default(),
            campaignConfig = CampaignConfiguration.default(),
        )
        setLanguageForCountry(VioConfiguration.shared.state.value.localization, userCountryCode ?: "US")
        VioConfiguration.setMarketAvailability(true, userCountryCode = userCountryCode, availableMarkets = emptyList())
        VioLogger.success("Applied default SDK configuration", COMPONENT)
        if (!userCountryCode.isNullOrBlank()) {
            checkMarketAvailability(userCountryCode)
        }
    }

    private fun detectConfigName(basePath: String): String? {
        // 1) Environment override: REACHU_CONFIG_TYPE -> vio-config-<type>.json
        val type = System.getenv("REACHU_CONFIG_TYPE")?.trim()?.lowercase()
        if (!type.isNullOrEmpty()) {
            val name = "vio-config-$type"
            if (File("$basePath$name.json").exists()) return name
        }

        // 2) Priority list similar a Swift
        val candidates = listOf(
            "vio-config",
            "vio-config-automatic",
            "vio-config-example",
            "vio-config-dark-streaming",
        )
        return candidates.firstOrNull { File("$basePath$it.json").exists() }
    }

    private fun readFile(path: String): String = File(path).takeIf { it.exists() }?.readText()
        ?: throw ConfigurationError.FileNotFound(path)
    private fun resolveConfigurationFromString(data: String): JsonConfiguration {
        // Try flat decode first
        runCatching { return json.decodeFromString<JsonConfiguration>(data) }.onFailure { /* try contexts */ }

        // Fallback: contexts/environments container (like iOS demo configs)
        return runCatching {
            val el = json.parseToJsonElement(data)
            val rootObj = el.jsonObject
            val contexts = rootObj["contexts"]?.jsonObject ?: return@runCatching json.decodeFromString<JsonConfiguration>(data)

            val type = System.getenv("REACHU_CONFIG_TYPE")?.trim()?.lowercase()
            val contextKey = when {
                !type.isNullOrEmpty() && contexts.containsKey(type) -> type
                contexts.size == 1 -> contexts.keys.first()
                else -> "appDemo"
            }
            val contextNode = contexts[contextKey]?.jsonObject
                ?: contexts.values.first().jsonObject

            val envs = contextNode["environments"]?.jsonObject
            val defaultEnv = contextNode["defaultEnvironment"]?.toString()?.trim('"')
            val envOverride = System.getenv("REACHU_ENVIRONMENT")?.trim()
            val envName = envOverride ?: defaultEnv ?: envs?.keys?.firstOrNull() ?: "production"
            val envNode = envs?.get(envName)?.jsonObject ?: envs?.values?.first()?.jsonObject

            val apiKey = envNode?.get("apiKey")?.toString()?.trim('"')
            val marketFallback = envNode?.get("marketFallback")?.jsonObject
            val marketJson = marketFallback?.let {
                MarketJSON(
                    countryCode = it["countryCode"]?.toString()?.trim('"'),
                    countryName = null,
                    currencyCode = it["currencyCode"]?.toString()?.trim('"'),
                    currencySymbol = null,
                    phoneCode = null,
                    flagURL = null,
                )
            }

            JsonConfiguration(
                apiKey = apiKey,
                environment = envName,
                theme = null,
                cart = null,
                network = null,
                ui = null,
                liveShow = null,
                marketFallback = marketJson,
            )
        }.getOrElse { json.decodeFromString<JsonConfiguration>(data) }
    }

    private fun loadTranslationsFromFile(fileName: String, basePath: String): Map<String, Map<String, String>>? {
        if (basePath.isBlank()) return null
        val normalized = if (fileName.endsWith(".json")) fileName else "$fileName.json"
        val path = "$basePath$normalized"
        return runCatching { readFile(path) }
            .mapCatching { json.parseToJsonElement(it) }
            .mapCatching { element ->
                val container = element.jsonObject["translations"]?.jsonObject ?: element.jsonObject
                container.mapValues { entry ->
                    entry.value.jsonObject.mapValues { inner ->
                        inner.value.jsonPrimitive.content
                    }
                }
            }
            .getOrNull()
    }

    fun loadDemoDataConfiguration(
        fileName: String = "demo-static-data",
        basePath: String,
    ): DemoDataConfiguration {
        val path = "$basePath$fileName.json"
        val file = File(path)
        if (!file.exists()) {
            VioLogger.warning("Demo data config file '$path' not found, using defaults", COMPONENT)
            return DemoDataConfiguration.default()
        }

        return try {
            val data = file.readText()
            val jsonConfig = json.decodeFromString<DemoDataJSON>(data)
            jsonConfig.toDomain()
        } catch (e: Exception) {
            VioLogger.error("Error loading demo data config: ${e.message}", COMPONENT)
            DemoDataConfiguration.default()
        }
    }

    private fun setLanguageForCountry(
        localization: LocalizationConfiguration,
        countryCode: String,
    ) {
        val languageCode = languageCodeForCountry(countryCode)
        val hasTranslation = localization.translations.containsKey(languageCode)
        val available = if (localization.translations.isEmpty()) "en (default)" else localization.translations.keys.joinToString(", ")

        if (hasTranslation) {
            VioLocalization.setLanguage(languageCode)
            VioLogger.success(
                "Language set to '$languageCode' (available: $available)",
                COMPONENT,
            )
        } else {
            VioLocalization.setLanguage(localization.defaultLanguage)
            VioLogger.warning(
                "Language '$languageCode' not available (available: $available). Using default '${localization.defaultLanguage}'",
                COMPONENT,
            )
        }
    }

    private fun languageCodeForCountry(code: String?): String {
        val normalized = code?.uppercase() ?: return "en"
        val mapping = mapOf(
            "DE" to "de", "AT" to "de", "CH" to "de",
            "US" to "en", "GB" to "en", "CA" to "en", "AU" to "en",
            "NO" to "no", "SE" to "sv", "DK" to "da", "FI" to "fi",
            "ES" to "es", "FR" to "fr", "IT" to "it", "NL" to "nl",
            "PL" to "pl", "PT" to "pt", "BR" to "pt",
            "MX" to "es", "AR" to "es", "CL" to "es", "CO" to "es",
            "JP" to "ja", "CN" to "zh", "KR" to "ko",
        )
        return mapping[normalized] ?: "en"
    }

    private fun checkMarketAvailability(countryCode: String) {
        scope.launch {
            val state = VioConfiguration.shared.state.value
            if (state.apiKey.isBlank()) {
                VioLogger.warning("Skipping market check - API key not configured", COMPONENT)
                VioConfiguration.setMarketAvailability(true, userCountryCode = countryCode, availableMarkets = emptyList())
                return@launch
            }
            try {
                val sdk = SdkClient(URL(state.environment.graphQLUrl), state.apiKey)
                VioLogger.debug("Checking market availability for $countryCode", COMPONENT)
                val markets = sdk.channel.market.getAvailable()
                val marketCodes = markets.mapNotNull { it.code?.uppercase() }
                val isAvailable = marketCodes.contains(countryCode.uppercase())
                VioConfiguration.setMarketAvailability(isAvailable, userCountryCode = countryCode, availableMarkets = markets)
                if (isAvailable) {
                    VioLogger.success(
                        "Market available for $countryCode (available markets: ${marketCodes.joinToString()})",
                        COMPONENT,
                    )
                } else {
                    VioLogger.warning(
                        "Market not available for $countryCode (available markets: ${marketCodes.joinToString()})",
                        COMPONENT,
                    )
                }
            } catch (notFound: NotFoundException) {
                VioConfiguration.setMarketAvailability(false, userCountryCode = countryCode, availableMarkets = emptyList())
                VioLogger.error("Channel market query failed (404) - SDK disabled for $countryCode", COMPONENT)
            } catch (sdkError: SdkException) {
                if (sdkError.code == "NOT_FOUND" || sdkError.status == 404) {
                    VioConfiguration.setMarketAvailability(false, userCountryCode = countryCode, availableMarkets = emptyList())
                    VioLogger.error("Channel market query failed - SDK disabled for $countryCode (${sdkError.messageText})", COMPONENT)
                } else {
                    VioConfiguration.setMarketAvailability(true, userCountryCode = countryCode, availableMarkets = emptyList())
                    VioLogger.warning("Market check failed but assuming available: ${sdkError.messageText}", COMPONENT)
                }
            } catch (t: Throwable) {
                VioConfiguration.setMarketAvailability(true, userCountryCode = countryCode, availableMarkets = emptyList())
                VioLogger.warning("Market check failed (network error) but assuming available: ${t.message}", COMPONENT)
            }
        }
    }
@Serializable
private data class JsonConfiguration(
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("campaignId") val campaignId: Int? = null,
    @SerialName("environment") val environment: String? = null,
    @SerialName("theme") val theme: ThemeJSON? = null,
    @SerialName("cart") val cart: CartJSON? = null,
    @SerialName("network") val network: NetworkJSON? = null,
    @SerialName("ui") val ui: UIJSON? = null,
    @SerialName("liveShow") val liveShow: LiveShowJSON? = null,
    @SerialName("marketFallback") val marketFallback: MarketJSON? = null,
    @SerialName("localization") val localization: LocalizationJSON? = null,
    @SerialName("productDetail") val productDetail: ProductDetailJSON? = null,
    @SerialName("campaigns") val campaigns: CampaignJSON? = null,
    @SerialName("analytics") val analytics: AnalyticsJSON? = null,
)

@Serializable
private data class ThemeJSON(
    val name: String? = null,
    val mode: String? = null,
    val colors: ThemeColorsJSON? = null,
    val lightColors: ThemeColorsJSON? = null,
    val darkColors: ThemeColorsJSON? = null,
    val borderRadius: BorderRadiusJSON? = null,
) {
    fun toDomain(): VioTheme {
        val light = (lightColors ?: colors)?.toDomain() ?: ThemeColors.vio()
        val dark = darkColors?.toDomain() ?: ThemeColors.vioDark()
        val border = borderRadius?.toDomain() ?: BorderRadiusScheme.default()
        return VioTheme(
            name = name ?: "Custom Theme",
            mode = mode?.let { parseEnum(it, ThemeMode.AUTOMATIC) } ?: ThemeMode.AUTOMATIC,
            lightColors = light,
            darkColors = dark,
            borderRadius = border,
        )
    }
}

@Serializable
private data class ThemeColorsJSON(
    val primary: String? = null,
    val secondary: String? = null,
    val success: String? = null,
    val warning: String? = null,
    val error: String? = null,
    val info: String? = null,
    val background: String? = null,
    val surface: String? = null,
    val surfaceSecondary: String? = null,
    val textPrimary: String? = null,
    val textSecondary: String? = null,
    val textTertiary: String? = null,
    val textOnPrimary: String? = null,
    val border: String? = null,
    val borderSecondary: String? = null,
    val priceColor: String? = null,
) {
    fun toDomain(): ThemeColors {
        val defaults = ThemeColors.vio()
        return ThemeColors(
            primary = primary ?: defaults.primary,
            secondary = secondary ?: defaults.secondary,
            success = success ?: defaults.success,
            warning = warning ?: defaults.warning,
            error = error ?: defaults.error,
            info = info ?: defaults.info,
            background = background ?: defaults.background,
            surface = surface ?: defaults.surface,
            surfaceSecondary = surfaceSecondary ?: defaults.surfaceSecondary,
            textPrimary = textPrimary ?: defaults.textPrimary,
            textSecondary = textSecondary ?: defaults.textSecondary,
            textTertiary = textTertiary ?: defaults.textTertiary,
            textOnPrimary = textOnPrimary ?: defaults.textOnPrimary,
            border = border ?: defaults.border,
            borderSecondary = borderSecondary ?: defaults.borderSecondary,
            priceColor = priceColor ?: primary ?: defaults.primary,
        )
    }
}

@Serializable
private data class BorderRadiusJSON(
    val none: Float? = null,
    val small: Float? = null,
    val medium: Float? = null,
    val large: Float? = null,
    val xl: Float? = null,
    @SerialName("extraLarge") val extraLarge: Float? = null,
    val round: Float? = null,
    val circle: Float? = null,
) {
    fun toDomain(): BorderRadiusScheme {
        val defaults = BorderRadiusScheme.default()
        return BorderRadiusScheme(
            none = none ?: defaults.none,
            small = small ?: defaults.small,
            medium = medium ?: defaults.medium,
            large = large ?: defaults.large,
            xl = xl ?: extraLarge ?: defaults.xl,
            circle = circle ?: round ?: defaults.circle,
        )
    }
}

@Serializable
private data class MarketJSON(
    val countryCode: String? = null,
    val countryName: String? = null,
    val currencyCode: String? = null,
    val currencySymbol: String? = null,
    val phoneCode: String? = null,
    val flagURL: String? = null,
) {
    fun toDomain() = MarketConfiguration(
        countryCode = countryCode ?: "US",
        countryName = countryName ?: "United States",
        currencyCode = currencyCode ?: "USD",
        currencySymbol = currencySymbol ?: "$",
        phoneCode = phoneCode ?: "+1",
        flagURL = flagURL ?: MarketConfiguration.default().flagURL,
    )
}

@Serializable
private data class LocalizationJSON(
    val defaultLanguage: String? = null,
    val fallbackLanguage: String? = null,
    val translations: Map<String, Map<String, String>>? = null,
    val translationsFile: String? = null,
) {
    fun toDomain(basePath: String): LocalizationConfiguration {
        val merged = translations
            ?.mapValues { it.value.toMutableMap() }
            ?.toMutableMap()
            ?: mutableMapOf<String, MutableMap<String, String>>()
        val external = translationsFile?.let { loadTranslationsFromFile(it, basePath) }.orEmpty()
        external.forEach { (language, values) ->
            val current = merged[language] ?: mutableMapOf()
            current.putAll(values)
            merged[language] = current
        }
        val finalTranslations: Map<String, Map<String, String>> =
            if (merged.isEmpty()) mapOf("en" to VioTranslationKey.defaultEnglish)
            else merged.mapValues { it.value.toMap() }

        return LocalizationConfiguration(
            defaultLanguage = defaultLanguage ?: "en",
            translations = finalTranslations,
            fallbackLanguage = fallbackLanguage ?: defaultLanguage ?: "en",
        )
    }
}

@Serializable
private data class CampaignJSON(
    val webSocketBaseURL: String? = null,
    val restAPIBaseURL: String? = null,
    val campaignAdminApiKey: String? = null,
    val autoDiscover: Boolean? = null,
    val channelId: Int? = null,
) {
    fun toDomain() = CampaignConfiguration(
        webSocketBaseURL = webSocketBaseURL ?: CampaignConfiguration.default().webSocketBaseURL,
        restAPIBaseURL = restAPIBaseURL ?: CampaignConfiguration.default().restAPIBaseURL,
        campaignAdminApiKey = campaignAdminApiKey ?: CampaignConfiguration.default().campaignAdminApiKey,
        autoDiscover = autoDiscover ?: CampaignConfiguration.default().autoDiscover,
        channelId = channelId ?: CampaignConfiguration.default().channelId,
    )
}

@Serializable
private data class AnalyticsJSON(
    val enabled: Boolean? = null,
    val mixpanelToken: String? = null,
    val apiHost: String? = null,
    val trackComponentViews: Boolean? = null,
    val trackComponentClicks: Boolean? = null,
    val trackImpressions: Boolean? = null,
    val trackTransactions: Boolean? = null,
    val trackProductEvents: Boolean? = null,
    val autocapture: Boolean? = null,
    val recordSessionsPercent: Int? = null,
) {
    fun toDomain(): AnalyticsConfiguration {
        val resolvedEnabled = enabled ?: !mixpanelToken.isNullOrBlank()
        return AnalyticsConfiguration(
            enabled = resolvedEnabled,
            mixpanelToken = mixpanelToken,
            apiHost = apiHost,
            trackComponentViews = trackComponentViews ?: true,
            trackComponentClicks = trackComponentClicks ?: true,
            trackImpressions = trackImpressions ?: true,
            trackTransactions = trackTransactions ?: true,
            trackProductEvents = trackProductEvents ?: true,
            autocapture = autocapture ?: false,
            recordSessionsPercent = recordSessionsPercent ?: 0,
        )
    }
}

@Serializable
private data class CartJSON(
    val floatingCartPosition: String? = null,
    val floatingCartDisplayMode: String? = null,
    val floatingCartSize: String? = null,
    val alwaysShowFloatingCart: Boolean? = null,
    val autoSaveCart: Boolean? = null,
    val cartPersistenceKey: String? = null,
    val maxQuantityPerItem: Int? = null,
    val showCartNotifications: Boolean? = null,
    val enableGuestCheckout: Boolean? = null,
    val requirePhoneNumber: Boolean? = null,
    val defaultShippingCountry: String? = null,
    val supportedPaymentMethods: List<String>? = null,
    val klarnaMode: String? = null,
) {
    fun toDomain(): CartConfiguration {
        val defaults = CartConfiguration.default()
        return defaults.copy(
            floatingCartPosition = floatingCartPosition?.let { parseEnum(it, defaults.floatingCartPosition) }
                ?: defaults.floatingCartPosition,
            floatingCartDisplayMode = floatingCartDisplayMode?.let { parseEnum(it, defaults.floatingCartDisplayMode) }
                ?: defaults.floatingCartDisplayMode,
            floatingCartSize = floatingCartSize?.let { parseEnum(it, defaults.floatingCartSize) }
                ?: defaults.floatingCartSize,
            alwaysShowFloatingCart = alwaysShowFloatingCart ?: defaults.alwaysShowFloatingCart,
            autoSaveCart = autoSaveCart ?: defaults.autoSaveCart,
            cartPersistenceKey = cartPersistenceKey ?: defaults.cartPersistenceKey,
            maxQuantityPerItem = maxQuantityPerItem ?: defaults.maxQuantityPerItem,
            showCartNotifications = showCartNotifications ?: defaults.showCartNotifications,
            enableGuestCheckout = enableGuestCheckout ?: defaults.enableGuestCheckout,
            requirePhoneNumber = requirePhoneNumber ?: defaults.requirePhoneNumber,
            defaultShippingCountry = defaultShippingCountry ?: defaults.defaultShippingCountry,
            supportedPaymentMethods = supportedPaymentMethods ?: defaults.supportedPaymentMethods,
            klarnaMode = klarnaMode?.let { parseEnum(it, defaults.klarnaMode) } ?: defaults.klarnaMode,
        )
    }
}

@Serializable
private data class NetworkJSON(
    val timeout: Double? = null,
    val retryAttempts: Int? = null,
    val enableCaching: Boolean? = null,
    val cacheDuration: Long? = null,
    val enableLogging: Boolean? = null,
    val logLevel: String? = null,
) {
    fun toDomain() = NetworkConfiguration(
        timeout = timeout?.toLong()?.milliseconds ?: NetworkConfiguration().timeout,
        retryAttempts = retryAttempts ?: NetworkConfiguration().retryAttempts,
        enableCaching = enableCaching ?: NetworkConfiguration().enableCaching,
        cacheDuration = cacheDuration?.milliseconds ?: NetworkConfiguration().cacheDuration,
        enableLogging = enableLogging ?: NetworkConfiguration().enableLogging,
        logLevel = logLevel?.let {
            runCatching { LogLevel.valueOf(it.uppercase()) }.getOrDefault(NetworkConfiguration().logLevel)
        } ?: NetworkConfiguration().logLevel,
    )
}

@Serializable
private data class UIJSON(
    val enableAnimations: Boolean? = null,
    val showProductBrands: Boolean? = null,
    val showDiscountBadge: Boolean? = null,
    val discountBadgeText: String? = null,
    val enableHapticFeedback: Boolean? = null,
    val shadowConfig: ShadowJSON? = null,
) {
    fun toDomain(): UIConfiguration {
        val defaults = UIConfiguration.default()
        return defaults.copy(
            enableProductCardAnimations = enableAnimations ?: defaults.enableProductCardAnimations,
            showProductBrands = showProductBrands ?: defaults.showProductBrands,
            showDiscountBadge = showDiscountBadge ?: defaults.showDiscountBadge,
            discountBadgeText = discountBadgeText ?: defaults.discountBadgeText,
            shadowConfig = shadowConfig?.toDomain() ?: defaults.shadowConfig,
            enableHapticFeedback = enableHapticFeedback ?: defaults.enableHapticFeedback,
        )
    }
}

@Serializable
private data class ShadowJSON(
    val cardShadowRadius: Float? = null,
    val cardShadowOpacity: Double? = null,
    val cardShadowOffset: SizeJSON? = null,
    val cardShadowColor: String? = null,
    val buttonShadowEnabled: Boolean? = null,
    val buttonShadowRadius: Float? = null,
    val buttonShadowOpacity: Double? = null,
    val modalShadowRadius: Float? = null,
    val modalShadowOpacity: Double? = null,
    val enableBlurEffects: Boolean? = null,
    val blurIntensity: Double? = null,
    val blurStyle: String? = null,
) {
    fun toDomain(): ShadowConfiguration {
        val defaults = ShadowConfiguration.default()
        return defaults.copy(
            cardShadowRadius = cardShadowRadius ?: defaults.cardShadowRadius,
            cardShadowOpacity = cardShadowOpacity ?: defaults.cardShadowOpacity,
            cardShadowOffsetX = cardShadowOffset?.width ?: defaults.cardShadowOffsetX,
            cardShadowOffsetY = cardShadowOffset?.height ?: defaults.cardShadowOffsetY,
            cardShadowColor = cardShadowColor?.let { parseEnum(it, defaults.cardShadowColor) } ?: defaults.cardShadowColor,
            buttonShadowEnabled = buttonShadowEnabled ?: defaults.buttonShadowEnabled,
            buttonShadowRadius = buttonShadowRadius ?: defaults.buttonShadowRadius,
            buttonShadowOpacity = buttonShadowOpacity ?: defaults.buttonShadowOpacity,
            modalShadowRadius = modalShadowRadius ?: defaults.modalShadowRadius,
            modalShadowOpacity = modalShadowOpacity ?: defaults.modalShadowOpacity,
            enableBlurEffects = enableBlurEffects ?: defaults.enableBlurEffects,
            blurIntensity = blurIntensity ?: defaults.blurIntensity,
            blurStyle = blurStyle?.let { parseEnum(it, defaults.blurStyle) } ?: defaults.blurStyle,
        )
    }
}

@Serializable
private data class SizeJSON(
    val width: Float? = null,
    val height: Float? = null,
)

@Serializable
private data class LiveShowJSON(
    val tipio: TipioJSON? = null,
    val streaming: StreamingJSON? = null,
    val shopping: ShoppingJSON? = null,
    val campaignId: Int? = null,
    val autoJoinChat: Boolean? = null,
    val enableShopping: Boolean? = null,
    val enableAutoplay: Boolean? = null,
) {
    fun toDomain(rootCampaignId: Int?): LiveShowConfiguration {
        val defaults = LiveShowConfiguration.default()
        val resolvedAutoJoin = streaming?.autoJoinChat ?: autoJoinChat ?: defaults.autoJoinChat
        val resolvedShopping = shopping?.enableShoppingDuringStream ?: enableShopping ?: defaults.enableShoppingDuringStream
        val resolvedAutoplay = streaming?.enableAutoplay ?: enableAutoplay ?: defaults.enableAutoplay
        val resolvedCampaignId = rootCampaignId ?: campaignId ?: defaults.campaignId
        return defaults.copy(
            autoJoinChat = resolvedAutoJoin,
            enableShoppingDuringStream = resolvedShopping,
            enableAutoplay = resolvedAutoplay,
            tipioApiKey = tipio?.apiKey ?: defaults.tipioApiKey,
            tipioBaseUrl = tipio?.baseUrl ?: defaults.tipioBaseUrl,
            campaignId = resolvedCampaignId,
        )
    }
}

@Serializable
private data class TipioJSON(
    val apiKey: String? = null,
    val baseUrl: String? = null,
)

@Serializable
private data class StreamingJSON(
    val autoJoinChat: Boolean? = null,
    val enableAutoplay: Boolean? = null,
)

@Serializable
private data class ShoppingJSON(
    val enableShoppingDuringStream: Boolean? = null,
)

@Serializable
private data class ProductDetailJSON(
    val modalHeight: String? = null,
    val imageFullWidth: Boolean? = null,
    val imageCornerRadius: Float? = null,
    val imageHeight: Float? = null,
    val showImageGallery: Boolean? = null,
    val headerStyle: String? = null,
    val enableImageZoom: Boolean? = null,
    val showNavigationTitle: Boolean? = null,
    val closeButtonStyle: String? = null,
    val showDescription: Boolean? = null,
    val showSpecifications: Boolean? = null,
    val showCloseButton: Boolean? = null,
    val dismissOnTapOutside: Boolean? = null,
    val enableShareButton: Boolean? = null,
) {
    fun toDomain(): ProductDetailConfiguration {
        val defaults = ProductDetailConfiguration.default()
        val modal = modalHeight?.lowercase()?.let {
            when (it) {
                "full" -> ProductDetailModalHeight.FULL
                "threequarters", "three_quarters", "three_quarter" -> ProductDetailModalHeight.THREE_QUARTERS
                "half" -> ProductDetailModalHeight.HALF
                else -> null
            }
        } ?: defaults.modalHeight

        val header = headerStyle?.lowercase()?.let {
            when (it) {
                "compact" -> ProductDetailHeaderStyle.COMPACT
                else -> ProductDetailHeaderStyle.STANDARD
            }
        } ?: defaults.headerStyle

        val closeStyle = closeButtonStyle?.lowercase()?.let {
            when (it) {
                "overlay_top_left", "overlaytopleft" -> CloseButtonStyle.OVERLAY_TOP_LEFT
                "overlay_top_right", "overlaytopright" -> CloseButtonStyle.OVERLAY_TOP_RIGHT
                else -> CloseButtonStyle.NAVIGATION_BAR
            }
        } ?: defaults.closeButtonStyle

        return defaults.copy(
            modalHeight = modal,
            imageFullWidth = imageFullWidth ?: defaults.imageFullWidth,
            imageCornerRadius = imageCornerRadius ?: defaults.imageCornerRadius,
            imageHeight = imageHeight ?: defaults.imageHeight,
            showImageGallery = showImageGallery ?: defaults.showImageGallery,
            headerStyle = header,
            enableImageZoom = enableImageZoom ?: defaults.enableImageZoom,
            showNavigationTitle = showNavigationTitle ?: defaults.showNavigationTitle,
            closeButtonStyle = closeStyle,
            showDescription = showDescription ?: defaults.showDescription,
            showSpecifications = showSpecifications ?: defaults.showSpecifications,
            showCloseButton = showCloseButton ?: defaults.showCloseButton,
            dismissOnTapOutside = dismissOnTapOutside ?: defaults.dismissOnTapOutside,
            enableShareButton = enableShareButton ?: defaults.enableShareButton,
        )
    }
}

@Serializable
private data class DemoDataJSON(
    val assets: AssetJSON? = null,
    val demoUsers: DemoUserJSON? = null,
    val productMappings: Map<String, ProductMappingJSON>? = null,
    val eventIds: EventIdJSON? = null,
    val matchDefaults: MatchDefaultJSON? = null,
    val offerBanner: OfferBannerJSON? = null,
    val carouselCards: CarouselCardsJSON? = null,
    val liveCards: LiveCardsJSON? = null,
    val sportClips: SportClipsJSON? = null,
    val matches: MatchesJSON? = null,
) {
    fun toDomain() = DemoDataConfiguration(
        assets = assets?.toDomain() ?: DemoDataConfiguration.AssetConfiguration(),
        demoUsers = demoUsers?.toDomain() ?: DemoDataConfiguration.DemoUserConfiguration(),
        productMappings = productMappings?.mapValues { it.value.toDomain() } ?: emptyMap(),
        eventIds = eventIds?.toDomain() ?: DemoDataConfiguration.EventIdConfiguration(),
        matchDefaults = matchDefaults?.toDomain() ?: DemoDataConfiguration.MatchDefaultConfiguration(),
        offerBanner = offerBanner?.toDomain() ?: DemoDataConfiguration.OfferBannerConfiguration(),
        carouselCards = carouselCards?.items?.mapNotNull { it.toDomain() } ?: emptyList(),
        liveCards = liveCards?.items?.mapNotNull { it.toDomain() } ?: emptyList(),
        sportClips = sportClips?.items?.mapNotNull { it.toDomain() } ?: emptyList(),
        matches = matches?.items?.mapNotNull { it.toDomain() } ?: emptyList(),
    )
}

@Serializable
private data class AssetJSON(
    val defaultLogo: String? = null,
    val defaultAvatar: String? = null,
    val backgroundImages: BackgroundImagesJSON? = null,
    val brandAssets: BrandAssetsJSON? = null,
    val contestAssets: ContestAssetsJSON? = null,
) {
    fun toDomain() = DemoDataConfiguration.AssetConfiguration(
        defaultLogo = defaultLogo ?: "logo1",
        defaultAvatar = defaultAvatar ?: "avatar_el",
        backgroundImages = backgroundImages?.toDomain() ?: DemoDataConfiguration.BackgroundImageAssets(),
        brandAssets = brandAssets?.toDomain() ?: DemoDataConfiguration.BrandImageAssets(),
        contestAssets = contestAssets?.toDomain() ?: DemoDataConfiguration.ContestImageAssets(),
    )
}

@Serializable
private data class BackgroundImagesJSON(
    val footballField: String? = null,
    val mainBackground: String? = null,
    val sportDetail: String? = null,
    val sportDetailImage: String? = null,
) {
    fun toDomain() = DemoDataConfiguration.BackgroundImageAssets(
        footballField = footballField ?: "football_field_bg",
        mainBackground = mainBackground ?: "bg-main",
        sportDetail = sportDetail ?: "bg",
        sportDetailImage = sportDetailImage ?: "img1",
    )
}

@Serializable
private data class BrandAssetsJSON(
    val icon: String? = null,
    val logo: String? = null,
) {
    fun toDomain() = DemoDataConfiguration.BrandImageAssets(
        icon = icon ?: "icon ",
        logo = logo ?: "logo",
    )
}

@Serializable
private data class ContestAssetsJSON(
    val giftCard: String? = null,
    val championsLeagueTickets: String? = null,
) {
    fun toDomain() = DemoDataConfiguration.ContestImageAssets(
        giftCard = giftCard ?: "elkjop_konk",
        championsLeagueTickets = championsLeagueTickets ?: "billeter_power",
    )
}

@Serializable
private data class DemoUserJSON(
    val defaultUsername: String? = null,
    val chatUsernames: List<ChatUsernameJSON>? = null,
    val socialAccounts: List<SocialAccountJSON>? = null,
) {
    fun toDomain() = DemoDataConfiguration.DemoUserConfiguration(
        defaultUsername = defaultUsername ?: "Usuario",
        chatUsernames = chatUsernames?.map { it.toDomain() } ?: emptyList(),
        socialAccounts = socialAccounts?.map { it.toDomain() } ?: emptyList(),
    )
}

@Serializable
private data class ChatUsernameJSON(
    val name: String,
    val color: String,
) {
    fun toDomain() = DemoDataConfiguration.ChatUsername(name, color)
}

@Serializable
private data class SocialAccountJSON(
    val name: String,
    val handle: String,
    val verified: Boolean,
) {
    fun toDomain() = DemoDataConfiguration.SocialAccount(name, handle, verified)
}

@Serializable
private data class ProductMappingJSON(
    val name: String,
    val productUrl: String,
    val checkoutUrl: String,
) {
    fun toDomain() = DemoDataConfiguration.ProductMapping(name, productUrl, checkoutUrl)
}

@Serializable
private data class EventIdJSON(
    val contestQuiz: String? = null,
    val contestGiveaway: String? = null,
    val productCombo: String? = null,
    val tweetHalftime1: String? = null,
    val tweetHalftime2: String? = null,
) {
    fun toDomain() = DemoDataConfiguration.EventIdConfiguration(
        contestQuiz = contestQuiz ?: "casting-contest-quiz",
        contestGiveaway = contestGiveaway ?: "casting-contest-giveaway",
        productCombo = productCombo ?: "casting-product-combo",
        tweetHalftime1 = tweetHalftime1 ?: "tweet-halftime-1",
        tweetHalftime2 = tweetHalftime2 ?: "tweet-halftime-2",
    )
}

@Serializable
private data class MatchDefaultJSON(
    val broadcastIdMappings: Map<String, String>? = null,
    val defaultScore: Int? = null,
) {
    fun toDomain() = DemoDataConfiguration.MatchDefaultConfiguration(
        broadcastIdMappings = broadcastIdMappings ?: mapOf("barcelona-psg" to "barcelona-psg-2025-01-23"),
        defaultScore = defaultScore ?: 3,
    )
}

@Serializable
private data class OfferBannerJSON(
    val countdown: CountdownJSON? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val discountText: String? = null,
    val buttonText: String? = null,
) {
    fun toDomain() = DemoDataConfiguration.OfferBannerConfiguration(
        countdown = countdown?.toDomain() ?: DemoDataConfiguration.CountdownConfiguration(),
        title = title ?: "Ukens tilbud",
        subtitle = subtitle ?: "Se denne ukes beste tilbud",
        discountText = discountText ?: "Opp til 30%",
        buttonText = buttonText ?: "Se alle tilbud",
    )
}

@Serializable
private data class CountdownJSON(
    val days: Int? = null,
    val hours: Int? = null,
    val minutes: Int? = null,
    val seconds: Int? = null,
) {
    fun toDomain() = DemoDataConfiguration.CountdownConfiguration(
        days = days ?: 2,
        hours = hours ?: 1,
        minutes = minutes ?: 59,
        seconds = seconds ?: 47,
    )
}

@Serializable
private data class CarouselCardsJSON(
    val items: List<CarouselCardItemJSON>? = null,
)

@Serializable
private data class CarouselCardItemJSON(
    val imageUrl: String? = null,
    val time: String? = null,
    val logo: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
) {
    fun toDomain(): DemoDataConfiguration.CarouselCardItem? {
        if (title == null || subtitle == null) return null
        return DemoDataConfiguration.CarouselCardItem(
            imageUrl = imageUrl ?: "img1",
            time = time ?: "",
            logo = logo ?: "",
            title = title,
            subtitle = subtitle,
        )
    }
}

@Serializable
private data class LiveCardsJSON(
    val items: List<LiveCardItemJSON>? = null,
)

@Serializable
private data class LiveCardItemJSON(
    val logo: String? = null,
    val logoIcon: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val time: String? = null,
    val backgroundImage: String? = null,
) {
    fun toDomain(): DemoDataConfiguration.LiveCardItem? {
        if (title == null || subtitle == null) return null
        return DemoDataConfiguration.LiveCardItem(
            logo = logo ?: "",
            logoIcon = logoIcon ?: "star.fill",
            title = title,
            subtitle = subtitle,
            time = time ?: "",
            backgroundImage = backgroundImage,
        )
    }
}

@Serializable
private data class SportClipsJSON(
    val items: List<SportClipItemJSON>? = null,
)

@Serializable
private data class SportClipItemJSON(
    val imageUrl: String? = null,
    val time: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val isLarge: Boolean? = null,
) {
    fun toDomain(): DemoDataConfiguration.SportClipItem? {
        if (title == null) return null
        return DemoDataConfiguration.SportClipItem(
            imageUrl = imageUrl ?: "img1",
            time = time ?: "",
            title = title,
            subtitle = subtitle ?: "",
            isLarge = isLarge ?: false,
        )
    }
}

@Serializable
private data class MatchesJSON(
    val items: List<MatchItemJSON>? = null,
)

@Serializable
private data class MatchItemJSON(
    val broadcastId: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val isLive: Boolean? = null,
) {
    fun toDomain(): DemoDataConfiguration.MatchItem? {
        if (broadcastId == null || title == null || subtitle == null || imageUrl == null) return null
        return DemoDataConfiguration.MatchItem(
            broadcastId = broadcastId,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            isLive = isLive ?: false,
        )
    }
}

private inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T {
    if (value.isNullOrBlank()) return default
    val normalized = value.trim()
        .replace("-", "_")
        .replace(" ", "_")
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .uppercase()
    return enumValues<T>().firstOrNull { it.name == normalized } ?: default
}
}
