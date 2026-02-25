package io.reachu.VioUI.Components.compose.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.concurrent.atomic.AtomicReference

fun interface VioImageLoader {
    @Composable
    fun Render(url: String?, contentDescription: String?, modifier: Modifier)
}

object VioPlaceholderImageLoader : VioImageLoader {
    @Composable
    override fun Render(url: String?, contentDescription: String?, modifier: Modifier) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

object VioImageLoaderDefaults {
    private val loaderRef = AtomicReference<VioImageLoader>(VioPlaceholderImageLoader)

    var current: VioImageLoader
        get() = loaderRef.get()
        set(value) {
            loaderRef.set(value)
        }

    fun install(loader: VioImageLoader) {
        loaderRef.set(loader)
        val name = loader::class.qualifiedName ?: "lambda"
        println("🖼️ [ImageLoaderDefaults] Installed loader=$name")
    }

    fun reset() {
        loaderRef.set(VioPlaceholderImageLoader)
        println("🖼️ [ImageLoaderDefaults] Reset to placeholder loader")
    }
}

@Composable
fun VioImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
) {
    val loaderLabel = when {
        imageLoader === VioPlaceholderImageLoader -> "placeholder"
        else -> imageLoader::class.qualifiedName ?: "lambda"
    }
    println("🖼️ [VioImage] Rendering url=${url ?: "null"} loader=$loaderLabel")
    imageLoader.Render(url, contentDescription, modifier)
}
