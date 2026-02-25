package com.reachu.viaplaydemo.casting

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class CastDevice(
    val id: String,
    val name: String,
    val type: CastDeviceType,
    val location: String?,
)

enum class CastDeviceType { CHROMECAST, AIRPLAY }

data class CastUiState(
    val isCasting: Boolean = false,
    val isConnecting: Boolean = false,
    val selectedDevice: CastDevice? = null,
)

class CastingManager private constructor() {

    private val _state = MutableStateFlow(CastUiState())
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    val devices = listOf(
        CastDevice("1", "Living TV", CastDeviceType.CHROMECAST, "Kolbotn - Nordstrand 2"),
        CastDevice("2", "Kitchen Display", CastDeviceType.AIRPLAY, null),
        CastDevice("3", "Bedroom TV", CastDeviceType.CHROMECAST, null),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun startCasting(device: CastDevice) {
        _state.value = _state.value.copy(isConnecting = true)
        scope.launch {
            delay(1500)
            _state.value = CastUiState(
                isCasting = true,
                isConnecting = false,
                selectedDevice = device,
            )
        }
    }

    fun stopCasting() {
        _state.value = CastUiState()
    }

    companion object {
        val shared = CastingManager()
    }
}
