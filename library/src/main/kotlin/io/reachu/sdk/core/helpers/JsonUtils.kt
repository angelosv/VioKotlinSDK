package io.reachu.sdk.core.helpers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonUtils {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)

    fun stringify(value: Any?): String = mapper.writeValueAsString(value)

    fun parseMap(json: String): Map<String, Any?> {
        if (json.isBlank()) return emptyMap()
        return mapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})
    }

    fun <T> convert(value: Any?, type: Class<T>): T = mapper.convertValue(value, type)

    inline fun <reified T> convert(value: Any?): T {
        return mapper.convertValue(value, object : TypeReference<T>() {})
    }
}
