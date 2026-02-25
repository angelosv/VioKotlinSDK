package io.reachu.VioEngagementUI.Components

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioUI.Components.compose.utils.toVioColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Overlay Compose que muestra un contest en pantalla completa con:
 * - Animación de rueda de premios giratoria
 * - Soporte para landscape/portrait (bottom en portrait, right side en landscape)
 * - Countdown timer visual
 * - Gestos de drag para dismiss
 * - Padding adaptable según isChatExpanded
 * - Transición entre vista de info y rueda
 *
 * @param name Nombre del contest
 * @param prize Premio principal
 * @param deadline Deadline opcional
 * @param maxParticipants Máximo de participantes opcional
 * @param prizes Lista de premios para la rueda (si está vacía, solo muestra el premio principal)
 * @param isChatExpanded Si el chat está expandido (ajusta padding)
 * @param sponsorLogoUrl URL del logo del sponsor (opcional)
 * @param modifier Modifier opcional
 * @param onJoin Callback al participar
 * @param onDismiss Callback para cerrar
 */
@Composable
fun VioEngagementContestOverlay(
    name: String,
    prize: String,
    deadline: String? = null,
    maxParticipants: Int? = null,
    prizes: List<String>? = null,
    isChatExpanded: Boolean = false,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var showWheel by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedPrizeIndex by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(30) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    // Rotación de la rueda
    val wheelRotation by animateFloatAsState(
        targetValue = if (isSpinning) {
            // Rotación completa múltiple + posición del premio seleccionado
            (360f * 5) + (selectedPrizeIndex * (360f / (prizes?.size ?: 1)))
        } else {
            0f
        },
        animationSpec = tween(
            durationMillis = 3000,
            easing = LinearEasing,
        ),
        label = "wheel-rotation",
    )
    
    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
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
                .fillMaxWidth(if (isLandscape) 0.5f else 1f),
        ) {
            VioEngagementCardBase(
                sponsorLogoUrl = sponsorLogoUrl,
                onDismiss = onDismiss,
            ) {
                if (!showWheel) {
                    // Vista de información inicial
                    ContestInfoView(
                        name = name,
                        prize = prize,
                        deadline = deadline,
                        maxParticipants = maxParticipants,
                        countdown = countdown,
                        onStartWheel = {
                            showWheel = true
                            isSpinning = true
                            selectedPrizeIndex = (prizes?.indices?.random() ?: 0)
                        },
                    )
                } else {
                    // Vista de rueda de premios
                    PrizeWheelView(
                        prizes = prizes ?: listOf(prize),
                        rotation = wheelRotation,
                        isSpinning = isSpinning,
                        selectedIndex = selectedPrizeIndex,
                        onSpinComplete = {
                            isSpinning = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContestInfoView(
    name: String,
    prize: String,
    deadline: String?,
    maxParticipants: Int?,
    countdown: Int,
    onStartWheel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = VioColors.textPrimary.toVioColor(),
        )
        
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
                text = prize,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = VioColors.primary.toVioColor(),
            )
        }
        
        deadline?.let {
            Text(
                text = "Deadline: $it",
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.textSecondary.toVioColor(),
            )
        }
        
        maxParticipants?.let {
            Text(
                text = "Maks deltakere: $it",
                style = MaterialTheme.typography.bodySmall,
                color = VioColors.textSecondary.toVioColor(),
            )
        }
        
        // Countdown timer
        Text(
            text = "Tid igjen: ${countdown}s",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = VioColors.primary.toVioColor(),
        )
        
        Spacer(modifier = Modifier.height(VioSpacing.sm.dp))
        
        Button(
            onClick = onStartWheel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioColors.primary.toVioColor(),
                contentColor = VioColors.textOnPrimary.toVioColor(),
            ),
            shape = RoundedCornerShape(VioBorderRadius.medium.dp),
        ) {
            Text(
                text = "Spinn hjulet!",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun PrizeWheelView(
    prizes: List<String>,
    rotation: Float,
    isSpinning: Boolean,
    selectedIndex: Int,
    onSpinComplete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VioSpacing.lg.dp),
    ) {
        Text(
            text = "Premiehjul",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = VioColors.textPrimary.toVioColor(),
        )
        
        // Rueda de premios
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(VioColors.surface.toVioColor()),
            contentAlignment = Alignment.Center,
        ) {
            // Segmentos de la rueda
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation),
            ) {
                val segmentAngle = 360f / prizes.size
                prizes.forEachIndexed { index, prize ->
                    val startAngle = index * segmentAngle
                    val centerAngle = startAngle + segmentAngle / 2
                    
                    // Dibujar segmento (simplificado - en producción usar Canvas)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    ) {
                        // Aquí se dibujarían los segmentos con Canvas
                        // Por ahora mostramos un círculo simple con el premio seleccionado destacado
                    }
                }
            }
            
            // Indicador fijo en la parte superior
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(20.dp, 40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VioColors.primary.toVioColor()),
            )
        }
        
        // Premio seleccionado
        if (!isSpinning) {
            Text(
                text = "Du vant: ${prizes[selectedIndex]}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VioColors.primary.toVioColor(),
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = VioColors.primary.toVioColor(),
            )
        }
        
        // LaunchedEffect para detectar cuando termina la animación
        LaunchedEffect(rotation) {
            if (isSpinning && rotation >= (360f * 5)) {
                delay(100)
                onSpinComplete()
            }
        }
    }
}
