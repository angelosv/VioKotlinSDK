package com.vio.viaplaydemo

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
import live.vio.VioUI.CheckoutDeepLinkBus
import live.vio.VioUI.Components.compose.feedback.VioToastOverlay
import live.vio.VioUI.Components.compose.theme.ProvideAdaptiveVioColors
import live.vio.VioUI.Components.compose.theme.VioTheme
import live.vio.VioUI.Components.compose.theme.adaptiveVioColors
import live.vio.VioUI.Components.compose.cart.VioFloatingCartIndicator
import live.vio.VioUI.Components.compose.product.VioImageLoaderDefaults
import live.vio.VioUI.adapters.VioCoilImageLoader
import live.vio.VioUI.VioCheckoutOverlay
import live.vio.VioUI.Components.VioCheckoutOverlayController as VioCheckoutController
import live.vio.VioUI.Managers.CartManager
import live.vio.VioUI.Managers.Product
import live.vio.VioUI.Managers.addProduct
import live.vio.VioUI.PaymentSheetBridge
import live.vio.VioUI.KlarnaBridge
import live.vio.liveshow.LiveShowManager
import live.vio.liveshow.models.LiveStream
import live.vio.liveshow.models.LiveStreamLayout
import live.vio.liveui.components.VioLiveMiniPlayer
import live.vio.liveui.components.VioLiveShowFullScreenOverlay
import com.google.firebase.messaging.FirebaseMessaging
import live.vio.VioCore.managers.DeviceTokenManager
import live.vio.liveui.components.VioLiveShowOverlayController
import kotlinx.coroutines.launch
import com.vio.viaplaydemo.casting.CastingManager
import com.vio.viaplaydemo.casting.CastingActiveOverlay
import com.vio.viaplaydemo.casting.CastingMiniPlayer
import com.vio.viaplaydemo.ui.ViaplayHomeView
import com.vio.viaplaydemo.ui.theme.ViaplayTheme
import com.vio.viaplaydemo.utils.CacheHelper
import com.vio.viaplaydemo.ui.model.TabItem
import live.vio.VioCore.managers.VioGooglePayManager
import live.vio.VioUI.Managers.confirmGooglePay
import live.vio.VioUI.Managers.updateCheckout
import live.vio.VioUI.Managers.toInputDto
import android.content.Intent
import android.app.Activity
import com.google.android.gms.wallet.AutoResolveHelper
import org.json.JSONObject

import live.vio.sdk.VioSDK
import live.vio.VioCore.configuration.VioEnvironment
import live.vio.VioEngagementUI.Components.VioEngagementProductOverlay
import androidx.compose.runtime.LaunchedEffect
import live.vio.sdk.domain.models.ProductDto
import live.vio.sdk.core.VioSdkClient
import java.net.URL

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ViaplayMainActivity"
    }

    private lateinit var cartManager: CartManager
    private var openedProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()

        // Initialize Firebase
        VioFirebaseInitializer.initialize(this)

        // Configuración simplificada: solo apiKey y entorno.
        // El resto de la configuración (endpoints, commerce, campañas, etc.)
        // se obtiene automáticamente desde el backend via VioSDK.
        val apiKey = BuildConfig.VIO_API_KEY
        VioSDK.configure(
            context = this,
            apiKey = apiKey,
            environment = VioEnvironment.SANDBOX,
        )
        VioSDK.setUserId("android_demo_001")

        // Register FCM token
        registerFCMToken("android_demo_001")

        cartManager = CartManager()
        VioImageLoaderDefaults.install(VioCoilImageLoader)
        println("🖼️ [ViaplayDemo] Installed VioCoilImageLoader")

        // Configurar listeners de limpieza/pre-carga de cache de logos de campaña.
        CacheHelper.setupCacheClearingListener()
        PaymentSheetBridge.attach(this)
        KlarnaBridge.init(this)


        setContent {
            VioTheme {
                ProvideAdaptiveVioColors {
                    Surface(Modifier.fillMaxSize()) {
                        ViaplayDemoApp(cartManager = cartManager, openedProductId = openedProductId)
                    }
                }
            }
        }

        // Handle initial intent if app was opened via notification
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            android.util.Log.w(TAG, "handleIntent called with null intent")
            return
        }

        val extras = intent.extras
        val extraKeys = extras?.keySet()?.joinToString(", ") ?: "(none)"
        android.util.Log.i(TAG, "handleIntent extras keys: $extraKeys")
        android.util.Log.i(TAG, "handleIntent raw extras: ${extras?.toString() ?: "(none)"}")
        val payload = parsePayloadJson(intent.getStringExtra("vio_payload"))
        android.util.Log.i(TAG, "handleIntent parsed vio_payload: $payload")

        val productId = firstNonBlank(
            intent.getStringExtra("productId"),
            intent.getStringExtra("vio_cartIntent_productId"),
            payload?.optString("product_id"),
        )
        val campaignId = firstNonBlank(
            intent.getStringExtra("campaignId"),
            intent.getStringExtra("vio_cartIntent_campaignId"),
            payload?.optString("campaign_id"),
        )
        val action = firstNonBlank(
            intent.getStringExtra("action"),
            intent.getStringExtra("vio_event_type"),
            intent.getStringExtra("vio_cartIntent_kind"),
        )
        val deepLink = firstNonBlank(
            intent.getStringExtra("deeplink"),
            payload?.optString("deeplink"),
        )

        android.util.Log.i(
            TAG,
            "handleIntent parsed values -> action=$action, productId=$productId, campaignId=$campaignId, deepLink=$deepLink, data=${intent.data}",
        )

        if (!productId.isNullOrBlank()) {
            println("🎁 [ViaplayDemo] App opened with productId: $productId")
            openedProductId = productId
        }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun parsePayloadJson(raw: String?): JSONObject? {
        if (raw.isNullOrBlank()) return null
        return try {
            JSONObject(raw)
        } catch (error: Exception) {
            android.util.Log.e(TAG, "Failed to parse vio_payload JSON: ${error.message}")
            null
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update current intent
        handleIntent(intent)
        
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Note: Google Pay result is now handled internally by VioCheckoutOverlay
    }

    private fun copyConfigToFilesDir(fileName: String) {
        // Ya no se usa vio-config.json local para este demo.
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun registerFCMToken(userId: String) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (token != null) {
                        android.util.Log.i("ViaplayDemo", "***** Success! FCM Token retrieved: ${token.take(15)}...")
                        DeviceTokenManager.register(userId, token)
                    }
                } else {
                    android.util.Log.e("ViaplayDemo", "***** CRITICAL: Failed to retrieve FCM token: ${task.exception?.message}", task.exception)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ViaplayDemo", "***** Error starting FCM token retrieval: ${e.message}")
        }
    }
}

@Composable
private fun ViaplayDemoApp(cartManager: CartManager, openedProductId: String? = null) {
    val castingManager = remember { CastingManager.shared }
    val castingState by castingManager.state.collectAsState()
    val colors = adaptiveVioColors()

    var detailProduct by remember { mutableStateOf<Product?>(null) }
    var isCheckoutOpen by remember { mutableStateOf(false) }
    var overlayInitialStep by remember { mutableStateOf(VioCheckoutController.CheckoutStep.OrderSummary) }
    var showCastingOverlay by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf<TabItem>(TabItem.HOME) }
    
    var openedProducts by remember { mutableStateOf<List<ProductDto>>(emptyList()) }

    LaunchedEffect(openedProductId) {
        openedProductId?.let { id ->
            // Fetch product DTO for the overlay
            launch {
                try {
                    val state = live.vio.VioCore.configuration.VioConfiguration.shared.state.value
                    val client = VioSdkClient(
                        baseUrl = URL(state.commerce?.endpoint ?: state.environment.graphQLUrl),
                        apiKey = state.commerce?.apiKey ?: state.apiKey
                    )
                    println("Fetching product for FCM: $id")
                    println("Fetching product for FCM: ${cartManager.currency}")
                    val dtos = client.channel.product.get(
                        currency = cartManager.currency, // simplified
                        productIds = listOf(id.toInt()),
                        imageSize = "medium",
                        shippingCountryCode = "US"
                    )
                    openedProducts = dtos
                } catch (e: Exception) {
                    println("Error fetching product for FCM: ${e.message}")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            TabItem.SPORT -> {
                com.vio.viaplaydemo.ui.ViaplaySportView(
                    cartManager = cartManager,
                    onBack = { selectedTab = TabItem.HOME },
                    onTabSelected = { selectedTab = it }
                )
            }
            else -> {
                ViaplayHomeView(
                    cartManager = cartManager,
                    castingState = castingState,
                    onShowCasting = { showCastingOverlay = true },
                    onOpenProductDetail = { detailProduct = it },
                    onOpenCheckout = {
                        overlayInitialStep = VioCheckoutController.CheckoutStep.OrderSummary
                        isCheckoutOpen = true
                    },
                    onOpenSport = { selectedTab = TabItem.SPORT },
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        if (castingState.isCasting) {
            CastingMiniPlayer(
                state = castingState,
                onClick = { showCastingOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ViaplayTheme.Spacing.lg),
            )
        }

        VioFloatingCartIndicator(
            cartManager = cartManager,
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center),
            customPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = if (castingState.isCasting) 180.dp else 100.dp,
                end = ViaplayTheme.Spacing.md,
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
            live.vio.VioUI.Components.compose.product.VioProductDetailOverlay(
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

    VioEngagementProductOverlay(
        products = openedProducts,
        isVisible = openedProducts.isNotEmpty(),
        onDismiss = {
            openedProducts = emptyList()
            VioSDK.clearOpenedProduct()
        },
        onAddToCart = { product ->
            // In a real app, convert DTO to domain Product and add to cart
            openedProducts = emptyList()
            VioSDK.clearOpenedProduct()
        }
    )
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
            androidx.compose.material3.Text("LIVE", color = androidx.compose.ui.graphics.Color.White, style = ViaplayTheme.Typography.caption)
            androidx.compose.material3.Text(
                stream.streamer.name,
                color = androidx.compose.ui.graphics.Color.White,
                style = ViaplayTheme.Typography.small,
                maxLines = 1,
            )
        }
    }
}
