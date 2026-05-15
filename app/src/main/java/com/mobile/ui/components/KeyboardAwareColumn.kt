package com.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Kotlin/Compose equivalent of KeyboardAwareScrollViewCompat.tsx.
 *
 * Uses Compose's imePadding() modifier which automatically adjusts the
 * layout when the software keyboard appears — no platform check needed
 * because this file is Android-only.
 *
 * Usage:
 *   KeyboardAwareColumn {
 *       // Your form fields here
 *   }
 */
@Composable
fun KeyboardAwareColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        content()
    }
}
