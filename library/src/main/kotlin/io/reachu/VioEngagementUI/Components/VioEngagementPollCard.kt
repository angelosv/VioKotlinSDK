package io.reachu.VioEngagementUI.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollOption
import io.reachu.VioEngagementSystem.models.PollOptionResults
import io.reachu.VioEngagementSystem.models.PollResults
import io.reachu.VioUI.Components.compose.utils.toVioColor

/**
 * Engagement poll card that shows a question, options and (optionally) results.
 *
 * @param poll Modelo de poll a mostrar.
 * @param pollResults Resultados opcionales del poll (totales y porcentajes).
 * @param sponsorLogoUrl Logo del sponsor (si aplica) que se mostrará en el badge.
 * @param modifier Modifier opcional para el contenedor.
 * @param onVote Callback al votar, recibe el optionId seleccionado.
 * @param onDismiss Callback para cerrar la card (drag o botón externo).
 */
@Composable
fun VioEngagementPollCard(
    poll: Poll,
    pollResults: PollResults? = null,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onVote: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedOptionId by rememberSaveable(poll.id) { mutableStateOf<String?>(null) }
    var hasVoted by rememberSaveable(poll.id) { mutableStateOf(false) }

    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = onDismiss,
    ) {
        Text(
            text = poll.question,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = VioColors.textPrimary.toVioColor(),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(VioSpacing.sm.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp),
        ) {
            poll.options.forEach { option ->
                val optionResult = pollResults?.options?.firstOrNull { it.optionId == option.id }
                PollOptionRow(
                    option = option,
                    result = optionResult,
                    isSelected = option.id == selectedOptionId,
                    showResults = pollResults != null,
                    enabled = !hasVoted,
                    onClick = {
                        if (!hasVoted) {
                            selectedOptionId = option.id
                            hasVoted = true
                            onVote(option.id)
                        }
                    },
                )
            }
        }

        if (pollResults != null) {
            Spacer(modifier = Modifier.height(VioSpacing.sm.dp))
            Text(
                text = "${pollResults.totalVotes} stemmer totalt",
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.textSecondary.toVioColor(),
            )
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PollOption,
    result: PollOptionResults?,
    isSelected: Boolean,
    showResults: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val baseBackground = if (isSelected) {
        VioColors.primary.toVioColor().copy(alpha = 0.16f)
    } else {
        VioColors.surfaceSecondary.toVioColor().copy(alpha = 0.9f)
    }

    val borderColor = if (isSelected) {
        VioColors.primary.toVioColor()
    } else {
        VioColors.borderSecondary.toVioColor()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(baseBackground)
            .clickable(enabled = enabled) { onClick() }
            .padding(
                horizontal = VioSpacing.md.dp,
                vertical = VioSpacing.sm.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(borderColor.copy(alpha = if (isSelected) 1f else 0.2f)),
            )

            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyMedium,
                color = VioColors.textPrimary.toVioColor(),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (showResults && result != null) {
                Text(
                    text = "${result.percentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = VioColors.textPrimary.toVioColor(),
                )
            }
        }

        if (showResults && result != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(VioColors.backgroundMuted.toVioColor()),
            ) {
                val percentageFraction = (result.percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentageFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) {
                                VioColors.primary.toVioColor()
                            } else {
                                VioColors.primary.toVioColor().copy(alpha = 0.4f)
                            },
                        ),
                )
            }
        }
    }
}

