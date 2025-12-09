package com.letsdoit.app.presentation.theme
import com.letsdoit.app.domain.model.ThemeColor
import com.letsdoit.app.domain.model.ThemeMode

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Blue80,
    tertiary = Pink80
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Blue40,
    tertiary = Pink40
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = Green80,
    tertiary = Pink80
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = Green40,
    tertiary = Pink40
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = Orange80,
    secondary = Orange80,
    tertiary = Pink80
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = Orange40,
    secondary = Orange40,
    tertiary = Pink40
)

@Composable
fun LetsDoItTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColor: ThemeColor = ThemeColor.PURPLE,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (themeColor) {
            ThemeColor.PURPLE -> if (darkTheme) DarkColorScheme else LightColorScheme
            ThemeColor.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
            ThemeColor.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
            ThemeColor.ORANGE -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
