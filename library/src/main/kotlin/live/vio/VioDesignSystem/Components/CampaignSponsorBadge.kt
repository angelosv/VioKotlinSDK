package live.vio.VioDesignSystem.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.vio.VioCore.models.SponsorAssets

/**
 * A reusable UI component that displays the sponsor badge with branding.
 * Uses colors and logos from [SponsorAssets].
 */
@Composable
fun CampaignSponsorBadge(
    modifier: Modifier = Modifier
) {
    val sponsorName = SponsorAssets.name
    val logoUrl = SponsorAssets.logoUrl
    val primaryColorHex = SponsorAssets.primaryColor
    val textOnPrimaryInt = SponsorAssets.textOnPrimary
    
    // Fallback colors if not provided by SponsorAssets
    val backgroundColor = if (primaryColorHex != null) Color(primaryColorHex).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
    val textColor = if (textOnPrimaryInt != null) Color(textOnPrimaryInt) else Color.White

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / Avatar
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            CachedAsyncImage(
                url = logoUrl,
                contentDescription = sponsorName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        if (sponsorName.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sponsorName,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
