package com.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import com.mobile.data.FinanceRepository
import com.mobile.data.Data
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.OutputStreamWriter



private data class Tool(
    val icon: ImageVector,
    val label: String,
    val sub: String,
    val color: Color
)

private val TOOLS = listOf(
    Tool(Icons.Default.TrendingUp,    "Transfer History", "View all transactions",       Color(0xFF6366F1)),
    Tool(Icons.Default.Repeat,        "Send Money",       "Transfer between accounts",   Color(0xFF8B5CF6)),
    Tool(Icons.Default.BarChart,      "Statement",        "Download account statement",  Color(0xFF0EA5E9)),
    Tool(Icons.Default.Notifications, "Alerts",           "Balance & transaction alerts",Color(0xFFF59E0B)),
    Tool(Icons.Default.Shield,        "Security",         "2FA and PIN management",      Color(0xFF10B981)),
    Tool(Icons.Default.FileDownload,  "Export Data",      "CSV or PDF export",           Color(0xFFEC4899))
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onNavigate: (String) -> Unit = {}) {

    val context = LocalContext.current
    val transactions by FinanceRepository.transactions.collectAsState()
    var showHistory by remember { mutableStateOf(false) }

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
            Toast.makeText(context, "No transactions found to generate statement", Toast.LENGTH_SHORT).show()
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
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "Financial Tools",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TOOLS.forEach { tool ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1.0f,
                    animationSpec = tween(100),
                    label = "toolScale"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0E1527))
                        .border(1.dp, Color(0xFF1A2240), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { 
                                when (tool.label) {
                                    "Transfer History" -> showHistory = true
                                    "Send Money" -> onNavigate("send_money")
                                    "Statement" -> initiateDownload()
                                    "Alerts" -> onNavigate("alerts")
                                    "Security" -> onNavigate("security")
                                    "Export Data" -> onNavigate("export_data")
                                }
                            }
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tool.color.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = tool.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.label,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tool.sub,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }

    if (showHistory) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = Color(0xFF0A0F20)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    "Transaction History",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (transactions.isEmpty()) {
                    Text("No transactions found.", color = Color.Gray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        transactions.take(10).forEach { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0E1527))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(t.title, color = Color.White, fontSize = 14.sp)
                                    Text(t.date, color = Color.Gray, fontSize = 11.sp)
                                }
                                Text(
                                    "${if (t.type == "credit") "+" else ""}${Data.formatBalance(t.amount)} ETB",
                                    color = if (t.type == "credit") Color(0xFF10B981) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
