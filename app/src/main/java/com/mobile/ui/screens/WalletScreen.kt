package com.mobile.ui.screens

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Data
import java.text.DecimalFormat

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import com.mobile.data.Bank
import com.mobile.data.FinanceRepository
import com.mobile.ui.components.AccountDetailSheet


@Composable
fun WalletScreen() {
    val context = LocalContext.current
    val banks by FinanceRepository.banks.collectAsState()
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    
    val allAccounts = banks.flatMap { bank -> bank.accounts.map { Pair(bank, it) } }
    val fmt = DecimalFormat("#,##0.00")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070912))
                .statusBarsPadding()
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 12.dp)) {
                Text("Cards & Accounts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                allAccounts.forEach { (bank, account) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(parseColor(bank.colorFrom)),
                                        Color(parseColor(bank.colorTo))
                                    )
                                )
                            )
                            .clickable { 
                                selectedBank = bank
                            }
                            .padding(24.dp)
                    ) {
                        // Glow
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .offset(x = 100.dp, y = (-60).dp)
                                .clip(RoundedCornerShape(90.dp))
                                .background(Color(0x0FFFFFFF))
                        )
                        Column {
                            // Card top row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bank.shortName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color(0x66FFFFFF),
                                    modifier = Modifier.size(18.dp).rotate(90f)
                                )
                            }

                            // Card number
                            Row(
                                modifier = Modifier.padding(bottom = 30.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("••••  ••••  ••••  ", color = Color(0x80FFFFFF), fontSize = 16.sp, letterSpacing = 3.sp)
                                Text(account.accountNumber.takeLast(4), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            }

                            // Card bottom
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ACCOUNT", color = Color(0x73FFFFFF), fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 4.dp))
                                    Text(account.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("BALANCE", color = Color(0x73FFFFFF), fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 4.dp))
                                    Text("${fmt.format(account.balance)} ETB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet
        AccountDetailSheet(
            bank = selectedBank,
            onClose = { selectedBank = null },
            onDelete = { bank ->
                FinanceRepository.removeBank(bank.id)
                selectedBank = null
                Toast.makeText(context, "${bank.name} removed", Toast.LENGTH_SHORT).show()
            }
        )

    }
}
