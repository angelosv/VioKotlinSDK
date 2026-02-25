package io.reachu.liveui.components

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlyingHeartModel(
    val id: String = UUID.randomUUID().toString(),
    val startX: Float,
    val startY: Float,
    val isUserGenerated: Boolean,
    val timestamp: Instant = Instant.now(),
)

class LiveLikesManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    companion object {
        val shared: LiveLikesManager by lazy { LiveLikesManager() }
    }
    private val _hearts = MutableStateFlow<List<FlyingHeartModel>>(emptyList())
    val hearts: StateFlow<List<FlyingHeartModel>> = _hearts.asStateFlow()

    private val _totalLikes = MutableStateFlow(0)
    val totalLikes: StateFlow<Int> = _totalLikes.asStateFlow()

    fun createUserLike(x: Float = 350f, y: Float = 450f) {
        appendHeart(FlyingHeartModel(startX = x, startY = y, isUserGenerated = true))
    }

    fun registerRemoteLike(
        x: Float = (100..300).random().toFloat(),
        y: Float = (400..700).random().toFloat(),
    ) {
        appendHeart(FlyingHeartModel(startX = x, startY = y, isUserGenerated = false))
    }

    /**
     * Mirrors the Swift manager behavior when a HEART socket event arrives by spawning
     * 1-2 remote hearts with a slight stagger to create a burst animation.
     */
    fun handleRemoteHeartEvent(
        countRange: IntRange = 1..2,
        spawnDelayMillis: Long = 150,
        randomX: IntRange = 100..300,
        randomY: IntRange = 400..700,
    ) {
        val heartsToSpawn = countRange.random()
        repeat(heartsToSpawn) { index ->
            scope.launch {
                delay(index * spawnDelayMillis)
                val startX = randomX.random().toFloat()
                val startY = randomY.random().toFloat()
                appendHeart(
                    FlyingHeartModel(
                        startX = startX,
                        startY = startY,
                        isUserGenerated = false,
                    ),
                )
            }
        }
    }

    fun addHeart(heart: FlyingHeartModel) {
        appendHeart(heart)
    }

    fun removeHeart(id: String) {
        _hearts.value = _hearts.value.filterNot { it.id == id }
    }

    private fun appendHeart(heart: FlyingHeartModel) {
        _hearts.value = (_hearts.value + heart).takeLast(20)
        _totalLikes.value = _totalLikes.value + 1

        scope.launch {
            delay(3_000)
            removeHeart(heart.id)
        }
    }
}
