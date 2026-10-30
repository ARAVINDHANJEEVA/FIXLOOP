package com.fixloop.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SentinelColorScheme = lightColorScheme(
    primary = HazardAmber,
    onPrimary = Ink,
    secondary = IdCardBlue,
    onSecondary = SurfaceCard,
    tertiary = ConeOrange,
    background = BgPaper,
    onBackground = Ink,
    surface = SurfaceCard,
    onSurface = Ink,
    surfaceVariant = BgPaper,
    onSurfaceVariant = InkMuted,
    outline = Ink,
    error = AlertRed
)

@Composable
fun FixLoopTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = HazardAmber.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = SentinelColorScheme,
        typography = SentinelTypography,
        content = content
    )
}
