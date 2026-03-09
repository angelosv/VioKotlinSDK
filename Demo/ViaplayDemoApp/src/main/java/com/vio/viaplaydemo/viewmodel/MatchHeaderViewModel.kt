package com.vio.viaplaydemo.viewmodel

import com.vio.viaplaydemo.services.BackendMatchDataService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import live.vio.VioEngagementSystem.BroadcastValidationService

/**
 * ViewModel ligero para alimentar el header del partido con:
 * - logos de equipos (via BroadcastValidationService)
 * - marcador en vivo (via BackendMatchDataService)
 *
 * No depende de AndroidX ViewModel para mantenerse simple y portable.
 */
class MatchHeaderViewModel(
    private val backendMatchDataService: BackendMatchDataService = BackendMatchDataService(),
) {

    data class State(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val homeTeamName: String? = null,
        val awayTeamName: String? = null,
        val homeLogoUrl: String? = null,
        val awayLogoUrl: String? = null,
        val homeScore: Int? = null,
        val awayScore: Int? = null,
        val minute: Int? = null,
        val period: String? = null,
    )

    private val scope = CoroutineScope(Job() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var currentBroadcastId: String? = null

    /**
     * Inicia la carga de logos y el polling del marcador a partir de un contentId/country.
     *
     * Se puede llamar varias veces; cada llamada reinicia el ciclo de polling.
     */
    fun start(contentId: String?, country: String?) {
        if (contentId.isNullOrBlank() || country.isNullOrBlank()) {
            _state.value = State(errorMessage = "ContentId o país inválidos")
            return
        }

        scope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            // 1) Resolver broadcast + logos
            val validation = runCatching {
                BroadcastValidationService.validate(contentId, country)
            }.getOrNull()

            val broadcastId = validation?.broadcastId
            if (validation == null || !validation.hasEngagement || broadcastId.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Ingen aktiv kamp for dette innholdet",
                )
                return@launch
            }

            currentBroadcastId = broadcastId

            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = null,
                homeTeamName = validation.homeTeam?.name ?: _state.value.homeTeamName,
                awayTeamName = validation.awayTeam?.name ?: _state.value.awayTeamName,
                homeLogoUrl = validation.homeTeam?.logoUrl ?: _state.value.homeLogoUrl,
                awayLogoUrl = validation.awayTeam?.logoUrl ?: _state.value.awayLogoUrl,
            )

            // 2) Primer fetch inmediato
            updateScoreOnce(broadcastId)

            // 3) Polling cada 30s mientras el scope esté activo
            while (isActive && currentBroadcastId == broadcastId) {
                delay(30_000)
                if (!isActive || currentBroadcastId != broadcastId) break
                updateScoreOnce(broadcastId)
            }
        }
    }

    private suspend fun updateScoreOnce(broadcastId: String) {
        val score = backendMatchDataService.fetchScore(broadcastId)
        if (score != null) {
            _state.value = _state.value.copy(
                homeScore = score.home,
                awayScore = score.away,
                minute = score.minute,
                period = score.period,
            )
        }
    }

    fun clear() {
        currentBroadcastId = null
        scope.cancel()
    }
}

