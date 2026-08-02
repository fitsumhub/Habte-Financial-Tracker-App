package com.mobile.ui.screens

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.mobile.data.Bank
import com.mobile.data.CalendarSystem
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import com.mobile.data.NetWorthPoint
import com.mobile.data.SettingsRepository
import com.mobile.data.monthlyNetWorthSeries
import com.mobile.ui.components.BankLogo
import java.util.Calendar

private val NetWorthAccent = Color(0xFF7C3AED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(onBack: () -> Unit) {
    val banks by FinanceRepository.banks.collectAsState()
    val transactions by FinanceRepository.transactions.collectAsState()
    val calendarSystemSetting by SettingsRepository.calendarSystem.collectAsState()
    val calendarSystem = remember(calendarSystemSetting) {
        if (calendarSystemSetting == "Ethiopian") CalendarSystem.ETHIOPIAN else CalendarSystem.GREGORIAN
    }

    val totalNetWorth = remember(banks) { Data.getTotalBalance(banks) }
    val activeBanks = remember(banks) {
        banks.filter { it.accounts.isNotEmpty() }.sortedByDescending { Data.getBankTotal(it) }
    }
    val trend = remember(transactions, calendarSystem) {
        monthlyNetWorthSeries(transactions, Calendar.getInstance(), calendarSystem, monthCount = 6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Net Worth",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(colors = listOf(NetWorthAccent, NetWorthAccent.copy(alpha = 0.82f)))
                    )
                    .padding(20.dp)
            ) {
                Text("Total Net Worth", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${Data.formatBalance(totalNetWorth)} ETB",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activeBanks.isEmpty()) "No accounts yet" else "Across ${activeBanks.size} institution${if (activeBanks.size == 1) "" else "s"}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "6-Month Trend",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                NetWorthTrendChart(points = trend)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By Institution",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (activeBanks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No accounts yet. Balances appear here once transactions sync in.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeBanks.forEach { bank ->
                        val bankTotal = Data.getBankTotal(bank)
                        val share = if (totalNetWorth > 0) (bankTotal / totalNetWorth).toFloat().coerceIn(0f, 1f) else 0f
                        InstitutionRow(bank = bank, total = bankTotal, share = share)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstitutionRow(bank: Bank, total: Double, share: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(colors = listOf(Color(parseColor(bank.colorFrom)), Color(parseColor(bank.colorTo))))
                    )
            ) {
                BankLogo(shortName = bank.logoText, size = 40.dp, fontSize = 10.sp, resId = bank.logoResId, domain = bank.domain)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bank.shortName, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${bank.accounts.size} account${if (bank.accounts.size == 1) "" else "s"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Data.formatBalance(total)} ETB", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${(share * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = share.coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(parseColor(bank.colorFrom)))
            )
        }
    }
}

@Composable
private fun NetWorthTrendChart(points: List<NetWorthPoint>) {
    if (points.isEmpty() || points.all { it.netWorth == 0.0 }) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Not enough transaction history yet to chart a trend.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    val maxVal = (points.maxOfOrNull { it.netWorth } ?: 0.0).coerceAtLeast(1.0)
    val minVal = (points.minOfOrNull { it.netWorth } ?: 0.0).coerceAtMost(0.0)
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    val scrollState = rememberScrollState()

    // 6 months of bars are wider than most screens, so this opens scrolled to the oldest
    // month by default — jump straight to the most recent one instead, since that's the
    // point a user opening a net worth trend actually cares about first.
    LaunchedEffect(points) {
        if (scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        points.forEach { point ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Text(
                    text = Data.formatBalance(point.netWorth, short = true),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.height(90.dp), verticalAlignment = Alignment.Bottom) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight(fraction = (((point.netWorth - minVal) / range).toFloat()).coerceIn(0.06f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(NetWorthAccent)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = point.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
