package com.letsdoit.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class ThemePreset(val key: String, @StringRes val label: Int, val config: ThemeConfig)

@Singleton
class PresetProvider @Inject constructor() {
    private val presets = listOf(
        ThemePreset(
            key = "cloud_pastel",
            label = com.letsdoit.app.R.string.theme_preset_cloud_pastel,
            config = ThemeConfig(cardFamily = CardFamily.Cloud, paletteFamily = PaletteFamily.Pastel, accentPackId = "pastel_clouds")
        ),
        ThemePreset(
            key = "sharp_dark",
            label = com.letsdoit.app.R.string.theme_preset_sharp_dark,
            config = ThemeConfig(cardFamily = CardFamily.Sharp, paletteFamily = PaletteFamily.Dark, accentPackId = "midnight_sharp")
        ),
        ThemePreset(
            key = "rounded_soft",
            label = com.letsdoit.app.R.string.theme_preset_rounded_soft,
            config = ThemeConfig(cardFamily = CardFamily.Rounded, paletteFamily = PaletteFamily.Soft, accentPackId = "soft_round")
        ),
        ThemePreset(
            key = "square_moody",
            label = com.letsdoit.app.R.string.theme_preset_square_moody,
            config = ThemeConfig(cardFamily = CardFamily.Square, paletteFamily = PaletteFamily.Moody, accentPackId = "moody_square")
        ),
        ThemePreset(
            key = "cloud_dark_minimal",
            label = com.letsdoit.app.R.string.theme_preset_cloud_dark_minimal,
            config = ThemeConfig(cardFamily = CardFamily.Cloud, paletteFamily = PaletteFamily.Dark, accentPackId = "dark_minimal", dynamicColour = true)
        ),
        ThemePreset(
            key = "rounded_pastel_nature",
            label = com.letsdoit.app.R.string.theme_preset_rounded_pastel_nature,
            config = ThemeConfig(cardFamily = CardFamily.Rounded, paletteFamily = PaletteFamily.Pastel, accentPackId = "nature_pastel")
        )
    )

    fun presets(): List<ThemePreset> = presets

    fun find(key: String?): ThemePreset? = presets.firstOrNull { it.key == key }
}
