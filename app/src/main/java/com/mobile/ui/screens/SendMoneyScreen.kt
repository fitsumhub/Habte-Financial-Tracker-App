package com.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.FinanceRepository
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val banks by FinanceRepository.banks.collectAsState()
    var amount by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<String?>(null) }
    var toAccount by remember { mutableStateOf("") }

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
                    text = "Send Money",
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
            Text(
                text = "From Account",
                color = Color(0xFF7B84A8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            // Simple account selector (just shows first account for now)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0E1527))
                    .border(1.dp, Color(0xFF1A2240), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = fromAccount ?: "Select Account",
                        color = if (fromAccount == null) Color(0xFF7B84A8) else Color.White,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // In a real app, we would show a list of accounts to select from
            // For now, we just set the first account when clicked
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable {
                        // Just pick the first account for demo
                        val firstAccount = banks.firstOrNull()?.accounts?.firstOrNull()
                        fromAccount = "${firstAccount?.label ?: "Account"} •••• ${firstAccount?.accountNumber?.takeLast(4) ?: ""}"
                    }
            ) {
                Text(
                    text = "Tap to select account",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Amount",
                color = Color(0xFF7B84A8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            androidx.compose.material3.TextField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = { Text("Enter amount", color = Color(0xFF3A4268)) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0E1527),
                    unfocusedContainerColor = Color(0xFF0E1527),
                    focusedIndicatorColor = Color(0xFF6366F1),
                    unfocusedIndicatorColor = Color(0xFF1A2240),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6366F1)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To Account",
                color = Color(0xFF7B84A8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            androidx.compose.material3.TextField(
                value = toAccount,
                onValueChange = { toAccount = it },
                placeholder = { Text("Enter account number or phone", color = Color(0xFF3A4268)) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0E1527),
                    unfocusedContainerColor = Color(0xFF0E1527),
                    focusedIndicatorColor = Color(0xFF6366F1),
                    unfocusedIndicatorColor = Color(0xFF1A2240),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6366F1)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Send Button
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1.0f,
                animationSpec = androidx.compose.animation.core.tween(100),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (amount.isNotBlank() && toAccount.isNotBlank() && fromAccount != null) {
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA)))
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (amount.isNotBlank() && toAccount.isNotBlank() && fromAccount != null) {
                                // Simulate sending money
                                Toast.makeText(context, "Money sent successfully!", Toast.LENGTH_SHORT).show()
                                // Clear form
                                amount = ""
                                toAccount = ""
                                fromAccount = null
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Send Money",
                    color = if (amount.isNotBlank() && toAccount.isNotBlank() && fromAccount != null) Color.White else Color(0xFF475569),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}