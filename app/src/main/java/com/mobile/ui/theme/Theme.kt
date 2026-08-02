package com.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// ── Colour tokens — dark variant (kept for system/manual dark mode) ──────────
private val Background   = Color(0xFF0B0B0F)
private val Card         = Color(0xFF16161B)
private val Primary      = Color(0xFF6366F1)
private val PrimaryDark  = Color(0xFF4F46E5)
private val TextColor    = Color(0xFFF5F5F7)
private val TextMuted    = Color(0xFF9494A0)
private val Green        = Color(0xFF10B981)
private val Red          = Color(0xFFEF4444)
private val DarkBorder    = Color(0xFF2C2C34)

private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    primaryContainer = PrimaryDark,
    secondary        = PrimaryDark,
    background       = Background,
    onBackground     = TextColor,
    surface          = Card,
    onSurface        = TextColor,
    surfaceVariant   = Color(0xFF26262C),
    onSurfaceVariant = TextMuted,
    outline          = DarkBorder,
    error            = Red,
    onError          = Color.White
)

// ── Colour tokens — light corporate palette (Wise/Chase-style default) ───────
private val LightBg          = Color(0xFFF6F7F9)
private val LightSurface     = Color(0xFFFFFFFF)
private val LightBorder      = Color(0xFFE7EAEE)
private val LightPrimary     = Color(0xFF4F46E5)
private val LightTextColor   = Color(0xFF10131A)
private val LightTextMuted   = Color(0xFF667085)
private val LightGreen       = Color(0xFF059669)
private val LightRed         = Color(0xFFDC2626)

private val LightColorScheme = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFEEF0FF),
    onPrimaryContainer = LightPrimary,
    secondary        = Color(0xFF6366F1),
    background       = LightBg,
    onBackground     = LightTextColor,
    surface          = LightSurface,
    onSurface        = LightTextColor,
    surfaceVariant   = Color(0xFFF1F4F8),
    onSurfaceVariant = LightTextMuted,
    outline          = LightBorder,
    error            = LightRed,
    onError          = Color.White
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
    val themeMode by com.mobile.data.SettingsRepository.theme.collectAsState(initial = "Light")
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
