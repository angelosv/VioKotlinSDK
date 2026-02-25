package io.reachu.liveui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.reachu.liveshow.chat.LiveChatManager
import io.reachu.liveshow.models.LiveChatMessage

/**
 * Headless port for the Swift `VioLiveChatComponent`. The controller keeps the
 * transient UI state (input text, username prompt, visibility) and delegates
 * message operations to `LiveChatManager`.
 */
class VioLiveChatComponentController(
    private val chatManager: LiveChatManager = LiveChatManager.shared,
    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Main),
) {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _userNameInput = MutableStateFlow("")
    val userNameInput: StateFlow<String> = _userNameInput.asStateFlow()

    private val _showChat = MutableStateFlow(true)
    val showChat: StateFlow<Boolean> = _showChat.asStateFlow()

    val messages: StateFlow<List<LiveChatMessage>> = chatManager.messages
    val pinnedMessage = chatManager.pinnedMessage
    val hasUserName = chatManager.hasUserName

    fun updateMessageText(value: String) {
        _input.value = value
    }

    fun updateUserName(value: String) {
        _userNameInput.value = value
    }

    fun toggleChatVisibility() {
        _showChat.value = !_showChat.value
    }

    fun setUserName() {
        val value = _userNameInput.value.trim()
        if (value.isNotEmpty()) {
            chatManager.setUserName(value)
            _userNameInput.value = ""
        }
    }

    fun sendMessage() {
        val text = _input.value.trim()
        if (text.isEmpty()) return
        scope.launch {
            chatManager.sendMessage(text)
            _input.value = ""
        }
    }
}
