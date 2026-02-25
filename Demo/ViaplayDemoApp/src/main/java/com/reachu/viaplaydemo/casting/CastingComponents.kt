package com.reachu.viaplaydemo.casting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reachu.viaplaydemo.ui.model.MatchMocks
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun CastingMiniPlayer(
    state: CastUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ViaplayTheme.Colors.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth(0.5f)
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(ViaplayTheme.Spacing.md)) {
            Text("Casting pågår", style = ViaplayTheme.Typography.caption, color = ViaplayTheme.Colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text("Barcelona - PSG", style = ViaplayTheme.Typography.body, color = ViaplayTheme.Colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = MatchMocks.barcelonaVsPsg.homeTeam.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentScale = ContentScale.Crop,
                )
                AsyncImage(
                    model = MatchMocks.barcelonaVsPsg.awayTeam.logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(state.selectedDevice?.name ?: "Velg enhet", style = ViaplayTheme.Typography.small, color = ViaplayTheme.Colors.textSecondary)
        }
    }
}

@Composable
fun CastingActiveOverlay(
    state: CastUiState,
    onStopCasting: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ViaplayTheme.Colors.surface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Casting til ${state.selectedDevice?.name ?: "enhet"}", style = ViaplayTheme.Typography.headline, color = ViaplayTheme.Colors.textPrimary)
                Text(MatchMocks.barcelonaVsPsg.title, style = ViaplayTheme.Typography.title, color = ViaplayTheme.Colors.textPrimary)
                Text(MatchMocks.barcelonaVsPsg.subtitle, style = ViaplayTheme.Typography.caption, color = ViaplayTheme.Colors.textSecondary)
                Divider(color = ViaplayTheme.Colors.surfaceLight)
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    TeamLogo(MatchMocks.barcelonaVsPsg.homeTeam.logo, MatchMocks.barcelonaVsPsg.homeTeam.name)
                    Text("vs", style = ViaplayTheme.Typography.headline, color = ViaplayTheme.Colors.textSecondary)
                    TeamLogo(MatchMocks.barcelonaVsPsg.awayTeam.logo, MatchMocks.barcelonaVsPsg.awayTeam.name)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onStopCasting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Stop casting")
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Lukk")
                }
            }
        }
    }
}

@Composable
private fun TeamLogo(url: String, description: String) {
    AsyncImage(
        model = url,
        contentDescription = description,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentScale = ContentScale.Fit,
    )
}
