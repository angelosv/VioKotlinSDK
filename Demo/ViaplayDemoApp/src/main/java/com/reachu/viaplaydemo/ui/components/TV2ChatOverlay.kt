package com.reachu.viaplaydemo.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.reachu.viaplaydemo.services.chat.ChatManager
import com.reachu.viaplaydemo.services.chat.ChatMessage
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme

@Composable
fun TV2ChatOverlay(
    showControls: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val chatManager = remember { ChatManager() }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var messageText by rememberSaveable { mutableStateOf("") }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val messages by chatManager.messages.collectAsState()
    val floatingLikes = remember { mutableStateListOf<Float>() }

    DisposableEffect(Unit) {
        chatManager.startSimulation()
        onDispose { chatManager.stopSimulation() }
    }

    val shouldShowChat = remember(showControls, isExpanded, isLandscape) {
        when {
            isExpanded -> true
            isLandscape -> showControls
            else -> true
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(visible = shouldShowChat, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .offset(y = dragOffset.dp * (if (isExpanded) 0.5f else 1f)),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .pointerInput(isExpanded) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragOffset < -50 -> {
                                            isExpanded = true
                                            onExpandedChange(true)
                                        }
                                        dragOffset > 50 -> {
                                            isExpanded = false
                                            onExpandedChange(false)
                                        }
                                    }
                                    dragOffset = 0f
                                },
                            ) { change, dragAmount ->
                                val newOffset = dragOffset + dragAmount
                                dragOffset = newOffset.coerceIn(-200f, 200f)
                            }
                        },
                ) {
                    Column {
                        ChatHeader(isExpanded = isExpanded, onToggle = {
                            isExpanded = it
                            onExpandedChange(it)
                        })
                        if (isExpanded) {
                            ChatMessages(messages = messages)
                            ChatInput(
                                value = messageText,
                                onValueChange = { messageText = it },
                                onSend = {
                                    chatManager.addMessage(
                                        ChatMessage(
                                            username = "Angelo",
                                            text = messageText,
                                            usernameColor = ViaplayTheme.Colors.secondary,
                                            likes = 0,
                                            timestampMillis = System.currentTimeMillis(),
                                        ),
                                    )
                                    messageText = ""
                                },
                                onLike = {
                                    floatingLikes += (-80..80).random().toFloat()
                                    chatManager.addMessage(
                                        ChatMessage(
                                            username = "Viewer",
                                            text = "👍",
                                            usernameColor = Color.White,
                                            likes = 0,
                                            timestampMillis = System.currentTimeMillis(),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        floatingLikes.forEach { offsetX ->
            FloatingLike(offsetX = offsetX) {
                floatingLikes.remove(offsetX)
            }
        }
    }
}

@Composable
private fun ChatHeader(isExpanded: Boolean, onToggle: (Boolean) -> Unit) {
    val currentCampaign by io.reachu.VioCore.managers.CampaignManager.shared.currentCampaign.collectAsState(initial = null)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TV2SponsorBadge(
            logoUrl = currentCampaign?.campaignLogo,
            showText = true,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onToggle(!isExpanded) }) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ChatMessages(messages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF120019))
            .height(220.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages.asReversed(), key = { it.id }) { message ->
            Row(Modifier.padding(horizontal = 12.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(message.usernameColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(message.username.first().uppercase(), style = ViaplayTheme.Typography.small.copy(fontWeight = FontWeight.Bold), color = message.usernameColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(message.username, style = ViaplayTheme.Typography.small.copy(fontWeight = FontWeight.Bold), color = message.usernameColor)
                    Text(message.text, style = ViaplayTheme.Typography.body, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF120019))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Send a message...", color = Color.White.copy(alpha = 0.6f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = ViaplayTheme.Colors.primary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onLike) {
            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = ViaplayTheme.Colors.primary)
        }
        IconButton(onClick = onSend, enabled = value.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = null, tint = if (value.isBlank()) Color.White.copy(alpha = 0.3f) else ViaplayTheme.Colors.primary)
        }
    }
}

@Composable
private fun FloatingLike(offsetX: Float, onFinished: () -> Unit) {
    var y by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    LaunchedEffect(offsetX) {
        val totalDistance = with(density) { 400.dp.toPx() }
        for (step in 0..100) {
            delay(15)
            y = -totalDistance * (step / 100f)
            alpha = 1f - (step / 100f)
        }
        onFinished()
    }
    Icon(
        imageVector = Icons.Default.ThumbUp,
        contentDescription = null,
        tint = ViaplayTheme.Colors.primary,
        modifier = Modifier
            .offset(x = offsetX.dp, y = y.dp)
            .alpha(alpha),
    )
}
