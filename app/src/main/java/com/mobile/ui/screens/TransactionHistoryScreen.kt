package com.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mobile.data.Transaction
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Locale

// BUG FIX: Screen still hardcoded the old dark palette; route through
// MaterialTheme.colorScheme tokens to match the light corporate theme.
private val IncomeColor = Color(0xFF059669)
private val ExpenseColor = Color(0xFFDC2626)

private enum class TxSortOption(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    AMOUNT_HIGH("Amount: High to low"),
    AMOUNT_LOW("Amount: Low to high")
}

private val fullDateTimeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

private fun Transaction.timestampMillis(): Long =
    runCatching { fullDateTimeFormat.parse("$date $time")?.time }.getOrNull() ?: 0L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(onBack: () -> Unit) {
    val transactions by FinanceRepository.transactions.collectAsState()
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }
    val selectedTransaction = remember(transactions, selectedTransactionId) {
        transactions.find { it.id == selectedTransactionId }
    }

    var sortOption by remember { mutableStateOf(TxSortOption.NEWEST) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sortedTransactions = remember(transactions, sortOption) {
        when (sortOption) {
            TxSortOption.NEWEST -> transactions.sortedByDescending { it.timestampMillis() }
            TxSortOption.OLDEST -> transactions.sortedBy { it.timestampMillis() }
            TxSortOption.AMOUNT_HIGH -> transactions.sortedByDescending { it.amount }
            TxSortOption.AMOUNT_LOW -> transactions.sortedBy { it.amount }
        }
    }
    val filteredTransactions = remember(sortedTransactions, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            sortedTransactions
        } else {
            // Amount search matches on the formatted number with separators stripped, so
            // typing "1500" finds a transaction displayed as "1,500.00" or "-1,500.00".
            val normalizedAmountQuery = query.replace(",", "").removePrefix("+").removePrefix("-")
            sortedTransactions.filter { tx ->
                tx.title.contains(query, ignoreCase = true) ||
                    tx.bankShortName.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.reason.contains(query, ignoreCase = true) ||
                    (normalizedAmountQuery.isNotEmpty() &&
                        Data.formatBalance(tx.amount).replace(",", "").contains(normalizedAmountQuery))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "Transactions",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${transactions.size}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                showSearch = !showSearch
                if (!showSearch) searchQuery = ""
            }) {
                Icon(
                    imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (showSearch) "Close search" else "Search transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sort by",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    TxSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortOption = option
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search by name, amount, category, bank…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Scrollable content — LazyColumn so only on-screen rows are composed/measured,
        // since this list can grow to the size of the user's entire SMS history.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (filteredTransactions.isEmpty()) {
                item {
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (transactions.isEmpty()) "No transactions found yet" else "No matches for \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (transactions.isEmpty()) "Sync your accounts to see transaction history" else "Try a different name, category, or bank",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { transaction ->
                    HistoryTransactionItem(
                        transaction = transaction,
                        onClick = { selectedTransactionId = transaction.id }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        com.mobile.ui.components.TransactionDetailSheet(
            transaction = selectedTransaction,
            onClose = { selectedTransactionId = null }
        )
    }
}

@Composable
private fun HistoryTransactionItem(transaction: Transaction, onClick: () -> Unit = {}) {
    val categoryIcon = when (transaction.category) {
        "Bills", "Bills & Utilities", "Rent" -> Icons.Default.List
        "Food", "Food & Dining" -> Icons.Default.ShoppingCart
        "Transfer", "Transfers", "Lend" -> Icons.Default.Sync
        "Income", "Salary" -> Icons.Default.KeyboardArrowUp
        "Shopping", "Cosmetics" -> Icons.Default.ShoppingCart
        else -> Icons.Default.Category
    }
    val categoryLabel = if (transaction.category == "Other") "Uncategorized" else transaction.category
    val bankFullName = remember(transaction.bankShortName) {
        Data.PRESET_BANKS.find { it.shortName == transaction.bankShortName }?.name ?: transaction.bankShortName
    }
    val directionLabel = if (transaction.type == "credit") "from" else "to"
    val amountColor = if (transaction.type == "credit") IncomeColor else ExpenseColor
    val dateFormat by com.mobile.data.SettingsRepository.dateFormat.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bankFullName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$directionLabel ${transaction.title}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = categoryLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == "credit") "+" else "-"}ETB ${Data.formatBalance(transaction.amount)}",
                    color = amountColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = com.mobile.data.formatDisplayDate(transaction.date, dateFormat),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (transaction.time.isNotBlank()) {
                    Text(
                        text = transaction.time,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
