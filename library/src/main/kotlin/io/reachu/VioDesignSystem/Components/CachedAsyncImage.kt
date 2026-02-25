package io.reachu.VioDesignSystem.Components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.SvgDecoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * AsyncImage with caching support to avoid loading indicators if the image is already cached.
 * Caches images in memory and disk.
 *
 * @param url The image URL to load
 * @param contentDescription Text used by accessibility services
 * @param modifier Modifier for this composable
 * @param contentScale Strategy used to determine how to scale the image
 * @param placeholder Optional placeholder to display while loading
 * @param error Optional error component to display if loading fails
 */
@Composable
fun CachedAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    val imageLoader = context.imageLoader.newBuilder()
        .components {
            add(SvgDecoder.Factory())
        }
        .build()

    coil.compose.SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            placeholder?.invoke()
        },
        error = {
            error?.invoke()
        }
    )
}
