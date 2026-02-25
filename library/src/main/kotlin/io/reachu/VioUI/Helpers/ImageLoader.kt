package io.reachu.VioUI.Helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.reachu.VioUI.Components.compose.product.VioImage
import io.reachu.VioUI.Components.compose.product.VioImageLoader
import io.reachu.VioUI.Components.compose.product.VioImageLoaderDefaults
import io.reachu.VioUI.Components.compose.product.VioPlaceholderImageLoader

typealias VioImageLoader = io.reachu.VioUI.Components.compose.product.VioImageLoader

@Suppress("UNUSED_PARAMETER")
@Composable
fun LoadedImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageLoader: VioImageLoader = VioImageLoaderDefaults.current,
    loadingContent: @Composable (() -> Unit)? = null,
    errorContent: @Composable (() -> Unit)? = null,
) {
    val loader = if (imageLoader === VioPlaceholderImageLoader) {
        VioImageLoaderDefaults.current
    } else {
        imageLoader
    }
    VioImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        imageLoader = loader,
    )
}
