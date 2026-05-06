package com.example.bentoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}