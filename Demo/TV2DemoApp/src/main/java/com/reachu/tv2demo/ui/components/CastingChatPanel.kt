package com.reachu.tv2demo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reachu.tv2demo.services.chat.ChatManager
import com.reachu.tv2demo.services.chat.ChatMessage
import com.reachu.tv2demo.ui.theme.TV2Theme

@Composable
fun CastingChatPanel(
    chatManager: ChatManager,
    modifier: Modifier = Modifier,
) {
    val messages by chatManager.messages.collectAsState()
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var messageText by rememberSaveable { mutableStateOf("") }

    DisposableEffect(chatManager) {
        chatManager.startSimulation()
        onDispose { chatManager.stopSimulation() }
    }

    Column(
        modifier = modifier
            .width(400.dp)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = Icons.Filled.Message, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("LIVE CHAT", color = Color.White, style = TV2Theme.Typography.small.copy(fontWeight = FontWeight.Bold))
            Text("(${messages.size})", color = Color.White.copy(alpha = 0.7f), style = TV2Theme.Typography.small)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .height(250.dp)
                    .padding(bottom = 12.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true,
                ) {
                    items(messages.takeLast(15).reversed(), key = { it.id }) { message ->
                        ChatMessageRow(message = message)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Skriv en melding...", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.weight(1f),
                        textStyle = TV2Theme.Typography.small,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        ),
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatManager.addMessage(
                                    ChatMessage(
                                        username = "Angelo",
                                        text = messageText.trim(),
                                        usernameColor = TV2Theme.Colors.primary,
                                        likes = 0,
                                        timestampMillis = System.currentTimeMillis(),
                                    ),
                                )
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                    ) {
                        Icon(imageVector = Icons.Filled.Send, contentDescription = null, tint = if (messageText.isBlank()) Color.White.copy(alpha = 0.4f) else TV2Theme.Colors.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(message: ChatMessage) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(message.usernameColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message.username.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                color = message.usernameColor,
                style = TV2Theme.Typography.small.copy(fontWeight = FontWeight.Bold),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(message.username, color = message.usernameColor, style = TV2Theme.Typography.small.copy(fontWeight = FontWeight.SemiBold))
            Text(
                message.text,
                color = Color.White,
                style = TV2Theme.Typography.small,
                overflow = TextOverflow.Visible,
            )
        }
    }
}
