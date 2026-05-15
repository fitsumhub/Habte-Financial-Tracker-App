package com.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mobile.ui.components.ErrorBoundary
import com.mobile.ui.navigation.AppNavigation

/**
 * Root layout — equivalent of app/_layout.tsx.
 * Wraps the full navigation stack in:
 *   - AppTheme (dark mode + M3 color scheme)
 *   - ErrorBoundary (crash safety)
 *   - Surface (background color)
 */
@Composable
fun RootLayout() {
    com.mobile.ui.theme.AppTheme {
        ErrorBoundary {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF070912)
            ) {
                AppNavigation()
            }
        }
    }
}
