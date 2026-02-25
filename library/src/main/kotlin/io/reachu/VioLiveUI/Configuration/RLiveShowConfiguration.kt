package io.reachu.liveui.configuration

/**
 * Kotlin replica of `VioLiveShowConfiguration`.
 * Instead of SwiftUI `Color` / `CGFloat` we keep primitive values so UI layers
 * can interpret them as needed.
 */
data class VioLiveShowConfiguration(
    val layout: Layout = Layout.default,
    val colors: Colors = Colors.default,
    val typography: Typography = Typography.default,
    val spacing: Spacing = Spacing.default,
) {

    data class Layout(
        val showCloseButton: Boolean = true,
        val showLiveBadge: Boolean = true,
        val showControls: Boolean = true,
        val showChat: Boolean = true,
        val showProducts: Boolean = true,
        val showLikes: Boolean = true,
    ) {
        companion object {
            val default = Layout()
            val minimal = Layout(
                showCloseButton = true,
                showLiveBadge = true,
                showControls = false,
                showChat = false,
                showProducts = false,
                showLikes = false,
            )
        }
    }

    data class Colors(
        val liveBadgeColor: String = "#FF3B30",
        val controlsBackground: String = "#66000000",
        val controlsStroke: String = "#33FFFFFF",
        val controlsTint: String = "#FFFFFFFF",
        val chatBackground: String = "#B3000000",
        val productsBackground: String = "#CC000000",
        val overlayBackground: String = "#00000000",
    ) {
        companion object {
            val default = Colors()
        }
    }

    data class Typography(
        val streamTitleSize: Float = 16f,
        val streamSubtitleSize: Float = 12f,
        val chatMessageSize: Float = 14f,
        val productTitleSize: Float = 14f,
        val productPriceSize: Float = 16f,
    ) {
        companion object {
            val default = Typography()
            val compact = Typography(
                streamTitleSize = 14f,
                streamSubtitleSize = 10f,
                chatMessageSize = 12f,
                productTitleSize = 12f,
                productPriceSize = 14f,
            )
        }
    }

    data class Spacing(
        val controlsSpacing: Float = 16f,
        val contentPadding: Float = 16f,
        val productSpacing: Float = 12f,
        val chatPadding: Float = 16f,
    ) {
        companion object {
            val default = Spacing()
            val compact = Spacing(
                controlsSpacing = 12f,
                contentPadding = 12f,
                productSpacing = 8f,
                chatPadding = 12f,
            )
        }
    }

    companion object {
        val default = VioLiveShowConfiguration()
        val minimal = VioLiveShowConfiguration(
            layout = Layout.minimal,
            typography = Typography.compact,
            spacing = Spacing.compact,
        )
    }
}
