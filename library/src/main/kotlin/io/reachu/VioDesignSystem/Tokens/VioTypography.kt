package io.reachu.VioDesignSystem.Tokens

import io.reachu.VioCore.configuration.VioConfiguration
import io.reachu.VioCore.configuration.TypographyToken

/**
 * Typography scale placeholders; map to actual font resources in the UI module.
 */
object VioTypography {
    private val scheme get() = VioConfiguration.shared.state.value.theme.typography

    val largeTitle: TypographyToken get() = TypographyToken(scheme.headline * 1.5f, scheme.headline * 1.8f, "bold")
    val title1: TypographyToken get() = TypographyToken(scheme.headline * 1.3f, scheme.headline * 1.6f, "bold")
    val title2: TypographyToken get() = TypographyToken(scheme.headline * 1.2f, scheme.headline * 1.5f, "semibold")
    val title3: TypographyToken get() = TypographyToken(scheme.headline * 1.1f, scheme.headline * 1.4f, "semibold")
    val headline: TypographyToken get() = TypographyToken(scheme.headline, scheme.headline * 1.2f, "semibold")
    val body: TypographyToken get() = TypographyToken(scheme.body, scheme.body * 1.4f, "normal")
    val bodyBold: TypographyToken get() = TypographyToken(scheme.body, scheme.body * 1.4f, "bold")
    val callout: TypographyToken get() = TypographyToken(scheme.callout, scheme.callout * 1.3f, "normal")
    val caption1: TypographyToken get() = TypographyToken(scheme.caption, scheme.caption * 1.2f, "normal")
    val caption2: TypographyToken get() = TypographyToken(scheme.caption * 0.9f, scheme.caption * 1.1f, "normal")
    val footnote: TypographyToken get() = TypographyToken(scheme.caption * 0.8f, scheme.caption * 1.0f, "normal")

    val headlineSize: Float get() = scheme.headline
    val bodySize: Float get() = scheme.body
    val captionSize: Float get() = scheme.caption
    val footnoteSize: Float get() = scheme.caption * 0.8f
}
