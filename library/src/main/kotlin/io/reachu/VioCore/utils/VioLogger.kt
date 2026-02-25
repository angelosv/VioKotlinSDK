package io.reachu.VioCore.utils

import io.reachu.VioCore.configuration.LogLevel
import io.reachu.VioCore.configuration.VioConfiguration

object VioLogger {
    enum class Level { DEBUG, INFO, WARNING, ERROR }

    private val networkConfig
        get() = VioConfiguration.shared.state.value.network

    private val isEnabled: Boolean
        get() = networkConfig.enableLogging

    private val minLevel: Level
        get() = when (networkConfig.logLevel) {
            LogLevel.DEBUG -> Level.DEBUG
            LogLevel.INFO -> Level.INFO
            LogLevel.WARNING -> Level.WARNING
            LogLevel.ERROR -> Level.ERROR
        }

    private fun log(level: Level, message: String, component: String? = null) {
        if (!isEnabled || level.ordinal < minLevel.ordinal) return
        val prefix = component?.let { "[$it]" } ?: "[Vio]"
        val emoji = when (level) {
            Level.DEBUG -> "🔍"
            Level.INFO -> "ℹ️"
            Level.WARNING -> "⚠️"
            Level.ERROR -> "❌"
        }
        println("$emoji $prefix $message")
    }

    fun debug(message: String, component: String? = null) = log(Level.DEBUG, message, component)
    fun info(message: String, component: String? = null) = log(Level.INFO, message, component)
    fun warning(message: String, component: String? = null) = log(Level.WARNING, message, component)
    fun error(message: String, component: String? = null) = log(Level.ERROR, message, component)

    fun success(message: String, component: String? = null) {
        if (!isEnabled) return
        val prefix = component?.let { "[$it]" } ?: "[Vio]"
        println("✅ $prefix $message")
    }
}
