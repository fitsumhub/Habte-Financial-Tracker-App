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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import com.mobile.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

// BUG FIX: Budget screen still used the old dark palette as raw hex literals,
// which read as broken/inconsistent against the app's light corporate theme.
// Route through MaterialTheme.colorScheme tokens (same pattern as AnalyticsScreen)
// so it re-skins correctly and stays visually consistent with the rest of the app.
private val ExpenseColor = Color(0xFFDC2626)

// Fallback limits shown until the user sets their own — once FinanceRepository.budgets
// has an entry for a period, that persisted value takes over.
private val DEFAULT_BUDGETS = mapOf("Daily" to 500.0, "Weekly" to 3500.0, "Monthly" to 15000.0, "Yearly" to 180000.0)

@Composable
fun BudgetScreen() {
    val transactions by FinanceRepository.transactions.collectAsState()
    val budgets by FinanceRepository.budgets.collectAsState()
    var selectedPeriod by remember { mutableStateOf("Monthly") }
    var showEditBudget by remember { mutableStateOf(false) }
    val periods = listOf("Daily", "Weekly", "Monthly", "Yearly")

    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val now = Calendar.getInstance()

    val filteredTransactions = remember(transactions, selectedPeriod) {
        transactions.filter { tx ->
            if (tx.type != "debit") return@filter false
            try {
                val date = sdf.parse(tx.date) ?: return@filter false
                val cal = Calendar.getInstance().apply { time = date }

                when (selectedPeriod) {
                    "Daily" -> com.mobile.data.isSameCalendarDay(cal, now)
                    "Weekly" -> com.mobile.data.isWithinRollingWindow(now, cal, 7 * 24 * 60 * 60 * 1000L)
                    "Monthly" -> com.mobile.data.isSameCalendarMonth(cal, now)
                    "Yearly" -> com.mobile.data.isSameCalendarYear(cal, now)
                    else -> true
                }
            } catch (e: Exception) {
                false
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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 12.dp)) {
            Text("Budget & Expenses", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Period Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
            // Main Summary Card — saturated brand-color hero, so text stays white by design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL SPENT ($selectedPeriod)",
                        color = Color(0xE6FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = Data.formatBalance(totalExpense),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " ETB",
                            color = Color(0xB3FFFFFF),
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
                        Text("Budget", color = Color(0xB3FFFFFF), fontSize = 12.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showEditBudget = true }
                        ) {
                            Text(
                                "${(budgetProgress * 100).toInt()}% of ${Data.formatBalance(currentBudget)}",
                                color = if (budgetProgress > 0.9) Color(0xFFFCA5A5) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit budget limit",
                                tint = Color(0xB3FFFFFF),
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
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(budgetProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Category Breakdown
            Text(
                "Spending by Category",
                color = MaterialTheme.colorScheme.onSurface,
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
                    Text("No expenses found for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                categoryExpenses.forEach { (category, amount) ->
                    val pct = (amount / totalExpense).toFloat()
                    CategoryItem(category, amount, pct)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Expenses List
            Text(
                "Recent Expenses",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            filteredTransactions.take(10).forEach { tx ->
                ExpenseItem(tx)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showEditBudget) {
        EditBudgetDialog(
            period = selectedPeriod,
            currentLimit = currentBudget,
            onDismiss = { showEditBudget = false },
            onSave = { newLimit ->
                FinanceRepository.setBudget(selectedPeriod, null, newLimit)
                showEditBudget = false
            }
        )
    }
}

@Composable
private fun EditBudgetDialog(
    period: String,
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var input by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "") }
    val parsed = input.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set $period Budget", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Limit (ETB)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { if (it > 0) onSave(it) } },
                enabled = parsed != null && parsed > 0
            ) {
                Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun CategoryItem(name: String, amount: Double, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(16.dp)
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when(name) {
                            "Bills & Utilities" -> Icons.Default.Receipt
                            "Food & Dining" -> Icons.Default.Restaurant
                            "Transport" -> Icons.Default.DirectionsCar
                            "Shopping" -> Icons.Default.ShoppingBag
                            "Cash" -> Icons.Default.Payments
                            else -> Icons.Default.Category
                        }
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(Data.formatBalance(amount), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(tx: Transaction) {
    val dateFormat by com.mobile.data.SettingsRepository.dateFormat.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ExpenseColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = ExpenseColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(com.mobile.data.formatDisplayDate(tx.date, dateFormat), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Text(
            "-${Data.formatBalance(tx.amount)}",
            color = ExpenseColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
