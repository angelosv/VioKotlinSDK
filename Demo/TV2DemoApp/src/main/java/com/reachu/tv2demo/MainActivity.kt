package com.reachu.tv2demo

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.reachu.VioCore.configuration.ConfigurationLoader
import io.reachu.VioUI.CheckoutDeepLinkBus
import io.reachu.VioUI.Components.compose.feedback.VioToastOverlay
import io.reachu.VioUI.Components.compose.theme.ProvideAdaptiveVioColors
import io.reachu.VioUI.Components.compose.theme.VioTheme
import io.reachu.VioUI.Components.compose.theme.adaptiveVioColors
import io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.adapters.VioCoilImageLoader
import io.reachu.VioUI.VioCheckoutOverlay
import io.reachu.VioUI.Components.VioCheckoutOverlayController as VioCheckoutController
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Managers.addProduct
import io.reachu.VioUI.PaymentSheetBridge
import io.reachu.VioUI.KlarnaBridge
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.LiveStreamLayout
import io.reachu.liveui.components.VioLiveMiniPlayer
import io.reachu.liveui.components.VioLiveShowFullScreenOverlay
import io.reachu.liveui.components.VioLiveShowOverlayController
import java.io.File
import kotlinx.coroutines.launch
import com.reachu.tv2demo.casting.CastingManager
import com.reachu.tv2demo.casting.CastingActiveOverlay
import com.reachu.tv2demo.casting.CastingMiniPlayer
import com.reachu.tv2demo.ui.TV2HomeScreen
import com.reachu.tv2demo.ui.theme.TV2Theme
import com.reachu.tv2demo.utils.CacheHelper

class MainActivity : AppCompatActivity() {

    private lateinit var cartManager: CartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        copyConfigToFilesDir("vio-config.json")
        ConfigurationLoader.loadConfiguration("vio-config", filesDir.absolutePath + "/")

        cartManager = CartManager()
        VioImageLoaderDefaults.install(VioCoilImageLoader)
        println("🖼️ [TV2Demo] Installed VioCoilImageLoader")

        // Configurar listeners de limpieza/pre-carga de cache de logos de campaña.
        CacheHelper.setupCacheClearingListener()
        PaymentSheetBridge.attach(this)
        KlarnaBridge.init(this)

        setContent {
            VioTheme {
                ProvideAdaptiveVioColors {
                    Surface(Modifier.fillMaxSize()) {
                        Tv2DemoApp(cartManager = cartManager)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val uri: Uri = intent.data ?: return
        if (uri.scheme == "vio-demo" && uri.host == "checkout") {
            val status = when (uri.pathSegments.getOrNull(0)) {
                "success" -> CheckoutDeepLinkBus.Status.Success
                "cancel" -> CheckoutDeepLinkBus.Status.Cancel
                else -> return
            }
            lifecycleScope.launch {
                CheckoutDeepLinkBus.emit(CheckoutDeepLinkBus.Event(status))
            }
        }
    }

    private fun copyConfigToFilesDir(fileName: String) {
        val output = File(filesDir, fileName)
        if (output.exists()) return
        assets.open(fileName).use { input ->
            output.outputStream().use { outputStream ->
                input.copyTo(outputStream)
            }
        }
    }
}

@Composable
private fun Tv2DemoApp(cartManager: CartManager) {
    val castingManager = remember { CastingManager.shared }
    val castingState by castingManager.state.collectAsState()
    val colors = adaptiveVioColors()

    var detailProduct by remember { mutableStateOf<Product?>(null) }
    var isCheckoutOpen by remember { mutableStateOf(false) }
    var overlayInitialStep by remember { mutableStateOf(VioCheckoutController.CheckoutStep.OrderSummary) }
    var showCastingOverlay by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        TV2HomeScreen(
            cartManager = cartManager,
            castingState = castingState,
            onShowCasting = { showCastingOverlay = true },
            onOpenProductDetail = { detailProduct = it },
            onOpenCheckout = {
                overlayInitialStep = VioCheckoutController.CheckoutStep.OrderSummary
                isCheckoutOpen = true
            },
        )

        if (castingState.isCasting) {
            CastingMiniPlayer(
                state = castingState,
                onClick = { showCastingOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(TV2Theme.Spacing.lg),
            )
        }

        VioFloatingCartIndicator(
            cartManager = cartManager,
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center),
            customPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = if (castingState.isCasting) 180.dp else 100.dp,
                end = TV2Theme.Spacing.md,
            ),
            onTap = {
                overlayInitialStep = VioCheckoutController.CheckoutStep.OrderSummary
                isCheckoutOpen = true
            },
        )

        LiveShowGlobalOverlay(cartManager = cartManager)
        VioToastOverlay()
    }

    if (isCheckoutOpen) {
        Box(modifier = Modifier.fillMaxSize()) {
            VioCheckoutOverlay(
                cartManager = cartManager,
                onBack = { isCheckoutOpen = false },
                initialStep = overlayInitialStep,
            )
        }
    }

    detailProduct?.let { product ->
        Box(modifier = Modifier.fillMaxSize()) {
            io.reachu.VioUI.Components.compose.product.VioProductDetailOverlay(
                product = product,
                currencySymbol = cartManager.currencySymbol,
                onAddToCart = { variant, quantity ->
                    cartManager.addProductAsync(product, quantity, variant)
                },
                onDismiss = { detailProduct = null },
            )
        }
    }

    if (showCastingOverlay && castingState.isCasting) {
        CastingActiveOverlay(
            state = castingState,
            onStopCasting = {
                CastingManager.shared.stopCasting()
                showCastingOverlay = false
            },
            onClose = { showCastingOverlay = false },
        )
    }
}

@Composable
private fun BoxScope.LiveShowGlobalOverlay(cartManager: CartManager) {
    val manager = remember { LiveShowManager.shared }
    val isOverlayVisible by manager.isLiveShowVisible.collectAsState()
    val isMiniVisible by manager.isMiniPlayerVisible.collectAsState()
    val isIndicatorVisible by manager.isIndicatorVisible.collectAsState()
    val currentStream by manager.currentStream.collectAsState()
    val activeStreams by manager.activeStreams.collectAsState()
    val overlayController = remember(cartManager) { VioLiveShowOverlayController(cartManager = cartManager) }

    if (isOverlayVisible) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center),
        ) {
            VioLiveShowFullScreenOverlay(controller = overlayController, modifier = Modifier.fillMaxSize())
        }
    }
    if (isMiniVisible && currentStream != null) {
        VioLiveMiniPlayer(
            stream = currentStream!!,
            onDismiss = { manager.hideLiveStream() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
    if (activeStreams.isNotEmpty() && isIndicatorVisible && !manager.isWatchingLiveStream) {
        LiveIndicatorChip(
            stream = activeStreams.first(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            onTap = { manager.showLiveStream(it, LiveStreamLayout.FULL_SCREEN_OVERLAY) },
        )
    }
}

@Composable
private fun LiveIndicatorChip(
    stream: LiveStream,
    modifier: Modifier = Modifier,
    onTap: (LiveStream) -> Unit,
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        modifier = modifier.clickable { onTap(stream) },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(8.dp),
            ) {
                drawCircle(color = androidx.compose.ui.graphics.Color.Red)
            }
            androidx.compose.material3.Text("LIVE", color = androidx.compose.ui.graphics.Color.White, style = TV2Theme.Typography.caption)
            androidx.compose.material3.Text(
                stream.streamer.name,
                color = androidx.compose.ui.graphics.Color.White,
                style = TV2Theme.Typography.small,
                maxLines = 1,
            )
        }
    }
}
