package io.reachu.VioEngagementUI.Components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import io.reachu.VioUI.Components.compose.common.SponsorBadge
import io.reachu.VioUI.Components.compose.utils.toVioColor
import kotlin.math.roundToInt

/**
 * Base container for engagement cards that provides:
 * - Blurred background & card styling aligned with Reachu design system
 * - Drag-to-dismiss gesture (vertical in portrait, horizontal in landscape)
 * - Optional sponsor badge support
 * - Shared padding & spacing tokens.
 */
@Composable
fun VioEngagementCardBase(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    sponsorLogoUrl: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = VioSpacing.lg.dp,
        vertical = VioSpacing.md.dp,
    ),
    dragDismissThreshold: Dp = 80.dp,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    val dragOffset: MutableState<Float> = remember { mutableStateOf(0f) }

    val animatedOffset by animateOffsetAsState(
        targetValue = if (isPortrait) {
            androidx.compose.ui.geometry.Offset(0f, dragOffset.value)
        } else {
            androidx.compose.ui.geometry.Offset(dragOffset.value, 0f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "engagement-card-offset",
    )

    val dragThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        dragDismissThreshold.toPx()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = VioSpacing.lg.dp)
                .padding(bottom = VioSpacing.xl.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = animatedOffset.x.roundToInt(),
                            y = animatedOffset.y.roundToInt(),
                        )
                    }
                    .pointerInput(isPortrait) {
                        detectDragGestures(
                            onDragCancel = { dragOffset.value = 0f },
                            onDragEnd = {
                                if (dragOffset.value >= dragThresholdPx) {
                                    dragOffset.value = 0f
                                    onDismiss()
                                } else {
                                    dragOffset.value = 0f
                                }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            val delta = if (isPortrait) dragAmount.y else dragAmount.x
                            if (delta > 0f) {
                                dragOffset.value += delta
                            }
                        }
                    },
                shape = RoundedCornerShape(VioBorderRadius.large.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VioColors.surface.toVioColor().copy(alpha = 0.96f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = VioColors.backgroundMuted.toVioColor().copy(alpha = 0.4f),
                        )
                        .blur(12.dp)
                        .clip(RoundedCornerShape(VioBorderRadius.large.dp)),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(contentPadding),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        VioEngagementDragIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = VioSpacing.sm.dp),
                        )

                        if (!sponsorLogoUrl.isNullOrBlank()) {
                            VioEngagementSponsorBadge(
                                logoUrl = sponsorLogoUrl,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = VioSpacing.sm.dp),
                            )
                        }

                        content()
                    }
                }
            }
        }
    }
}

/**
 * Small capsule-shaped drag indicator shown at the top of engagement cards.
 */
@Composable
fun VioEngagementDragIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(VioBorderRadius.pill.dp))
                .background(VioColors.borderSecondary.toVioColor().copy(alpha = 0.9f)),
        )
    }
}

/**
 * Wrapper around the shared Sponsor badge used throughout the SDK.
 */
@Composable
fun VioEngagementSponsorBadge(
    logoUrl: String?,
    modifier: Modifier = Modifier,
    text: String = "Sponset av",
) {
    if (logoUrl.isNullOrBlank()) return

    Box(modifier = modifier) {
        SponsorBadge(
            logoUrl = logoUrl,
            text = text,
            textColor = VioColors.textSecondary.toVioColor(),
        )
    }
}

