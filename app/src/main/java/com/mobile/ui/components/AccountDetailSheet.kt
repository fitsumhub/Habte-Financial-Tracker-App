package com.mobile.ui.components

import android.graphics.Color.parseColor
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
import com.mobile.data.Bank
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailSheet(
    bank: Bank?,
    onClose: () -> Unit,
    onDelete: (Bank) -> Unit
) {
    if (bank == null) return

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0A0F20),
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
                    .background(Color(0xFF1A2240))
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bank.accounts.size} accounts · ${Data.formatBalance(Data.getBankTotal(bank))} ETB",
                        color = Color(0xFF7B84A8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0E1527))
                        .border(1.dp, Color(0xFF1A2240), RoundedCornerShape(18.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF7B84A8),
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
                                Text(
                                    text = "••••${account.accountNumber.takeLast(4)}",
                                    color = Color(0x8CFFFFFF),
                                    fontSize = 12.sp
                                )
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
                color = Color.White,
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

            val haptic = LocalHapticFeedback.current

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
                    containerColor = Color(0xFF1A0A0A),
                    contentColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF3D1515))
            ) {
                Text("Remove Bank Account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
