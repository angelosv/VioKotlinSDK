package live.vio.VioCore.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Model representing the payload of a component with type "sponsor_slots".
 * This is used for "Sponsor Moments" (Shoppable Ads).
 */
data class SponsorSlot(
    @JsonProperty("type") val type: String = "product",
    @JsonProperty("config") val config: Map<String, Any?> = emptyMap()
)
