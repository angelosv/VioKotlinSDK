package io.reachu.VioCastingUI.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCastingUI.models.Match
import io.reachu.VioCastingUI.components.RCastingVideoPlayer

@Composable
fun LiveMatchView(
    match: Match,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF1B1B25)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simplified Header
            MatchHeader(match = match, onDismiss = onDismiss)

            // Content Area (e.g., Stats, Lineups, etc.)
            Box(modifier = Modifier.weight(1f)) {
                RCastingVideoPlayer(match = match, onDismiss = onDismiss)
            }

            // Timeline Control
            VideoTimelineControl()
        }
    }
}

@Composable
private fun MatchHeader(match: Match, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = match.title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = "LIVE - 85'", color = Color.Red, fontSize = 12.sp)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun VideoTimelineControl() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Slider(
            value = 0.8f,
            onValueChange = {},
            colors = SliderDefaults.colors(
                thumbColor = Color.Magenta,
                activeTrackColor = Color.Magenta
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("85:00", color = Color.White, fontSize = 11.sp)
            Text("LIVE", color = Color.Magenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

