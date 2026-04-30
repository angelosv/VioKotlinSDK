package live.vio.VioCore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * v2 mobile bootstrap response.
 *
 * Source: `GET /v2/mobile/config`
 */
@Serializable
data class SdkBootstrapResponse(
    @SerialName("endpoints") val endpoints: Endpoints? = null,
    @SerialName("campaign") val campaign: Campaign? = null,
    @SerialName("primarySponsor") val primarySponsor: SponsorBlock? = null,
    @SerialName("secondarySponsors") val secondarySponsors: List<SponsorBlock>? = null,
    @SerialName("features") val features: Features? = null,
) {
    @Serializable
    data class Endpoints(
        @SerialName("webSocketBase") val webSocketBase: String? = null,
        @SerialName("commerceGraphQL") val commerceGraphQL: String? = null,
        @SerialName("restBase") val restBase: String? = null,
    )

    @Serializable
    data class Campaign(
        @SerialName("id") val id: Int? = null,
        @SerialName("name") val name: String? = null,
        @SerialName("logo") val logo: String? = null,
        @SerialName("isActive") val isActive: Boolean? = null,
        @SerialName("isPaused") val isPaused: Boolean? = null,
    )

    @Serializable
    data class SponsorBlock(
        @SerialName("id") val id: Int,
        @SerialName("name") val name: String,
        @SerialName("avatarUrl") val avatarUrl: String? = null,
        @SerialName("logoUrl") val logoUrl: String? = null,
        @SerialName("primaryColor") val primaryColor: String? = null,
        @SerialName("secondaryColor") val secondaryColor: String? = null,
        @SerialName("commerce") val commerce: CommerceBlock? = null,
    ) {
        @Serializable
        data class CommerceBlock(
            @SerialName("apiKey") val apiKey: String,
            @SerialName("channelId") val channelId: String? = null,
            @SerialName("paymentMethods") val paymentMethods: List<String>? = null,
        )

        fun toVioSponsor(): VioSponsor =
            VioSponsor(
                id = id,
                name = name,
                avatarUrl = avatarUrl,
                logoUrl = logoUrl,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                commerce = commerce?.let {
                    VioSponsor.CommerceBlock(
                        apiKey = it.apiKey,
                        channelId = it.channelId,
                        paymentMethods = it.paymentMethods ?: emptyList(),
                    )
                },
            )
    }

    @Serializable
    data class Features(
        @SerialName("engagement") val engagement: Boolean? = null,
        @SerialName("shoppable") val shoppable: Boolean? = null,
        @SerialName("lineup") val lineup: Boolean? = null,
    )
}

