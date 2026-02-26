package io.reachu.liveui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.LiveStreamLayout

/**
 * Kotlin façade mirroring the Swift `VioLiveUI` convenience API.
 *
 * The object simply delegates to the existing `LiveShowManager` singleton so that
 * host apps can keep calling the same entry points while the actual UI rendering
 * is handled by platform‑specific layers (Compose, React, etc.).
 */
object VioLiveUI {

    private val liveShowManager = LiveShowManager.shared

    fun configure() {
        // Live UI does not require additional bootstrap work on Kotlin side.
    }

    suspend fun showLiveStream(
        stream: LiveStream,
        layout: LiveStreamLayout = LiveStreamLayout.FULL_SCREEN_OVERLAY,
    ) = withContext(Dispatchers.Main) {
        liveShowManager.showLiveStream(stream, layout)
    }

    suspend fun showLiveStream(
        id: String,
        layout: LiveStreamLayout = LiveStreamLayout.FULL_SCREEN_OVERLAY,
    ) = withContext(Dispatchers.Main) {
        liveShowManager.showLiveStream(id, layout)
    }

    suspend fun showFeaturedLiveStream(
        layout: LiveStreamLayout = LiveStreamLayout.FULL_SCREEN_OVERLAY,
    ) = withContext(Dispatchers.Main) {
        val featured = liveShowManager.featuredLiveStream ?: return@withContext
        liveShowManager.showLiveStream(featured, layout)
    }

    suspend fun hideLiveStream() = withContext(Dispatchers.Main) {
        liveShowManager.hideLiveStream()
    }

    suspend fun showMiniPlayer() = withContext(Dispatchers.Main) {
        liveShowManager.showMiniPlayer()
    }

    val hasActiveLiveStreams: Boolean
        get() = liveShowManager.hasActiveLiveStreams

    val totalViewerCount: Int
        get() = liveShowManager.totalViewerCount

    val demoStream: LiveStream?
        get() = liveShowManager.activeStreams.value.firstOrNull()

    val demoStreams: List<LiveStream>
        get() = liveShowManager.activeStreams.value

    suspend fun simulateNewChatMessage() = withContext(Dispatchers.Main) {
        liveShowManager.simulateNewChatMessage()
    }
}
