package live.vio.VioEngagementUI.Components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.vio.VioCore.models.SponsorSlot
import live.vio.VioDesignSystem.Tokens.VioColors
import live.vio.VioDesignSystem.Tokens.VioSpacing
import live.vio.VioUI.Components.compose.utils.toVioColor

/**
 * A generic UI component for "Sponsor Moments" (Sponsor Slots).
 * This component renders different content based on the [SponsorSlot.type].
 */
@Composable
fun VioSponsorMomentCard(
    sponsorSlot: SponsorSlot,
    sponsorLogoUrl: String? = null,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onProductClick: (() -> Unit)? = null,
    onCtaClick: ((String) -> Unit)? = null
) {
    VioEngagementCardBase(
        modifier = modifier,
        sponsorLogoUrl = sponsorLogoUrl,
        onDismiss = { onDismiss?.invoke() }
    ) {
        when (sponsorSlot.type) {
            "product" -> {
                SponsorMomentProductContent(
                    config = sponsorSlot.config,
                    onClick = onProductClick
                )
            }
            "lead", "poll_cta", "contest_cta", "link" -> {
                SponsorMomentGenericContent(
                    type = sponsorSlot.type,
                    config = sponsorSlot.config,
                    onClick = { onCtaClick?.invoke(sponsorSlot.type) }
                )
            }
            else -> {
                // Unknown type fallback
                Text(
                    text = "Sponset innhold",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SponsorMomentProductContent(
    config: Map<String, Any?>,
    onClick: (() -> Unit)?
) {
    val title = config["title"] as? String ?: "Sponset produkt"
    val description = config["description"] as? String
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(VioSpacing.xs.dp))
            Text(
                text = description,
                color = VioColors.textSecondary.toVioColor(),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SponsorMomentGenericContent(
    type: String,
    config: Map<String, Any?>,
    onClick: () -> Unit
) {
    val title = config["title"] as? String ?: when (type) {
        "lead" -> "Bli med"
        "poll_cta" -> "Delta i avstemning"
        "contest_cta" -> "Delta i konkurransen"
        "link" -> "Les mer"
        else -> "Sponset innhold"
    }
    
    val text = config["text"] as? String
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (text != null) {
            Spacer(modifier = Modifier.height(VioSpacing.xs.dp))
            Text(
                text = text,
                color = VioColors.textSecondary.toVioColor(),
                fontSize = 14.sp
            )
        }
    }
}
