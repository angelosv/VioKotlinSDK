package com.reachu.viaplaydemo.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reachu.viaplaydemo.ui.model.ContentItem
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun ContentCard(
    item: ContentItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(280.dp),
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .clip(RoundedCornerShape(ViaplayTheme.CornerRadius.medium)),
        ) {
            if (item.isMatchCard) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2B2438),
                                    Color(0xFF16001A),
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TeamCircleLogo(item.homeTeamLogo)
                    TeamCircleLogo(item.awayTeamLogo)
                }
            } else {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (item.isLive) {
                LiveBadge(modifier = Modifier.align(Alignment.TopStart))
            }
        }
        Spacer(Modifier.height(ViaplayTheme.Spacing.sm))
        Text(
            item.title,
            style = if (item.isMatchCard) ViaplayTheme.Typography.headline else ViaplayTheme.Typography.caption,
            color = ViaplayTheme.Colors.textPrimary,
            maxLines = 1,
        )
        item.subtitle?.let {
            Text(
                it,
                style = ViaplayTheme.Typography.small,
                color = ViaplayTheme.Colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(ViaplayTheme.Spacing.sm)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ViaplayTheme.Colors.live),
        )
        Text("DIREKTE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun TeamCircleLogo(url: String?) {
    if (url == null) return
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color.White),
    )
}
