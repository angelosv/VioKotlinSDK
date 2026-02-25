package io.reachu.VioCore.utils

import io.reachu.VioCore.configuration.VioConfiguration

/**
 * Utility for resolving asset URLs (banners, logos, etc.) from the campaign API.
 */
object UrlUtils {
    /**
     * Resolves a potentially relative asset URL using the configured campaign API base URL.
     * 
     * @param path The asset path (can be absolute or relative)
     * @return The absolute URL, or null if path is null/blank
     */
    fun resolveAssetUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        
        val base = VioConfiguration.shared.state.value.campaign.restAPIBaseURL.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$base$normalizedPath"
    }
}
