package io.reachu.liveui.components

import io.reachu.VioCore.models.LiveShowCartManaging
import io.reachu.VioCore.models.Product
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.LiveShowCartManagerProvider
import io.reachu.liveui.configuration.VioLiveShowConfiguration
import io.reachu.liveshow.models.LiveStream

data class LiveShowOverlayState(
    val stream: LiveStream? = null,
    val isVisible: Boolean = false,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = true,
    val controlsVisible: Boolean = true,
    val selectedProduct: Product? = null,
    val showProductsGrid: Boolean = false,
    val configuration: VioLiveShowConfiguration = VioLiveShowConfiguration.default,
    val activeComponents: List<RenderedComponent> = emptyList(),
)

class VioLiveShowOverlayController(
    private val liveShowManager: LiveShowManager = LiveShowManager.shared,
    private val cartManager: LiveShowCartManaging = LiveShowCartManagerProvider.default,
    private val configuration: VioLiveShowConfiguration = VioLiveShowConfiguration.default,
    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Default),
    private val dynamicManager: DynamicComponentManager = DynamicComponentManager(),
    val likesManager: LiveLikesManager = LiveLikesManager.shared,
) {

    private val renderer = DynamicComponentRenderer(dynamicManager)
    private val _state = MutableStateFlow(LiveShowOverlayState(configuration = configuration))
    val state: StateFlow<LiveShowOverlayState> = _state.asStateFlow()

    private val isStarted = AtomicBoolean(false)

    init {
        scope.launch {
            liveShowManager.currentStream.collectLatest { stream ->
                handleStreamChange(stream)
            }
        }
        scope.launch {
            renderer.renderedComponents.collectLatest { components ->
                _state.value = _state.value.copy(activeComponents = components)
            }
        }
        scope.launch {
            liveShowManager.heartEvents.collect {
                likesManager.handleRemoteHeartEvent()
            }
        }
    }

    private suspend fun handleStreamChange(stream: LiveStream?) {
        if (stream == null) {
            _state.value = _state.value.copy(
                stream = null,
                isVisible = false,
                isLoading = false,
                isPlaying = false,
            )
            dynamicManager.reset()
            return
        }

        _state.value = _state.value.copy(
            stream = stream,
            isVisible = true,
            isLoading = true,
            isPlaying = true,
            isMuted = true,
        )

        runCatching {
            DynamicComponentsService.fetch(stream.id)
        }.onSuccess { components ->
            dynamicManager.reset()
            dynamicManager.register(components)
        }.onFailure {
            // keep overlay usable even if no dynamic components are returned
        }

        _state.value = _state.value.copy(isLoading = false)
        isStarted.set(true)
    }

    fun toggleControls() {
        _state.value = _state.value.copy(controlsVisible = !_state.value.controlsVisible)
    }

    fun togglePlayback() {
        _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
    }

    fun toggleMute() {
        _state.value = _state.value.copy(isMuted = !_state.value.isMuted)
    }

    fun selectProduct(product: Product) {
        _state.value = _state.value.copy(selectedProduct = product)
    }

    fun dismissProductDetail() {
        _state.value = _state.value.copy(selectedProduct = null)
    }

    fun toggleProductGrid() {
        _state.value = _state.value.copy(showProductsGrid = !_state.value.showProductsGrid)
    }

    fun addSelectedProductToCart() {
        val product = _state.value.selectedProduct ?: return
        scope.launch {
            cartManager.addProduct(product, quantity = 1)
        }
    }

    fun sendHeart(isVideoLive: Boolean) {
        liveShowManager.sendHeartForCurrentStream(isVideoLive)
        likesManager.createUserLike()
    }

    fun dismissOverlay() {
        liveShowManager.hideLiveStream()
        dynamicManager.reset()
        _state.value = _state.value.copy(stream = null, isVisible = false)
    }
}
