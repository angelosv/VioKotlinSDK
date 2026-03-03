package com.vio.viaplaydemo.services.chat

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val text: String,
    val usernameColor: Color,
    val likes: Int,
    val timestampMillis: Long,
)

/**
 * Compose-friendly port of the Swift `ChatManager`. It simulates live chat
 * traffic by emitting synthetic messages and viewer-count fluctuations.
 */
class ChatManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _viewerCount = MutableStateFlow(0)
    val viewerCount: StateFlow<Int> = _viewerCount.asStateFlow()

    private var messageJob: Job? = null
    private var viewerJob: Job? = null

    private val simulatedUsers = listOf(
        "SportsFan23" to Color.Cyan,
        "GoalKeeper" to Color.Green,
        "MatchMaster" to Color(0xFFFFA726),
        "TeamCaptain" to Color.Red,
        "ElClásico" to Color(0xFFAB47BC),
        "FutbolLoco" to Color.Yellow,
        "DefenderPro" to Color(0xFF42A5F5),
        "StrikerKing" to Color(0xFFF06292),
        "MidFielder" to Color(0xFF4DB6AC),
        "CoachView" to Color(0xFF5C6BC0),
        "TacticsGuru" to Color(0xFF00897B),
        "FanZone" to Color(0xFFFFB74D),
        "LiveScore" to Color(0xFF66BB6A),
        "TeamSpirit" to Color(0xFF9575CD),
        "UltrasGroup" to Color(0xFFE57373),
    )

    private val simulatedMessages = listOf(
        "Hvilket mål! 🔥",
        "For en redning!",
        "UTROLIG SPILL!!!",
        "Forsvaret sover...",
        "Dommeren er forferdelig",
        "KOM IGJEN! 💪",
        "Nydelig pasning",
        "Det burde vært straffe",
        "Keeperen er på et annet nivå",
        "SKYT!",
        "Hvorfor skjøt han ikke?",
        "Perfekt posisjonering",
        "Denne kampen er gal",
        "Vi trenger mål nå",
        "Taktikken fungerer",
        "Kom igjen, våkn opp!",
        "Nesten! Så nært!",
        "Beste kampen denne sesongen",
        "Dommeren så ingenting",
        "FOR EN PASNING!",
        "Utrolig ballkontroll",
        "Det var offside!",
        "Kom igjen da!",
        "Perfekt timing",
        "Dette blir episk",
        "KJØR PÅ!!!",
        "Hvilken spilling!",
        "Fantastisk lagspill",
        "Publikum er tent 🔥",
        "NÅ SKJER DET!",
    )

    fun startSimulation() {
        if (messageJob == null) {
            _viewerCount.value = (8000..15000).random()
            repeat(5) { addSimulatedMessage() }
            messageJob = scope.launch {
                while (isActive) {
                    delay((1500L..4000L).random())
                    addSimulatedMessage()
                }
            }
        }
        if (viewerJob == null) {
            viewerJob = scope.launch {
                while (isActive) {
                    delay(8000)
                    val delta = (-100..200).random()
                    _viewerCount.value = maxOf(5000, _viewerCount.value + delta)
                }
            }
        }
    }

    fun stopSimulation() {
        messageJob?.cancel()
        viewerJob?.cancel()
        messageJob = null
        viewerJob = null
    }

    fun addMessage(message: ChatMessage) {
        val updated = (_messages.value + message).takeLast(100)
        _messages.value = updated
    }

    private fun addSimulatedMessage() {
        val (username, color) = simulatedUsers.random()
        val text = simulatedMessages.random()
        val message = ChatMessage(
            username = username,
            text = text,
            usernameColor = color,
            likes = (0..12).random(),
            timestampMillis = System.currentTimeMillis(),
        )
        addMessage(message)
    }
}
