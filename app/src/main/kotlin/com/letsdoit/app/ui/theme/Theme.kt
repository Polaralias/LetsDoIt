package com.letsdoit.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class CardShapeFamily {
    Rounded,
    Cut,
    Pill
}

enum class PaletteFamily {
    Calm,
    Vibrant,
    Night
}

enum class AccentPack {
    Fresh,
    Citrus,
    Blossom
}

@Immutable
data class ThemeConfig(
    val paletteFamily: PaletteFamily,
    val accentPack: AccentPack,
    val cardShapeFamily: CardShapeFamily
) {
    companion object {
        val Default = ThemeConfig(PaletteFamily.Calm, AccentPack.Fresh, CardShapeFamily.Rounded)
    }
}

@Immutable
data class ThemePreset(val name: String, val config: ThemeConfig)

val ThemePresets = listOf(
    ThemePreset("Fresh morning", ThemeConfig(PaletteFamily.Calm, AccentPack.Fresh, CardShapeFamily.Rounded)),
    ThemePreset("Citrus punch", ThemeConfig(PaletteFamily.Vibrant, AccentPack.Citrus, CardShapeFamily.Cut)),
    ThemePreset("Blossom dusk", ThemeConfig(PaletteFamily.Night, AccentPack.Blossom, CardShapeFamily.Pill))
)

val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig.Default }

@Composable
fun AppTheme(config: ThemeConfig = ThemeConfig.Default, content: @Composable () -> Unit) {
    val colors = rememberColorScheme(config)
    val shapes = rememberShapes(config.cardShapeFamily)
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = Typography(),
        content = content
    )
}

@Composable
private fun rememberColorScheme(config: ThemeConfig): ColorScheme {
    val palette = when (config.paletteFamily) {
        PaletteFamily.Calm -> PaletteColors(
            background = Color(0xFFF1FAEE),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D3557)
        )
        PaletteFamily.Vibrant -> PaletteColors(
            background = Color(0xFFFFFBEB),
            surface = Color(0xFFFFF3C4),
            onSurface = Color(0xFF3F1D38)
        )
        PaletteFamily.Night -> PaletteColors(
            background = Color(0xFF101820),
            surface = Color(0xFF15232F),
            onSurface = Color(0xFFECEFF4)
        )
    }
    val accent = when (config.accentPack) {
        AccentPack.Fresh -> AccentColors(Color(0xFF0FA3B1), Color(0xFF2E5077))
        AccentPack.Citrus -> AccentColors(Color(0xFFF4A259), Color(0xFFD1495B))
        AccentPack.Blossom -> AccentColors(Color(0xFFD26AC2), Color(0xFF6B2C91))
    }
    return if (config.paletteFamily == PaletteFamily.Night) {
        darkColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            secondary = accent.secondary,
            onSecondary = Color.White,
            background = palette.background,
            surface = palette.surface,
            onSurface = palette.onSurface,
            onBackground = palette.onSurface,
            tertiary = accent.secondary
        )
    } else {
        lightColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            secondary = accent.secondary,
            onSecondary = Color.White,
            background = palette.background,
            surface = palette.surface,
            onSurface = palette.onSurface,
            onBackground = palette.onSurface,
            tertiary = accent.secondary
        )
    }
}

private fun rememberShapes(cardShapeFamily: CardShapeFamily): Shapes {
    val shape: CornerBasedShape = when (cardShapeFamily) {
        CardShapeFamily.Rounded -> RoundedCornerShape(16.dp)
        CardShapeFamily.Cut -> CutCornerShape(12.dp)
        CardShapeFamily.Pill -> RoundedCornerShape(28.dp)
    }
    return Shapes(
        extraSmall = shape,
        small = shape,
        medium = shape,
        large = shape,
        extraLarge = shape
    )
}

@Immutable
private data class PaletteColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color
)

@Immutable
private data class AccentColors(
    val primary: Color,
    val secondary: Color
)
