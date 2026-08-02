package com.mobile.ui.components

import android.graphics.Color.parseColor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Account
import com.mobile.data.Bank
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** True once the user has filled in their real, full account number — bank SMS only ever reveals the last few digits, so a freshly auto-detected account never starts out with one. */
private fun Account.hasRealAccountNumber(): Boolean =
    accountNumber.isNotBlank() && accountNumber != "Unknown" && !accountNumber.startsWith("••••")

private fun formatAccountNumberForDisplay(accountNumber: String): String =
    accountNumber.chunked(4).joinToString(" ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailSheet(
    bank: Bank?,
    onClose: () -> Unit,
    onDelete: (Bank) -> Unit
) {
    if (bank == null) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BankLogo(shortName = bank.logoText, size = 42.dp, fontSize = 11.sp, resId = bank.logoResId)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bank.accounts.size} accounts · ${Data.formatBalance(Data.getBankTotal(bank))} ETB",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account list
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                bank.accounts.forEach { account ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(parseColor(bank.colorFrom)).copy(alpha = 0.87f),
                                        Color(parseColor(bank.colorTo)).copy(alpha = 0.87f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.label,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { editingAccount = account }
                                ) {
                                    if (account.hasRealAccountNumber()) {
                                        Text(
                                            text = formatAccountNumberForDisplay(account.accountNumber),
                                            color = Color(0xE6FFFFFF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy account number",
                                            tint = Color(0xCCFFFFFF),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    clipboardManager.setText(AnnotatedString(account.accountNumber))
                                                    Toast.makeText(context, "Account number copied", Toast.LENGTH_SHORT).show()
                                                }
                                        )
                                    } else {
                                        Text(
                                            text = "Tap to add your account number",
                                            color = Color(0xB3FFFFFF),
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit account number",
                                        tint = Color(0x99FFFFFF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Data.formatBalance(account.balance),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ETB",
                                    color = Color(0x8CFFFFFF),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Section
            Text(
                "Customize Card Style",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val colorPresets = listOf(
                Pair("#4338CA", "#1E1B4B"), // Indigo
                Pair("#1E40AF", "#1E3A8A"), // Blue
                Pair("#0891B2", "#164E63"), // Cyan
                Pair("#059669", "#064E3B"), // Emerald
                Pair("#F59E0B", "#B45309"), // Amber
                Pair("#E11D48", "#881337"), // Rose
                Pair("#7C3AED", "#4C1D95"), // Violet
                Pair("#1F2937", "#111827")  // Dark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorPresets.forEach { (from, to) ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(parseColor(from)), Color(parseColor(to))))
                            )
                            .border(
                                if (bank.colorFrom == from) 2.dp else 0.dp,
                                Color.White,
                                CircleShape
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                FinanceRepository.updateBankColors(bank.id, from, to)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Delete Button
            Button(
                onClick = { onDelete(bank) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626).copy(alpha = 0.08f),
                    contentColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.25f))
            ) {
                Text("Remove Bank Account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    editingAccount?.let { account ->
        AccountNumberEditDialog(
            account = account,
            onSave = { newNumber ->
                FinanceRepository.updateAccountNumber(account.id, newNumber)
                editingAccount = null
            },
            onDismiss = { editingAccount = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountNumberEditDialog(
    account: Account,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember(account.id) {
        mutableStateOf(if (account.hasRealAccountNumber()) account.accountNumber else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account Number", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column {
                Text(
                    "Bank SMS only ever shows the last few digits — enter your real, full account number here so you can share it with customers.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() }.take(20) },
                    label = { Text("Account number") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(input) },
                enabled = input.length >= 6
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
