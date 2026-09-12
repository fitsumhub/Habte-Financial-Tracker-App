package com.mobile.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.ui.screens.*

private data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val NAV_ITEMS = listOf(
    NavItem("home", Icons.Default.Home, "Home"),
    NavItem("tools", Icons.Default.Build, "Tools"),
    NavItem("transactions", Icons.AutoMirrored.Filled.ReceiptLong, "Transactions"),
    NavItem("analytics", Icons.Default.BarChart, "Analytics"),
    NavItem("budget", Icons.Default.Savings, "Budget"),
    NavItem("profile", Icons.Default.Person, "Profile")
)

@Composable
fun AppNavigation() {
    var currentRoute by remember { mutableStateOf("home") }
    var previousRoute by remember { mutableStateOf("home") }

    fun navigateTo(route: String) {
        previousRoute = currentRoute
        currentRoute = route
    }

    val pendingTransactionId by com.mobile.data.FinanceRepository.pendingTransactionId.collectAsState()
    LaunchedEffect(pendingTransactionId) {
        if (pendingTransactionId != null && currentRoute != "home") {
            navigateTo("home")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x33000000), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NAV_ITEMS.forEach { item ->
                        val selected = currentRoute == item.route

                        val unselectedTint = MaterialTheme.colorScheme.onSurfaceVariant
                        val selectedBg = MaterialTheme.colorScheme.primary
                        val iconTint by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onPrimary else unselectedTint,
                            label = "iconTint"
                        )

                        val boxBgColor by animateColorAsState(
                            if (selected) selectedBg else Color.Transparent,
                            label = "boxBgColor"
                        )

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "buttonScale"
                        )

                        Box(
                            modifier = Modifier
                                .widthIn(min = 46.dp)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { currentRoute = item.route }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(boxBgColor)
                                    .padding(horizontal = if (selected) 14.dp else 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
 { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                "home"         -> HomeScreen(
                    onNavigateToProfile = { navigateTo("profile") },
                    onNavigateToTransactionHistory = { navigateTo("transactions") },
                    onNavigateToAlerts = { navigateTo("alerts") }
                )
                "transactions" -> TransactionHistoryScreen(onBack = { currentRoute = "home" })
                "analytics"    -> AnalyticsScreen()
                "budget"       -> BudgetScreen()
                "profile"      -> ProfileScreen(onBack = { currentRoute = "home" }, onNavigate = { navigateTo(it) })
                "tools"        -> ToolsScreen(onNavigate = { navigateTo(it) })
                "settings"     -> SettingsScreen(onNavigate = { navigateTo(it) })
                "transaction_history" -> TransactionHistoryScreen(onBack = { currentRoute = previousRoute })
                "alerts"       -> AlertsScreen(onBack = { currentRoute = previousRoute })
                "security"     -> SecurityScreen(onBack = { currentRoute = previousRoute })
                "payment_reminders" -> PaymentRemindersScreen(onBack = { currentRoute = previousRoute })
                "export_data"  -> ExportDataScreen(onBack = { currentRoute = previousRoute })
                "export_financial_statement" -> ExportFinancialStatementScreen(onBack = { currentRoute = previousRoute })
                "net_worth"    -> NetWorthScreen(onBack = { currentRoute = previousRoute })
                "notification_capture" -> NotificationCaptureScreen(onBack = { currentRoute = previousRoute })
                "achievement_certificates" -> AchievementCertificatesScreen(onBack = { currentRoute = previousRoute })
                "support"      -> SupportScreen(onBack = { currentRoute = previousRoute })
            }
        }
    }
}
