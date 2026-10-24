package com.cmo.hospital.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CmoPrimary,
    onPrimary = CmoSurface,
    primaryContainer = CmoPrimarySoft,
    onPrimaryContainer = CmoPrimaryDark,
    secondary = CmoPrimaryLight,
    onSecondary = CmoSurface,
    background = CmoBackground,
    onBackground = CmoText,
    surface = CmoSurface,
    onSurface = CmoText,
    surfaceVariant = CmoSurfaceMuted,
    onSurfaceVariant = CmoTextMuted
)

@Composable
fun CMOHospitalTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CmoBackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
