package com.mobile.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import com.mobile.data.PaymentReminder
import com.mobile.data.PaymentReminderRepository
import com.mobile.data.SettingsRepository
import com.mobile.data.Transaction
import com.mobile.data.findPotentialDuplicates
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Tool(
    val icon: ImageVector,
    val label: String,
    val sub: String,
    val color: Color
)

private data class ToolSection(
    val title: String,
    val tools: List<Tool>
)

// Grouped into sections (rather than one flat grid) so the workspace reads like a
// proper product surface — payments, reports, exchange, and security each get their
// own labelled shelf instead of nine same-weight tiles in a row.
private val TOOL_SECTIONS = listOf(
    ToolSection(
        title = "Payments",
        tools = listOf(
            Tool(Icons.Default.NotificationsActive, "Payment Reminders", "Bills & subscriptions", Color(0xFF8B5CF6)),
            Tool(Icons.Default.Calculate, "Loan Calc", "EMI calculator", Color(0xFFF59E0B)),
            Tool(Icons.Default.Receipt, "Tax Calc", "Income tax estimator", Color(0xFFEF4444))
        )
    ),
    ToolSection(
        title = "Reports & Data",
        tools = listOf(
            Tool(Icons.Default.TrendingUp, "Transfer History", "View transactions", Color(0xFF6366F1)),
            Tool(Icons.Default.BarChart, "Statement", "Download statements", Color(0xFF0EA5E9)),
            Tool(Icons.Default.FileDownload, "Export Data", "CSV or PDF export", Color(0xFFEC4899)),
            Tool(Icons.Default.AccountBalanceWallet, "Net Worth", "Balances across accounts", Color(0xFF7C3AED)),
            Tool(Icons.Default.ContentCopy, "Duplicate Check", "Find repeated entries", Color(0xFFF43F5E)),
            Tool(Icons.Default.EmojiEvents, "Certificates", "Achievement certificates", Color(0xFFFBBF24))
        )
    ),
    ToolSection(
        title = "Exchange",
        tools = listOf(
            Tool(Icons.Default.CurrencyExchange, "Converter", "Exchange rates", Color(0xFF10B981))
        )
    ),
    ToolSection(
        title = "Security & Alerts",
        tools = listOf(
            Tool(Icons.Default.Notifications, "Alerts", "Balance alerts", Color(0xFFF97316)),
            Tool(Icons.Default.Shield, "Security", "2FA and PIN", Color(0xFF14B8A6))
        )
    )
)

// Keeps only digits and, when allowed, a single decimal point — `keyboardType = Number`
// is just a soft-keyboard hint and does not by itself block pasted/typed letters.
private fun sanitizeNumericInput(raw: String, allowDecimal: Boolean): String {
    val sb = StringBuilder()
    var dotSeen = false
    for (c in raw) {
        when {
            c.isDigit() -> sb.append(c)
            c == '.' && allowDecimal && !dotSeen -> { sb.append(c); dotSeen = true }
        }
    }
    return sb.toString()
}

// Named distinctly from TransactionHistoryScreen's own `timestampMillis()` — private
// top-level declarations still collide by name across files in this package (see memory).
private fun Transaction.statementTimestampMillis(): Long =
    runCatching {
        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).parse("$date $time")?.time
    }.getOrNull() ?: 0L

// Quotes/escapes a field for CSV so a comma or quote inside a counterparty name,
// reason, or category can never silently shift the rest of that row into the wrong columns.
private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

private fun buildStatementCsv(transactions: List<Transaction>, accountHolder: String): String {
    if (transactions.isEmpty()) {
        return "Habte Financial Tracker - Account Statement\nNo transactions to report.\n"
    }

    // Bank statements read oldest-to-newest with a running balance, unlike the
    // newest-first order the app uses elsewhere for browsing.
    val chronological = transactions.sortedBy { it.statementTimestampMillis() }
    val generatedAt = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())
    val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount }
    val totalDebit = transactions.filter { it.type == "debit" }.sumOf { it.amount }

    return buildString {
        appendLine("Habte Financial Tracker - Account Statement")
        appendLine("Account Holder,${csvField(accountHolder)}")
        appendLine("Statement Period,${csvField("${chronological.first().date} - ${chronological.last().date}")}")
        appendLine("Generated,$generatedAt")
        appendLine("Currency,ETB")
        appendLine("Transactions Covered,${transactions.size}")
        appendLine("Total Credit (ETB),${Data.formatBalance(totalCredit)}")
        appendLine("Total Debit (ETB),${Data.formatBalance(totalDebit)}")
        appendLine("Net (ETB),${Data.formatBalance(totalCredit - totalDebit)}")
        appendLine()
        appendLine("Date,Time,Description,Reference,Amount (ETB),Type,Category,Bank,Balance (ETB)")
        chronological.forEach { t ->
            val signedAmount = "${if (t.type == "credit") "+" else "-"}${Data.formatBalance(t.amount)}"
            val balanceText = t.balance?.let { Data.formatBalance(it) } ?: ""
            appendLine(
                listOf(
                    t.date,
                    t.time,
                    csvField(t.title),
                    csvField(t.reason),
                    signedAmount,
                    t.type.replaceFirstChar { it.uppercase() },
                    csvField(t.category),
                    csvField(t.bankShortName),
                    balanceText
                ).joinToString(",")
            )
        }
        appendLine()
        appendLine("This is a system-generated statement produced by Habte from your device's SMS records. It is not an official bank document.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val transactions by FinanceRepository.transactions.collectAsState()
    val userName by SettingsRepository.userName.collectAsState()
    val reminders by PaymentReminderRepository.reminders.collectAsState()
    var showCurrencyConverter by remember { mutableStateOf(false) }
    var showLoanCalc by remember { mutableStateOf(false) }
    var showTaxCalc by remember { mutableStateOf(false) }
    var showDuplicateCheck by remember { mutableStateOf(false) }

    val upcomingReminder = remember(reminders) {
        reminders.filter { it.enabled }.minByOrNull { it.dueDateMillis }
    }

    // File creation launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(buildStatementCsv(transactions, userName.ifBlank { "Account Holder" }))
                    }
                }
                Toast.makeText(context, "Statement saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun initiateDownload() {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions found", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "Habte_Statement_${System.currentTimeMillis()}.csv"
        createDocumentLauncher.launch(fileName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Workspace",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your financial operations with precision.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(bottom = 100.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UpcomingPaymentCard(
                    reminder = upcomingReminder,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate("payment_reminders")
                    }
                )
            }

            TOOL_SECTIONS.forEach { section ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ToolSectionHeader(title = section.title)
                }
                items(section.tools) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (tool.label) {
                                // The full Transactions screen already lists every transaction
                                // (virtualized) with search-by-name/amount and sorting — reuse it
                                // instead of the old 10-row preview sheet this tile used to open.
                                "Transfer History" -> onNavigate("transaction_history")
                                "Statement" -> initiateDownload()
                                "Converter" -> showCurrencyConverter = true
                                "Loan Calc" -> showLoanCalc = true
                                "Tax Calc" -> showTaxCalc = true
                                "Payment Reminders" -> onNavigate("payment_reminders")
                                "Alerts" -> onNavigate("alerts")
                                "Security" -> onNavigate("security")
                                "Export Data" -> onNavigate("export_data")
                                "Net Worth" -> onNavigate("net_worth")
                                "Duplicate Check" -> showDuplicateCheck = true
                                "Certificates" -> onNavigate("achievement_certificates")
                            }
                        }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(8.dp))
                QuickActionBanner {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate("support")
                }
            }
        }
    }

    if (showCurrencyConverter) CurrencyConverterModal(onClose = { showCurrencyConverter = false })
    if (showLoanCalc) LoanCalcModal(onClose = { showLoanCalc = false })
    if (showTaxCalc) TaxCalcModal(onClose = { showTaxCalc = false })
    if (showDuplicateCheck) DuplicateCheckModal(onClose = { showDuplicateCheck = false })
}

@Composable
private fun ToolSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

// Hero card at the top of the workspace surfacing the soonest enabled payment
// reminder (see PaymentRemindersScreen) — turns Tools from a static launcher grid
// into something that reflects the user's actual upcoming obligations.
@Composable
private fun UpcomingPaymentCard(reminder: PaymentReminder?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f))
                )
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (reminder == null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("No upcoming payments", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Tap to set a reminder so you never miss a bill", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 16.sp)
            }
        } else {
            val daysLeft = ((reminder.dueDateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
            val option = reminderCategoryOption(reminder.category)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(option.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        daysLeft <= 0 -> "${reminder.label} is due today"
                        else -> "${reminder.label} due in $daysLeft day${if (daysLeft == 1) "" else "s"}"
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (reminder.amount > 0) "ETB ${Data.formatBalance(reminder.amount)}" else "Upcoming payment reminder",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
    }
}

private val ConverterAccent = Color(0xFF059669)
private val LoanAccent = Color(0xFFD97706)
private val TaxAccent = Color(0xFFDC2626)
private val CurrencyPresets = listOf("USD" to "113.5", "EUR" to "122.3", "GBP" to "144.1")

// Shared styling for every numeric input across the calculator sheets — replaces
// six near-identical OutlinedTextField blocks that only differed by accent color.
@Composable
private fun ToolOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterModal(onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var isEtbToForeign by remember { mutableStateOf(false) }

    val a = amount.toDoubleOrNull() ?: 0.0
    val r = rate.toDoubleOrNull() ?: 0.0
    val result = if (r > 0) {
        if (isEtbToForeign) a / r else a * r
    } else 0.0

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
                "Currency Converter",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Rates are entered manually — not live market data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = if (isEtbToForeign) "ETB to Foreign" else "Foreign to ETB",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isEtbToForeign = !isEtbToForeign
                }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap direction", tint = ConverterAccent)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolOutlinedField(
                    value = amount,
                    onValueChange = { amount = sanitizeNumericInput(it, allowDecimal = true) },
                    placeholder = if (isEtbToForeign) "Amount (ETB)" else "Amount (Foreign)",
                    accentColor = ConverterAccent,
                    modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                )
                ToolOutlinedField(
                    value = rate,
                    onValueChange = { rate = sanitizeNumericInput(it, allowDecimal = true) },
                    placeholder = "Exchange Rate",
                    accentColor = ConverterAccent,
                    modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                )
            }

            // Quick rates
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CurrencyPresets.size) { index ->
                    val preset = CurrencyPresets[index]
                    val isSelected = rate == preset.second
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ConverterAccent else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                rate = preset.second
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.first,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, ConverterAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Converted Amount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Data.formatBalance(result)} ${if (isEtbToForeign) "Foreign" else "ETB"}",
                        color = ConverterAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanCalcModal(onClose: () -> Unit) {
    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }

    val p = principal.toDoubleOrNull() ?: 0.0
    val rAnnual = rate.toDoubleOrNull() ?: 0.0
    val r = rAnnual / 12 / 100
    val n = months.toDoubleOrNull() ?: 1.0

    val emi = if (p > 0 && n > 0) {
        if (r > 0) {
            (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1)
        } else {
            p / n
        }
    } else 0.0

    val totalPayment = emi * n
    val totalInterest = Math.max(0.0, totalPayment - p)

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
                "Loan Calculator",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            ToolOutlinedField(
                value = principal,
                onValueChange = { principal = sanitizeNumericInput(it, allowDecimal = true) },
                placeholder = "Loan Amount (ETB)",
                accentColor = LoanAccent,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolOutlinedField(
                    value = rate,
                    onValueChange = { rate = sanitizeNumericInput(it, allowDecimal = true) },
                    placeholder = "Annual Rate (%)",
                    accentColor = LoanAccent,
                    modifier = Modifier.weight(1f)
                )
                ToolOutlinedField(
                    value = months,
                    onValueChange = { months = sanitizeNumericInput(it, allowDecimal = false) },
                    placeholder = "Months",
                    accentColor = LoanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, LoanAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Monthly EMI", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("${Data.formatBalance(emi)} ETB", color = LoanAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Interest", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("${Data.formatBalance(totalInterest)} ETB", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Payment", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${Data.formatBalance(totalPayment)} ETB", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCalcModal(onClose: () -> Unit) {
    var salary by remember { mutableStateOf("") }

    val s = salary.toDoubleOrNull() ?: 0.0
    // Rough estimation based on Ethiopian tax brackets
    val estimatedTax = when {
        s <= 600 -> 0.0
        s <= 1650 -> (s * 0.1) - 60
        s <= 3200 -> (s * 0.15) - 142.5
        s <= 5250 -> (s * 0.2) - 302.5
        s <= 7800 -> (s * 0.25) - 565
        s <= 10900 -> (s * 0.3) - 955
        else -> (s * 0.35) - 1500
    }
    val tax = Math.max(0.0, estimatedTax)
    val netSalary = Math.max(0.0, s - tax)

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
                "Income Tax Estimator",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            ToolOutlinedField(
                value = salary,
                onValueChange = { salary = sanitizeNumericInput(it, allowDecimal = true) },
                placeholder = "Gross Monthly Salary (ETB)",
                accentColor = TaxAccent,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, TaxAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross Salary", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("${Data.formatBalance(s)} ETB", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Tax", color = TaxAccent, fontSize = 14.sp)
                    Text("- ${Data.formatBalance(tax)} ETB", color = TaxAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Salary", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${Data.formatBalance(netSalary)} ETB", color = ConverterAccent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private val DuplicateAccent = Color(0xFFF43F5E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicateCheckModal(onClose: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val transactions by FinanceRepository.transactions.collectAsState()
    val duplicateGroups = remember(transactions) { findPotentialDuplicates(transactions) }

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
                "Duplicate Check",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Groups the same bank, amount, and type recorded within a few minutes of each other — usually a resent SMS alert, not a real second transaction.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (duplicateGroups.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No duplicates found", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Your transaction history looks clean.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    duplicateGroups.forEach { group ->
                        DuplicateGroupCard(
                            group = group,
                            onDelete = { tx ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                FinanceRepository.deleteTransaction(tx.id)
                                Toast.makeText(context, "Deleted duplicate", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: List<Transaction>, onDelete: (Transaction) -> Unit) {
    val sorted = remember(group) { group.sortedBy { it.statementTimestampMillis() } }
    val first = sorted.first()
    val dateFormat by com.mobile.data.SettingsRepository.dateFormat.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DuplicateAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DuplicateAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${first.bankShortName} • ${Data.formatBalance(first.amount)} ETB",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text("${sorted.size} entries", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sorted.forEachIndexed { index, t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${com.mobile.data.formatDisplayDate(t.date, dateFormat)} • ${t.time}${if (index == 0) " · oldest" else ""}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { onDelete(t) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete this entry", tint = DuplicateAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(tool: Tool, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "toolScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(tool.color.copy(alpha = 0.3f), MaterialTheme.colorScheme.outline)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(tool.color.copy(alpha = 0.2f), tool.color.copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = tool.color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tool.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tool.sub,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionBanner(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(150),
        label = "bannerScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Need Assistance?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Contact our 24/7 support team",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = "Support",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
