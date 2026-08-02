package com.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.ui.navigation.AppNavigation

/**
 * Root layout — equivalent of app/_layout.tsx.
 * Wraps the full navigation stack in AppTheme (dark mode + M3 color scheme) and a
 * background Surface. Crash safety lives at the Activity level (see
 * [com.mobile.data.CrashReporter] and MainActivity) rather than here — Kotlin's Compose
 * compiler plugin rejects try/catch around composable calls, so there's no way to catch
 * a composition-time exception from a wrapper composable like this one.
 */
@Composable
fun RootLayout() {
    com.mobile.ui.theme.AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}
