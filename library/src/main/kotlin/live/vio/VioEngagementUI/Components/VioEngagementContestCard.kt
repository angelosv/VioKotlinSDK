package live.vio.VioEngagementUI.Components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import live.vio.VioDesignSystem.Tokens.VioBorderRadius
import live.vio.VioDesignSystem.Tokens.VioSpacing
import live.vio.VioEngagementSystem.models.Contest
import live.vio.VioUI.Components.compose.product.VioImage
import live.vio.VioUI.Components.compose.product.VioImageLoader
import live.vio.VioUI.Components.compose.product.VioImageLoaderDefaults

/**
 * Reusable Compose component for displaying a contest card.
 * 
 * @deprecated Use VioEngagementContestCard instead, which uses VioEngagementCardBase
 * @param contest The contest data to display
 * @param onParticipate Callback when user participates in the contest
 */
@Deprecated("Use VioEngagementContestCard instead", ReplaceWith("VioEngagementContestCard(contest, sponsorLogoUrl, modifier, onJoin, onDismiss)"))
@Composable
fun VioEngagementContestCard(
    contest: Contest,
    sponsor: live.vio.VioDesignSystem.SponsorAssets? = live.vio.VioCore.models.SponsorAssets.current,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    onJoin: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    VioEngagementCardBase(
        modifier = modifier,
        onDismiss = { onDismiss?.invoke() }
    ) {
        // Branding Header
        sponsor?.let {
            live.vio.VioDesignSystem.Components.SponsorAvatar(sponsor = it)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        }
        if (!contest.imageUrl.isNullOrBlank()) {
            VioImage(
                url = contest.imageUrl,
                contentDescription = "Contest image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(VioBorderRadius.medium.dp)),
                imageLoader = imageLoader
            )
            Spacer(modifier = Modifier.height(VioSpacing.md.dp))
        }

        androidx.compose.material3.Text(
            text = contest.title,
            color = androidx.compose.ui.graphics.Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        if (contest.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(VioSpacing.xs.dp))
            androidx.compose.material3.Text(
                text = contest.description,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
    }
}
