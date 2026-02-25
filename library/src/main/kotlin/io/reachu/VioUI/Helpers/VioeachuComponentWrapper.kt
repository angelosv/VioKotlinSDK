package io.reachu.VioUI.Helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.managers.CampaignManager

/**
 * Utility helpers that mirror the Swift `ReachuComponentWrapper`.
 * They make it easy to hide Reachu UI elements whenever the SDK
 * configuration or campaign state disables commerce features.
 */
object ReachuComponentGate {
    fun isReachuContentEnabled(): Boolean {
        val campaignActive = CampaignManager.shared.isCampaignActive.value
        return VioConfiguration.shared.shouldUseSDK && campaignActive
    }
}

@Composable
fun ReachuComponentWrapper(content: @Composable () -> Unit) {
    val configState by VioConfiguration.shared.state.collectAsState()
    val isCampaignActive by CampaignManager.shared.isCampaignActive.collectAsState()
    val shouldUseSDK = remember(configState) { VioConfiguration.shared.shouldUseSDK }
    if (shouldUseSDK && isCampaignActive) {
        content()
    }
}

@Composable
fun ReachuOnly(content: @Composable () -> Unit) {
    ReachuComponentWrapper(content)
}
