package com.odialer.pro

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Green = Color(0xFF1FB66B)
val Red = Color(0xFFF2514A)

private val LightScheme = lightColorScheme(
    primary = Green, onPrimary = Color.White,
    background = Color(0xFFF4F5F7), onBackground = Color(0xFF16181B),
    surface = Color.White, onSurface = Color(0xFF16181B),
    surfaceVariant = Color(0xFFE8EAEE), onSurfaceVariant = Color(0xFF6B7078),
    error = Red
)
private val DarkScheme = darkColorScheme(
    primary = Green, onPrimary = Color.White,
    background = Color(0xFF0E0F11), onBackground = Color(0xFFF1F2F4),
    surface = Color(0xFF1A1C1F), onSurface = Color(0xFFF1F2F4),
    surfaceVariant = Color(0xFF2A2D31), onSurfaceVariant = Color(0xFF9AA0A8),
    error = Red
)

@Composable
fun ODialerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window ?: return@SideEffect
        w.statusBarColor = scheme.background.toArgb()
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
