package com.example.billingapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Fill,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = Fill,
    onSecondaryContainer = Ink,
    tertiary = Ink,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Fill,
    onSurfaceVariant = Muted,
    surfaceTint = Paper,
    outline = Outline,
    outlineVariant = Line,
    error = Danger,
    onError = Paper,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Fill,
    surfaceContainer = Paper,
    surfaceContainerHigh = Fill,
    surfaceContainerHighest = Fill
)

// Light only, on purpose: the whole look is white and black, so dynamic
// (wallpaper) colour and dark mode are left out until we design them.
@Composable
fun BillingAppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Paper.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
