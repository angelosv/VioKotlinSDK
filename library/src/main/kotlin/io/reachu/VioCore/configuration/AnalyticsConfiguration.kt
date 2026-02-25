package io.reachu.VioCore.configuration

/**
 * Mirrors the Swift `AnalyticsConfiguration` structure. All fields are opt-in so
 * integrators can enable Mixpanel (or any other backend) without touching the
 * Kotlin sources.
 */
data class AnalyticsConfiguration(
    val enabled: Boolean = false,
    val mixpanelToken: String? = null,
    val apiHost: String? = null,
    val trackComponentViews: Boolean = true,
    val trackComponentClicks: Boolean = true,
    val trackImpressions: Boolean = true,
    val trackTransactions: Boolean = true,
    val trackProductEvents: Boolean = true,
    val autocapture: Boolean = false,
    val recordSessionsPercent: Int = 0,
) {
    companion object {
        fun default(): AnalyticsConfiguration = AnalyticsConfiguration()
    }
}
