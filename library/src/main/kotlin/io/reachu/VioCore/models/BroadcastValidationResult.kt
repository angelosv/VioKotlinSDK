package io.reachu.VioCore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resultado de la validación de broadcast via el endpoint GET /v1/sdk/broadcast.
 *
 * Indica si hay engagement activo para un contentId dado y, en caso afirmativo,
 * proporciona el broadcastId necesario para cargar polls/contests.
 */
data class BroadcastValidationResult(
    /** Si hay engagement activo para este broadcast. */
    val hasEngagement: Boolean,
    /** ID del broadcast a usar para cargar polls/contests. Null si no hay engagement. */
    val broadcastId: String?,
    /** Nombre descriptivo del broadcast. */
    val broadcastName: String?,
    /** Estado del broadcast (e.g. "active", "scheduled", "ended"). */
    val status: String?,
    /** ID de la campaña asociada. */
    val campaignId: String?,
    /** Canal de WebSocket para recibir actualizaciones en tiempo real. */
    val websocketChannel: String?,
    /** Componentes disponibles en la campaña (e.g. "polls", "contests", "chat"). */
    val campaignComponents: List<String> = emptyList(),
    /** Componentes disponibles en el broadcast. */
    val broadcastComponents: List<String> = emptyList(),
)

/**
 * DTO interno para parsear la respuesta JSON del endpoint /v1/sdk/broadcast.
 */
@Serializable
internal data class BroadcastValidationDto(
    @SerialName("hasEngagement")
    val hasEngagement: Boolean = false,
    @SerialName("broadcastId")
    val broadcastId: String? = null,
    @SerialName("broadcastName")
    val broadcastName: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("campaignId")
    val campaignId: String? = null,
    @SerialName("websocketChannel")
    val websocketChannel: String? = null,
    @SerialName("campaignComponents")
    val campaignComponents: List<String> = emptyList(),
    @SerialName("broadcastComponents")
    val broadcastComponents: List<String> = emptyList(),
) {
    fun toDomain() = BroadcastValidationResult(
        hasEngagement = hasEngagement,
        broadcastId = broadcastId,
        broadcastName = broadcastName,
        status = status,
        campaignId = campaignId,
        websocketChannel = websocketChannel,
        campaignComponents = campaignComponents,
        broadcastComponents = broadcastComponents,
    )
}
