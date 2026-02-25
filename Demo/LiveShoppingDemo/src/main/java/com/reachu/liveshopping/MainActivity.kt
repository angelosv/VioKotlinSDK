package com.reachu.liveshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.reachu.VioCore.configuration.ConfigurationLoader
import io.reachu.VioUI.Components.compose.theme.VioTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
// VideoPlayer moved inline to resolve NCDFE
import java.io.File
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.reachu.VioUI.Managers.CartManager
import io.reachu.VioUI.Managers.Product
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.adapters.VioCoilImageLoader

class MainActivity : ComponentActivity() {
    
    private lateinit var cartManager: CartManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Vio SDK
        try {
            copyConfigToFilesDir("vio-config.json")
            ConfigurationLoader.loadConfiguration("vio-config", filesDir.absolutePath + "/")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cartManager = CartManager()
        VioImageLoaderDefaults.install(VioCoilImageLoader)

        setContent {
            VioTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LiveShoppingScreen(cartManager = cartManager)
                }
            }
        }
    }

    private fun copyConfigToFilesDir(fileName: String) {
        val output = File(filesDir, fileName)
        if (output.exists()) return
        try {
            assets.open(fileName).use { input ->
                output.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun LiveShoppingScreen(cartManager: CartManager, modifier: Modifier = Modifier) {
    // Public test HLS stream (Big Buck Bunny)
    val testVideoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    
    // Mock Data for Carousel
    val mockProducts = remember {
        listOf(
            createMockProduct(101, "Camera Lens 50mm", "BrandA", "Great lens", 100f, "https://dummyimage.com/600x400/000/fff&text=Lens"),
            createMockProduct(102, "Tripod Pro", "BrandB", "Sturdy tripod", 150f, "https://dummyimage.com/600x400/000/fff&text=Tripod"),
            createMockProduct(103, "Ring Light", "BrandC", "Bright light", 50f, "https://dummyimage.com/600x400/000/fff&text=Light")
        )
    }

    // Store Controller to manually set products
    val storeController = remember {
        io.reachu.VioUI.Components.VioProductStore().apply {
            // Manually inject products since we are not using a real campaign ID
             setProducts(mockProducts)
        }
    }
    
    // Ensure products are set when composable enters
    LaunchedEffect(Unit) {
        storeController.setProducts(mockProducts)
    }

    Box(modifier = modifier.fillMaxSize()) {
        VideoPlayer(
            videoUrl = testVideoUrl,
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp) // Overlay full screen
        ) {
             // Product Store Overlay
            // We use a Box with weight or align to position it.
            // For this demo, let's put it in the bottom half or as an overlay.
            // VioProductStore handles its own scrolling.
            
            io.reachu.VioUI.Components.compose.product.VioProductStore(
                 cartManager = cartManager,
                 controller = storeController,
                 modifier = Modifier
                     .align(Alignment.BottomCenter)
                     .height(400.dp) // Fixed height for the store overlay
                     .padding(bottom = 80.dp), // Space above nav bar if any
                 isCampaignGated = false, // Show immediately
                 isScrollEnabled = true,
                 imageBackgroundColor = androidx.compose.ui.graphics.Color.White,
                 showSponsor = true,
                 sponsorPosition = "top_right", 
             )

             // Floating Cart Indicator
             io.reachu.VioUI.Components.compose.cart.VioFloatingCartIndicator(
                 cartManager = cartManager,
                 modifier = Modifier
                     .align(Alignment.TopEnd)
                     .padding(16.dp),
                 onTap = {
                     // TODO: Open checkout
                 }
             )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = isPlaying
            repeatMode = ExoPlayer.REPEAT_MODE_ONE // Loop for "live" feel if file ends
        }
    }

    LaunchedEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide controls for a "Live" look
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill screen
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun createMockProduct(id: Int, title: String, brand: String, desc: String, price: Float, imageUrl: String): io.reachu.VioUI.Managers.Product {
    val priceObj = io.reachu.VioUI.Managers.Price(
        amount = price,
        currencyCode = "USD",
        amountInclTaxes = price,
        taxAmount = 0f,
        taxRate = 0f,
        compareAt = null,
        compareAtInclTaxes = null
    )
    
    val imgObj = io.reachu.VioUI.Managers.ProductImage(
        id = "img_$id",
        url = imageUrl,
        width = 600,
        height = 400,
        order = 0
    )

    return io.reachu.VioUI.Managers.Product(
        id = id,
        title = title,
        brand = brand,
        description = desc,
        tags = null,
        sku = "SKU_$id",
        quantity = 100,
        price = priceObj,
        variants = emptyList(),
        barcode = null,
        options = null,
        categories = null,
        images = listOf(imgObj),
        productShipping = null,
        supplier = "Demo Supplier",
        supplierId = 1,
        importedProduct = false,
        referralFee = 0,
        optionsEnabled = false,
        digital = false,
        origin = "US",
        returnInfo = null
    )
}
