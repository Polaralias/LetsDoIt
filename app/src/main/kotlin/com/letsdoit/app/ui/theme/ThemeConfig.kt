package com.letsdoit.app.ui.theme

import androidx.compose.runtime.Immutable

enum class CardFamily { Cloud, Square, Sharp, Rounded }

enum class PaletteFamily { Dark, Moody, Pastel, Soft }

@Immutable
data class ThemeConfig(
    val cardFamily: CardFamily = CardFamily.Rounded,
    val paletteFamily: PaletteFamily = PaletteFamily.Soft,
    val accentPackId: String? = null,
    val dynamicColour: Boolean = false,
    val highContrast: Boolean = false
) {
    companion object {
        val Default = ThemeConfig()
    }
}
