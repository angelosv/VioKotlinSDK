package live.vio.VioCore.models

/**
 * Sponsor model (multi-sponsor v2 bootstrap).
 *
 * Mirrors Swift `VioSponsor`.
 */
data class VioSponsor(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
    val logoUrl: String? = null,
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val commerce: CommerceBlock? = null,
) {
    data class CommerceBlock(
        val apiKey: String,
        val channelId: String? = null,
        val paymentMethods: List<String> = emptyList(),
    )
}

