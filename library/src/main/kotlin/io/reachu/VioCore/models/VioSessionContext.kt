package io.reachu.VioCore.models

import java.util.UUID

/**
 * Contexto de sesión de usuario para un broadcast concreto.
 *
 * A diferencia de [BroadcastContext] — que identifica el broadcast —, `VioSessionContext`
 * encapsula la identidad del usuario que participa en él. Es una clase instanciable
 * (no singleton) para soportar múltiples sesiones simultáneas.
 *
 * ## Uso básico
 * ```kotlin
 * val session = VioSessionContext(
 *     broadcastContext = BroadcastContext(broadcastId = "broadcast-123"),
 *     userId = "user-abc",  // opcional: se genera UUID local si se omite
 * )
 * EngagementManager.shared.voteInPoll(pollId, optionId, broadcastContext, session)
 * ```
 *
 * ## Paridad Swift
 * Equivalente a `VioSessionContext` del Swift SDK.
 */
data class VioSessionContext(
    /** Contexto del broadcast al que pertenece esta sesión. */
    val broadcastContext: BroadcastContext,
    /**
     * Identificador del usuario. Si es null, se genera automáticamente un UUID local
     * persistido para el dispositivo (ver [generateLocalUserId]).
     */
    val userId: String = generateLocalUserId(),
) {
    companion object {
        // Almacén en memoria del UUID local (persiste durante la vida del proceso).
        // En producción real, se persistiría en SharedPreferences/DataStore.
        @Volatile
        private var _localUserId: String? = null

        /**
         * Devuelve un UUID estable por proceso. La primera llamada genera un UUID aleatorio
         * que se reutiliza durante el ciclo de vida del proceso.
         *
         * Si la app integra el SDK puede sobreescribir el userId pasando explícitamente el
         * identificador de su sistema de autenticación.
         */
        fun generateLocalUserId(): String {
            return _localUserId ?: synchronized(this) {
                _localUserId ?: UUID.randomUUID().toString().also { _localUserId = it }
            }
        }

        /**
         * Permite sobreescribir el UUID local (útil en tests o cuando el sistema de auth
         * provee un userId externo que debe persistir globalmente).
         */
        fun setLocalUserId(userId: String) {
            _localUserId = userId
        }
    }
}
