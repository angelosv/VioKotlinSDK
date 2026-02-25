package io.reachu.demo.util

import io.reachu.sdk.core.helpers.JsonUtils
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

object Logger {
    private val mapper = JsonUtils.mapper.writerWithDefaultPrettyPrinter()

    fun section(title: String) {
        println("\n=== $title ===")
    }

    fun info(message: String) = println("[INFO] $message")

    fun warn(message: String) = println("[WARN] $message")

    fun error(message: String) = println("[ERROR] $message")

    fun success(message: String) = println("[SUCCESS] $message")

    fun json(value: Any?, label: String? = null) {
        runCatching {
            val json = mapper.writeValueAsString(value)
            val prefix = label?.let { "$it:\n" } ?: ""
            println("$prefix$json")
        }.onFailure {
            warn("Failed to encode JSON (${it.message})")
        }
    }

    suspend fun <T> measure(name: String, block: suspend () -> T): Pair<T, Duration> {
        val start = System.nanoTime()
        val result = block()
        val elapsed = (System.nanoTime() - start).nanoseconds
        info("$name completed in ${format(elapsed)}")
        return result to elapsed
    }

    fun warnFailures(failures: List<Pair<String, String>>) {
        if (failures.isEmpty()) return
        warn("Failures:")
        failures.forEach { (id, message) -> warn(" - $id: $message") }
    }

    private fun format(duration: Duration): String {
        val millis = duration.inWholeMilliseconds
        return if (millis >= 1000) {
            val seconds = duration.inWholeMilliseconds / 1000.0
            "${seconds.roundToInt()}s"
        } else {
            "${millis}ms"
        }
    }
}

