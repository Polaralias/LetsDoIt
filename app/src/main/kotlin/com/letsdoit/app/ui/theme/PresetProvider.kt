package com.letsdoit.app.ui.theme

import androidx.compose.runtime.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class ThemePreset(val key: String, val label: String, val config: ThemeConfig)

@Singleton
class PresetProvider @Inject constructor() {
    private val presets = listOf(
        ThemePreset(
            key = "cloud_pastel",
            label = "Cloud Pastel",
            config = ThemeConfig(cardFamily = CardFamily.Cloud, paletteFamily = PaletteFamily.Pastel, accentPackId = "pastel_clouds")
        ),
        ThemePreset(
            key = "sharp_dark",
            label = "Sharp Dark",
            config = ThemeConfig(cardFamily = CardFamily.Sharp, paletteFamily = PaletteFamily.Dark, accentPackId = "midnight_sharp")
        ),
        ThemePreset(
            key = "rounded_soft",
            label = "Rounded Soft",
            config = ThemeConfig(cardFamily = CardFamily.Rounded, paletteFamily = PaletteFamily.Soft, accentPackId = "soft_round")
        ),
        ThemePreset(
            key = "square_moody",
            label = "Square Moody",
            config = ThemeConfig(cardFamily = CardFamily.Square, paletteFamily = PaletteFamily.Moody, accentPackId = "moody_square")
        ),
        ThemePreset(
            key = "cloud_dark_minimal",
            label = "Cloud Dark Minimal",
            config = ThemeConfig(cardFamily = CardFamily.Cloud, paletteFamily = PaletteFamily.Dark, accentPackId = "dark_minimal", dynamicColour = true)
        ),
        ThemePreset(
            key = "rounded_pastel_nature",
            label = "Rounded Pastel Nature",
            config = ThemeConfig(cardFamily = CardFamily.Rounded, paletteFamily = PaletteFamily.Pastel, accentPackId = "nature_pastel")
        )
    )

    fun presets(): List<ThemePreset> = presets

    fun find(key: String?): ThemePreset? = presets.firstOrNull { it.key == key }
}
