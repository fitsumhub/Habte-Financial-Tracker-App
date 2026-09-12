package com.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Multi-Theme Color Tokens for Habte (ሀብቴ)
 */
@Immutable
data class EthiopianColors(
    val emeraldPrimary: Color,
    val emeraldDark: Color,
    val goldAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val telebirrOrange: Color,
)

// 1. Emerald Dark (Luxury Fintech Black)
val DarkEthiopianColors = EthiopianColors(
    emeraldPrimary    = Color(0xFF00C853),
    emeraldDark       = Color(0xFF087F3E),
    goldAccent        = Color(0xFFFFD54F),
    background        = Color(0xFF0B0B0B),
    surface           = Color(0xFF151515),
    surfaceElevated   = Color(0xFF1C1C1C),
    border            = Color(0xFF252525),
    textPrimary       = Color(0xFFF5F5F5),
    textSecondary     = Color(0xFFA0A0A0),
    textMuted         = Color(0xFF666666),
    income            = Color(0xFF00C853),
    expense           = Color(0xFFFF5252),
    warning           = Color(0xFFFFB300),
    telebirrOrange    = Color(0xFFFF6B00),
)

// 2. Minimalist White (Clean Light Banking)
val LightEthiopianColors = EthiopianColors(
    emeraldPrimary    = Color(0xFF008937),
    emeraldDark       = Color(0xFF087F3E),
    goldAccent        = Color(0xFFD97706),
    background        = Color(0xFFF8F9FA),
    surface           = Color(0xFFFFFFFF),
    surfaceElevated   = Color(0xFFF0F2F5),
    border            = Color(0xFFE5E7EB),
    textPrimary       = Color(0xFF111827),
    textSecondary     = Color(0xFF4B5563),
    textMuted         = Color(0xFF9CA3AF),
    income            = Color(0xFF059669),
    expense           = Color(0xFFDC2626),
    warning           = Color(0xFFD97706),
    telebirrOrange    = Color(0xFFFF6B00),
)

// 3. Binance Pro (Binance Crypto Midnight Charcoal & Gold)
val BinanceColors = EthiopianColors(
    emeraldPrimary    = Color(0xFFF0B90B), // Binance Gold
    emeraldDark       = Color(0xFFB8860B),
    goldAccent        = Color(0xFFF0B90B),
    background        = Color(0xFF0B0E11), // Binance Deep Charcoal
    surface           = Color(0xFF181A20), // Binance Card Surface
    surfaceElevated   = Color(0xFF2B313A), // Binance Elevated
    border            = Color(0xFF2B313A),
    textPrimary       = Color(0xFFEAECEF),
    textSecondary     = Color(0xFF848E9C),
    textMuted         = Color(0xFF5E6673),
    income            = Color(0xFF0ECB81), // Binance Buy Green
    expense           = Color(0xFFF6465D), // Binance Sell Red
    warning           = Color(0xFFF0B90B),
    telebirrOrange    = Color(0xFFFF6B00),
)

// 4. Addis Gold (Ethiopian Luxury Gold)
val AddisGoldColors = EthiopianColors(
    emeraldPrimary    = Color(0xFFD4A017), // Ethiopian Gold
    emeraldDark       = Color(0xFFB8860B),
    goldAccent        = Color(0xFFF0C040),
    background        = Color(0xFF0F0D0A),
    surface           = Color(0xFF1E1812),
    surfaceElevated   = Color(0xFF2A2218),
    border            = Color(0xFF3D2E0A),
    textPrimary       = Color(0xFFFAF6EF),
    textSecondary     = Color(0xFF9E916E),
    textMuted         = Color(0xFF6B5E3A),
    income            = Color(0xFF2E8B57),
    expense           = Color(0xFFC62828),
    warning           = Color(0xFFF59E0B),
    telebirrOrange    = Color(0xFFFF6B00),
)

val LocalEthiopianColors = compositionLocalOf { DarkEthiopianColors }

fun EthiopianColors.categoryColor(category: String): Color = when (category) {
    "Income", "Salary"                   -> income
    "Bills", "Bills & Utilities", "Rent" -> Color(0xFF8B5CF6)
    "Food", "Food & Dining"              -> Color(0xFFF59E0B)
    "Transport"                          -> Color(0xFF3B82F6)
    "Cash"                               -> Color(0xFF6B7280)
    "Transfers", "Transfer", "Lend"      -> income
    "Shopping", "Cosmetics"              -> Color(0xFFEC4899)
    else                                 -> emeraldPrimary
}
