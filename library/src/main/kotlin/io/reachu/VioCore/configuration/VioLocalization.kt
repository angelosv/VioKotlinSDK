package io.reachu.VioCore.configuration

object VioLocalization {
    private var configuration: LocalizationConfiguration = LocalizationConfiguration.default()
    private var currentLanguage: String = configuration.defaultLanguage

    fun configure(config: LocalizationConfiguration) {
        configuration = config
        currentLanguage = config.defaultLanguage
    }

    fun setLanguage(language: String) {
        currentLanguage = language
    }

    val language: String
        get() = currentLanguage

    fun string(key: String, language: String? = null, defaultValue: String? = null): String {
        val translation = configuration.translation(key, language ?: currentLanguage)
            ?: VioTranslationKey.defaultEnglish[key]
            ?: defaultValue
        return translation ?: key
    }

    fun string(key: String, vararg args: Any, language: String? = null, defaultValue: String? = null): String {
        val format = string(key, language, defaultValue)
        return String.format(format, *args)
    }
}

fun RLocalizedString(key: String, defaultValue: String? = null): String =
    VioLocalization.string(key, defaultValue = defaultValue)

fun RLocalizedString(key: String, vararg args: Any, defaultValue: String? = null): String =
    VioLocalization.string(key, *args, defaultValue = defaultValue)
