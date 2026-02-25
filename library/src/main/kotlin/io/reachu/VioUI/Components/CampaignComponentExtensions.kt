package io.reachu.VioUI.Components

import io.reachu.VioCore.models.Component
import io.reachu.sdk.core.helpers.JsonUtils

inline fun <reified T> Component.decodeConfig(): T? {
    return runCatching {
        JsonUtils.mapper.convertValue(config, T::class.java)
    }.getOrNull()
}
