package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MementoSilver,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = MementoDarkCardElevated,
    onPrimaryContainer = MementoSilver,
    secondary = MementoSilverMuted,
    onSecondary = Color(0xFF0F172A),
    background = MementoDarkBackground,
    onBackground = MementoTextPrimary,
    surface = MementoDarkSurface,
    onSurface = MementoTextPrimary,
    surfaceVariant = MementoDarkCard,
    onSurfaceVariant = MementoTextSecondary,
    outline = MementoDarkBorder,
    outlineVariant = MementoDarkBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF334155),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F5F9),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    background = MementoLightBackground,
    onBackground = MementoLightTextPrimary,
    surface = MementoLightSurface,
    onSurface = MementoLightTextPrimary,
    surfaceVariant = MementoLightCard,
    onSurfaceVariant = MementoLightTextSecondary,
    outline = MementoLightBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun MementoTheme(
    themeMode: String = "DARK", // "DARK", "LIGHT", "ADAPTIVE"
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "ADAPTIVE" -> isSystemDark
        else -> true // default DARK first!
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
