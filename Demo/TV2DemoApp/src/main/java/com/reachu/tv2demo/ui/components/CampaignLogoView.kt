package com.reachu.tv2demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import com.reachu.tv2demo.utils.CachedImageLoader
import io.reachu.VioCore.managers.CampaignManager

/**
 * Vista reutilizable para mostrar el logo de campaña usando el sistema de cache del demo.
 *
 * Versión que se conecta directamente al CampaignManager y observa el logo actual.
 */
@Composable
fun CampaignLogoView(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val campaignManager = CampaignManager.shared
    val campaign by campaignManager.currentCampaign.collectAsState()
    CampaignLogoView(
        logoUrl = campaign?.campaignLogo,
        modifier = modifier,
        contentDescription = contentDescription,
    )
}

/**
 * Implementación base que recibe explícitamente la URL del logo.
 */
@Composable
fun CampaignLogoView(
    logoUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (logoUrl.isNullOrBlank()) {
        // Fallback simple cuando no hay logo configurado.
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val cached = CachedImageLoader.isCached(logoUrl)

    SubcomposeAsyncImage(
        model = logoUrl,
        contentDescription = contentDescription,
        modifier = modifier,
    ) {
        when (val state = painter.state) {
            is AsyncImagePainter.State.Loading -> {
                if (cached) {
                    // Ya lo tenemos en \"cache\" lógico: no mostrar spinner.
                    SubcomposeAsyncImageContent()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            is AsyncImagePainter.State.Error -> {
                // Error: mostrar fallback.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = contentDescription,
                        tint = Color.Gray,
                    )
                }
            }
            else -> {
                // Éxito (o estado intermedio no-error): marcar como cacheado y renderizar contenido real.
                LaunchedEffect(logoUrl) {
                    CachedImageLoader.markCached(logoUrl)
                }
                SubcomposeAsyncImageContent()
            }
        }
    }
}
