package com.mobile.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mobile.data.CertificatePeriod
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import com.mobile.data.SettingsRepository
import com.mobile.data.SummaryScheduler
import com.mobile.data.computeCertificateAchievement
import com.mobile.ui.components.AxumiteCrossWatermark
import com.mobile.ui.components.EthiopianTricolorBar
import com.mobile.ui.theme.LocalEthiopianColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val colors = LocalEthiopianColors.current

    val banks by FinanceRepository.banks.collectAsState()
    val transactions by FinanceRepository.transactions.collectAsState()
    val savedName by SettingsRepository.userName.collectAsState()
    val savedEmail by SettingsRepository.userEmail.collectAsState()
    val savedProfilePhotoUri by SettingsRepository.profilePhotoUri.collectAsState()
    val currentTheme by SettingsRepository.theme.collectAsState()
    val selectedOverviewPeriod by SettingsRepository.financialOverviewPeriod.collectAsState()
    val calendarSystem by SettingsRepository.calendarSystem.collectAsState()
    val autoHideBalances by SettingsRepository.autoHideBalances.collectAsState()
    val biometricEnabled by SettingsRepository.biometricEnabled.collectAsState()
    val privacyMode by SettingsRepository.privacyMode.collectAsState()
    val hasPinSet by SettingsRepository.hasPinSet.collectAsState()
    val widgetsEnabled by SettingsRepository.widgetsEnabled.collectAsState()
    val preferredWidgetStyle by SettingsRepository.preferredWidgetStyle.collectAsState()
    val profileAvatarRing by SettingsRepository.profileAvatarRing.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarStudioModal by remember { mutableStateOf(false) }
    var editName by remember(savedName) { mutableStateOf(savedName) }
    var editEmail by remember(savedEmail) { mutableStateOf(savedEmail) }
    var showChangePinModal by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            SettingsRepository.setProfilePhotoUri(uri.toString())
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, "Executive profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
                SettingsRepository.setProfilePhotoUri(android.net.Uri.fromFile(file).toString())
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Executive camera photo updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val totalAccounts = banks.sumOf { it.accounts.size }
    val totalBalance = Data.getTotalBalance(banks)

    // Filter transactions based on selected overview period
    val filteredTransactions = remember(transactions, selectedOverviewPeriod) {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val limitDate = java.util.Calendar.getInstance()
        val filterActive = when (selectedOverviewPeriod) {
            "Weekly" -> { limitDate.add(java.util.Calendar.DAY_OF_YEAR, -7); true }
            "Monthly" -> { limitDate.add(java.util.Calendar.MONTH, -1); true }
            "6 Months" -> { limitDate.add(java.util.Calendar.MONTH, -6); true }
            "Yearly" -> { limitDate.add(java.util.Calendar.YEAR, -1); true }
            else -> false
        }
        if (!filterActive) transactions else {
            transactions.filter {
                try {
                    val dateObj = sdf.parse(it.date)
                    dateObj != null && dateObj.time >= limitDate.timeInMillis
                } catch (e: Exception) {
                    true
                }
            }
        }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "credit" }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "debit" }.sumOf { it.amount }
    }
    val savingsRate = remember(totalIncome, totalExpense) {
        if (totalIncome > 0) {
            val rate = ((totalIncome - totalExpense) / totalIncome) * 100.0
            rate.coerceIn(0.0, 100.0)
        } else 0.0
    }

    // Stable Habte Account UID based on name hash
    val userHabteId = remember(savedName) {
        val hash = (savedName.hashCode().toLong() and 0xFFFFFFFFL) % 900000L + 100000L
        "HBT-ET-$hash"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // ── TOP NAVIGATION BAR ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Profile & Account",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Executive Settings & Security",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Sync Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.emeraldPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ON-DEVICE",
                    color = colors.emeraldPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // ── SCROLLABLE PROFILE CONTENT ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 1. LUXURY EXECUTIVE IDENTITY CARD ──────────────────────────────
            ExecutiveProfileCard(
                name = savedName,
                email = savedEmail,
                photoUri = savedProfilePhotoUri,
                avatarRing = profileAvatarRing,
                habteId = userHabteId,
                onEditClick = {
                    editName = savedName
                    editEmail = savedEmail
                    showEditProfileDialog = true
                },
                onPhotoClick = { showAvatarStudioModal = true },
                onCopyId = {
                    clipboardManager.setText(AnnotatedString(userHabteId))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Habte ID copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )

            // ── 2. WEALTH MATRIX & PORTFOLIO SNAPSHOT ──────────────────────────
            ExecutiveFinancialSnapshot(
                totalBalance = totalBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                savingsRate = savingsRate,
                totalAccounts = totalAccounts,
                totalBanks = banks.size,
                selectedPeriod = selectedOverviewPeriod,
                onPeriodSelected = { SettingsRepository.setFinancialOverviewPeriod(it) },
                onNavigate = onNavigate
            )

            // ── 3. EXECUTIVE FINANCIAL CERTIFICATES & HONORS ────────────────────
            val monthlyAchievement = remember(transactions) {
                computeCertificateAchievement(transactions, CertificatePeriod.MONTHLY)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                colors.surfaceElevated,
                                colors.emeraldDark.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(1.2.dp, colors.goldAccent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigate("achievement_certificates")
                    }
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.goldAccent.copy(alpha = 0.15f))
                                    .border(1.dp, colors.goldAccent.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Honors & Certificates",
                                    tint = colors.goldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Executive Certificates",
                                        color = colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.goldAccent.copy(alpha = 0.15f))
                                            .border(0.6.dp, colors.goldAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "OFFICIAL",
                                            color = colors.goldAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "Verified financial milestone awards & proof",
                                    color = colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Certificates",
                            tint = colors.goldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Certificate Highlight Metric Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface.copy(alpha = 0.7f))
                            .border(0.8.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Recognition",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = monthlyAchievement.title,
                                color = colors.goldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Savings Discipline",
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${monthlyAchievement.savingsRatePercent?.toInt() ?: 0}% Verified",
                                color = colors.emeraldPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigate("achievement_certificates")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate & Export Official Certificate",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 4. APP AESTHETICS & THEME STUDIO ───────────────────────────────
            ExecutiveThemeStudio(
                currentTheme = currentTheme,
                onSelectTheme = { themeName ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SettingsRepository.setTheme(themeName)
                }
            )

            // ── 5. SECURITY & PRIVACY HUB ──────────────────────────────────────
            ExecutiveSettingsSection(
                title = "Security & Privacy",
                subtitle = "Hardware-level protection and screen privacy"
            ) {
                ExecutiveSettingToggle(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Authentication",
                    subtitle = "Unlock Habte with fingerprint or Face ID",
                    checked = biometricEnabled,
                    onCheckedChange = { SettingsRepository.setBiometric(it) }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                ExecutiveSettingToggle(
                    icon = Icons.Default.Shield,
                    title = "Privacy Guard",
                    subtitle = "Mask application screens in recent task switcher",
                    checked = privacyMode,
                    onCheckedChange = { SettingsRepository.setPrivacyMode(it) }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                ExecutiveSettingToggle(
                    icon = Icons.Default.VisibilityOff,
                    title = "Auto-Hide Balances",
                    subtitle = "Conceal sensitive account numbers on home screen",
                    checked = autoHideBalances,
                    onCheckedChange = { SettingsRepository.setAutoHideBalances(it) }
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                // App PIN Config Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.emeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App PIN Code",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasPinSet) "Configured & active" else "Not configured",
                            color = if (hasPinSet) colors.emeraldPrimary else colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { showChangePinModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (hasPinSet) "Change PIN" else "Set PIN",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 5.5 HOMESCREEN & LOCKSCREEN WIDGETS ──────────────────────────────
            ExecutiveSettingsSection(
                title = "Homescreen & Lockscreen Widgets",
                subtitle = "Enable Android widgets and choose your preferred default style"
            ) {
                ExecutiveSettingToggle(
                    icon = Icons.Default.Widgets,
                    title = "Enable Android Widgets",
                    subtitle = "Allow live spending & balance widgets on your device screens",
                    checked = widgetsEnabled,
                    onCheckedChange = { SettingsRepository.setWidgetsEnabled(it) }
                )

                if (widgetsEnabled) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = colors.goldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Preferred Widget Style",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Select your default home screen widget design",
                                    color = colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val widgetStyles = listOf(
                            Triple("Daily Digest", "4x2 Standard", Icons.Default.Assessment),
                            Triple("Net Worth Card", "2x2 Compact", Icons.Default.AccountBalanceWallet),
                            Triple("Quick Tracker", "4x1 Slim Bar", Icons.Default.FlashOn),
                            Triple("Multi-Bank Grid", "4x3 Bank Grid", Icons.Default.AccountBalance),
                            Triple("Budget Pulse", "2x1 Capsule", Icons.Default.Speed)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            widgetStyles.forEach { (styleName, badge, icon) ->
                                val isSelected = preferredWidgetStyle == styleName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) colors.emeraldPrimary.copy(alpha = 0.15f) else colors.surfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.emeraldPrimary else colors.border,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            SettingsRepository.setPreferredWidgetStyle(styleName)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) colors.emeraldPrimary else colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = styleName,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = badge,
                                                color = if (isSelected) colors.emeraldPrimary else colors.textMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            SettingsRepository.setPreferredWidgetStyle(styleName)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.emeraldPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. REGIONAL & PREFERENCES ──────────────────────────────────────
            ExecutiveSettingsSection(
                title = "Regional & Calendar",
                subtitle = "Date formats and Ethiopian calendar integration"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = colors.goldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calendar System",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (calendarSystem == "Ethiopian") "የኢትዮጵያ ዘመን አቆጣጠር (Geez)" else "Gregorian Standard (GC)",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        val isEth = calendarSystem == "Ethiopian"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isEth) colors.emeraldPrimary else Color.Transparent)
                                .clickable { SettingsRepository.setCalendarSystem("Gregorian") }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "GC",
                                color = if (!isEth) Color.White else colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isEth) colors.emeraldPrimary else Color.Transparent)
                                .clickable { SettingsRepository.setCalendarSystem("Ethiopian") }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ኢትዮ",
                                color = if (isEth) Color.White else colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── 7. SYSTEM INTEGRITY & ABOUT ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDiagnosticsDialog = true
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colors.emeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Habte Financial Intelligence",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${transactions.size} Encrypted Records · Local Room DB",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Diagnostics",
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // ── SYSTEM DIAGNOSTICS MODAL ───────────────────────────────────────────────
    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = colors.emeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "System Diagnostics",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Real-time health status of your private offline financial engine.",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Encrypted Transactions:", color = colors.textMuted, fontSize = 12.sp)
                        Text("${transactions.size} records", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tracked Institutions:", color = colors.textMuted, fontSize = 12.sp)
                        Text("${banks.size} banks / wallets", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Bank Accounts:", color = colors.textMuted, fontSize = 12.sp)
                        Text("$totalAccounts accounts", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Automatic SMS Parser:", color = colors.textMuted, fontSize = 12.sp)
                        Text("ACTIVE (Broadcast)", color = colors.emeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Push Notification Interceptor:", color = colors.textMuted, fontSize = 12.sp)
                        Text("ACTIVE (Service)", color = colors.emeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Architecture & DB:", color = colors.textMuted, fontSize = 12.sp)
                        Text("Room SQLite v1.0", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Data Storage Mode:", color = colors.textMuted, fontSize = 12.sp)
                        Text("100% On-Device", color = colors.emeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDiagnosticsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── EDIT PROFILE DIALOG ────────────────────────────────────────────────────
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.emeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Profile",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Update your executive display name, email, and photo.",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = colors.textMuted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated,
                            focusedBorderColor = colors.emeraldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = colors.textMuted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated,
                            focusedBorderColor = colors.emeraldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Photo Action Buttons inside Edit Dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                profilePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Photo", fontSize = 11.sp)
                        }

                        if (savedProfilePhotoUri != null) {
                            OutlinedButton(
                                onClick = {
                                    SettingsRepository.setProfilePhotoUri(null)
                                    Toast.makeText(context, "Photo removed", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = colors.expense, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", color = colors.expense, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            SettingsRepository.setUserName(editName.trim())
                            SettingsRepository.setUserEmail(editEmail.trim())
                            showEditProfileDialog = false
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    // ── CHANGE PIN MODAL ───────────────────────────────────────────────────────
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

    // ── EXECUTIVE AVATAR STUDIO MODAL ─────────────────────────────────────────
    if (showAvatarStudioModal) {
        ProfilePhotoStudioSheet(
            photoUri = savedProfilePhotoUri,
            userName = savedName,
            currentRing = profileAvatarRing,
            onClose = { showAvatarStudioModal = false },
            onPickGallery = {
                showAvatarStudioModal = false
                profilePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTakePhoto = {
                showAvatarStudioModal = false
                cameraPhotoLauncher.launch(null)
            },
            onRemovePhoto = {
                SettingsRepository.setProfilePhotoUri(null)
                Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
            },
            onSelectRing = { ring ->
                SettingsRepository.setProfileAvatarRing(ring)
            }
        )
    }
}

// ── LUXURY EXECUTIVE CARD COMPONENT ────────────────────────────────────────────

@Composable
private fun ExecutiveProfileCard(
    name: String,
    email: String,
    photoUri: String?,
    avatarRing: String,
    habteId: String,
    onEditClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onCopyId: () -> Unit
) {
    val colors = LocalEthiopianColors.current

    val userInitials = remember(name) {
        name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .take(2).map { it.first().uppercaseChar() }.joinToString("").ifBlank { "H" }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(colors.surfaceElevated, colors.surface)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colors.goldAccent.copy(alpha = 0.6f),
                        colors.emeraldPrimary.copy(alpha = 0.4f),
                        colors.border
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        // Subtle Ethiopian Axumite watermark in background
        AxumiteCrossWatermark(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 30.dp),
            color = colors.goldAccent,
            alpha = 0.04f
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Card Top Row: Ethiopian Tricolor tag + VIP Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tier Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.goldAccent.copy(alpha = 0.14f))
                        .border(1.dp, colors.goldAccent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = colors.goldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "HABTE EXECUTIVE",
                        color = colors.goldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Ethiopian Cultural Tricolor Accent Bar
                EthiopianTricolorBar(
                    modifier = Modifier.width(36.dp),
                    height = 5.dp
                )
            }

            // User Identity & Monogram Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Monogram Avatar / Photo with luxury ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(getAvatarRingBrush(avatarRing))
                        .padding(3.dp)
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = userInitials,
                                color = colors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Mini Camera Studio Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.emeraldPrimary)
                            .border(1.5.dp, colors.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Avatar Studio",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = email.ifBlank { "Fintech Account Active" },
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Account UID Pill with Copy Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface.copy(alpha = 0.7f))
                            .border(0.8.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable { onCopyId() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = habteId,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UID",
                            tint = colors.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Edit Profile Button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── WEALTH MATRIX & SNAPSHOT COMPONENT ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExecutiveFinancialSnapshot(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    savingsRate: Double,
    totalAccounts: Int,
    totalBanks: Int,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val colors = LocalEthiopianColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = colors.emeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Portfolio Intelligence",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Savings efficiency badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.emeraldPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Savings: ${String.format("%.1f", savingsRate)}%",
                        color = colors.emeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Period Filter Chips
            val overviewOptions = listOf("Monthly", "Yearly", "Weekly", "6 Months", "All Time")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                overviewOptions.forEach { option ->
                    val isSelected = selectedPeriod == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPeriodSelected(option) },
                        label = { Text(option, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.emeraldPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = colors.border,
                            selectedBorderColor = colors.emeraldPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // High-Density 4-Metric Grid (All interactive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Portfolio",
                    value = "ETB ${Data.formatBalance(totalBalance)}",
                    valueColor = colors.textPrimary,
                    icon = Icons.Default.AccountBalance,
                    iconTint = colors.goldAccent,
                    onClick = { onNavigate("home") }
                )
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Period Inflow",
                    value = "+${Data.formatBalance(totalIncome)}",
                    valueColor = colors.income,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = colors.income,
                    onClick = { onNavigate("transactions") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Period Outflow",
                    value = "-${Data.formatBalance(totalExpense)}",
                    valueColor = colors.expense,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconTint = colors.expense,
                    onClick = { onNavigate("transactions") }
                )
                ExecutiveMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Bank Accounts",
                    value = "$totalAccounts in $totalBanks banks",
                    valueColor = colors.textSecondary,
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconTint = colors.emeraldPrimary,
                    onClick = { onNavigate("transactions") }
                )
            }
        }
    }
}

@Composable
private fun ExecutiveMetricCard(
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalEthiopianColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.border, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── THEME & AESTHETICS STUDIO COMPONENT ─────────────────────────────────────────

@Composable
private fun ExecutiveThemeStudio(
    currentTheme: String,
    onSelectTheme: (String) -> Unit
) {
    val colors = LocalEthiopianColors.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = colors.emeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aesthetics & Theme Studio",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExecutiveThemeCard(
                    title = "Emerald Dark",
                    subtitle = "Executive Night",
                    previewBg = Color(0xFF0B0B0B),
                    previewCard = Color(0xFF151515),
                    previewAccent = Color(0xFF00C853),
                    isSelected = currentTheme == "Dark" || currentTheme == "Emerald Dark",
                    onClick = { onSelectTheme("Dark") },
                    modifier = Modifier.weight(1f)
                )
                ExecutiveThemeCard(
                    title = "Clean White",
                    subtitle = "Minimalist Light",
                    previewBg = Color(0xFFF8F9FA),
                    previewCard = Color(0xFFFFFFFF),
                    previewAccent = Color(0xFF008937),
                    isSelected = currentTheme == "Light" || currentTheme == "Minimalist White",
                    onClick = { onSelectTheme("Light") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExecutiveThemeCard(
                    title = "Binance Pro",
                    subtitle = "Midnight Charcoal",
                    previewBg = Color(0xFF0B0E11),
                    previewCard = Color(0xFF181A20),
                    previewAccent = Color(0xFFF0B90B),
                    isSelected = currentTheme == "Binance" || currentTheme == "Binance Pro",
                    onClick = { onSelectTheme("Binance") },
                    modifier = Modifier.weight(1f)
                )
                ExecutiveThemeCard(
                    title = "Addis Gold",
                    subtitle = "Ethiopian Luxury",
                    previewBg = Color(0xFF0F0D0A),
                    previewCard = Color(0xFF1E1812),
                    previewAccent = Color(0xFFD4A017),
                    isSelected = currentTheme == "Addis Gold" || currentTheme == "Gold",
                    onClick = { onSelectTheme("Addis Gold") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExecutiveThemeCard(
    title: String,
    subtitle: String,
    previewBg: Color,
    previewCard: Color,
    previewAccent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEthiopianColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) previewAccent else colors.border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            // Theme Mini-Canvas Swatch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewBg)
                    .border(0.8.dp, Color(0x33888888), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(previewAccent)
                )
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(previewCard)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = previewAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = if (isSelected) previewAccent else colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
    }
}

// ── REUSABLE SETTINGS SECTION & ROWS ───────────────────────────────────────────

@Composable
private fun ExecutiveSettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalEthiopianColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            content()
        }
    }
}

@Composable
private fun ExecutiveSettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalEthiopianColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) colors.emeraldPrimary else colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.emeraldPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.border
            )
        )
    }
}

// ── EXECUTIVE AVATAR RING HELPER ──────────────────────────────────────────────
private fun getAvatarRingBrush(ringKey: String): Brush {
    return when (ringKey) {
        "Gold" -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFEF08A), Color(0xFFB45309)))
        "Sapphire" -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF93C5FD), Color(0xFF1E3A8A)))
        "Habesha" -> Brush.linearGradient(listOf(Color(0xFF009B4D), Color(0xFFFFFE00), Color(0xFFFF0000)))
        "Obsidian" -> Brush.linearGradient(listOf(Color(0xFF4B5563), Color(0xFF9CA3AF), Color(0xFF111827)))
        else -> Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF6EE7B7), Color(0xFF064E3B))) // Emerald default
    }
}

// ── EXECUTIVE AVATAR STUDIO BOTTOM SHEET ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePhotoStudioSheet(
    photoUri: String?,
    userName: String,
    currentRing: String,
    onClose: () -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSelectRing: (String) -> Unit
) {
    val colors = LocalEthiopianColors.current
    val haptic = LocalHapticFeedback.current
    val userInitials = remember(userName) {
        userName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .take(2).map { it.first().uppercaseChar() }.joinToString("").ifBlank { "H" }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.emeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = colors.emeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Executive Avatar Studio",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personalize your photo & luxury border ring",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Avatar Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(getAvatarRingBrush(currentRing))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Executive Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = userInitials,
                                    color = colors.textPrimary,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = userName,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Executive Account Active",
                        color = colors.emeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Border Ring Accent Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "LUXURY BORDER RING STYLE",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                val ringPresets = listOf(
                    "Emerald" to "Emerald",
                    "Gold" to "Gold",
                    "Sapphire" to "Sapphire",
                    "Habesha" to "Habesha",
                    "Obsidian" to "Obsidian"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ringPresets.forEach { (key, label) ->
                        val isSelected = currentRing == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.emeraldPrimary.copy(alpha = 0.15f) else colors.surfaceElevated)
                                .border(
                                    1.2.dp,
                                    if (isSelected) colors.emeraldPrimary else colors.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRing(key)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarRingBrush(key))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (isSelected) colors.textPrimary else colors.textMuted,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTakePhoto()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPickGallery()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Gallery", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (photoUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRemovePhoto()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = colors.expense, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove Profile Photo", color = colors.expense, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}



