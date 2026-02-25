package io.reachu.VioCore.configuration

/**
 * Mirrors the Swift `LocalizationConfiguration` structure.
 * Holds translations per language and provides consistent fallback logic.
 */
data class LocalizationConfiguration(
    val defaultLanguage: String = "en",
    val translations: Map<String, Map<String, String>> = mapOf("en" to VioTranslationKey.defaultEnglish),
    val fallbackLanguage: String = "en",
) {
    companion object {
        fun default() = LocalizationConfiguration()
    }

    /**
     * Returns the translation for the given key using the same priority order
     * as the Swift implementation:
     * 1. Requested language (or defaultLanguage if none provided)
     * 2. Default language
     * 3. fallbackLanguage
     * 4. Built-in English translations (VioTranslationKey.defaultEnglish)
     */
    fun translation(key: String, language: String? = null): String? {
        val langToUse = language ?: defaultLanguage
        translations[langToUse]?.get(key)?.let { return it }

        if (langToUse != defaultLanguage) {
            translations[defaultLanguage]?.get(key)?.let { return it }
        }

        if (fallbackLanguage != defaultLanguage) {
            translations[fallbackLanguage]?.get(key)?.let { return it }
        }

        return VioTranslationKey.defaultEnglish[key]
    }
}
