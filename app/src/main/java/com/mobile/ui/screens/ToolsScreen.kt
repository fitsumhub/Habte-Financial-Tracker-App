package com.mobile.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import java.io.OutputStreamWriter

private data class Tool(
    val icon: ImageVector,
    val label: String,
    val sub: String,
    val color: Color
)

private val TOOLS = listOf(
    Tool(Icons.Default.TrendingUp,    "Transfer History", "View transactions",       Color(0xFF6366F1)),
    Tool(Icons.Default.BarChart,      "Statement",        "Download statements",     Color(0xFF0EA5E9)),
    Tool(Icons.Default.CurrencyExchange, "Converter",     "Exchange rates",          Color(0xFF10B981)),
    Tool(Icons.Default.Calculate,     "Loan Calc",        "EMI calculator",          Color(0xFFF59E0B)),
    Tool(Icons.Default.Receipt,       "Tax Calc",         "Income tax estimator",    Color(0xFFEF4444)),
    Tool(Icons.Default.Notifications, "Alerts",           "Balance alerts",          Color(0xFFF97316)),
    Tool(Icons.Default.Shield,        "Security",         "2FA and PIN",             Color(0xFF14B8A6)),
    Tool(Icons.Default.FileDownload,  "Export Data",      "CSV or PDF export",       Color(0xFFEC4899))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val transactions by FinanceRepository.transactions.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    var showCurrencyConverter by remember { mutableStateOf(false) }
    var showLoanCalc by remember { mutableStateOf(false) }
    var showTaxCalc by remember { mutableStateOf(false) }

    // File creation launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    val csv = StringBuilder("Date,Title,Amount,Type,Category,Bank\n")
                    transactions.forEach { t ->
                        csv.append("${t.date},${t.title},${t.amount},${t.type},${t.category},${t.bankShortName}\n")
                    }
                    writer.write(csv.toString())
                    writer.flush()
                }
                Toast.makeText(context, "Statement saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun initiateDownload() {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions found", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "Habte_Statement_${System.currentTimeMillis()}.csv"
        createDocumentLauncher.launch(fileName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070912))
            .statusBarsPadding()
    ) {
        // Top Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Workspace",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your financial operations with precision.",
                color = Color(0xFF94A3B8),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(bottom = 100.dp)
        ) {
            items(TOOLS) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = {
                        when (tool.label) {
                            "Transfer History" -> showHistory = true
                            "Statement" -> initiateDownload()
                            "Converter" -> showCurrencyConverter = true
                            "Loan Calc" -> showLoanCalc = true
                            "Tax Calc" -> showTaxCalc = true
                            "Alerts" -> onNavigate("alerts")
                            "Security" -> onNavigate("security")
                            "Export Data" -> onNavigate("export_data")
                        }
                    }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(24.dp))
                QuickActionBanner(onNavigate)
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = Color(0xFF0A0F20)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    "Recent Operations",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (transactions.isEmpty()) {
                    Text("No transactions found.", color = Color(0xFF64748B))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        transactions.take(10).forEach { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0E1527))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (t.type == "credit") Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (t.type == "credit") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (t.type == "credit") Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(t.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text(t.date, color = Color(0xFF64748B), fontSize = 12.sp)
                                    }
                                }
                                Text(
                                    "${if (t.type == "credit") "+" else ""}${Data.formatBalance(t.amount)} ETB",
                                    color = if (t.type == "credit") Color(0xFF10B981) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyConverter) CurrencyConverterModal(onClose = { showCurrencyConverter = false })
    if (showLoanCalc) LoanCalcModal(onClose = { showLoanCalc = false })
    if (showTaxCalc) TaxCalcModal(onClose = { showTaxCalc = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterModal(onClose: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var isEtbToForeign by remember { mutableStateOf(false) }

    val a = amount.toDoubleOrNull() ?: 0.0
    val r = rate.toDoubleOrNull() ?: 0.0
    val result = if (r > 0) {
        if (isEtbToForeign) a / r else a * r
    } else 0.0

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF0A0F20)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Currency Converter",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = if (isEtbToForeign) "ETB to Foreign" else "Foreign to ETB",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
                IconButton(onClick = { isEtbToForeign = !isEtbToForeign }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = Color(0xFF10B981))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = { Text(if (isEtbToForeign) "Amount (ETB)" else "Amount (Foreign)", color = Color(0xFF3A4268)) },
                    modifier = Modifier.weight(1f).padding(bottom = 12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E1527),
                        unfocusedContainerColor = Color(0xFF0E1527),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1A2240),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                androidx.compose.material3.OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    placeholder = { Text("Exchange Rate", color = Color(0xFF3A4268)) },
                    modifier = Modifier.weight(1f).padding(bottom = 12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E1527),
                        unfocusedContainerColor = Color(0xFF0E1527),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1A2240),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Quick rates
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(3) { index ->
                    val preset = listOf("USD" to "113.5", "EUR" to "122.3", "GBP" to "144.1")[index]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (rate == preset.second) Color(0xFF10B981) else Color(0xFF1A2240))
                            .clickable { rate = preset.second }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.first,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1527))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Converted Amount", color = Color(0xFF64748B), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Data.formatBalance(result)} ${if (isEtbToForeign) "Foreign" else "ETB"}",
                        color = Color(0xFF10B981),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanCalcModal(onClose: () -> Unit) {
    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }

    val p = principal.toDoubleOrNull() ?: 0.0
    val rAnnual = rate.toDoubleOrNull() ?: 0.0
    val r = rAnnual / 12 / 100
    val n = months.toDoubleOrNull() ?: 1.0

    val emi = if (p > 0 && n > 0) {
        if (r > 0) {
            (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1)
        } else {
            p / n
        }
    } else 0.0

    val totalPayment = emi * n
    val totalInterest = Math.max(0.0, totalPayment - p)

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF0A0F20)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Loan Calculator",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            androidx.compose.material3.OutlinedTextField(
                value = principal,
                onValueChange = { principal = it },
                placeholder = { Text("Loan Amount (ETB)", color = Color(0xFF3A4268)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0E1527),
                    unfocusedContainerColor = Color(0xFF0E1527),
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = Color(0xFF1A2240),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    placeholder = { Text("Annual Rate (%)", color = Color(0xFF3A4268)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E1527),
                        unfocusedContainerColor = Color(0xFF0E1527),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF1A2240),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                androidx.compose.material3.OutlinedTextField(
                    value = months,
                    onValueChange = { months = it },
                    placeholder = { Text("Months", color = Color(0xFF3A4268)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0E1527),
                        unfocusedContainerColor = Color(0xFF0E1527),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF1A2240),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1527))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Monthly EMI", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text("${Data.formatBalance(emi)} ETB", color = Color(0xFFF59E0B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Interest", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text("${Data.formatBalance(totalInterest)} ETB", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                androidx.compose.material3.Divider(color = Color(0xFF1A2240))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Payment", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${Data.formatBalance(totalPayment)} ETB", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCalcModal(onClose: () -> Unit) {
    var salary by remember { mutableStateOf("") }

    val s = salary.toDoubleOrNull() ?: 0.0
    // Rough estimation based on Ethiopian tax brackets
    val estimatedTax = when {
        s <= 600 -> 0.0
        s <= 1650 -> (s * 0.1) - 60
        s <= 3200 -> (s * 0.15) - 142.5
        s <= 5250 -> (s * 0.2) - 302.5
        s <= 7800 -> (s * 0.25) - 565
        s <= 10900 -> (s * 0.3) - 955
        else -> (s * 0.35) - 1500
    }
    val tax = Math.max(0.0, estimatedTax)
    val netSalary = Math.max(0.0, s - tax)

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF0A0F20)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Income Tax Estimator",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            androidx.compose.material3.OutlinedTextField(
                value = salary,
                onValueChange = { salary = it },
                placeholder = { Text("Gross Monthly Salary (ETB)", color = Color(0xFF3A4268)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0E1527),
                    unfocusedContainerColor = Color(0xFF0E1527),
                    focusedBorderColor = Color(0xFFEF4444),
                    unfocusedBorderColor = Color(0xFF1A2240),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1527))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross Salary", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text("${Data.formatBalance(s)} ETB", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Tax", color = Color(0xFFEF4444), fontSize = 14.sp)
                    Text("- ${Data.formatBalance(tax)} ETB", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                androidx.compose.material3.Divider(color = Color(0xFF1A2240))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Salary", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${Data.formatBalance(netSalary)} ETB", color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ToolCard(tool: Tool, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "toolScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0E1527))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(tool.color.copy(alpha = 0.3f), Color(0xFF1A2240))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(tool.color.copy(alpha = 0.2f), tool.color.copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = tool.color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = tool.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = tool.sub,
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionBanner(onNavigate: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(150),
        label = "bannerScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onNavigate("support") }
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Need Assistance?",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Contact our 24/7 support team",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = "Support",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

