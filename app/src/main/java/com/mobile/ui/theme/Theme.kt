package com.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// ── 1. Emerald Dark (Luxury Black & Emerald) ──────────────────────────────────
private val EmeraldDarkScheme = darkColorScheme(
    primary              = Color(0xFF00C853),
    onPrimary            = Color(0xFF003913),
    primaryContainer     = Color(0xFF087F3E),
    onPrimaryContainer   = Color(0xFF00C853),
    secondary            = Color(0xFFFFD54F),
    onSecondary          = Color(0xFF3F2E00),
    secondaryContainer   = Color(0xFF2A2210),
    onSecondaryContainer = Color(0xFFFFD54F),
    background           = Color(0xFF0B0B0B),
    onBackground         = Color(0xFFF5F5F5),
    surface              = Color(0xFF151515),
    onSurface            = Color(0xFFF5F5F5),
    surfaceVariant       = Color(0xFF1C1C1C),
    onSurfaceVariant     = Color(0xFFA0A0A0),
    outline              = Color(0xFF252525),
    outlineVariant       = Color(0xFF252525),
    error                = Color(0xFFFF5252),
    onError              = Color.White,
)

// ── 2. Minimalist White (Clean Light Banking) ────────────────────────────────
private val CleanLightScheme = lightColorScheme(
    primary              = Color(0xFF008937),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFD4F8DE),
    onPrimaryContainer   = Color(0xFF00521F),
    secondary            = Color(0xFFD97706),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFF3DC),
    onSecondaryContainer = Color(0xFF6B4200),
    background           = Color(0xFFF8F9FA),
    onBackground         = Color(0xFF111827),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF111827),
    surfaceVariant       = Color(0xFFF0F2F5),
    onSurfaceVariant     = Color(0xFF4B5563),
    outline              = Color(0xFFE5E7EB),
    outlineVariant       = Color(0xFFE5E7EB),
    error                = Color(0xFFDC2626),
    onError              = Color.White,
)

// ── 3. Binance Pro (Binance Midnight Charcoal & Gold) ────────────────────────
private val BinanceDarkScheme = darkColorScheme(
    primary              = Color(0xFFF0B90B), // Binance Yellow-Gold
    onPrimary            = Color(0xFF181A20),
    primaryContainer     = Color(0xFF2B313A),
    onPrimaryContainer   = Color(0xFFF0B90B),
    secondary            = Color(0xFF0ECB81), // Binance Green
    onSecondary          = Color(0xFF181A20),
    secondaryContainer   = Color(0xFF1E2329),
    onSecondaryContainer = Color(0xFF0ECB81),
    background           = Color(0xFF0B0E11), // Binance Deep Charcoal
    onBackground         = Color(0xFFEAECEF),
    surface              = Color(0xFF181A20), // Binance Card Surface
    onSurface            = Color(0xFFEAECEF),
    surfaceVariant       = Color(0xFF2B313A),
    onSurfaceVariant     = Color(0xFF848E9C),
    outline              = Color(0xFF2B313A),
    outlineVariant       = Color(0xFF2B313A),
    error                = Color(0xFFF6465D), // Binance Sell Red
    onError              = Color.White,
)

// ── 4. Addis Gold (Ethiopian Luxury Gold) ────────────────────────────────────
private val AddisGoldScheme = darkColorScheme(
    primary              = Color(0xFFD4A017),
    onPrimary            = Color(0xFF0D0A00),
    primaryContainer     = Color(0xFFB8860B),
    onPrimaryContainer   = Color(0xFFFFF3DC),
    secondary            = Color(0xFFD4A017),
    onSecondary          = Color(0xFF0D0A00),
    secondaryContainer   = Color(0xFF2D1F0A),
    onSecondaryContainer = Color(0xFFF0C040),
    background           = Color(0xFF0F0D0A),
    onBackground         = Color(0xFFFAF6EF),
    surface              = Color(0xFF1E1812),
    onSurface            = Color(0xFFFAF6EF),
    surfaceVariant       = Color(0xFF2A2218),
    onSurfaceVariant     = Color(0xFF9E916E),
    outline              = Color(0xFF3D2E0A),
    outlineVariant       = Color(0xFF2A2218),
    error                = Color(0xFFC62828),
    onError              = Color.White,
)

/**
 * Multi-Theme Provider for Habte (ሀብቴ)
 * Supports:
 *  - "Dark" / "Emerald Dark" (Default)
 *  - "Light" / "Minimalist White"
 *  - "Binance" / "Binance Pro"
 *  - "Addis Gold"
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val themeMode by com.mobile.data.SettingsRepository.theme.collectAsState(initial = "Dark")
    val systemDark = isSystemInDarkTheme()

    val (colorScheme, ethiopianColors) = when (themeMode) {
        "Light", "Minimalist White" -> Pair(CleanLightScheme, LightEthiopianColors)
        "Binance", "Binance Pro"    -> Pair(BinanceDarkScheme, BinanceColors)
        "Addis Gold", "Gold"        -> Pair(AddisGoldScheme, AddisGoldColors)
        "Dark", "Emerald Dark"      -> Pair(EmeraldDarkScheme, DarkEthiopianColors)
        else                        -> if (systemDark) Pair(EmeraldDarkScheme, DarkEthiopianColors) else Pair(CleanLightScheme, LightEthiopianColors)
    }

    CompositionLocalProvider(LocalEthiopianColors provides ethiopianColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
