package com.mobile.ui.screens

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.*


// Data derived from FinanceRepository


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onNavigateToAi: () -> Unit) {
    val context = LocalContext.current
    val banks by FinanceRepository.banks.collectAsState()
    val transactions by FinanceRepository.transactions.collectAsState()
    var selectedBank by remember { mutableStateOf<com.mobile.data.Bank?>(null) }
    
    val total = Data.getTotalBalance(banks)
    
    // Derived trend data from transactions
    val trendData = remember(transactions) {
        if (transactions.isEmpty()) emptyList() 
        else transactions.take(5).map { it.amount }.reversed()
    }
    val trendMonths = listOf("T1", "T2", "T3", "T4", "T5") // Simplified for now



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070912))
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 12.dp)) {
            Text("Analytics", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            // Net Worth Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))))
                    .padding(22.dp)
            ) {
                Column {
                    Text("NET WORTH", color = Color(0xA6FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(Data.formatBalance(total), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("ETB", color = Color(0xA6FFFFFF), fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("↑ 12.4% this month", color = Color(0xFF6EE7B7), fontSize = 13.sp)
                }
            }

            // Bar Chart
            Text("Growth Trend", color = Color(0xFF7B84A8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0E1527))
                    .border(1.dp, Color(0xFF1A2240), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                if (trendData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                        Text("Sync banks to see growth trend", color = Color(0xFF3A4268), fontSize = 13.sp)
                    }
                } else {
                    val maxVal = trendData.map { kotlin.math.abs(it) }.max().coerceAtLeast(1.0)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        trendData.forEachIndexed { i, value ->
                            val absVal = kotlin.math.abs(value)
                            val barHeight = ((absVal / maxVal) * 110).dp
                            val isLast = i == trendData.lastIndex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isLast) Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF6366F1)))
                                            else Brush.linearGradient(listOf(Color(0xFF1A2240), Color(0xFF0E1527)))
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = trendMonths.getOrElse(i) { "" },
                                    color = if (isLast) Color(0xFF818CF8) else Color(0xFF3A4268),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // By Bank section
            Text("By Bank", color = Color(0xFF7B84A8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
            banks.forEach { bank ->

                val bankTotal = Data.getBankTotal(bank)
                val pct = if (total > 0) (bankTotal / total * 100) else 0.0
                Box(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(parseColor(bank.colorFrom)).copy(alpha = 0.8f),
                                    Color(parseColor(bank.colorTo)).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .clickable { selectedBank = bank }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(bank.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                            // Progress bar
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x26FFFFFF))) {
                                Box(modifier = Modifier.fillMaxWidth(pct.toFloat() / 100f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xB3FFFFFF)))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Data.formatBalance(bankTotal), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", pct)}%", color = Color(0x8CFFFFFF), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            // AI Insights Section
            Row(
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI Insights", color = Color(0xFF818CF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF818CF8)))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                        )
                    )
                    .border(1.dp, Color(0xFF312E81), RoundedCornerShape(22.dp))
                    .clickable { onNavigateToAi() }
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Spending Alert", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (transactions.isEmpty()) "Connect your banks to receive automated spending insights."
                        else "Analysis in progress. Your spending patterns will appear here as more data is synced.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                        )
                    )
                    .border(1.dp, Color(0xFF1E1B4B), RoundedCornerShape(22.dp))
                    .clickable { onNavigateToAi() }
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Savings Forecast", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (transactions.isEmpty()) "We'll project your wealth growth once your transaction history is synced."
                        else "Calculating your projected net worth based on recent cash flow trends...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                }
            }
        }
    }

    // Bank Detail Bottom Sheet
    if (selectedBank != null) {
        val bank = selectedBank!!
        val bankTx = transactions.filter { it.bankShortName == bank.shortName }
        val bankTotal = Data.getBankTotal(bank)
        val income = bankTx.filter { it.type == "credit" }.sumOf { it.amount }
        val expense = bankTx.filter { it.type == "debit" }.sumOf { it.amount }

        ModalBottomSheet(
            onDismissRequest = { selectedBank = null },
            containerColor = Color(0xFF0A0F20),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 48.dp)
            ) {
                Box(
                    modifier = Modifier.width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1A2240))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(bank.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${bank.accounts.size} account(s) • ${bankTx.size} transactions", color = Color(0xFF64748B), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnalyticsStatBox(Modifier.weight(1f), "Balance", Data.formatBalance(bankTotal), Color(0xFF6366F1))
                    AnalyticsStatBox(Modifier.weight(1f), "Income", Data.formatBalance(income), Color(0xFF10B981))
                    AnalyticsStatBox(Modifier.weight(1f), "Expense", Data.formatBalance(expense), Color(0xFFEF4444))
                }

                if (bankTx.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Recent Transactions", color = Color(0xFF7B84A8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    bankTx.take(5).forEach { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(t.title, color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(
                                "${if (t.type == "credit") "+" else ""}${Data.formatBalance(t.amount)}",
                                color = if (t.type == "credit") Color(0xFF10B981) else Color.White,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatBox(modifier: Modifier, label: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(14.dp)
    ) {
        Column {
            Text(label, color = Color(0xFF7B84A8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
