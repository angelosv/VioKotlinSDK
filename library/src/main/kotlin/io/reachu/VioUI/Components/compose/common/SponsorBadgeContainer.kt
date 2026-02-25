package io.reachu.VioUI.Components.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.reachu.VioCore.managers.CampaignManager

import androidx.compose.ui.graphics.Color

/**
 * Container that positions a sponsor badge relative to content.
 * 
 * @param showSponsor Whether to show the sponsor badge
 * @param sponsorPosition Position of the badge: "topRight", "topLeft", "bottomRight", "bottomLeft"
 * @param sponsorLogoUrl URL of the sponsor logo
 * @param sponsorText Text to display in the badge (default: "Sponset av")
 * @param sponsorTextColor Color of the sponsor text (default: Unspecified)
 * @param imageLoader Image loader for the badge logo
 * @param badgeContent Optional custom badge content (overrides default SponsorBadge)
 * @param content Main content to display
 */
@Composable
fun SponsorBadgeContainer(
    showSponsor: Boolean,
    sponsorPosition: String?,
    sponsorLogoUrl: String?,
    modifier: Modifier = Modifier,
    sponsorText: String = "Sponset av",
    sponsorTextColor: Color = Color.Unspecified,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    badgeContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val currentCampaign by CampaignManager.shared.currentCampaign.collectAsState(initial = null)
    
    val effectiveSponsorLogoUrl = remember(sponsorLogoUrl, currentCampaign, showSponsor) {
        val rawUrl = sponsorLogoUrl ?: if (showSponsor) currentCampaign?.campaignLogo else null
        io.reachu.VioCore.utils.UrlUtils.resolveAssetUrl(rawUrl)
    }
    
    // Debug log to verify logo resolution
    if (showSponsor) {
        println("SponsorBadgeContainer: showSponsor=true, inputUrl=$sponsorLogoUrl, campaignLogo=${currentCampaign?.campaignLogo}, effective=$effectiveSponsorLogoUrl")
    }

    if (!showSponsor || effectiveSponsorLogoUrl.isNullOrBlank()) {
        content()
        return
    }

    val position = sponsorPosition?.lowercase() ?: "topright"
    val isTop = position.startsWith("top")
    val isRight = position.endsWith("right")

    val badge: @Composable () -> Unit = badgeContent ?: {
        SponsorBadge(
            logoUrl = effectiveSponsorLogoUrl,
            text = sponsorText,
            textColor = sponsorTextColor,
            imageLoader = imageLoader,
        )
    }

    Column(modifier = modifier) {
        // Top badge
        if (isTop) {
            Row(
                horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRight) Spacer(modifier = Modifier.weight(1f))
                badge()
                if (!isRight) Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Main content
        content()

        // Bottom badge
        if (!isTop) {
            Row(
                horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRight) Spacer(modifier = Modifier.weight(1f))
                badge()
                if (!isRight) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
