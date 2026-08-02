package com.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.SettingsRepository
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(onBack: () -> Unit) {
    val notifications by SettingsRepository.notificationsEnabled.collectAsState()
    val smsAlerts by SettingsRepository.smsAlerts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "Alerts",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            // We'll reuse the same toggle rows from SettingsScreen for simplicity
            SettingToggleRow(
                icon = Icons.Default.Notifications,
                label = "Push Notifications",
                description = "Transaction alerts and updates",
                checked = notifications,
                onCheckedChange = { 
                    SettingsRepository.setNotifications(it) 
                }
            )
            SettingToggleRow(
                icon = Icons.Default.Send,
                label = "SMS Alerts",
                description = "Transaction notifications via SMS",
                checked = smsAlerts,
                onCheckedChange = { 
                    SettingsRepository.setSmsAlerts(it)
                }
            )
        }
    }
}