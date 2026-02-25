package io.reachu.VioEngagementUI.Components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioEngagementSystem.managers.EngagementManager
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioUI.Components.compose.utils.toVioColor

/**
 * Overlay Compose que muestra un poll usando [VioEngagementPollCard].
 *
 * - Se apoya en [EngagementManager] para leer resultados del poll.
 * - Usa animaciones de aparición/desaparición.
 */
@Composable
fun ReachuEngagementPollOverlay(
    poll: Poll,
    isVisible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onVote: ((optionId: String) -> Unit)? = null,
) {
    val pollResultsMap by EngagementManager.shared.pollResults.collectAsState()
    val results = pollResultsMap[poll.id]
    val currentCampaign by CampaignManager.shared.currentCampaign.collectAsState()
    val sponsorLogoUrl = currentCampaign?.campaignLogo

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VioColors.background.toVioColor().copy(alpha = 0.35f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            VioEngagementPollCard(
                poll = poll,
                pollResults = results,
                sponsorLogoUrl = sponsorLogoUrl,
                onVote = { optionId ->
                    // Disparar callback externo
                    onVote?.invoke(optionId)
                    // Dejar que la capa superior decida cuándo cerrar o actualizar el engagement
                },
                onDismiss = { onDismiss?.invoke() },
            )
        }
    }
}

