package io.reachu.VioEngagementUI.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioEngagementSystem.models.Contest
import io.reachu.VioEngagementSystem.models.ContestType
import io.reachu.VioUI.Components.compose.utils.toVioColor

/**
 * Componente Compose que muestra un contest en formato card con información de premio,
 * tipo, y botón de participación, usando VioEngagementCardBase como contenedor.
 *
 * @param contest Modelo de contest a mostrar
 * @param sponsorLogoUrl URL del logo del sponsor (opcional)
 * @param modifier Modifier opcional
 * @param onJoin Callback al participar en el contest
 * @param onDismiss Callback para cerrar la card
 */
@Composable
fun VioEngagementContestCard(
    contest: Contest,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isJoining by remember { mutableStateOf(false) }
    
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = onDismiss,
    ) {
        // Indicador de tipo (quiz/giveaway)
        ContestTypeBadge(
            contestType = contest.contestType,
            modifier = Modifier.padding(bottom = VioSpacing.sm.dp),
        )
        
        // Título
        Text(
            text = contest.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = VioColors.textPrimary.toVioColor(),
        )
        
        Spacer(modifier = Modifier.height(VioSpacing.xs.dp))
        
        // Descripción
        if (contest.description.isNotBlank()) {
            Text(
                text = contest.description,
                style = MaterialTheme.typography.bodyMedium,
                color = VioColors.textSecondary.toVioColor(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(VioSpacing.sm.dp))
        }
        
        // Premio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Premio:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = VioColors.textPrimary.toVioColor(),
            )
            Text(
                text = contest.prize,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = VioColors.primary.toVioColor(),
            )
        }
        
        // Información de deadline si aplica
        contest.endTime?.let { endTime ->
            Spacer(modifier = Modifier.height(VioSpacing.xs.dp))
            Text(
                text = "Deadline: $endTime",
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.textSecondary.toVioColor(),
            )
        }
        
        Spacer(modifier = Modifier.height(VioSpacing.md.dp))
        
        // Botón de participación
        Button(
            onClick = {
                if (!isJoining) {
                    isJoining = true
                    onJoin()
                    // Reset después de un delay para permitir que el estado se actualice
                    // En producción, esto debería manejarse con un callback de éxito/error
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isJoining && contest.isActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = VioColors.primary.toVioColor(),
                contentColor = VioColors.textOnPrimary.toVioColor(),
            ),
            shape = RoundedCornerShape(VioBorderRadius.medium.dp),
        ) {
            if (isJoining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = VioColors.textOnPrimary.toVioColor(),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isJoining) "Deltar..." else "Deltar nå",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        
        if (!contest.isActive) {
            Spacer(modifier = Modifier.height(VioSpacing.xs.dp))
            Text(
                text = "Denne konkurransen er ikke lenger aktiv",
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.error.toVioColor(),
            )
        }
    }
}

@Composable
private fun ContestTypeBadge(
    contestType: ContestType,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (contestType) {
        ContestType.quiz -> "Quiz" to VioColors.info.toVioColor()
        ContestType.giveaway -> "Giveaway" to VioColors.primary.toVioColor()
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VioBorderRadius.pill.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}
