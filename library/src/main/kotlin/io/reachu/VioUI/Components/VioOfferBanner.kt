package io.reachu.VioUI.Components

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.models.OfferBannerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant

/**
 * Business-logic counterpart of the Swift VioOfferBanner view.
 * Holds helpers and countdown producer so UI layers (e.g., Compose) stay dumb.
 */
class VioOfferBanner(val config: OfferBannerConfig) {

    companion object {
        private const val DEFAULT_BASE_URL = "https://event-streamer-angelo100.replit.app"
        private const val DEFAULT_BUTTON_COLOR = "#800080"
    }

    /** Resolve a full URL from a path or absolute URL. */
    fun buildFullURL(path: String?): String? {
        val value = path?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val base = VioConfiguration.shared.state.value.campaign.restAPIBaseURL
            .takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
        val normalizedBase = base.trimEnd('/')
        val normalizedPath = value.trimStart('/')
        return "$normalizedBase/$normalizedPath"
    }

    /** Button color hex with fallback to a default purple if not provided. */
    fun resolvedButtonColorHex(): String = config.buttonColor ?: DEFAULT_BUTTON_COLOR

    /**
     * Emits remaining time every second until the end date is reached.
     * Mirrors the Swift Timer-based countdown.
     */
    fun countdownFlow(): Flow<TimeParts> = flow {
        val end = runCatching { Instant.parse(config.countdownEndDate) }.getOrNull()
        if (end == null) return@flow
        while (true) {
            val now = Instant.now()
            if (!now.isBefore(end)) break
            val dur = Duration.between(now, end)
            val total = dur.seconds
            val days = (total / 86_400).toInt()
            val hours = ((total % 86_400) / 3_600).toInt()
            val minutes = ((total % 3_600) / 60).toInt()
            val seconds = (total % 60).toInt()
            emit(TimeParts(days, hours, minutes, seconds))
            delay(1_000)
        }
    }
}

data class TimeParts(val days: Int, val hours: Int, val minutes: Int, val seconds: Int)
