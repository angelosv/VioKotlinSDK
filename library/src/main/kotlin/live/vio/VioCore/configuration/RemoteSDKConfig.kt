package live.vio.VioCore.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo Kotlin para la respuesta del endpoint remoto de configuración del SDK.
 *
 * GET /v1/sdk/config?apiKey=<apiKey>
 */
@Serializable
data class VioRemoteConfig(
    @SerialName("clientApp") val clientApp: RemoteClientApp? = null,
    @SerialName("endpoints") val endpoints: RemoteEndpoints? = null,
    @SerialName("features") val features: RemoteFeatures? = null,
    @SerialName("commerce") val commerce: RemoteCommerceConfig? = null,
    @SerialName("theme") val theme: RemoteThemeConfig? = null,
    @SerialName("markets") val markets: List<String>? = null,
)

@Serializable
data class RemoteClientApp(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("apiKey") val apiKey: String? = null,
)

@Serializable
data class RemoteEndpoints(
    @SerialName("restBase") val restBase: String? = null,
    @SerialName("webSocketBase") val webSocketBase: String? = null,
    @SerialName("commerceGraphQL") val commerceGraphQL: String? = null,
)

@Serializable
data class RemoteFeatures(
    @SerialName("engagement") val engagement: Boolean? = null,
    @SerialName("adPlacements") val adPlacements: Boolean? = null,
    @SerialName("commerce") val commerce: Boolean? = null,
    @SerialName("lineup") val lineup: Boolean? = null,
)

@Serializable
data class RemoteCommerceConfig(
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("endpoint") val endpoint: String? = null,
    @SerialName("channelId") val channelId: Int? = null,
)

@Serializable
data class RemoteThemeConfig(
    @SerialName("primaryColor") val primaryColor: String? = null,
    @SerialName("accentColor") val accentColor: String? = null,
)

