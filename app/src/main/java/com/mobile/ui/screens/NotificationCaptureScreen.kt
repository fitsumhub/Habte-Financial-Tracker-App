package com.mobile.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mobile.data.InstalledBankAppMatcher
import com.mobile.data.MonitorableApp
import com.mobile.data.SettingsRepository

private fun isNotificationAccessGranted(context: android.content.Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCaptureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val captureEnabled by SettingsRepository.notificationCaptureEnabled.collectAsState()
    val monitoredApps by SettingsRepository.monitoredApps.collectAsState()

    // Notification access is granted/revoked from outside this screen (Android Settings),
    // so re-check whenever the screen resumes rather than only once on first composition.
    var accessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val monitorableApps = remember { InstalledBankAppMatcher.findMonitorableApps(context) }

    // Auto-enable any bank/wallet app found on the device that isn't already tracked yet —
    // the same call NotificationCaptureListenerService makes on connect, run here too so a
    // newly installed bank app (or the very first load, before the listener service has ever
    // connected) shows up pre-checked instead of requiring a manual tap.
    LaunchedEffect(monitorableApps) {
        SettingsRepository.autoEnableDetectedApps(monitorableApps)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Notification Capture",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Some banks and wallets push transaction alerts through their own app's notifications instead of (or in addition to) SMS. Once you grant notification access below, Habte automatically reads those for any matching bank/wallet app already on your device — the same way it already reads SMS — with no per-app setup needed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            SecuritySettingRow(
                title = "Capture bank app notifications",
                subtitle = "Master switch — on automatically once access is granted",
                icon = Icons.Default.NotificationsActive,
                checked = captureEnabled,
                onCheckedChange = { SettingsRepository.setNotificationCaptureEnabled(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionStatusCard(
                granted = accessGranted,
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Bank & wallet apps found on this device",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Only apps checked here are ever inspected. Everything else — messages, social apps, every other notification on your phone — is left completely alone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (monitorableApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No matching bank or wallet apps found installed on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    monitorableApps.forEach { app ->
                        MonitorableAppRow(
                            app = app,
                            checked = monitoredApps.containsKey(app.packageName),
                            onCheckedChange = { checked ->
                                val updated = monitoredApps.toMutableMap()
                                if (checked) updated[app.packageName] = app.institution.id else updated.remove(app.packageName)
                                SettingsRepository.setMonitoredApps(updated)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(granted: Boolean, onOpenSettings: () -> Unit) {
    val accentColor = if (granted) Color(0xFF059669) else Color(0xFFD97706)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (granted) "Notification access granted" else "Notification access needed",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (granted) {
                        "Habte can read notifications from the apps you enable below."
                    } else {
                        "This is a system-level Android permission — grant it from Settings."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        if (!granted) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Open Notification Access Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MonitorableAppRow(app: MonitorableApp, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Matched to ${app.institution.name}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
