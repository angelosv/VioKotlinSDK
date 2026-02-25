package io.reachu.VioCastingUI.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.reachu.VioCastingUI.models.Match

@Composable
fun RCastingVideoPlayer(
    match: Match,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var isChatExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Video Placeholder (Real implementation would use ExoPlayer/Media3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.DarkGray)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text("VIDEO PLAYER PLACEHOLDER", color = Color.White)
        }

        // Chat Overlay
        RCastingChatOverlay(
            showControls = showControls,
            onExpandedChange = { isChatExpanded = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Controls
        if (showControls) {
            TopBar(onDismiss = onDismiss)
            BottomControls(modifier = Modifier.align(Alignment.BottomCenter))
        }
        
        // Live Badge
        LiveBadge(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))
    }
}

@Composable
private fun TopBar(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
             IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Airplay, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun BottomControls(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 140.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple progress bar
        LinearProgressIndicator(
            progress = 0.5f,
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Color.Magenta,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = {}, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = {}) {
                Icon(Icons.Default.Forward10, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color.Red, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
