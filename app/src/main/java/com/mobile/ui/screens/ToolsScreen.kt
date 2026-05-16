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
    Tool(Icons.Default.Repeat,        "Send Money",       "Transfer funds",          Color(0xFF8B5CF6)),
    Tool(Icons.Default.BarChart,      "Statement",        "Download statements",     Color(0xFF0EA5E9)),
    Tool(Icons.Default.Notifications, "Alerts",           "Balance alerts",          Color(0xFFF59E0B)),
    Tool(Icons.Default.Shield,        "Security",         "2FA and PIN",             Color(0xFF10B981)),
    Tool(Icons.Default.FileDownload,  "Export Data",      "CSV or PDF export",       Color(0xFFEC4899))
)

@OptIn(ExperimentalMaterial3Api::class)
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
                            "Send Money" -> onNavigate("send_money")
                            "Statement" -> initiateDownload()
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

