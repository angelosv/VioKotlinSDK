package com.reachu.demoapp.viaplay.models

/**
 * Demo model for casting contests, following the PowerContestEvent pattern in Swift.
 */
data class CastingContestEvent(
    val id: String,
    val title: String,
    val description: String,
    val prize: String,
    val contestType: String, // "Quiz" or "Giveaway"
    val displayTime: String? = null,
    val isActive: Boolean = true,
    val metadata: Map<String, String>? = null
)

/**
 * Demo model for casting products, following the PowerProductEvent pattern in Swift.
 */
data class CastingProductEvent(
    val id: String,
    val productId: String? = null,
    val productIds: List<String>? = null,
    val title: String,
    val description: String,
    val imageAsset: String? = null,
    val videoTimestamp: Long? = null,
    val castingProductUrl: String? = null,
    val castingCheckoutUrl: String? = null,
    val metadata: Map<String, String>? = null
) {
    val allProductIds: List<String>
        get() = productIds ?: listOfNotNull(productId)
}
