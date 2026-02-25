package io.reachu.VioUI.adapters

import android.util.Log
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import io.reachu.VioCore.utils.VioLogger
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Components.compose.product.VioPlaceholderImageLoader


private const val TAG = "ImageLoader"

/**
 * Validates that a URL is safe to load (http/https only).
 * Prevents crashes from malformed URLs or attempts to load local/data URIs.
 */
private fun isValidImageUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return try {
        val uri = android.net.Uri.parse(url)
        val scheme = uri.scheme?.lowercase()
        scheme == "http" || scheme == "https"
    } catch (e: Exception) {
        Log.w(TAG, "Invalid URL format: $url", e)
        VioLogger.warning("Invalid URL format: $url", TAG)
        false
    }
}

/**
 * Default Coil-backed implementation of [VioImageLoader] so apps can plug
 * images into the shared Compose components without redefining the slot.
 */
val VioCoilImageLoader: VioImageLoader = VioImageLoader { url, contentDescription, modifier ->
    // Validate URL before attempting to load
    if (!isValidImageUrl(url)) {
        Log.w(TAG, "Skipping invalid image URL: $url")
        VioLogger.warning("Skipping invalid image URL: $url", TAG)
        // Return empty composable for invalid URLs
        return@VioImageLoader
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = coil.ImageLoader.Builder(context)
        .components {
            add(coil.decode.SvgDecoder.Factory())
        }
        .build()

    // Use AsyncImage for better scrolling performance than SubcomposeAsyncImage
    coil.compose.AsyncImage(
        model = coil.request.ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Fit, // Changed to Fit for logos (usually better for SVG/sponsors)
        onState = { state ->
            if (state is coil.compose.AsyncImagePainter.State.Error) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Error loading $url", state.result.throwable)
                    VioLogger.error("Failed to load image: $url - ${state.result.throwable?.message}", TAG)
                }
            } else if (state is coil.compose.AsyncImagePainter.State.Success) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Loaded $url")
                    VioLogger.debug("Loaded image: $url", TAG)
                }
            }
        }
    )
}

@Suppress("unused")
private val registerDefaultLoader = run {
    if (VioImageLoaderDefaults.current === VioPlaceholderImageLoader) {
        VioImageLoaderDefaults.install(VioCoilImageLoader)
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Auto-installed Coil loader as default")
            VioLogger.info("Auto-installed Coil loader as default", TAG)
        }
    }
}
