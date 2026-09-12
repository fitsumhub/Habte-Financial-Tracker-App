package com.mobile.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import com.mobile.data.Transaction
import com.mobile.ui.theme.LocalEthiopianColors
import java.text.SimpleDateFormat
import java.util.*

private val DEFAULT_BUDGETS = mapOf("Daily" to 500.0, "Weekly" to 3500.0, "Monthly" to 15000.0, "Yearly" to 180000.0)

@Composable
fun BudgetScreen() {
    val colors = LocalEthiopianColors.current
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val transactions by FinanceRepository.transactions.collectAsState()
    val budgets by FinanceRepository.budgets.collectAsState()
    var selectedPeriod by remember { mutableStateOf("Monthly") }
    var showEditBudget by remember { mutableStateOf(false) }
    val periods = listOf("Daily", "Weekly", "Monthly", "Yearly")

    val now = Calendar.getInstance()

    val filteredTransactions = remember(transactions, selectedPeriod) {
        transactions.filter { tx ->
            if (tx.type != "debit") return@filter false
            val cal = com.mobile.data.parseTransactionDate(tx.date) ?: return@filter false
            when (selectedPeriod) {
                "Daily" -> com.mobile.data.isSameCalendarDay(cal, now)
                "Weekly" -> com.mobile.data.isWithinRollingWindow(now, cal, 7 * 24 * 60 * 60 * 1000L)
                "Monthly" -> com.mobile.data.isSameCalendarMonth(cal, now)
                "Yearly" -> com.mobile.data.isSameCalendarYear(cal, now)
                else -> true
            }
        }
    }

    val totalExpense = filteredTransactions.sumOf { it.amount }

    val currentBudget = remember(budgets, selectedPeriod) {
        budgets.find { it.period == selectedPeriod && it.category == null }?.limit
            ?: DEFAULT_BUDGETS[selectedPeriod]
            ?: 10000.0
    }
    val budgetProgress = com.mobile.data.budgetProgressFraction(totalExpense, currentBudget)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 12.dp)) {
            Text("Budget & Expenses", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Period Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) colors.emeraldPrimary else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period,
                        color = if (isSelected) onPrimaryColor else colors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            // Main Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL SPENT ($selectedPeriod)",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = Data.formatBalance(totalExpense),
                            color = if (budgetProgress > 1f) colors.expense else colors.textPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = " ETB",
                            color = colors.textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Budget Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Budget limit", color = colors.textMuted, fontSize = 12.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showEditBudget = true }
                        ) {
                            Text(
                                "${(budgetProgress * 100).toInt()}% of ${Data.formatBalance(currentBudget)}",
                                color = if (budgetProgress > 0.9) colors.expense else colors.emeraldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit budget limit",
                                tint = colors.emeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(budgetProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    if (budgetProgress > 1f) colors.expense
                                    else colors.emeraldPrimary
                                )
                        )
                    }
                }
            }

            // Category Breakdown
            Text(
                "Spending by Category",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )

            val categoryExpenses = filteredTransactions.groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            if (categoryExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses found for this period.", color = colors.textMuted, fontSize = 14.sp)
                }
            } else {
                categoryExpenses.forEach { (cat, amount) ->
                    val progress = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                    CategoryProgressRow(category = cat, amount = amount, progress = progress)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showEditBudget) {
        var limitInput by remember { mutableStateOf(currentBudget.toString()) }
        AlertDialog(
            onDismissRequest = { showEditBudget = false },
            title = { Text("Set $selectedPeriod Budget Limit", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Limit (ETB)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated,
                        focusedBorderColor = colors.emeraldPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val limit = limitInput.toDoubleOrNull()
                        if (limit != null && limit > 0) {
                            FinanceRepository.setBudget(
                                period = selectedPeriod,
                                category = null,
                                limit = limit
                            )
                        }
                        showEditBudget = false
                    }
                ) {
                    Text("Save", color = colors.emeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBudget = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun CategoryProgressRow(category: String, amount: Double, progress: Float) {
    val colors = LocalEthiopianColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("ETB ${Data.formatBalance(amount)}", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colors.emeraldPrimary)
                )
            }
        }
    }
}
