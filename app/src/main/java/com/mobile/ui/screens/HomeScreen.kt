package com.mobile.ui.screens

import android.graphics.Color.parseColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import kotlinx.coroutines.launch


import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Account
import com.mobile.data.AccountType
import com.mobile.data.Bank
import com.mobile.data.Data
import com.mobile.ui.components.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.mobile.data.Transaction


import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.mobile.data.FinanceRepository


// Using Tab from com.mobile.ui.components



@Composable
fun HomeScreen(onNavigateToAi: () -> Unit, onNavigateToProfile: () -> Unit, onNavigateToTransactionHistory: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val banks by FinanceRepository.banks.collectAsState()
    val transactions by FinanceRepository.transactions.collectAsState()
    
    val topTabs = remember(banks) {
        listOf(Tab("summary", "Summary"), Tab("today", "Today")) + 
        banks.map { Tab(it.shortName.lowercase(), it.shortName) }
    }

    
    var activeTab by remember { mutableStateOf("summary") }
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAddModal by remember { mutableStateOf(false) }


    val coroutineScope = rememberCoroutineScope()

    // Permission launcher for SMS
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            coroutineScope.launch {
                FinanceRepository.syncHistoricalSms(context)
            }
        } else {
            Toast.makeText(context, "SMS Permission denied. Cannot auto-categorize.", Toast.LENGTH_SHORT).show()
        }
    }


    LaunchedEffect(Unit) {
        val readSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (readSmsGranted) {
            launch { FinanceRepository.syncHistoricalSms(context) }
        } else {
            requestPermissionLauncher.launch(

                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }


    val totalBalance = remember(banks) { Data.getTotalBalance(banks) }
    val totalAccounts = remember(banks) { banks.sumOf { it.accounts.size } }

    val displayedBanks = remember(banks, activeTab) {
        if (activeTab == "summary" || activeTab == "today") {
            banks
        } else {
            banks.filter { it.shortName.lowercase() == activeTab }
        }
    }


    fun handleAddAccount(bankName: String, accountNumber: String) {
        val preset = Data.PRESET_BANKS.find { it.name.equals(bankName, ignoreCase = true) }
        val shortName = preset?.shortName ?: bankName.take(3).uppercase()
        
        // Find latest balance from SMS transactions for this bank
        val initialBalance = transactions
            .filter { it.bankShortName == shortName }
            .mapNotNull { it.balance }
            .firstOrNull() ?: 0.0

        val newBank = Bank(
            id = preset?.id ?: (bankName.lowercase().replace(" ", "-") + System.currentTimeMillis()),
            name = preset?.name ?: bankName,
            shortName = shortName,
            colorFrom = preset?.colorFrom ?: "#4338CA",
            colorTo = preset?.colorTo ?: "#1E1B4B",
            logoText = preset?.logoText ?: bankName.take(3).uppercase(),
            logoResId = preset?.logoResId,
            accounts = listOf(
                Account(
                    id = "new-" + System.currentTimeMillis(),
                    bankId = preset?.id ?: bankName.lowercase(),
                    accountNumber = accountNumber,
                    label = "Main",
                    balance = initialBalance,
                    currency = "ETB",
                    type = AccountType.SAVINGS
                )
            )
        )
        FinanceRepository.addBank(newBank)
        Toast.makeText(context, "${newBank.name} Added with auto-synced balance", Toast.LENGTH_SHORT).show()
    }


    // Compute trend data dynamically or fallback to empty
    val trendData = remember(transactions) {
        if (transactions.size >= 2) {
            transactions.take(7).map { it.amount.toFloat() }.reversed()
        } else {
            listOf(0f, 0f, 0f, 0f) // flatline fallback
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070912), Color(0xFF0E1527))
                )
            )
            .statusBarsPadding()

    ) {
        // Header
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Aplushustler",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeaderIconButton(icon = Icons.Default.Notifications) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                }
                
// Profile Avatar Placeholder
                 Box(
                     modifier = Modifier
                         .size(40.dp)
                         .clip(CircleShape)
                         .background(
                             Brush.linearGradient(
                                 listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                             )
                         )
                         .border(1.5.dp, Color(0x33FFFFFF), CircleShape)
                         .clickable { 
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              onNavigateToProfile()
                         },
                     contentAlignment = Alignment.Center
                 ) {
                     Text("AH", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                 }
            }
        }


        // Top Tab Bar
        TopTabBar(tabs = topTabs, activeKey = activeTab, onSelect = { activeTab = it })


        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 100.dp)
        ) {
            BalanceCard(
                totalBalance = totalBalance,
                bankCount = banks.size,
                accountCount = totalAccounts,
                trendData = trendData
            )


            // AI Quick Insights
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81))))
                    .clickable { onNavigateToAi() }
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Habte AI", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val aiSummary = if (transactions.isEmpty()) "Connect your accounts to unlock smart financial insights." 
                                       else "Your spending analysis is being updated..."
                        Text(aiSummary, color = Color(0xB3FFFFFF), fontSize = 12.sp)
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0x66FFFFFF))
                }
            }

            // Bank cards grid — two per row
            val chunkedBanks = displayedBanks.chunked(2)
            chunkedBanks.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEachIndexed { colIndex, bank ->
                        val index = rowIndex * 2 + colIndex
                        key(bank.id) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(500, delayMillis = index * 100)) +
                                        slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = index * 100)),
                                modifier = Modifier.weight(1f)
                            ) {
                                BankCard(
                                    bank = bank,
                                    onPress = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedBank = it 
                                    }
                                )
                            }
                        }
                    }

                    // Fill remaining slot with Add card on last incomplete row
                    if (row.size == 1 && (activeTab == "summary" || activeTab == "today")) {
                        AddAccountCard(
                            onPress = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAddModal = true 
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Show Add card on its own row if grid is full
            if (displayedBanks.size % 2 == 0 && (activeTab == "summary" || activeTab == "today")) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AddAccountCard(
                        onPress = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddModal = true 
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Activities Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Activities",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                 Text(
                     "View All",
                     color = Color(0xFF818CF8),
                     fontSize = 13.sp,
                     fontWeight = FontWeight.Medium,
                     modifier = Modifier.clickable { 
                         haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                         onNavigateToTransactionHistory()
                     }
                 )
            }


            if (transactions.isEmpty()) {
                Text(
                    "No transactions found yet.",
                    color = Color(0xFF7B84A8),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                transactions.take(10).forEach { transaction ->
                    key(transaction.id) {
                        TransactionItem(
                            transaction = transaction,
                            onClick = { selectedTransaction = transaction }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

        }
    }



    AccountDetailSheet(
        bank = selectedBank, 
        onClose = { selectedBank = null },
        onDelete = { bank ->
            FinanceRepository.removeBank(bank.id)
            selectedBank = null
            Toast.makeText(context, "${bank.name} removed", Toast.LENGTH_SHORT).show()
        }
    )

    AddBankModal(
        visible = showAddModal,
        onClose = { showAddModal = false },
        onAdd = { name, acc -> handleAddAccount(name, acc) }
    )

    TransactionDetailSheet(
        transaction = selectedTransaction,
        onClose = { selectedTransaction = null }
    )

}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1527))
            .border(1.dp, Color(0xFF1A2240), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF7B84A8), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit = {}) {

    val categoryIcon = when (transaction.category) {
        "Bills & Utilities" -> androidx.compose.material.icons.Icons.Default.List
        "Food & Dining" -> androidx.compose.material.icons.Icons.Default.ShoppingCart
        "Transfers" -> androidx.compose.material.icons.Icons.Default.Sync
        "Income" -> androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
        else -> androidx.compose.material.icons.Icons.Default.Info
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0E1527))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (transaction.type == "credit") Color(0xFF064E3B).copy(alpha = 0.3f) 
                        else Color(0xFF7F1D1D).copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = if (transaction.type == "credit") Color(0xFF10B981) else Color(0xFFF87171),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.title, 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        transaction.bankShortName, 
                        color = Color(0xFF818CF8), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(" • ", color = Color(0xFF334155), fontSize = 11.sp)
                    Text(transaction.date, color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }

            val autoHide by com.mobile.data.SettingsRepository.autoHideBalances.collectAsState()
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (autoHide) "••••" else "${if (transaction.type == "credit") "+" else ""}${Data.formatBalance(transaction.amount)}",
                    color = if (transaction.type == "credit") Color(0xFF10B981) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ETB",
                    color = Color(0xFF475569),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



