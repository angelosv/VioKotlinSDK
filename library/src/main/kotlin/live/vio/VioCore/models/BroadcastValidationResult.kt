package live.vio.VioCore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Información básica de un equipo asociada a un broadcast.
 *
 * Incluye, entre otros, la URL del logo que se usará en cabeceras como el
 * marcador del partido.
 */
data class MatchTeamInfo(
    val name: String? = null,
    val shortName: String? = null,
    val logoUrl: String? = null,
)

/**
 * Resultado de la validación de broadcast via el endpoint GET /v1/sdk/broadcast.
 *
 * Indica si hay engagement activo para un contentId dado y, en caso afirmativo,
 * proporciona el broadcastId necesario para cargar polls/contests y metadatos
 * como los equipos y sus logos.
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
    /** Información del equipo local (nombre, logo, etc.). */
    val homeTeam: MatchTeamInfo? = null,
    /** Información del equipo visitante (nombre, logo, etc.). */
    val awayTeam: MatchTeamInfo? = null,
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
    @SerialName("homeTeam")
    val homeTeam: MatchTeamInfoDto? = null,
    @SerialName("awayTeam")
    val awayTeam: MatchTeamInfoDto? = null,
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
        homeTeam = homeTeam?.toDomain(),
        awayTeam = awayTeam?.toDomain(),
    )
}

@Serializable
internal data class MatchTeamInfoDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("shortName")
    val shortName: String? = null,
    @SerialName("logoUrl")
    val logoUrl: String? = null,
) {
    fun toDomain() = MatchTeamInfo(
        name = name,
        shortName = shortName,
        logoUrl = logoUrl,
    )
}
