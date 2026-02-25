package io.reachu.VioCore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contexto de broadcast para el sistema de engagement.
 *
 * Alineado con la versión de Swift:
 * - `broadcastId`: identificador único del broadcast/partido.
 * - `channelId`: identificador opcional del canal.
 * - `startTime` / `endTime`: timestamps ISO8601.
 *
 * Incluye campos legacy (`matchId`, `matchStartTime`) para mantener
 * compatibilidad hacia atrás con integraciones antiguas.
 */
@Serializable
data class BroadcastContext(
    val broadcastId: String,
    val channelId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    // --- Backward compatibility con modelos antiguos ---
    @Deprecated("Usar broadcastId en su lugar", level = DeprecationLevel.WARNING)
    @SerialName("matchId")
    val legacyMatchId: String? = null,
    @Deprecated("Usar startTime en su lugar", level = DeprecationLevel.WARNING)
    @SerialName("matchStartTime")
    val legacyMatchStartTime: String? = null,
)

