package com.reachu.tv2demo.utils

import android.util.Log
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioCore.managers.CampaignNotification
import io.reachu.VioCore.utils.VioLogger
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Utilidad de alto nivel para escuchar eventos del SDK y
 * coordinar la limpieza/pre-carga del cache de imágenes del demo.
 */
object CacheHelper {

    private const val TAG = "CacheHelper"

    // Evita registrar listeners más de una vez.
    private var listenersSetup: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Configura los listeners de limpieza de cache.
     * Debe llamarse una sola vez, idealmente al inicializar la app.
     */
    fun setupCacheClearingListener() {
        if (listenersSetup) {
            Log.d(TAG, "Listeners already setup, skipping")
            VioLogger.debug("Listeners already setup, skipping", TAG)
            return
        }
        listenersSetup = true

        // Listener para eventos de logo de campaña del SDK.
        scope.launch {
            CampaignManager.shared.events.collect { event ->
                when (event) {
                    is CampaignNotification.CampaignLogoChanged -> handleCampaignLogoChanged(event)
                    // En el futuro se puede manejar aquí un evento global "VioCacheCleared"
                    else -> Unit
                }
            }
        }
    }

    private suspend fun handleCampaignLogoChanged(event: CampaignNotification.CampaignLogoChanged) {
        val oldLogoUrl = event.oldLogoUrl
        val newLogoUrl = event.newLogoUrl

        // Limpieza específica de logo anterior
        if (!oldLogoUrl.isNullOrBlank() && isValidHttpUrl(oldLogoUrl)) {
            Log.d(TAG, "Clearing cache for old logo: $oldLogoUrl")
            VioLogger.debug("Cleared cache for logo: $oldLogoUrl", "ImageLoader")
            CachedImageLoader.clearCacheForUrl(oldLogoUrl)
        }

        // Pre-carga del nuevo logo con timeout
        if (!newLogoUrl.isNullOrBlank() && isValidHttpUrl(newLogoUrl)) {
            Log.d(TAG, "Preloading new logo with timeout: $newLogoUrl")
            VioLogger.info("Preloading new logo with timeout: $newLogoUrl", "ImageLoader")
            preloadLogoWithTimeout(newLogoUrl, timeoutSeconds = 10)
        }
    }

    private fun isValidHttpUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme?.lowercase()
            scheme == "http" || scheme == "https"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pre-carga una imagen con un timeout dado. Si expira, solo se loguea un warning.
     */
    private fun preloadLogoWithTimeout(url: String, timeoutSeconds: Int) {
        scope.launch {
            try {
                withTimeout(timeoutSeconds * 1000L) {
                    CachedImageLoader.preload(url)
                }
            } catch (t: TimeoutCancellationException) {
                Log.w(TAG, "Timeout while preloading logo: $url")
                VioLogger.warning("Timeout while preloading logo: $url", "ImageLoader")
            } catch (t: Throwable) {
                Log.w(TAG, "Error preloading logo: $url - ${t.message}")
                VioLogger.error("Error preloading logo: $url - ${t.message}", "ImageLoader")
            }
        }
    }
}

/**
 * Adaptador mínimo de cache para el demo.
 * Mantiene un registro en memoria de URLs \"ya vistas\" y ofrece
 * operaciones para limpiar/pre-cargar.
 *
 * Nota: esto no reemplaza el cache de Coil, pero permite a la UI
 * decidir si mostrar placeholders de carga.
 */
object CachedImageLoader {

    private val cachedUrls: MutableSet<String> = mutableSetOf()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isCached(url: String?): Boolean =
        url != null && cachedUrls.contains(url)

    fun markCached(url: String?) {
        if (!url.isNullOrBlank()) {
            cachedUrls.add(url)
        }
    }

    fun clearCacheForUrl(url: String) {
        cachedUrls.remove(url)
        // En un escenario real, aquí podría integrarse con el cache de Coil (disk/memory).
        VioLogger.debug("Cleared in-memory cache for URL: $url", "ImageLoader")
    }

    /**
     * Pre-carga \"a mano\" una URL para calentar caches HTTP.
     * En este demo usamos una simple petición HTTP.
     */
    fun preload(url: String) {
        scope.launch {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val status = connection.responseCode
                if (status in 200..299) {
                    connection.inputStream.use { it.readBytes() }
                    markCached(url)
                    Log.d("CachedImageLoader", "Preloaded logo into HTTP cache: $url")
                    VioLogger.debug("Preloaded logo into HTTP cache: $url", "ImageLoader")
                } else {
                    Log.w("CachedImageLoader", "Preload failed, status=$status for $url")
                    VioLogger.warning("Preload failed, status=$status for $url", "ImageLoader")
                }
                connection.disconnect()
            } catch (t: Throwable) {
                Log.w("CachedImageLoader", "Error preloading $url: ${t.message}")
                VioLogger.error("Error preloading $url: ${t.message}", "ImageLoader")
            }
        }
    }
}

