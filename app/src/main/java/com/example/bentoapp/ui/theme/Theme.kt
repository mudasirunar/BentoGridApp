package com.example.bentoapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.bentoapp.utils.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary               = BentoPrimaryDark,
    onPrimary             = BentoOnPrimaryDark,
    primaryContainer      = BentoPrimaryContainerDark,
    secondary             = BentoSecondaryDark,
    onSecondary           = BentoOnSecondaryDark,
    secondaryContainer    = BentoSecondaryContainerDark,
    background            = BentoBackgroundDark,
    onBackground          = BentoOnBackgroundDark,
    surface               = BentoSurfaceDark,
    onSurface             = BentoOnSurfaceDark,
    surfaceVariant        = BentoSurfaceVariantDark,
    onSurfaceVariant      = BentoOnSurfaceVariantDark,
    error                 = BentoErrorDark,
    onError               = BentoOnErrorDark,
    errorContainer        = BentoErrorContainerDark,
    onErrorContainer      = BentoOnErrorContainerDark,
    outline               = BentoOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary               = BentoPrimary,
    onPrimary             = BentoOnPrimary,
    primaryContainer      = BentoPrimaryContainer,
    secondary             = BentoSecondary,
    onSecondary           = BentoOnSecondary,
    secondaryContainer    = BentoSecondaryContainer,
    background            = BentoBackground,
    onBackground          = BentoOnBackground,
    surface               = BentoSurface,
    onSurface             = BentoOnSurface,
    surfaceVariant        = BentoSurfaceVariant,
    onSurfaceVariant      = BentoOnSurfaceVariant,
    error                 = BentoError,
    onError               = BentoOnError,
    errorContainer        = BentoErrorContainer,
    onErrorContainer      = BentoOnErrorContainer,
    outline               = BentoOutline
)

@Composable
fun BentoAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Keep status bar and navigation bar transparent for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            val controller = WindowCompat.getInsetsController(window, view)
            // Light icons for dark theme, dark icons for light theme
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
