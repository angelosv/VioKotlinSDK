package live.vio.VioUI.Helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import live.vio.VioUI.Components.compose.product.VioImage
import live.vio.VioUI.Components.compose.product.VioImageLoader
import live.vio.VioUI.Components.compose.product.VioImageLoaderDefaults
import live.vio.VioUI.Components.compose.product.VioPlaceholderImageLoader

typealias VioImageLoader = live.vio.VioUI.Components.compose.product.VioImageLoader

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
