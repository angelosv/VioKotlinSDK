package io.reachu.liveui.components

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coroutine-based port of the Swift `DynamicComponentManager`.
 *
 * The manager keeps track of registered components, automatically activates them
 * based on start/end times and exposes the current active set via StateFlow so
 * UI layers can render banners or product spotlights.
 */
class DynamicComponentManager(
    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Default),
) {

    private val _registered = MutableStateFlow<Map<String, DynamicComponent>>(emptyMap())
    val registered: StateFlow<Map<String, DynamicComponent>> = _registered.asStateFlow()

    private val _activeComponents = MutableStateFlow<List<DynamicComponent>>(emptyList())
    val activeComponents: StateFlow<List<DynamicComponent>> = _activeComponents.asStateFlow()

    private val jobs = ConcurrentHashMap<String, Job>()

    fun reset() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _registered.value = emptyMap()
        _activeComponents.value = emptyList()
    }

    fun register(components: List<DynamicComponent>) {
        components.forEach { register(it) }
    }

    fun register(component: DynamicComponent) {
        _registered.value = _registered.value + (component.id to component)
        scheduleActivation(component)
    }

    fun activate(id: String) {
        val component = _registered.value[id] ?: return
        if (_activeComponents.value.any { it.id == id }) return
        _activeComponents.value = _activeComponents.value + component
        scheduleDeactivation(component)
    }

    fun deactivate(id: String) {
        _activeComponents.value = _activeComponents.value.filterNot { it.id == id }
        jobs.remove(id)?.cancel()
    }

    private fun scheduleActivation(component: DynamicComponent) {
        val start = component.startTime
        val now = Instant.now()
        when {
            start != null && start.isAfter(now) -> {
                val delayMillis = Duration.between(now, start).toMillis()
                jobs[component.id] = scope.launch {
                    delay(delayMillis)
                    activate(component.id)
                }
            }
            component.triggerOn == DynamicComponentTrigger.STREAM_START || start == null -> {
                activate(component.id)
            }
        }
    }

    private fun scheduleDeactivation(component: DynamicComponent) {
        val end = component.endTime
        val now = Instant.now()
        val durationMillis: Long? = when {
            end != null && end.isAfter(now) -> Duration.between(now, end).toMillis()
            component.data is DynamicComponentData.Banner -> {
                (component.data as DynamicComponentData.Banner).duration?.toMillis()
            }
            else -> null
        }

        if (durationMillis != null) {
            jobs[component.id] = scope.launch {
                delay(durationMillis)
                deactivate(component.id)
            }
        }
    }
}
