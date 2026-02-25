package io.reachu.VioEngagementUI.Components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioEngagementSystem.models.Poll
import io.reachu.VioEngagementSystem.models.PollResults
import io.reachu.VioEngagementSystem.models.PollOption
import io.reachu.VioUI.Components.compose.utils.toVioColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Modelo de opción para el overlay de poll.
 */
data class VioEngagementPollOverlayOption(
    val id: String,
    val text: String,
    val avatarUrl: String? = null,
)

/**
 * Overlay Compose que muestra un poll en pantalla completa con:
 * - Soporte para landscape/portrait (bottom en portrait, right side en landscape)
 * - Animación de flip 3D para mostrar resultados después de votar
 * - Gestos de drag para dismiss (vertical en portrait, horizontal en landscape)
 * - Timer countdown visual
 * - Padding adaptable según isChatExpanded
 *
 * @param poll Modelo de poll a mostrar
 * @param pollResults Resultados opcionales del poll
 * @param duration Duración en segundos para el timer
 * @param isChatExpanded Si el chat está expandido (ajusta padding)
 * @param sponsorLogoUrl URL del logo del sponsor (opcional)
 * @param modifier Modifier opcional
 * @param onVote Callback al votar
 * @param onDismiss Callback para cerrar
 */
@Composable
fun VioEngagementPollOverlay(
    poll: Poll,
    pollResults: PollResults? = null,
    duration: Int = 30,
    isChatExpanded: Boolean = false,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onVote: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current.density
    
    var showResults by remember(poll.id) { mutableStateOf(false) }
    var selectedOptionId by remember(poll.id) { mutableStateOf<String?>(null) }
    var timeRemaining by remember(poll.id) { mutableIntStateOf(duration) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    // Animación de flip 3D
    val rotationY by animateFloatAsState(
        targetValue = if (showResults) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "flip-animation",
    )
    
    // Timer countdown
    LaunchedEffect(poll.id, duration) {
        timeRemaining = duration
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }
    
    // Padding adaptable según chat expandido
    val bottomPadding = if (isChatExpanded) {
        if (isLandscape) 140.dp else 200.dp
    } else {
        if (isLandscape) 80.dp else 120.dp
    }
    
    val sidePadding = if (isChatExpanded) {
        if (isLandscape) 200.dp else 16.dp
    } else {
        if (isLandscape) 120.dp else 16.dp
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VioColors.background.toVioColor().copy(alpha = 0.35f)),
    ) {
        Box(
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
                .offset {
                    IntOffset(
                        x = if (isLandscape) dragOffset.roundToInt() else 0,
                        y = if (isLandscape) 0 else dragOffset.roundToInt(),
                    )
                }
                .pointerInput(isLandscape) {
                    detectDragGestures(
                        onDragCancel = { dragOffset = 0f },
                        onDragEnd = {
                            val threshold = 100f
                            if (kotlin.math.abs(dragOffset) >= threshold) {
                                dragOffset = 0f
                                onDismiss()
                            } else {
                                dragOffset = 0f
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = if (isLandscape) dragAmount.x else dragAmount.y
                        if (delta > 0f) {
                            dragOffset += delta
                        }
                    }
                }
                .padding(
                    bottom = if (isLandscape) 0.dp else bottomPadding,
                    end = if (isLandscape) sidePadding else 0.dp,
                )
                .fillMaxWidth(if (isLandscape) 0.4f else 1f)
                .graphicsLayer {
                    this.rotationY = rotationY
                },
        ) {
            // Contenido con flip
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Vista frontal (pregunta y opciones)
                if (rotationY < 90f) {
                    PollFrontView(
                        poll = poll,
                        timeRemaining = timeRemaining,
                        sponsorLogoUrl = sponsorLogoUrl,
                        onVote = { optionId ->
                            selectedOptionId = optionId
                            showResults = true
                            onVote(optionId)
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                this.rotationY = rotationY
                            },
                    )
                }
                
                // Vista trasera (resultados) - rotada 180 grados
                if (rotationY >= 90f) {
                    PollResultsView(
                        poll = poll,
                        pollResults = pollResults,
                        selectedOptionId = selectedOptionId,
                        sponsorLogoUrl = sponsorLogoUrl,
                        modifier = Modifier
                            .graphicsLayer {
                                this.rotationY = rotationY - 180f
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun PollFrontView(
    poll: Poll,
    timeRemaining: Int,
    sponsorLogoUrl: String?,
    onVote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = {},
    ) {
        // Timer countdown
        Text(
            text = "${timeRemaining}s",
            style = MaterialTheme.typography.bodySmall,
            color = VioColors.textSecondary.toVioColor(),
            modifier = Modifier.padding(bottom = VioSpacing.xs.dp),
        )
        
        // Pregunta
        Text(
            text = poll.question,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = VioColors.textPrimary.toVioColor(),
        )
        
        // Opciones
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VioSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp),
        ) {
            poll.options.forEach { option ->
                PollOverlayOptionRow(
                    option = option,
                    onClick = { onVote(option.id) },
                )
            }
        }
    }
}

@Composable
private fun PollResultsView(
    poll: Poll,
    pollResults: PollResults?,
    selectedOptionId: String?,
    sponsorLogoUrl: String?,
    modifier: Modifier = Modifier,
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = {},
    ) {
        Text(
            text = "Resultater",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = VioColors.textPrimary.toVioColor(),
        )
        
        Text(
            text = "Takk for at du stemte!",
            style = MaterialTheme.typography.bodyMedium,
            color = VioColors.primary.toVioColor(),
            modifier = Modifier.padding(top = VioSpacing.xs.dp),
        )
        
        // Mostrar resultados usando VioEngagementPollCard
        if (pollResults != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VioSpacing.md.dp),
            ) {
                poll.options.forEach { option ->
                    val result = pollResults.options.firstOrNull { it.optionId == option.id }
                    val isSelected = option.id == selectedOptionId
                    
                    ResultBar(
                        optionText = option.text,
                        percentage = result?.percentage ?: 0.0,
                        isSelected = isSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun PollOverlayOptionRow(
    option: PollOption,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VioColors.surfaceSecondary.toVioColor().copy(alpha = 0.9f))
            .clickable { onClick() }
            .padding(
                horizontal = VioSpacing.md.dp,
                vertical = VioSpacing.sm.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = option.text,
            style = MaterialTheme.typography.bodyMedium,
            color = VioColors.textPrimary.toVioColor(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResultBar(
    optionText: String,
    percentage: Double,
    isSelected: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.textPrimary.toVioColor(),
            )
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) VioColors.primary.toVioColor() else VioColors.textPrimary.toVioColor(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(VioColors.backgroundMuted.toVioColor()),
        ) {
            val percentageFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentageFraction)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            VioColors.primary.toVioColor()
                        } else {
                            VioColors.primary.toVioColor().copy(alpha = 0.5f)
                        },
                    ),
            )
        }
    }
}
