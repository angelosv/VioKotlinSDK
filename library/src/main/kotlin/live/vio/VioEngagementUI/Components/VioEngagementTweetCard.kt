package live.vio.VioEngagementUI.Components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.vio.VioDesignSystem.SponsorAssets
import live.vio.VioDesignSystem.Components.SponsorAvatar
import live.vio.VioDesignSystem.Tokens.VioColors
import live.vio.VioUI.Components.compose.utils.toVioColor

/**
 * A specialized engagement card for social media style "Tweet" content.
 */
@Composable
fun VioEngagementTweetCard(
    author: String,
    text: String,
    title: String? = null,
    sponsor: SponsorAssets,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    VioEngagementCardBase(
        modifier = modifier,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                SponsorAvatar(sponsor = sponsor)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = sponsor.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = author,
                        color = VioColors.textSecondary.toVioColor(),
                        fontSize = 12.sp
                    )
                }
            }

            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}
