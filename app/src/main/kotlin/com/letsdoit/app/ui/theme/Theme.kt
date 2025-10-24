package com.letsdoit.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig.Default }

@Composable
fun AppTheme(config: ThemeConfig = ThemeConfig.Default, content: @Composable () -> Unit) {
    val colorScheme = rememberColorScheme(config)
    val shapes = rememberShapes(config.cardFamily)
    CompositionLocalProvider(LocalThemeConfig provides config) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = Typography(),
            content = content
        )
    }
}

@Composable
private fun rememberColorScheme(config: ThemeConfig): ColorScheme {
    val context = LocalContext.current
    if (config.dynamicColour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (config.paletteFamily == PaletteFamily.Dark || config.paletteFamily == PaletteFamily.Moody) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
    val palette = when (config.paletteFamily) {
        PaletteFamily.Dark -> Palette(
            primary = Color(0xFF8D99AE),
            primaryVariant = Color(0xFF2B2D42),
            secondary = Color(0xFFEDF2F4),
            surface = Color(0xFF1B1D2A),
            background = Color(0xFF14161F),
            onSurface = Color(0xFFE5E9F0)
        )
        PaletteFamily.Moody -> Palette(
            primary = Color(0xFF7F5AF0),
            primaryVariant = Color(0xFF2CB1BC),
            secondary = Color(0xFFEF4565),
            surface = Color(0xFF16161A),
            background = Color(0xFF0F0F13),
            onSurface = Color(0xFFE4E5F1)
        )
        PaletteFamily.Pastel -> Palette(
            primary = Color(0xFFA3C4F3),
            primaryVariant = Color(0xFFF6D6AD),
            secondary = Color(0xFFE6B8A2),
            surface = Color(0xFFFFFBF0),
            background = Color(0xFFFFF5EC),
            onSurface = Color(0xFF413C58)
        )
        PaletteFamily.Soft -> Palette(
            primary = Color(0xFFB8E1DD),
            primaryVariant = Color(0xFFEEC4C4),
            secondary = Color(0xFF6F5E76),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF5F5F5),
            onSurface = Color(0xFF2D2A32)
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
