package com.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.*
import androidx.compose.ui.draw.scale
import com.mobile.data.FinanceRepository
import com.mobile.data.SettingsRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val biometric by SettingsRepository.biometricEnabled.collectAsState()
    val autoHide by SettingsRepository.autoHideBalances.collectAsState()
    val privacyMode by SettingsRepository.privacyMode.collectAsState()
    val notifications by SettingsRepository.notificationsEnabled.collectAsState()
    val emailUpdates by SettingsRepository.emailUpdates.collectAsState()
    val smsAlerts by SettingsRepository.smsAlerts.collectAsState()
    val currency by SettingsRepository.currency.collectAsState()
    val language by SettingsRepository.language.collectAsState()
    val dateFormat by SettingsRepository.dateFormat.collectAsState()
    val theme by SettingsRepository.theme.collectAsState()
    
    var isSyncing by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    
    var userName by remember { mutableStateOf("Fitsum") }
    var userEmail by remember { mutableStateOf("fitsum@example.com") }
    var cacheSize by remember { mutableStateOf(12.4) }

    val transactions by FinanceRepository.transactions.collectAsState()

    // File creation launcher
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val writer = java.io.OutputStreamWriter(outputStream)
                    val csv = java.lang.StringBuilder("Date,Title,Amount,Type,Category,Bank\n")
                    transactions.forEach { t ->
                        csv.append("${t.date},${t.title},${t.amount},${t.type},${t.category},${t.bankShortName}\n")
                    }
                    writer.write(csv.toString())
                    writer.flush()
                }
                Toast.makeText(context, "Data exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp)
        ) {
            // Profile Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .clickable { 
                         haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                         showProfileDialog = true
                    }
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x4DFFFFFF),
                                        Color(0x00FFFFFF)
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0x4DFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp).align(Alignment.Center)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userEmail,
                            color = Color(0xD9FFFFFF),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Group: Security
            SettingsGroup("SECURITY") {
                SettingToggleRow(
                    icon = Icons.Default.Lock,
                    label = "Biometric Login",
                    description = "Fingerprint or face ID authentication",
                    checked = biometric,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SettingsRepository.setBiometric(it) 
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingToggleRow(
                    icon = Icons.Default.Search, // Placeholder for VisibilityOff
                    label = "Auto-hide Balances",
                    description = "Hide amounts when app opens",
                    checked = autoHide,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SettingsRepository.setAutoHideBalances(it) 
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingToggleRow(
                    icon = Icons.Default.Build, // Placeholder for Shield
                    label = "Privacy Mode",
                    description = "Enhanced transaction privacy",
                    checked = privacyMode,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SettingsRepository.setPrivacyMode(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: Notifications
            SettingsGroup("NOTIFICATIONS") {
                SettingToggleRow(
                    icon = Icons.Default.Notifications,
                    label = "Push Notifications",
                    description = "Transaction alerts and updates",
                    checked = notifications,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SettingsRepository.setNotifications(it) 
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingToggleRow(
                    icon = Icons.Default.Email,
                    label = "Email Updates",
                    description = "Monthly statements and news",
                    checked = emailUpdates,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SettingsRepository.setEmailUpdates(it)
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingToggleRow(
                    icon = Icons.Default.Send,
                    label = "SMS Alerts",
                    description = "Transaction notifications via SMS",
                    checked = smsAlerts,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SettingsRepository.setSmsAlerts(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: Preferences
            SettingsGroup("PREFERENCES") {
                SettingOptionRow(
                    icon = Icons.Default.ShoppingCart,
                    label = "Currency",
                    value = currency,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showCurrencyDialog = true 
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.Face,
                    label = "Language",
                    value = language,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showLanguageDialog = true 
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.DateRange,
                    label = "Date Format",
                    value = dateFormat,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDateFormatDialog = true
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.Settings,
                    label = "Theme",
                    value = theme,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showThemeDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: Data & Storage
            SettingsGroup("DATA & STORAGE") {
                SettingOptionRow(
                    icon = Icons.Default.Refresh,
                    label = "Manual Data Sync",
                    value = if (isSyncing) "Syncing..." else "Last synced: Just now",
                    onClick = { 
                        if (!isSyncing) {
                            scope.launch {
                                isSyncing = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                FinanceRepository.syncHistoricalSms(context)
                                kotlinx.coroutines.delay(1500)
                                isSyncing = false
                                Toast.makeText(context, "Financial data synchronized!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.Delete,
                    label = "Cache Size",
                    value = String.format("%.1f MB", cacheSize),
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            context.cacheDir.deleteRecursively()
                            cacheSize = 0.0
                            Toast.makeText(context, "Cache cleared successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.Share,
                    label = "Export Data",
                    value = "",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (transactions.isEmpty()) {
                            Toast.makeText(context, "No transactions found to export", Toast.LENGTH_SHORT).show()
                        } else {
                            val fileName = "Habte_Export_${System.currentTimeMillis()}.csv"
                            createDocumentLauncher.launch(fileName)
                        }
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.Add,
                    label = "Import Data",
                    value = "",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "CSV Import coming in v2.2.0", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group: About
            SettingsGroup("ABOUT") {
                SettingOptionRow(
                    icon = Icons.Default.Info,
                    label = "Version",
                    value = "2.1.0",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Version 2.1.0", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.List,
                    label = "Terms of Service",
                    value = "",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTermsDialog = true
                    }
                )
                HorizontalDivider(color = Color(0x0DFFFFFF), modifier = Modifier.padding(start = 56.dp))
                SettingOptionRow(
                    icon = Icons.Default.Lock,
                    label = "Privacy Policy",
                    value = "",
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showPrivacyDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Sign Out Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A0A0A))
                    .border(1.dp, Color(0xFF3D1515), RoundedCornerShape(16.dp))
                    .clickable { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSignOutConfirm = true
                    }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Out",
                    color = Color(0xFFEF4444),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showCurrencyDialog) {
        SelectionDialog(
            title = "Select Currency",
            options = listOf("ETB", "USD", "EUR", "GBP"),
            selected = currency,
            onSelect = { SettingsRepository.setCurrency(it) },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = "Select Language",
            options = listOf("English", "Amharic", "Oromifa"),
            selected = language,
            onSelect = { SettingsRepository.setLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showDateFormatDialog) {
        SelectionDialog(
            title = "Select Date Format",
            options = listOf("MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD"),
            selected = dateFormat,
            onSelect = { SettingsRepository.setDateFormat(it) },
            onDismiss = { showDateFormatDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = "Select Theme",
            options = listOf("Light", "Dark", "System Default"),
            selected = theme,
            onSelect = { SettingsRepository.setTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            currentName = userName,
            currentEmail = userEmail,
            onSave = { name, email -> userName = name; userEmail = email },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showTermsDialog) {
        InfoDialog(
            title = "Terms of Service",
            content = "By using this application, you agree to the following terms...\n\n1. Data Privacy: Your financial data is stored locally on your device.\n2. SMS Access: This app requires SMS permission to automatically track your transactions.\n3. Security: You are responsible for maintaining the security of your device and biometric data.",
            onDismiss = { showTermsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        InfoDialog(
            title = "Privacy Policy",
            content = "Your privacy is our priority.\n\n- We do not upload your financial data to any external servers.\n- Your SMS messages are processed locally on your device.\n- No personal information is shared with third parties.\n- Analytics data is anonymized and used only for app improvement.",
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of your account?", color = Color(0xFF9CA3AF)) },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = { 
                        showSignOutConfirm = false
                        Toast.makeText(context, "Signed out successfully.", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("Confirm Sign Out", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel", color = Color(0xFF818CF8))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditDialog(
    currentName: String,
    currentEmail: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF818CF8),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF4B5563)
                    )
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF818CF8),
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color(0xFF4B5563)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, email); onDismiss() }) {
                Text("Save Changes", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9CA3AF))
            }
        }
    )
}

@Composable
private fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { 
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(content, color = Color(0xFF9CA3AF), lineHeight = 20.sp)
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color(0xFF6B7280),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0E1527))
                .border(1.dp, Color(0xFF1A2240), RoundedCornerShape(20.dp))
        ) {
            content()
        }
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector? = null,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF818CF8) else Color(0xFF4B5563),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (enabled) Color.White else Color(0xFF6B7280),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = if (enabled) Color(0xFF9CA3AF) else Color(0xFF4B5563),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6366F1),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color(0xFF9CA3AF),
                uncheckedTrackColor = Color(0xFF1E293B),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingOptionRow(
    icon: ImageVector? = null,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Next",
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(24.dp),
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(option); onDismiss() }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option); onDismiss() },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF818CF8),
                                unselectedColor = Color(0xFF4B5563)
                            )
                        )
                        Text(
                            text = option, 
                            color = if (option == selected) Color.White else Color(0xFF9CA3AF), 
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF818CF8), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

