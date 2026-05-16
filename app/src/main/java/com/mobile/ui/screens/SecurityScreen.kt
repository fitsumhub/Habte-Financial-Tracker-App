package com.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.SettingsRepository
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val biometricEnabled by SettingsRepository.biometricEnabled.collectAsState()
    val autoHideBalances by SettingsRepository.autoHideBalances.collectAsState()
    val privacyMode by SettingsRepository.privacyMode.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070912))
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Security",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            
            // Biometric Login
            SecuritySettingRow(
                title = "Biometric Login",
                subtitle = "Use fingerprint or Face ID",
                icon = Icons.Default.Fingerprint,
                checked = biometricEnabled,
                onCheckedChange = { SettingsRepository.setBiometric(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto Hide Balances
            SecuritySettingRow(
                title = "Auto-hide Balances",
                subtitle = "Hide balances on app open",
                icon = Icons.Default.VisibilityOff,
                checked = autoHideBalances,
                onCheckedChange = { SettingsRepository.setAutoHideBalances(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Privacy Mode
            SecuritySettingRow(
                title = "Privacy Mode",
                subtitle = "Blur screens in task switcher",
                icon = Icons.Default.Lock,
                checked = privacyMode,
                onCheckedChange = { SettingsRepository.setPrivacyMode(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Change PIN Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1527))
                    .clickable { Toast.makeText(context, "Change PIN flow...", Toast.LENGTH_SHORT).show() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Change App PIN", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Update your 4-digit security PIN", color = Color(0xFF64748B), fontSize = 13.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun SecuritySettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1527))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF64748B), fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6366F1),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
