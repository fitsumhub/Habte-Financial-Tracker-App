package com.mobile.ui.components

import androidx.compose.runtime.*
import com.mobile.ui.components.ErrorFallback

/**
 * Wraps content in a composable error boundary.
 * If an exception is caught during composition, ErrorFallback is shown instead.
 * Note: Compose does not have a built-in class-based error boundary like React.
 * Runtime rendering errors should be handled at the Activity level via
 * Thread.setDefaultUncaughtExceptionHandler or a crash-reporting SDK.
 * This wrapper handles caught exceptions from state/logic inside composables.
 */
@Composable
fun ErrorBoundary(
    content: @Composable () -> Unit
) {
    var error by remember { mutableStateOf<Throwable?>(null) }

    if (error != null) {
        ErrorFallback(
            error = error!!,
            resetError = { error = null }
        )
    } else {
        content()
    }
}
