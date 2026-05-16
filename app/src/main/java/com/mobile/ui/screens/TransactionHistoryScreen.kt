package com.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val transactions by FinanceRepository.transactions.collectAsState()
    var selectedTransaction by remember { mutableStateOf<com.mobile.data.Transaction?>(null) }

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
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Transaction History",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No transactions found yet",
                            color = Color(0xFF7B84A8),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Sync your accounts to see transaction history",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                transactions.forEach { transaction ->
                    HistoryTransactionItem(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        com.mobile.ui.components.TransactionDetailSheet(
            transaction = selectedTransaction,
            onClose = { selectedTransaction = null }
        )
    }
}

@Composable
private fun HistoryTransactionItem(transaction: com.mobile.data.Transaction, onClick: () -> Unit = {}) {
    val categoryIcon = when (transaction.category) {
        "Bills & Utilities" -> Icons.Default.List
        "Food & Dining" -> Icons.Default.ShoppingCart
        "Transfers" -> Icons.Default.Sync
        "Income" -> Icons.Default.KeyboardArrowUp
        else -> Icons.Default.Info
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == "credit") "+" else ""}${Data.formatBalance(transaction.amount)}",
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