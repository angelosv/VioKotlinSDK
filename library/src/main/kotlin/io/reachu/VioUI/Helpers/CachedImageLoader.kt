package io.reachu.VioUI.Helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Sistema de cache para logos de campaña con invalidación específica.
 * 
 * Implementa cache en memoria (LruCache) y disco para evitar recargas innecesarias,
 * permitiendo invalidar logos específicos cuando cambian sin limpiar todo el cache.
 */
class CachedImageLoader private constructor(context: Context) {
    
    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val CACHE_DIR_NAME = "campaignLogos"
        
        @Volatile
        private var INSTANCE: CachedImageLoader? = null
        
        /**
         * Obtiene la instancia singleton del CachedImageLoader.
         * Requiere un Context de Android para acceder al directorio de cache.
         */
        fun getInstance(context: Context): CachedImageLoader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CachedImageLoader(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Cache en memoria usando LruCache (similar a NSCache en Swift)
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }
    
    // Directorio de cache en disco
    private val diskCacheDir: File = File(context.cacheDir, CACHE_DIR_NAME).apply {
        if (!exists()) {
            mkdirs()
        }
    }
    
    /**
     * Limpia el cache de una URL específica.
     * 
     * @param url URL del logo a invalidar. Si es null o vacía, no hace nada.
     */
    suspend fun clearCacheForUrl(url: String?) = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext
        
        val cacheKey = urlToCacheKey(url)
        
        // Limpiar de memoria
        synchronized(memoryCache) {
            memoryCache.remove(cacheKey)
        }
        
        // Eliminar archivo del disco si existe
        val cacheFile = File(diskCacheDir, cacheKey)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }
    
    /**
     * Limpia todo el cache (memoria y disco).
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        // Limpiar memoria
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
        
        // Eliminar directorio completo del disco
        if (diskCacheDir.exists()) {
            diskCacheDir.deleteRecursively()
            diskCacheDir.mkdirs()
        }
    }
    
    /**
     * Carga una imagen desde cache o red.
     * 
     * Estrategia:
     * 1. Verificar memoria primero
     * 2. Si no está, verificar disco
     * 3. Si no está, cargar de red y guardar en ambos
     * 
     * @param url URL de la imagen a cargar
     * @return Bitmap de la imagen o null si falla la carga
     */
    suspend fun loadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        
        val cacheKey = urlToCacheKey(url)
        
        // 1. Verificar memoria primero
        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
        }
        
        // 2. Verificar disco
        val cacheFile = File(diskCacheDir, cacheKey)
        if (cacheFile.exists()) {
            try {
                FileInputStream(cacheFile).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        // Guardar en memoria para acceso rápido
                        synchronized(memoryCache) {
                            memoryCache.put(cacheKey, bitmap)
                        }
                        return@withContext bitmap
                    }
                }
            } catch (e: Exception) {
                // Si falla la lectura del disco, continuar con carga de red
            }
        }
        
        // 3. Cargar de red y guardar en ambos
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        // Guardar en memoria
                        synchronized(memoryCache) {
                            memoryCache.put(cacheKey, bitmap)
                        }
                        
                        // Guardar en disco
                        try {
                            FileOutputStream(cacheFile).use { output ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                            }
                        } catch (e: Exception) {
                            // Si falla guardar en disco, no es crítico
                        }
                        
                        return@withContext bitmap
                    }
                }
            }
        } catch (e: Exception) {
            // Error al cargar de red
        }
        
        null
    }
    
    /**
     * Convierte una URL en una clave de cache válida para nombres de archivo.
     * Usa hash MD5 para evitar caracteres inválidos en nombres de archivo.
     */
    private fun urlToCacheKey(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(url.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
