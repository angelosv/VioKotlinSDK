package live.vio.VioCore.utils

import android.util.Log
import live.vio.VioCore.configuration.LogLevel
import live.vio.VioCore.configuration.VioConfiguration

object VioLogger {
    enum class Level { DEBUG, INFO, WARNING, ERROR }

    private const val DEFAULT_TAG = "VioSDK"

    private val networkConfig
        get() = VioConfiguration.shared.state.value.network

    private val isEnabled: Boolean
        get() = VioConfiguration.loggingEnabled

    private val minLevel: Level
        get() = when (networkConfig.logLevel) {
            LogLevel.DEBUG -> Level.DEBUG
            LogLevel.INFO -> Level.INFO
            LogLevel.WARNING -> Level.WARNING
            LogLevel.ERROR -> Level.ERROR
        }

    private fun log(level: Level, message: String, component: String? = null) {
        if (!isEnabled || level.ordinal < minLevel.ordinal) return
        val tag = component ?: DEFAULT_TAG
        when (level) {
            Level.DEBUG -> Log.d(tag, "🔍 $message")
            Level.INFO -> Log.i(tag, "ℹ️ $message")
            Level.WARNING -> Log.w(tag, "⚠️ $message")
            Level.ERROR -> Log.e(tag, "❌ $message")
        }
    }

    fun debug(message: String, component: String? = null) = log(Level.DEBUG, message, component)
    fun info(message: String, component: String? = null) = log(Level.INFO, message, component)
    fun warning(message: String, component: String? = null) = log(Level.WARNING, message, component)
    fun error(message: String, component: String? = null) = log(Level.ERROR, message, component)

    fun success(message: String, component: String? = null) {
        if (!isEnabled) return
        val tag = component ?: DEFAULT_TAG
        Log.i(tag, "✅ $message")
    }
}
