package live.vio.VioUI.Components

import live.vio.VioCore.models.Component
import live.vio.sdk.core.helpers.JsonUtils

inline fun <reified T> Component.decodeConfig(): T? {
    return runCatching {
        JsonUtils.mapper.convertValue(config, T::class.java)
    }.getOrNull()
}
