package com.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
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
import com.mobile.data.Transaction
import com.mobile.data.FinanceRepository
import com.mobile.data.SettingsRepository
import com.mobile.data.formatDisplayDate
import com.mobile.ui.theme.LocalEthiopianColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction?,
    onClose: () -> Unit
) {
    if (transaction == null) return

    val colors = LocalEthiopianColors.current
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val dateFormat by SettingsRepository.dateFormat.collectAsState()
    val baseCategories = listOf("Food", "Bills", "Transfer", "Income", "Lend", "Cosmetics", "Transport", "Shopping", "Entertainment", "Other")

    var reasonInput by remember(transaction.id) { mutableStateOf(transaction.reason) }

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
                .padding(top = 14.dp, bottom = 48.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction Header Details
            val categoryIcon = when (transaction.category) {
                "Bills & Utilities" -> Icons.Default.List
                "Food & Dining" -> Icons.Default.ShoppingCart
                "Transfers" -> Icons.Default.Sync
                "Income" -> Icons.Default.KeyboardArrowUp
                else -> Icons.Default.Info
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (transaction.type == "credit") colors.income.copy(alpha = 0.12f)
                            else colors.expense.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = if (transaction.type == "credit") colors.income else colors.expense,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.title,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${transaction.bankShortName} • ${formatDisplayDate(transaction.date, dateFormat)}",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (transaction.reason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            transaction.reason,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (transaction.type == "credit") "+" else ""}${Data.formatBalance(transaction.amount)}",
                        color = if (transaction.type == "credit") colors.income else colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "ETB",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Category",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Category Selection Chips
            val chunkedCategories = baseCategories.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chunkedCategories.forEach { rowCategories ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowCategories.forEach { category ->
                            val isSelected = transaction.category == category
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.emeraldPrimary else colors.surfaceElevated)
                                    .border(1.dp, if (isSelected) colors.emeraldPrimary else colors.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        FinanceRepository.updateTransactionCategory(transaction.id, category)
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    category,
                                    color = if (isSelected) onPrimaryColor else colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowCategories.size < 3) {
                            repeat(3 - rowCategories.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                "Reason",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "An optional note about this transaction — kept separate from its category.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Reason Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text("e.g. Paid rent for July", color = colors.textMuted) },
                    modifier = Modifier.weight(1f),
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
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        FinanceRepository.updateTransactionReason(transaction.id, reasonInput.trim())
                    },
                    enabled = reasonInput.trim() != transaction.reason,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = colors.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
