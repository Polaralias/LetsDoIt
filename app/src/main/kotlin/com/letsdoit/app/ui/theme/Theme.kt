package com.letsdoit.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig.Default }

@Composable
fun AppTheme(
    config: ThemeConfig = ThemeConfig.Default,
    fontScaleOverride: Float? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberColorScheme(config)
    val shapes = rememberShapes(config.cardFamily)
    CompositionLocalProvider(LocalThemeConfig provides config) {
        val baseDensity = LocalDensity.current
        if (fontScaleOverride != null) {
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScaleOverride)) {
                MaterialTheme(
                    colorScheme = colorScheme,
                    shapes = shapes,
                    typography = Typography(),
                    content = content
                )
            }
        } else {
            MaterialTheme(
                colorScheme = colorScheme,
                shapes = shapes,
                typography = Typography(),
                content = content
            )
        }
    }
}

@Composable
private fun rememberColorScheme(config: ThemeConfig): ColorScheme {
    val context = LocalContext.current
    if (config.highContrast) {
        return if (config.paletteFamily == PaletteFamily.Dark || config.paletteFamily == PaletteFamily.Moody) {
            HighContrastDarkScheme
        } else {
            HighContrastLightScheme
        }
    }
    if (config.dynamicColour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (config.paletteFamily == PaletteFamily.Dark || config.paletteFamily == PaletteFamily.Moody) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
    val palette = when (config.paletteFamily) {
        PaletteFamily.Dark -> Palette(
            primary = Color(0xFF8AB4F8),
            primaryVariant = Color(0xFF1A73E8),
            secondary = Color(0xFF03DAC6),
            surface = Color(0xFF202124),
            background = Color(0xFF121212),
            onSurface = Color(0xFFE8EAED)
        )
        PaletteFamily.Moody -> Palette(
            primary = Color(0xFFBB86FC),
            primaryVariant = Color(0xFF6750A4),
            secondary = Color(0xFFFFB4AB),
            surface = Color(0xFF1C1B1F),
            background = Color(0xFF131218),
            onSurface = Color(0xFFE6E1E5)
        )
        PaletteFamily.Pastel -> Palette(
            primary = Color(0xFF669BBC),
            primaryVariant = Color(0xFFE9AFA3),
            secondary = Color(0xFF9B6A6C),
            surface = Color(0xFFFFFBFF),
            background = Color(0xFFFFF7F2),
            onSurface = Color(0xFF1C1B1F)
        )
        PaletteFamily.Soft -> Palette(
            primary = Color(0xFF3A7D44),
            primaryVariant = Color(0xFFD0A98F),
            secondary = Color(0xFF005F73),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF1F3F5),
            onSurface = Color(0xFF121214)
        )
    }
    return if (config.paletteFamily == PaletteFamily.Dark || config.paletteFamily == PaletteFamily.Moody) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.secondary,
            onSecondary = Color.White,
            background = palette.background,
            surface = palette.surface,
            onSurface = palette.onSurface,
            onBackground = palette.onSurface,
            tertiary = palette.primaryVariant
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color(0xFF1C1C1C),
            secondary = palette.secondary,
            onSecondary = Color(0xFF1C1C1C),
            background = palette.background,
            surface = palette.surface,
            onSurface = palette.onSurface,
            onBackground = palette.onSurface,
            tertiary = palette.primaryVariant
        )
    }
}

private val HighContrastLightScheme = lightColorScheme(
    primary = Color(0xFF003E9A),
    onPrimary = Color.White,
    secondary = Color(0xFF005A2B),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFF0F3F8),
    onSurface = Color(0xFF0A0A0A),
    tertiary = Color(0xFF7A003C)
)

private val HighContrastDarkScheme = darkColorScheme(
    primary = Color(0xFF66B1FF),
    onPrimary = Color.Black,
    secondary = Color(0xFFFFD13B),
    onSecondary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF111111),
    onSurface = Color.White,
    tertiary = Color(0xFF00E68E)
)

private fun rememberShapes(cardFamily: CardFamily): Shapes {
    val shape: CornerBasedShape = when (cardFamily) {
        CardFamily.Cloud -> RoundedCornerShape(28.dp)
        CardFamily.Square -> RoundedCornerShape(4.dp)
        CardFamily.Sharp -> CutCornerShape(12.dp)
        CardFamily.Rounded -> RoundedCornerShape(16.dp)
    }
    val smallShape: CornerBasedShape = when (cardFamily) {
        CardFamily.Cloud -> RoundedCornerShape(20.dp)
        CardFamily.Square -> RoundedCornerShape(4.dp)
        CardFamily.Sharp -> CutCornerShape(8.dp)
        CardFamily.Rounded -> RoundedCornerShape(12.dp)
    }
    return Shapes(
        extraSmall = smallShape,
        small = smallShape,
        medium = shape,
        large = shape,
        extraLarge = shape
    )
}

@Immutable
private data class Palette(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val surface: Color,
    val background: Color,
    val onSurface: Color
)
