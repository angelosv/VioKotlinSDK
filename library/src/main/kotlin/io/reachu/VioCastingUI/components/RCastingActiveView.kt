package io.reachu.VioCastingUI.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCastingUI.models.Match
import io.reachu.VioDesignSystem.Components.CachedAsyncImage

@Composable
fun RCastingActiveView(
    match: Match,
    onDismiss: () -> Unit,
    onStopCasting: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isChatExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        // Background Image (Blurred)
        CachedAsyncImage(
            url = match.backgroundImage,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CastingHeader(
                match = match,
                onDismiss = onDismiss,
                onStopCasting = onStopCasting
            )

            Spacer(modifier = Modifier.weight(1f))

            MatchInfo(match = match)

            Spacer(modifier = Modifier.weight(1f))

            PlaybackControls(
                isPlaying = isPlaying,
                onPlayPauseToggle = { isPlaying = !isPlaying }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Chat Panel
            RCastingChatOverlay(
                showControls = true,
                onExpandedChange = { isChatExpanded = it },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun CastingHeader(
    match: Match,
    onDismiss: () -> Unit,
    onStopCasting: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = match.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = match.subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }

        Button(
            onClick = onStopCasting,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TvOff, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MatchInfo(match: Match) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Casting to Living TV", color = Color.White, fontSize = 17.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Slider / Line
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .background(Color.Magenta)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.width(240.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "3:24:39", color = Color.White, fontSize = 15.sp)
            Text(text = "LIVE", color = Color.Magenta, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        IconButton(onClick = {}) {
            Icon(Icons.Default.Replay30, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.Magenta)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = {}) {
            Icon(Icons.Default.Forward30, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}
