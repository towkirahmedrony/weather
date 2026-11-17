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
import com.example.domain.model.ThemePreference

private val AtmosDarkColorScheme = darkColorScheme(
    primary = AtmosSkyBlue,
    onPrimary = AtmosDeepNavy,
    primaryContainer = Color(0xFF173A5E),
    onPrimaryContainer = Color(0xFFD0E9FF),
    secondary = AtmosSunAmber,
    onSecondary = AtmosDeepNavy,
    tertiary = AtmosRainCyan,
    background = AtmosDeepNavy,
    onBackground = Color(0xFFF1F5F9),
    surface = AtmosSlateSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1C2F4B),
    onSurfaceVariant = AtmosInkMutedDark,
    outline = Color(0xFF2D4363),
    error = AtmosAlertCoral
)

private val AtmosLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    tertiary = Color(0xFF0891B2),
    background = AtmosLightSurface,
    onBackground = AtmosInkDark,
    surface = AtmosLightCard,
    onSurface = AtmosInkDark,
    surfaceVariant = Color(0xFFE6EEF8),
    onSurfaceVariant = AtmosInkMutedLight,
    outline = Color(0xFFCBD8E8),
    error = Color(0xFFE11D48)
)

@Composable
fun AtmosTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AtmosDarkColorScheme
        else -> AtmosLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AtmosTypography,
        content = content
    )
}
