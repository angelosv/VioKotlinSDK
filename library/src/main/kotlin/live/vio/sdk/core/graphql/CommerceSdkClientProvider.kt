package live.vio.sdk.core.graphql

import live.vio.VioCore.configuration.VioConfiguration
import live.vio.VioCore.utils.VioLogger
import live.vio.sdk.core.VioSdkClient
import java.net.URL

object CommerceSdkClientProvider {
    private const val TAG = "CommerceSdkClientProvider"

    @Volatile
    var activeSponsorId: Int? = null
        private set

    private data class SdkClientEntry(
        val client: VioSdkClient,
        val url: String,
        val apiKey: String,
    )

    @Volatile
    private var primaryClient: VioSdkClient? = null
    @Volatile
    private var primaryUrl: String? = null
    @Volatile
    private var primaryApiKey: String? = null

    private val sponsorClients: MutableMap<Int, SdkClientEntry> = mutableMapOf()
    private val lock = Any()

    fun client(configuration: VioConfiguration = VioConfiguration.shared): VioSdkClient {
        val state = configuration.state.value
        val url = state.sdkBootstrapCommerceGraphQLURL
            ?: state.commerce?.endpoint
            ?: state.environment.graphQLUrl
        val apiKey = state.sdkBootstrapCommerceApiKey
            ?: state.commerce?.apiKey
            ?: state.apiKey

        return synchronized(lock) {
            val cached = primaryClient
            if (cached != null && primaryUrl == url && primaryApiKey == apiKey) {
                return@synchronized cached
            }

            val newClient = VioSdkClient(baseUrl = URL(url), apiKey = apiKey)
            primaryClient = newClient
            primaryUrl = url
            primaryApiKey = apiKey
            newClient
        }
    }

    fun client(forSponsorId: Int?, configuration: VioConfiguration = VioConfiguration.shared): VioSdkClient {
        if (forSponsorId == null) return client(configuration)

        val sponsor = VioConfiguration.sponsor(withId = forSponsorId)
        val sponsorCommerce = VioConfiguration.commerce(forSponsorId = forSponsorId)
        if (sponsor == null || sponsorCommerce == null) {
            VioLogger.warning("no commerce key for sponsor=$forSponsorId, using primary", TAG)
            return client(configuration)
        }

        val state = configuration.state.value
        val url = state.sdkBootstrapCommerceGraphQLURL
            ?: state.commerce?.endpoint
            ?: state.environment.graphQLUrl
        val sponsorApiKey = sponsorCommerce.apiKey

        return synchronized(lock) {
            val cached = sponsorClients[forSponsorId]
            if (cached != null && cached.url == url && cached.apiKey == sponsorApiKey) {
                activeSponsorId = forSponsorId
                VioLogger.info("[CommerceSdkClientProvider] activeSponsorId=$forSponsorId", TAG)
                return@synchronized cached.client
            }

            val newClient = VioSdkClient(baseUrl = URL(url), apiKey = sponsorApiKey)
            sponsorClients[forSponsorId] = SdkClientEntry(
                client = newClient,
                url = url,
                apiKey = sponsorApiKey,
            )
            activeSponsorId = forSponsorId
            val maskedKey = if (sponsorApiKey.length > 8) sponsorApiKey.take(8) + "…" else sponsorApiKey
            VioLogger.info("[CommerceSdkClientProvider] client for sponsorId=$forSponsorId -> apiKey=$maskedKey", TAG)
            VioLogger.info("[CommerceSdkClientProvider] activeSponsorId=$forSponsorId", TAG)
            newClient
        }
    }

    fun clear() {
        synchronized(lock) {
            primaryClient = null
            primaryUrl = null
            primaryApiKey = null
            sponsorClients.clear()
            activeSponsorId = null
        }
    }
}
