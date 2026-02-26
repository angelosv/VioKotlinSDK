package com.reachu.demoapp

import android.os.Bundle
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import io.reachu.VioUI.PaymentSheetBridge
import io.reachu.VioUI.CheckoutDeepLinkBus
import io.reachu.VioUI.KlarnaBridge
import io.reachu.VioUI.Managers.VippsPaymentHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.reachu.VioCore.configuration.ConfigurationLoader
import io.reachu.VioUI.Components.compose.market.VioMarketSelector
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Market
import io.reachu.VioUI.Managers.addProduct
import io.reachu.VioUI.Components.VioProductSliderLayout
import io.reachu.VioUI.adapters.VioCoilImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import java.io.File
import io.reachu.VioDesignSystem.Tokens.VioBorderRadius
import io.reachu.VioDesignSystem.Tokens.VioTypography
import io.reachu.VioCore.configuration.TypographyToken
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.reachu.VioDesignSystem.Tokens.VioColors
import io.reachu.VioUI.Components.compose.theme.AdaptiveVioColorsCompose
import io.reachu.VioUI.Components.compose.theme.ProvideAdaptiveVioColors
import io.reachu.VioUI.Components.compose.theme.VioTheme
import io.reachu.VioUI.Components.compose.theme.adaptiveVioColors
import io.reachu.VioUI.Components.compose.feedback.VioToastOverlay
import io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator
import io.reachu.VioUI.VioCheckoutOverlay
import io.reachu.VioUI.Components.VioCheckoutOverlayController as VioCheckoutController
import io.reachu.VioUI.Components.compose.product.VioProductBanner
import io.reachu.VioUI.Components.compose.offers.VioOfferBannerDynamic
import io.reachu.VioCore.models.OfferBannerConfig
import io.reachu.VioUI.Components.compose.product.VioProductCarousel
import io.reachu.VioUI.Components.compose.product.VioProductSlider
import io.reachu.VioUI.Components.compose.product.VioProductSpotlight
import io.reachu.VioUI.Components.compose.product.VioProductStore
import io.reachu.VioUI.Components.compose.common.SponsorBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import io.reachu.VioUI.Components.compose.product.VioProductCard
import io.reachu.VioUI.Components.compose.product.VioProductDetailOverlay
import io.reachu.VioUI.Components.VioProductCardConfig
import io.reachu.VioUI.Managers.CartItem
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import io.reachu.VioUI.Managers.updateQuantity
import io.reachu.VioUI.Managers.removeItem
import io.reachu.VioUI.Managers.loadProducts
import io.reachu.VioUI.Managers.ensureCartIDForCheckout
import io.reachu.VioUI.Managers.discountApplyOrCreate
import io.reachu.VioUI.Managers.discountRemoveApplied
import io.reachu.VioUI.Managers.clearCart
import io.reachu.VioUI.Managers.Product
import androidx.compose.runtime.collectAsState
import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.FloatingCartPosition
import io.reachu.VioCore.configuration.FloatingCartDisplayMode
import io.reachu.VioCore.configuration.FloatingCartSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import io.reachu.VioDesignSystem.Tokens.VioSpacing
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import io.reachu.liveshow.LiveShowManager
import io.reachu.liveshow.models.LiveProduct
import io.reachu.liveshow.models.LiveStream
import io.reachu.liveshow.models.LiveStreamLayout
import io.reachu.liveui.components.DynamicComponentsService
import io.reachu.liveui.components.VioLiveMiniPlayer
import io.reachu.liveui.components.VioLiveMiniPlayer
import io.reachu.liveui.components.VioLiveShowFullScreenOverlay
import io.reachu.liveui.components.VioLiveShowOverlayController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reachu.demoapp.cart.CartUiState
import com.reachu.demoapp.cart.CartViewModel
import com.reachu.demoapp.cart.CartViewModelFactory
import com.reachu.demoapp.navigation.DemoDestination

class MainActivity : AppCompatActivity() {

    private lateinit var cartManager: CartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the Vio SDK configuration from assets by copying it to a temporary file
        val configFileName = "vio-config.json"
        val configFile = File(filesDir, configFileName)
        assets.open(configFileName).use { input ->
            configFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        ConfigurationLoader.loadConfiguration("vio-config", filesDir.absolutePath + "/")

        // Initialize CartManager (auto-bootstrap creates cart and loads markets)
        cartManager = CartManager()
        VioImageLoaderDefaults.install(VioCoilImageLoader)
        println("🖼️ [DemoApp] Installed VioCoilImageLoader")

        // Initialize native SDK bridges early (before STARTED)
        PaymentSheetBridge.attach(this)
        KlarnaBridge.init(this)
        
        handleIntent(intent)

        setContent {
            VioTheme {
                ProvideAdaptiveVioColors {
                    Surface {
                        DemoScreen(cartManager)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "reachu-demo" && uri.host == "checkout") {
            // Update global Vipps handler state from deep link
            VippsPaymentHandler.handleReturnUrl(uri.toString())

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
}

@Composable
fun DemoScreen(cartManager: CartManager) {
    val cartViewModel: CartViewModel = viewModel(factory = CartViewModelFactory(cartManager))
    val cartUiState by cartViewModel.uiState.collectAsState()
    var selectedMarket by remember { mutableStateOf(cartManager.selectedMarket) }
    val scope = rememberCoroutineScope()
    val colors = adaptiveVioColors()

    var destination by remember { mutableStateOf<DemoDestination>(DemoDestination.Home) }
    var isCheckoutOpen by remember { mutableStateOf(false) }
    var overlayInitialStep by remember { mutableStateOf(VioCheckoutController.CheckoutStep.OrderSummary) }
    var detailProduct: Product? by remember { mutableStateOf(null) }

    fun openProductDetail(product: Product) {
        detailProduct = product
    }

    fun openProductDetailById(productId: String?) {
        val id = productId?.toIntOrNull() ?: return
        cartManager.products.firstOrNull { it.id == id }?.let { product ->
            detailProduct = product
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
        Text(
            "Vio SDK",
            style = VioTypography.largeTitle.toComposeTextStyle(),
            color = VioColors.primary.toColor()
        )
        Text(
            "Demo Kotlin App",
            style = VioTypography.body.toComposeTextStyle(),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section title to mirror iOS demo
        Text(
            "Market & Currency",
            style = VioTypography.headline.toComposeTextStyle(),
            color = colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        VioMarketSelector(
            cartManager = cartManager,
            onSelected = { market ->
                selectedMarket = market
            },
            imageLoader = VioCoilImageLoader,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Removed the explicit "Selected:" label to match iOS layout
        Spacer(modifier = Modifier.height(24.dp))

        Text("🚀 Auto-Loading Products", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        Text(
            "Products load automatically from the API - no manual code needed!",
            style = VioTypography.caption1.toComposeTextStyle(),
            color = colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (destination == DemoDestination.Home) {
            VioProductSlider(
                cartManager = cartManager,
                title = "Featured Products",
                layout = VioProductSliderLayout.CARDS,
                showSeeAll = true,
                onProductTap = { product ->
                    println("👆 [MainActivity] Product tapped: ${product.title} (ID: ${product.id})")
                    detailProduct = product
                },
                onAddToCart = { product ->
                    println("🛒 [MainActivity] Product added to cart via Slider: ${product.title}")
                },
                currency = cartManager.currency,
                country = cartManager.country,
                imageLoader = VioCoilImageLoader,
                isCampaignGated = false,
                products = cartManager.products,
                isLoading = cartManager.isProductsLoading,
                showSponsor = true,
                sponsorPosition = "top_right",      
                sponsorLogoUrl = "https://png.pngtree.com/png-vector/20211031/ourmid/pngtree-round-country-flag-norway-png-image_4017299.png"          
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "✨ Auto-Configured Campaign Components",
                style = VioTypography.headline.toComposeTextStyle(),
                color = colors.textPrimary,
            )
            Text(
                "These components automatically configure themselves from the active campaign!",
                style = VioTypography.caption1.toComposeTextStyle(),
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(12.dp))

            VioProductBanner(
                modifier = Modifier.fillMaxWidth(),
                imageLoader = VioCoilImageLoader,
                onBannerClick = { state -> openProductDetailById(state.productId) },
                onCtaClick = { state -> openProductDetailById(state.productId) },
                showSponsor = true,
                sponsorPosition = "top_right",                
            )

            Spacer(Modifier.height(24.dp))

            VioOfferBannerDynamic(
                showSponsor = true,
                sponsorPosition = "top_right",  
                sponsorLogoUrl = "https://png.pngtree.com/png-vector/20211031/ourmid/pngtree-round-country-flag-norway-png-image_4017299.png",
                onNavigateToStore = {
                    println("🚀 [MainActivity] VioOfferBanner: Navigating to store...")
                }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Featured Carousel",
                style = VioTypography.headline.toComposeTextStyle(),
                color = colors.textPrimary,
            )
            Text(
                "Auto-plays products configured in the active campaign.",
                style = VioTypography.caption1.toComposeTextStyle(),
                color = colors.textSecondary,
            )

            VioProductCarousel(
                cartManager = cartManager,
                modifier = Modifier.fillMaxWidth(),
                layout = "full",
                onProductTap = { product -> openProductDetail(product) },
                showSponsor = true,
                sponsorPosition = "top_right",
                
            )

            VioProductCarousel(
                cartManager = cartManager,
                modifier = Modifier.fillMaxWidth(),
                layout = "compact",
                onProductTap = { product -> openProductDetail(product) },
                showSponsor = true,
                sponsorPosition = "top_right",                
            )

            VioProductCarousel(
                cartManager = cartManager,
                modifier = Modifier.fillMaxWidth(),
                layout = "horizontal",
                onProductTap = { product -> openProductDetail(product) },
                showSponsor = true,
                sponsorPosition = "top_right",                
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Product Store",
                style = VioTypography.headline.toComposeTextStyle(),
                color = colors.textPrimary,
            )
            Text(
                "Grid or list layouts controlled directly from the campaign.",
                style = VioTypography.caption1.toComposeTextStyle(),
                color = colors.textSecondary,
            )
            VioProductStore(
                cartManager = cartManager,
                modifier = Modifier.fillMaxWidth(),
                isCampaignGated = false,
                isScrollEnabled = false,
                showSponsor = true,
                sponsorPosition = "top_right",                
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Spotlight Product",
                style = VioTypography.headline.toComposeTextStyle(),
                color = colors.textPrimary,
            )
            Text(
                "Highlights a single SKU picked by the merchandising team.",
                style = VioTypography.caption1.toComposeTextStyle(),
                color = colors.textSecondary,
            )
            VioProductSpotlight(
                cartManager = cartManager,
                modifier = Modifier.fillMaxWidth(),
                onProductTap = { product -> openProductDetail(product) },
                isCampaignGated = false,
                showSponsor = true,
                sponsorPosition = "top_right",                
            )

            Spacer(Modifier.height(24.dp))
            // Demo sections (navigation-like)
            DemoSection(
                title = "Product Catalog",
                description = "Browse and add products to cart",
                onClick = { destination = DemoDestination.ProductCatalog },
                colors = colors,
            )
            DemoSection(
                title = "Product Sliders",
                description = "Horizontal scrolling product collections",
                onClick = { destination = DemoDestination.ProductSliders },
                colors = colors,
            )
            DemoSection(
                title = "Shopping Cart",
                description = "Manage items in your cart",
                onClick = { destination = DemoDestination.ShoppingCart },
                colors = colors,
            )
            DemoSection(
                title = "Checkout Flow",
                description = "Simulate the checkout process",
                onClick = { destination = DemoDestination.Checkout },
                colors = colors,
            )
            DemoSection(
                title = "Floating Cart Options",
                description = "Test different positions and styles",
                onClick = { destination = DemoDestination.FloatingCart },
                colors = colors,
            )
            DemoSection(
                title = "Live Show Experience",
                description = "Explore the live streaming commerce stack",
                onClick = { destination = DemoDestination.LiveShow },
                colors = colors,
            )
        } else when (destination) {
            DemoDestination.Home -> Unit
            DemoDestination.ProductCatalog -> ProductCatalogDemoView(
                cartManager = cartManager,
                onBack = { destination = DemoDestination.Home },
                onProductSelected = { detailProduct = it },
            )
            DemoDestination.ProductSliders -> ProductSliderDemoView(
                cartManager = cartManager,
                onBack = { destination = DemoDestination.Home },
                onProductSelected = { detailProduct = it },
            )
            DemoDestination.ShoppingCart -> ShoppingCartScreen(
                currencySymbol = cartManager.currencySymbol,
                currencyCode = cartManager.currency,
                uiState = cartUiState,
                onBack = { destination = DemoDestination.Home },
                onCheckout = {
                    overlayInitialStep = VioCheckoutController.CheckoutStep.Review
                    isCheckoutOpen = true
                },
                onIncrement = cartViewModel::increment,
                onDecrement = cartViewModel::decrement,
                onRemove = cartViewModel::remove,
                onClear = cartViewModel::clear,
                onApplyDiscount = cartViewModel::applyDiscount,
                onRemoveDiscount = cartViewModel::removeDiscount,
            )
            DemoDestination.Checkout -> CheckoutDemoView(
                uiState = cartUiState,
                currencySymbol = cartManager.currencySymbol,
                onBack = { destination = DemoDestination.Home },
                onOpen = {
                overlayInitialStep = VioCheckoutController.CheckoutStep.OrderSummary
                isCheckoutOpen = true
            })
            DemoDestination.FloatingCart -> FloatingCartDemoView(cartManager = cartManager, onBack = { destination = DemoDestination.Home })
            DemoDestination.LiveShow -> LiveShowDemoView(cartManager = cartManager, onBack = { destination = DemoDestination.Home })
        }

        // Extra bottom space so the floating cart doesn't overlap the last demo option
        Spacer(Modifier.height(120.dp))
        }

        // Overlays similar to Swift ContentView
        if (isCheckoutOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                VioCheckoutOverlay(
                    cartManager = cartManager,
                    onBack = { isCheckoutOpen = false },
                    initialStep = overlayInitialStep,
                    isCampaignGated = false,
                )
            }
        }
        detailProduct?.let { p ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                VioProductDetailOverlay(
                    product = p,
                    currencySymbol = cartManager.currencySymbol,
                    onAddToCart = { variant, quantity -> cartManager.addProductAsync(p, 1, variant) },
                    onDismiss = { detailProduct = null },
                    imageLoader = VioCoilImageLoader,
                    isCampaignGated = false,
                )
            }
        }
        VioFloatingCartIndicator(
            cartManager = cartManager,
            modifier = Modifier.fillMaxSize(),
            onTap = {
                if (detailProduct == null) {
                    overlayInitialStep = VioCheckoutController.CheckoutStep.OrderSummary
                    isCheckoutOpen = true
                }
            },
            isCampaignGated = false,
        )
        LiveShowGlobalOverlay(cartManager = cartManager)
        // Place toast overlay last so it renders above everything
        VioToastOverlay()
    }
}

// MarketInfo removed for visual parity with iOS (no extra label under chips)

private fun TypographyToken.toComposeTextStyle(): TextStyle {
    val weight = when (fontWeight.lowercase()) {
        "bold" -> FontWeight.Bold
        "semibold" -> FontWeight.SemiBold
        "medium" -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    return TextStyle(
        fontSize = this.fontSize.sp,
        lineHeight = this.lineHeight.sp,
        fontWeight = weight,
    )
}

private fun String.toColor(): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))

@Composable
private fun DemoSection(title: String, description: String, onClick: () -> Unit, colors: AdaptiveVioColorsCompose) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(description, style = VioTypography.body.toComposeTextStyle(), color = colors.textSecondary)
            }
            Text("›", style = VioTypography.title2.toComposeTextStyle(), color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProductCatalogDemoView(
    cartManager: CartManager,
    onBack: () -> Unit,
    onProductSelected: (Product) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = adaptiveVioColors()
    var variant by remember { mutableStateOf(VioProductCardConfig.Variant.GRID) }
    var products by remember { mutableStateOf(cartManager.products) }
    var loading by remember { mutableStateOf(cartManager.isProductsLoading) }

    LaunchedEffect(cartManager.country, cartManager.currency) {
        loading = true
        // Ensure products are loaded with current market
        cartManager.loadProducts(
            currency = cartManager.currency,
            shippingCountryCode = cartManager.country,
            useCache = true,
        )
        products = cartManager.products
        loading = false
    }

    Text("Choose Layout", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))

    // Segmented layout selector
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        @Composable
        fun Chip(label: String, isSelected: Boolean, onClick: () -> Unit) {
            val bg = if (isSelected) VioColors.primary.toColor() else colors.surface
            val fg = if (isSelected) Color.White else colors.textPrimary
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = if (isSelected) null else CardDefaults.outlinedCardBorder(),
                modifier = Modifier.clickable { onClick() }
            ) { Text(label, color = fg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
        }
        Chip("Grid", variant == VioProductCardConfig.Variant.GRID) { variant = VioProductCardConfig.Variant.GRID }
        Chip("List", variant == VioProductCardConfig.Variant.LIST) { variant = VioProductCardConfig.Variant.LIST }
        Chip("Hero", variant == VioProductCardConfig.Variant.HERO) { variant = VioProductCardConfig.Variant.HERO }
        Chip("Minimal", variant == VioProductCardConfig.Variant.MINIMAL) { variant = VioProductCardConfig.Variant.MINIMAL }

    }

    Spacer(Modifier.height(12.dp))

    // Sponsor Badge Controls
    var showSponsor by remember { mutableStateOf(true) }
    var sponsorPosition by remember { mutableStateOf("topRight") }

    Text("Sponsor Badge Demo", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text("Standalone Component:", style = VioTypography.caption1.toComposeTextStyle(), color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SponsorBadge(
                    logoUrl = "https://www.pngfind.com/pngs/m/293-2939359_power-logo-grey-power-logo-hd-png-download.png", // Example URL
                    text = "Powered by",
                    imageLoader = VioCoilImageLoader,
                )
            }
        }
    }
    
    Spacer(Modifier.height(12.dp))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { showSponsor = !showSponsor }) {
            Text(if (showSponsor) "Hide Sponsor" else "Show Sponsor")
        }
        Spacer(Modifier.width(8.dp))
        if(showSponsor) {
            Button(onClick = { 
                sponsorPosition = when(sponsorPosition) {
                    "topRight" -> "topLeft"
                    "topLeft" -> "bottomRight"
                    "bottomRight" -> "bottomLeft"
                    else -> "topRight"
                }
            }) {
                Text("Pos: $sponsorPosition")
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Spacer(Modifier.height(12.dp))

    if (loading) {
        Text("Loading products…", color = colors.textSecondary)
    } else if (products.isEmpty()) {
        Text("No products loaded", color = colors.textSecondary)
    } else {
        when (variant) {
            VioProductCardConfig.Variant.GRID -> {
                // Simple 2-column grid using rows
                val rows = products.chunked(2)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { p ->
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                VioProductCard(
                                    product = p,
                                    variant = VioProductCardConfig.Variant.GRID,
                                    onTap = { onProductSelected(p) },
                                    onAddToCart = { variant, quantity -> cartManager.addProductAsync(p, 1, variant) },
                                    modifier = Modifier.fillMaxWidth(),
                                    imageLoader = VioCoilImageLoader,
                                    showSponsor = showSponsor,
                                    sponsorPosition = sponsorPosition,                                
                                )
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            VioProductCardConfig.Variant.LIST -> {
                products.forEach { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(12.dp)) {
                                VioProductCard(
                                    product = p,
                                    variant = VioProductCardConfig.Variant.LIST,
                                    onTap = { onProductSelected(p) },
                                    onAddToCart = { variant, quantity -> cartManager.addProductAsync(p, 1, variant) },
                                    modifier = Modifier.fillMaxWidth(),
                                    imageLoader = VioCoilImageLoader,
                                    showSponsor = showSponsor,
                                    sponsorPosition = sponsorPosition,
                                )
                        }
                    }
                }
            }
            VioProductCardConfig.Variant.HERO -> {
                products.forEach { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(12.dp)) {
                                VioProductCard(
                                    product = p,
                                    variant = VioProductCardConfig.Variant.HERO,
                                    onTap = { onProductSelected(p) },
                                    onAddToCart = { variant, quantity -> cartManager.addProductAsync(p, 1, variant) },
                                    modifier = Modifier.fillMaxWidth(),
                                    imageLoader = VioCoilImageLoader,
                                    showSponsor = showSponsor,
                                    sponsorPosition = sponsorPosition,
                                )
                        }
                    }
                }
            }
            VioProductCardConfig.Variant.MINIMAL -> {
                // Minimal as a horizontal row of mini cards
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    products.forEach { p ->
        VioProductCard(
            product = p,
            variant = VioProductCardConfig.Variant.MINIMAL,
            onTap = { onProductSelected(p) },
            onAddToCart = { variant, quantity -> cartManager.addProductAsync(p, 1, variant) },
            imageLoader = VioCoilImageLoader,
            showSponsor = showSponsor,
            sponsorPosition = sponsorPosition,
        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Button(onClick = onBack) { Text("Back") }
}

@Composable
private fun ProductSliderDemoView(
    cartManager: CartManager,
    onBack: () -> Unit,
    onProductSelected: (Product) -> Unit,
) {
    val colors = adaptiveVioColors()
    var layout by remember { mutableStateOf(VioProductSliderLayout.FEATURED) }

    Text("Product Sliders", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Text("Choose Layout", style = VioTypography.body.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        @Composable
        fun Chip(label: String, isSelected: Boolean, onClick: () -> Unit) {
            val bg = if (isSelected) VioColors.primary.toColor() else colors.surface
            val fg = if (isSelected) Color.White else colors.textPrimary
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = if (isSelected) null else CardDefaults.outlinedCardBorder(),
                modifier = Modifier.clickable { onClick() }
            ) { Text(label, color = fg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
        }
        Chip("Showcase", layout == VioProductSliderLayout.SHOWCASE) { layout = VioProductSliderLayout.SHOWCASE }
        Chip("Wide", layout == VioProductSliderLayout.WIDE) { layout = VioProductSliderLayout.WIDE }
        Chip("Featured", layout == VioProductSliderLayout.FEATURED) { layout = VioProductSliderLayout.FEATURED }
        Chip("Cards", layout == VioProductSliderLayout.CARDS) { layout = VioProductSliderLayout.CARDS }
        Chip("Compact", layout == VioProductSliderLayout.COMPACT) { layout = VioProductSliderLayout.COMPACT }
        Chip("Micro", layout == VioProductSliderLayout.MICRO) { layout = VioProductSliderLayout.MICRO }
    }

    Spacer(Modifier.height(12.dp))
    VioProductSlider(
        cartManager = cartManager,
        title = layout.name.lowercase().replaceFirstChar { it.uppercase() },
        layout = layout,
        showSeeAll = true,
        currency = cartManager.currency,
        country = cartManager.country,
        imageLoader = VioCoilImageLoader,
        onProductTap = { product -> onProductSelected(product) },
        isCampaignGated = false,
    )

    Spacer(Modifier.height(16.dp))
    Button(onClick = onBack) { Text("Back") }
}

@Composable
private fun ShoppingCartScreen(
    currencySymbol: String,
    currencyCode: String,
    uiState: CartUiState,
    onBack: () -> Unit,
    onCheckout: () -> Unit = {},
    onIncrement: (CartItem) -> Unit,
    onDecrement: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onClear: () -> Unit,
    onApplyDiscount: (String) -> Unit,
    onRemoveDiscount: () -> Unit,
) {
    val colors = adaptiveVioColors()
    val items = uiState.items
    Text("Shopping Cart", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(12.dp))
    if (items.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = VioColors.surfaceSecondary.toColor())) {
            Text(
                "Your cart is empty",
                modifier = Modifier.padding(16.dp),
                style = VioTypography.body.toComposeTextStyle(),
                color = colors.textSecondary,
            )
        }
    } else {
        LazyColumn(
            Modifier.fillMaxWidth().height(360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(items, key = { it.id }) { item ->
                CartRow(
                    item = item,
                    onInc = { onIncrement(item) },
                    onDec = { onDecrement(item) },
                    onRemove = { onRemove(item) },
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    val subtotal = uiState.subtotal
    val ship = uiState.shipping
    val total = uiState.total
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", color = colors.textSecondary)
            Text("$currencyCode ${String.format("%.2f", subtotal)}", color = colors.textPrimary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Shipping", color = colors.textSecondary)
            Text(if (ship == 0.0) "Free" else "$currencyCode ${String.format("%.2f", ship)}", color = colors.textPrimary)
        }
        val tax = uiState.tax
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tax", color = colors.textSecondary)
            Text("$currencyCode ${String.format("%.2f", tax)}", color = colors.textPrimary)
        }
        val disc = uiState.discount
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Discount", color = colors.textSecondary)
            val dc = uiState.discountCode
            val label = if (!dc.isNullOrBlank()) " ($dc)" else ""
            Text("$currencyCode ${String.format("%.2f", -disc)}$label", color = if (disc > 0) Color(0xFF2E7D32) else colors.textPrimary)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$currencyCode ${String.format("%.2f", uiState.total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primary)
        }
    }
    Spacer(Modifier.height(16.dp))
    var discountCode by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = discountCode,
            onValueChange = { discountCode = it },
            label = { Text("Discount code") },
            modifier = Modifier.weight(1f),
        )
        Button(onClick = {
            onApplyDiscount(discountCode)
            discountCode = ""
        }, enabled = discountCode.isNotBlank()) { Text("Apply") }
        if (!uiState.discountCode.isNullOrBlank()) {
            Button(onClick = {
                onRemoveDiscount()
            }) { Text("Remove") }
        }
    }
    val discountMsg = uiState.error ?: uiState.discountCode?.let { "Discount applied ($it)" }
    if (!discountMsg.isNullOrBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(discountMsg, color = colors.textSecondary, style = VioTypography.caption1.toComposeTextStyle())
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onBack) { Text("Back") }
        Button(onClick = onClear, enabled = items.isNotEmpty()) { Text("Clear cart") }
        Button(onClick = onCheckout, enabled = items.isNotEmpty() && !uiState.isLoading) { Text("Proceed to Checkout") }
    }
}

@Composable
private fun CartRow(item: CartItem, onInc: () -> Unit, onDec: () -> Unit, onRemove: () -> Unit) {
    val colors = adaptiveVioColors()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!item.imageUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.height(56.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
            val lineTotal = item.price * item.quantity
            Text("${item.currency} ${String.format("%.2f", lineTotal)}", color = colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onDec) { Text("–") }
            Text(item.quantity.toString())
            Button(onClick = onInc) { Text("+") }
            Button(onClick = onRemove) { Text("Remove") }
        }
    }
}

@Composable
private fun CheckoutDemoView(
    uiState: CartUiState,
    currencySymbol: String,
    onBack: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = adaptiveVioColors()
    Text("Checkout Flow", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Text("Preview your cart and open the full checkout.", style = VioTypography.body.toComposeTextStyle(), color = colors.textSecondary)
    Spacer(Modifier.height(12.dp))
    val items = uiState.items
    if (items.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = VioColors.surfaceSecondary.toColor())) {
            Text("Your cart is empty", modifier = Modifier.padding(16.dp), color = colors.textSecondary)
        }
    } else {
        LazyColumn(Modifier.fillMaxWidth().height(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    val line = item.price * item.quantity
                    Text("${item.currency.ifBlank { currencySymbol }} ${String.format("%.2f", line)}")
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onOpen, enabled = items.isNotEmpty()) { Text("Open Checkout") }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onBack) { Text("Back") }
}

@Composable
private fun FloatingCartDemoView(cartManager: CartManager, onBack: () -> Unit) {
    val colors = adaptiveVioColors()
    val cfg by VioConfiguration.shared.state.collectAsState()
    var position by remember { mutableStateOf(cfg.cart.floatingCartPosition) }
    var display by remember { mutableStateOf(cfg.cart.floatingCartDisplayMode) }
    var size by remember { mutableStateOf(cfg.cart.floatingCartSize) }

    fun applyChanges() {
        VioConfiguration.updateCartConfiguration(
            cfg.cart.copy(
                floatingCartPosition = position,
                floatingCartDisplayMode = display,
                floatingCartSize = size,
            )
        )
    }

    @Composable
    fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
        val bg = if (selected) VioColors.primary.toColor() else colors.surface
        val fg = if (selected) Color.White else colors.textPrimary
        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            border = if (selected) null else CardDefaults.outlinedCardBorder(),
            modifier = Modifier.clickable { onClick() }
        ) {
            Text(label, color = fg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }

    Text("Floating Cart", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Text("Configure position, style and size.", style = VioTypography.body.toComposeTextStyle(), color = colors.textSecondary)
    Spacer(Modifier.height(16.dp))

    // Position
    Text("Position", style = VioTypography.title3.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Top Left", position == FloatingCartPosition.TOP_LEFT) { position = FloatingCartPosition.TOP_LEFT; applyChanges() }
        Chip("Top Center", position == FloatingCartPosition.TOP_CENTER) { position = FloatingCartPosition.TOP_CENTER; applyChanges() }
        Chip("Top Right", position == FloatingCartPosition.TOP_RIGHT) { position = FloatingCartPosition.TOP_RIGHT; applyChanges() }
        Chip("Center Left", position == FloatingCartPosition.CENTER_LEFT) { position = FloatingCartPosition.CENTER_LEFT; applyChanges() }
        Chip("Center Right", position == FloatingCartPosition.CENTER_RIGHT) { position = FloatingCartPosition.CENTER_RIGHT; applyChanges() }
        Chip("Bottom Left", position == FloatingCartPosition.BOTTOM_LEFT) { position = FloatingCartPosition.BOTTOM_LEFT; applyChanges() }
        Chip("Bottom Center", position == FloatingCartPosition.BOTTOM_CENTER) { position = FloatingCartPosition.BOTTOM_CENTER; applyChanges() }
        Chip("Bottom Right", position == FloatingCartPosition.BOTTOM_RIGHT) { position = FloatingCartPosition.BOTTOM_RIGHT; applyChanges() }
    }

    Spacer(Modifier.height(16.dp))

    // Display Mode
    Text("Display Mode", style = VioTypography.title3.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Full", display == FloatingCartDisplayMode.FULL) { display = FloatingCartDisplayMode.FULL; applyChanges() }
        Chip("Compact", display == FloatingCartDisplayMode.COMPACT) { display = FloatingCartDisplayMode.COMPACT; applyChanges() }
        Chip("Minimal", display == FloatingCartDisplayMode.MINIMAL) { display = FloatingCartDisplayMode.MINIMAL; applyChanges() }
        Chip("Icon Only", display == FloatingCartDisplayMode.ICON_ONLY) { display = FloatingCartDisplayMode.ICON_ONLY; applyChanges() }
    }

    Spacer(Modifier.height(16.dp))

    // Size
    Text("Size", style = VioTypography.title3.toComposeTextStyle(), color = colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Small", size == FloatingCartSize.SMALL) { size = FloatingCartSize.SMALL; applyChanges() }
        Chip("Medium", size == FloatingCartSize.MEDIUM) { size = FloatingCartSize.MEDIUM; applyChanges() }
        Chip("Large", size == FloatingCartSize.LARGE) { size = FloatingCartSize.LARGE; applyChanges() }
    }

    Spacer(Modifier.height(16.dp))
    Text("Note: Indicator appears only when cart has items.", style = VioTypography.caption1.toComposeTextStyle(), color = colors.textSecondary)
    Spacer(Modifier.height(16.dp))
    Button(onClick = onBack) { Text("Back") }
}

@Composable
private fun LiveShowDemoView(cartManager: CartManager, onBack: () -> Unit) {
    val manager = remember { LiveShowManager.shared }
    val colors = adaptiveVioColors()
    val activeStreams by manager.activeStreams.collectAsState()
    val isLiveVisible by manager.isLiveShowVisible.collectAsState()
    val isMiniVisible by manager.isMiniPlayerVisible.collectAsState()
    val isIndicatorVisible by manager.isIndicatorVisible.collectAsState()
    val connectionStatus by manager.connectionStatus.collectAsState()
    val viewerCount by manager.currentViewerCount.collectAsState()
    val currentStream by manager.currentStream.collectAsState()
    var selectedLayout by remember { mutableStateOf(LiveStreamLayout.FULL_SCREEN_OVERLAY) }
    var selectedStream by remember { mutableStateOf<LiveStream?>(null) }
    var hasLoadedComponents by remember { mutableStateOf(false) }
    var componentFetchInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeStreams) {
        if (activeStreams.isEmpty()) return@LaunchedEffect
        if (selectedStream == null || activeStreams.none { it.id == selectedStream?.id }) {
            selectedStream = activeStreams.first()
        }
        if (!hasLoadedComponents) {
            runCatching {
                val target = selectedStream ?: activeStreams.first()
                val components = DynamicComponentsService.fetch(target.id)
                componentFetchInfo = "Loaded ${components.size} dynamic components"
            }.onFailure {
                componentFetchInfo = "Failed to load components: ${it.message}"
            }
            hasLoadedComponents = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(VioSpacing.lg.dp)) {
        Text("🎬 Live Show Experience", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        Text(
            "Interactive live streaming with shopping integration. Test all layouts and observe the global overlay system.",
            style = VioTypography.body.toComposeTextStyle(),
            color = colors.textSecondary,
        )

        HorizontalDivider()

        Text("📺 Available Live Streams", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        if (activeStreams.isEmpty()) {
            Text("No active streams", style = VioTypography.body.toComposeTextStyle(), color = colors.textSecondary)
        } else {
            activeStreams.forEach { stream ->
                LiveStreamSelectableCard(
                    stream = stream,
                    isSelected = stream.id == selectedStream?.id,
                    colors = colors,
                    onClick = { selectedStream = stream },
                )
            }
        }
        componentFetchInfo?.let {
            Text(it, style = VioTypography.caption1.toComposeTextStyle(), color = colors.textSecondary)
        }

        Text("📱 Layout Options", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp)) {
            LiveStreamLayout.values().forEach { layout ->
                LayoutOptionCard(
                    layout = layout,
                    isSelected = layout == selectedLayout,
                    colors = colors,
                    onClick = { selectedLayout = layout },
                )
            }
        }

        Text("🎛️ Controls", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(VioSpacing.sm.dp)) {
            Button(
                onClick = { selectedStream?.let { manager.showLiveStream(it, selectedLayout) } },
                enabled = selectedStream != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show Live Stream", modifier = Modifier.padding(vertical = VioSpacing.xs.dp))
            }
            OutlinedButton(
                onClick = {
                    if (isLiveVisible) {
                        manager.showMiniPlayer()
                    } else {
                        selectedStream?.let {
                            manager.showLiveStream(it, selectedLayout)
                            manager.showMiniPlayer()
                        }
                    }
                },
                enabled = selectedStream != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show Mini Player", modifier = Modifier.padding(vertical = VioSpacing.xs.dp))
            }
            OutlinedButton(
                onClick = { manager.hideLiveStream() },
                enabled = isLiveVisible || isMiniVisible,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            ) {
                Text("Hide Stream", modifier = Modifier.padding(vertical = VioSpacing.xs.dp))
            }
        }

        Text("📊 Current Status", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp)) {
            LiveShowStatusRow("Live Stream Visible", if (isLiveVisible) "Yes" else "No", colors)
            LiveShowStatusRow("Mini Player Visible", if (isMiniVisible) "Yes" else "No", colors)
            LiveShowStatusRow("Indicator Visible", if (isIndicatorVisible) "Yes" else "No", colors)
            LiveShowStatusRow("Active Streams", activeStreams.size.toString(), colors)
            LiveShowStatusRow("Current Viewers", viewerCount.toString(), colors)
            LiveShowStatusRow("Layout", selectedLayout.displayName(), colors)
            LiveShowStatusRow("Connection", connectionStatus, colors)
            currentStream?.let { stream ->
                LiveShowStatusRow("Current Stream", stream.title, colors)
            }
        }

        Text("🧪 Demo Actions", style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp)) {
            OutlinedButton(
                onClick = { manager.simulateNewChatMessage() },
                enabled = currentStream != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Simulate Chat Message") }
            OutlinedButton(
                onClick = { manager.toggleIndicator() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isIndicatorVisible) "Hide Indicator" else "Show Indicator")
            }
            if (selectedStream?.featuredProducts?.isNotEmpty() == true) {
                Button(
                    onClick = {
                        selectedStream?.featuredProducts?.randomOrNull()?.let {
                            manager.addProductToCart(it, cartManager)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add Random Product") }
            }
        }

        selectedStream?.let { stream ->
            LiveStreamActionCard(
                stream = stream,
                colors = colors,
                onTap = { manager.showLiveStream(stream, LiveStreamLayout.FULL_SCREEN_OVERLAY) },
            )
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Home") }
    }
}

@Composable
private fun LiveStreamSelectableCard(
    stream: LiveStream,
    isSelected: Boolean,
    colors: AdaptiveVioColorsCompose,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) VioColors.primary.toColor() else colors.border),
        shape = RoundedCornerShape(VioBorderRadius.large.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VioSpacing.xs.dp)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(VioSpacing.md.dp),
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = stream.thumbnailUrl,
                contentDescription = stream.title,
                modifier = Modifier
                    .size(width = 80.dp, height = 60.dp)
                    .clip(RoundedCornerShape(VioBorderRadius.medium.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stream.title, style = VioTypography.body.toComposeTextStyle(), color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AsyncImage(
                        model = stream.streamer.avatarUrl,
                        contentDescription = stream.streamer.name,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape),
                    )
                    Text(stream.streamer.name, style = VioTypography.caption1.toComposeTextStyle(), color = colors.textSecondary)
                    if (stream.streamer.isVerified) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = VioColors.primary.toColor(), modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    "${stream.viewerCount} viewers • ${stream.featuredProducts.size} products",
                    style = VioTypography.caption2.toComposeTextStyle(),
                    color = colors.textTertiary,
                )
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = VioColors.primary.toColor())
            }
        }
    }
}

@Composable
private fun LayoutOptionCard(
    layout: LiveStreamLayout,
    isSelected: Boolean,
    colors: AdaptiveVioColorsCompose,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) colors.surface else colors.surfaceSecondary),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) VioColors.primary.toColor() else colors.borderSecondary),
        shape = RoundedCornerShape(VioBorderRadius.large.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(VioSpacing.md.dp),
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (isSelected) VioColors.primary.toColor() else colors.primary,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface, CircleShape)
                    .padding(6.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(layout.displayName(), style = VioTypography.body.toComposeTextStyle(), color = colors.textPrimary)
                Text(layout.description(), style = VioTypography.caption1.toComposeTextStyle(), color = colors.textSecondary)
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = VioColors.primary.toColor())
            }
        }
    }
}

@Composable
private fun LiveShowStatusRow(label: String, value: String, colors: AdaptiveVioColorsCompose) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceSecondary, RoundedCornerShape(VioBorderRadius.medium.dp))
            .padding(horizontal = VioSpacing.md.dp, vertical = VioSpacing.sm.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = VioTypography.caption1.toComposeTextStyle(), color = colors.textSecondary)
        Text(value, style = VioTypography.body.toComposeTextStyle(), color = colors.textPrimary, textAlign = TextAlign.End)
    }
}

@Composable
private fun LiveStreamActionCard(
    stream: LiveStream,
    colors: AdaptiveVioColorsCompose,
    onTap: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(VioBorderRadius.medium.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
    ) {
        Row(
            modifier = Modifier.padding(VioSpacing.md.dp),
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = stream.thumbnailUrl,
                contentDescription = stream.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(VioBorderRadius.medium.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stream.title, style = VioTypography.headline.toComposeTextStyle(), color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("by ${stream.streamer.name}", style = VioTypography.body.toComposeTextStyle(), color = colors.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (stream.isLive) Color.Red else colors.border),
                    )
                    Text(
                        if (stream.isLive) "LIVE" else "OFFLINE",
                        style = VioTypography.caption1.toComposeTextStyle(),
                        color = if (stream.isLive) Color.Red else colors.textSecondary,
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
        }
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
                .fillMaxSize()
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.clickable { onTap(stream) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = VioSpacing.md.dp, vertical = VioSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VioSpacing.xs.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
            )
            Text("LIVE", color = Color.White, style = VioTypography.caption1.toComposeTextStyle())
            Text(stream.streamer.name, color = Color.White, style = VioTypography.caption2.toComposeTextStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun LiveStreamLayout.displayName(): String = when (this) {
    LiveStreamLayout.FULL_SCREEN_OVERLAY -> "Full Screen Overlay"
    LiveStreamLayout.BOTTOM_SHEET -> "Bottom Sheet"
    LiveStreamLayout.MODAL -> "Modal"
}

private fun LiveStreamLayout.description(): String = when (this) {
    LiveStreamLayout.FULL_SCREEN_OVERLAY -> "Immersive TikTok-style experience"
    LiveStreamLayout.BOTTOM_SHEET -> "Bottom drawer with preview"
    LiveStreamLayout.MODAL -> "Classic modal player with tabs"
}
