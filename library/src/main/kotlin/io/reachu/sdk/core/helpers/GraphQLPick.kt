package io.reachu.sdk.core.helpers

import io.reachu.sdk.core.errors.SdkException

object GraphQLPick {
    @Suppress("UNCHECKED_CAST")
    fun <T> pickPath(
        root: Map<String, Any?>?,
        path: List<Any>,
    ): T? {
        var current: Any? = root
        for (segment in path) {
            current = when {
                segment is String && current is Map<*, *> -> current[segment]
                segment is Int && current is List<*> && segment in current.indices -> current[segment]
                else -> return null
            }
        }
        return current as? T
    }

    fun <T> pickPathRequired(
        root: Map<String, Any?>?,
        path: List<Any>,
        code: String = "EMPTY_RESPONSE",
        message: String? = null,
    ): T {
        return pickPath<T>(root, path)
            ?: throw SdkException(message ?: "Required value not found at path $path", code = code)
    }

    inline fun <reified T> decodeJSON(obj: Any?): T {
        return JsonUtils.convert(obj)
    }
}
