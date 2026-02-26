package io.reachu.VioUI.Helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.managers.CampaignManager

/**
 * Utility helpers that mirror the Swift `VioComponentWrapper`.
 * They make it easy to hide Vio UI elements whenever the SDK
 * configuration or campaign state disables commerce features.
 */
object VioComponentGate {
    fun isVioContentEnabled(): Boolean {
        val campaignActive = CampaignManager.shared.isCampaignActive.value
        return VioConfiguration.shared.shouldUseSDK && campaignActive
    }
}

@Composable
fun VioComponentWrapper(content: @Composable () -> Unit) {
    val configState by VioConfiguration.shared.state.collectAsState()
    val isCampaignActive by CampaignManager.shared.isCampaignActive.collectAsState()
    val shouldUseSDK = remember(configState) { VioConfiguration.shared.shouldUseSDK }
    if (shouldUseSDK && isCampaignActive) {
        content()
    }
}

@Composable
fun VioOnly(content: @Composable () -> Unit) {
    VioComponentWrapper(content)
}
