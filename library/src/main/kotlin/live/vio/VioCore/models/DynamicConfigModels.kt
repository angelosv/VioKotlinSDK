package live.vio.VioCore.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration for the sponsor of a campaign.
 * Mirrors the Swift `SponsorConfig`.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SponsorConfig(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("name") val name: String = "",
    @JsonProperty("logoUrl") val logoUrl: String? = null,
    @JsonProperty("avatarUrl") val avatarUrl: String? = null,
    @JsonProperty("primaryColor") val primaryColor: String? = null,
    @JsonProperty("secondaryColor") val secondaryColor: String? = null,
    @JsonProperty("badgeText") val badgeText: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("config") val config: Map<String, Any?>? = null
)

/**
 * Configuration for commerce integrations.
 * Mirrors the Swift `CommerceConfig`.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CommerceConfig(
    @JsonProperty("enabled") val enabled: Boolean = false,
    @JsonProperty("apiKey") val apiKey: String? = null,
    @JsonProperty("endpoint") val endpoint: String? = null,
    @JsonProperty("channelId") val channelId: Int? = null
)

/**
 * Root wrapper for dynamic configuration received from the backend.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DynamicConfig(
    @JsonProperty("sponsor") val sponsor: SponsorConfig? = null,
    @JsonProperty("integrations") val integrations: Map<String, CommerceConfig>? = null,
    @JsonProperty("checkout") val checkout: CheckoutConfig? = null,
    @JsonProperty("commerce") val directCommerce: CommerceConfig? = null
) {
    val commerce: CommerceConfig?
        get() = directCommerce ?: integrations?.get("commerce")
}
