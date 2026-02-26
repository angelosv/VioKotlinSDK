package com.reachu.viaplaydemo.ui.components

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.reachu.viaplaydemo.ui.theme.ViaplayTheme
import io.reachu.VioUI.Managers.CartManager
import io.reachu.sdk.core.VioSdkClient
import io.reachu.VioUI.VioCheckoutOverlay
import io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.managers.CampaignManager
import com.reachu.viaplaydemo.ui.model.Match
import com.reachu.viaplaydemo.ui.model.toMatchContext

@Composable
fun TV2VideoPlayer(
    match: Match,
    cartManager: CartManager,
    sdkClient: VioSdkClient,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val playerState = remember { PlayerStateHolder(context) }
    var showControls by remember { mutableStateOf(true) }
    var showCheckout by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    val campaignManager = remember { CampaignManager.shared }

    suspend fun setupMatchContext() {
        val config = VioConfiguration.shared
        val autoDiscover = config.state.value.campaign.autoDiscover
        val matchContext = match.toMatchContext(
            channelId = config.state.value.campaign.channelId
        )
        if (autoDiscover) {
            campaignManager.discoverCampaigns(matchId = matchContext.matchId)
            campaignManager.setMatchContext(matchContext)
        } else {
            campaignManager.setMatchContext(matchContext)
        }
    }

    LaunchedEffect(match.id) {
        setupMatchContext()
    }

    DisposableEffect(Unit) {
        playerState.initialize()
        onDispose { playerState.release() }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    player = playerState.player
                    useController = false
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VideoControls(
                title = match.title,
                subtitle = match.subtitle,
                isLandscape = isLandscape,
                isPlaying = playerState.isPlaying.collectAsState().value,
                onBack = onDismiss,
                onTogglePlay = { playerState.togglePlay() },
                onToggleChat = { showChat = !showChat },
            )
        }

        TV2ChatOverlay(
            showControls = showControls,
            onExpandedChange = { expanded ->
                showChat = expanded
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        VioFloatingCartIndicator(
            cartManager = cartManager,
            modifier = Modifier.fillMaxSize(),
            customPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
            onTap = { showCheckout = true },
        )

        if (showCheckout) {
            VioCheckoutOverlay(
                cartManager = cartManager,
                onBack = { showCheckout = false },
            )
        }
    }
}

@Composable
private fun VideoControls(
    title: String,
    subtitle: String,
    isLandscape: Boolean,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleChat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = ViaplayTheme.Typography.title, color = Color.White)
                Text(subtitle, style = ViaplayTheme.Typography.caption, color = Color.White.copy(alpha = 0.7f))
            }
            IconButton(onClick = onToggleChat) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}

private class PlayerStateHolder(context: android.content.Context) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun initialize() {
        val mediaItem = MediaItem.fromUri(Uri.parse("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        _isPlaying.value = true
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        _isPlaying.value = player.isPlaying
    }

    fun release() {
        player.release()
    }
}
