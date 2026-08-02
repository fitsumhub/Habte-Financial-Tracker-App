package com.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val hasPinSet by SettingsRepository.hasPinSet.collectAsState()
    val context = LocalContext.current

    var showChangePinModal by remember { mutableStateOf(false) }

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
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Security",
                    color = MaterialTheme.colorScheme.onSurface,
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { showChangePinModal = true }
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
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (hasPinSet) "Change App PIN" else "Set App PIN", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (hasPinSet) "Update your 4-digit security PIN" else "Create a 4-digit security PIN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showChangePinModal) {
        ChangePinModal(
            hasPinSet = hasPinSet,
            verifyOldPin = { SettingsRepository.verifyPin(it) },
            onClose = { showChangePinModal = false },
            onPinChanged = { newPin ->
                SettingsRepository.setAppPin(newPin)
                Toast.makeText(context, "PIN successfully updated!", Toast.LENGTH_SHORT).show()
                showChangePinModal = false
            }
        )
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
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
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePinModal(hasPinSet: Boolean, verifyOldPin: (String) -> Boolean, onClose: () -> Unit, onPinChanged: (String) -> Unit) {
    var step by remember { mutableStateOf(if (hasPinSet) 1 else 2) } // 1: Old PIN, 2: New PIN, 3: Confirm New PIN
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                if (hasPinSet) "Change App PIN" else "Set App PIN",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                when (step) {
                    1 -> "Enter your current 4-digit PIN."
                    2 -> "Enter your new 4-digit PIN."
                    else -> "Confirm your new 4-digit PIN."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val (currentInput, onInputChange) = when (step) {
                1 -> oldPinInput to { it: String -> if (it.length <= 4) { oldPinInput = it; errorMessage = null } }
                2 -> newPinInput to { it: String -> if (it.length <= 4) { newPinInput = it; errorMessage = null } }
                else -> confirmPinInput to { it: String -> if (it.length <= 4) { confirmPinInput = it; errorMessage = null } }
            }

            OutlinedTextField(
                value = currentInput,
                onValueChange = onInputChange,
                placeholder = { Text("****", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (currentInput.length != 4) {
                        errorMessage = "PIN must be exactly 4 digits."
                        return@Button
                    }
                    when (step) {
                        1 -> {
                            if (verifyOldPin(oldPinInput)) step = 2
                            else errorMessage = "Incorrect PIN."
                        }
                        2 -> step = 3
                        3 -> {
                            if (confirmPinInput == newPinInput) onPinChanged(newPinInput)
                            else errorMessage = "PINs do not match."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Continue", fontWeight = FontWeight.Bold) }
        }
    }
}
