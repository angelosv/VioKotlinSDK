package live.vio.VioUI.Components

import live.vio.VioCore.models.Component
import live.vio.sdk.core.helpers.JsonUtils

inline fun <reified T> Component.decodeConfig(): T? {
    return runCatching {
        JsonUtils.mapper.convertValue(config, T::class.java)
    }.getOrNull()
}

/**
 * Busca un componente en una lista basándose en su tipo y opcionalmente en su locationId o componentId.
 * Prioriza locationId (slot) si se proporciona.
 *
 * @param type Tipo de componente (e.g., "product_banner").
 * @param locationId Opcional. ID de ubicación/slot asignado desde el dashboard.
 * @param componentId Opcional. ID específico del componente (fallback).
 */
fun List<Component>.findComponent(
    type: String,
    locationId: String? = null,
    componentId: String? = null
): Component? {
    return firstOrNull { component ->
        if (component.type != type || !component.isActive) return@firstOrNull false
        
        when {
            locationId != null -> component.locationId == locationId
            componentId != null -> component.id == componentId
            else -> true // Return first active component of this type
        }
    }
}
