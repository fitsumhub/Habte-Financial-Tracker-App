package com.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// ── Colour tokens (mirrors constants/colors.ts) ──────────────────────────────
private val Background   = Color(0xFF070912)
private val Card         = Color(0xFF0E1527)
private val Primary      = Color(0xFF6366F1)
private val PrimaryDark  = Color(0xFF4338CA)
private val TextColor    = Color(0xFFF0F2FF)
private val TextMuted    = Color(0xFF7B84A8)
private val Green        = Color(0xFF10B981)
private val Red          = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    primaryContainer = PrimaryDark,
    secondary        = Color(0xFF7C3AED),
    background       = Background,
    onBackground     = TextColor,
    surface          = Card,
    onSurface        = TextColor,
    surfaceVariant   = Color(0xFF1A2240),
    onSurfaceVariant = TextMuted,
    error            = Red,
    onError          = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    primaryContainer = PrimaryDark,
    background       = Color(0xFFF5F5F5),
    onBackground     = Color(0xFF111827),
    surface          = Color.White,
    onSurface        = Color(0xFF111827)
)

/**
 * App-wide Material 3 theme.
 * Replaces both useColors.ts + the Poppins font setup from _layout.tsx.
 * Dark mode is applied automatically based on system preference.
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val themeMode by com.mobile.data.SettingsRepository.theme.collectAsState(initial = "Dark")
    val systemDark = isSystemInDarkTheme()
    
    val darkTheme = when (themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> systemDark
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
