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

@Composable
fun BudgetScreen() {
    val transactions by FinanceRepository.transactions.collectAsState()
    var selectedPeriod by remember { mutableStateOf("Monthly") }
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
                    "Daily" -> {
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    }
                    "Weekly" -> {
                        val diff = now.timeInMillis - cal.timeInMillis
                        diff <= 7 * 24 * 60 * 60 * 1000L
                    }
                    "Monthly" -> {
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                    }
                    "Yearly" -> {
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    }
                    else -> true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    val totalExpense = filteredTransactions.sumOf { it.amount }
    
    // Mock budget for progress bars
    val budgetMap = mapOf("Daily" to 500.0, "Weekly" to 3500.0, "Monthly" to 15000.0, "Yearly" to 180000.0)
    val currentBudget = budgetMap[selectedPeriod] ?: 10000.0
    val budgetProgress = (totalExpense / currentBudget).coerceIn(0.0, 1.0).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070912))
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 12.dp)) {
            Text("Budget & Expenses", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Period Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0E1527))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period,
                        color = if (isSelected) Color.White else Color(0xFF64748B),
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
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF070912))
                        )
                    )
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL SPENT ($selectedPeriod)",
                        color = Color(0xFF818CF8),
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
                            color = Color(0xFF64748B),
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
                        Text("Budget", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(
                            "${(budgetProgress * 100).toInt()}% of ${Data.formatBalance(currentBudget)}",
                            color = if (budgetProgress > 0.9) Color(0xFFEF4444) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0E1527))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(budgetProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                    )
                                )
                        )
                    }
                }
            }

            // Category Breakdown
            Text(
                "Spending by Category",
                color = Color.White,
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
                    Text("No expenses found for this period.", color = Color(0xFF3A4268), fontSize = 14.sp)
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
                color = Color.White,
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
}

@Composable
fun CategoryItem(name: String, amount: Double, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0E1527))
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
                            .background(Color(0xFF1A2240)),
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
                        Icon(icon, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(Data.formatBalance(amount), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2240))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color(0xFF818CF8))
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(tx: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1527).copy(alpha = 0.5f))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7F1D1D).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(tx.date, color = Color(0xFF64748B), fontSize = 11.sp)
        }
        Text(
            "-${Data.formatBalance(tx.amount)}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
