package com.mobile.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.mobile.ui.screens.*



private data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val NAV_ITEMS = listOf(
    NavItem("analytics", Icons.Default.BarChart, "Analytics"),
    NavItem("budget", Icons.Default.Savings, "Budget"),
    NavItem("home", Icons.Default.Home, "Home"),
    NavItem("tools", Icons.Default.Build, "Tools"),
    NavItem("settings", Icons.Default.Settings, "Settings")
)

@Composable
fun AppNavigation() {
    var currentRoute by remember { mutableStateOf("home") }
    var previousRoute by remember { mutableStateOf("home") }

    fun navigateTo(route: String) {
        previousRoute = currentRoute
        currentRoute = route
    }

    // A tapped transaction notification always resolves on the Home tab (where
    // TransactionDetailSheet lives) — jump there if the user was elsewhere.
    val pendingTransactionId by com.mobile.data.FinanceRepository.pendingTransactionId.collectAsState()
    LaunchedEffect(pendingTransactionId) {
        if (pendingTransactionId != null && currentRoute != "home") {
            navigateTo("home")
        }
    }

    // Content transitions handled in Scaffold



    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(8.dp, RoundedCornerShape(32.dp), clip = false)
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
                            if (selected) Color.White else unselectedTint,
                            label = "iconTint"
                        )

                        val boxBgColor by animateColorAsState(
                            if (selected) selectedBg else Color.Transparent,
                            label = "boxBgColor"
                        )

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.9f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "buttonScale"
                        )

                        Box(
                            // BUG FIX: weight(1f) forced every item — selected or not — into
                            // an equal fixed-width slice of the row, so the label that appears
                            // on the selected item (e.g. "Analytics", "Settings") had nowhere to
                            // expand into and got clipped mid-word ("Ana", "Ho"). widthIn(min)
                            // keeps unselected items at a comfortable tap-target size while
                            // letting the selected item grow past it to fit its full label;
                            // SpaceAround on the parent Row still distributes the slack evenly.
                            modifier = Modifier
                                .widthIn(min = 56.dp)
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
                                    .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 10.dp),
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
                                            color = Color.White,
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
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                "home"      -> HomeScreen(
                    onNavigateToProfile = { navigateTo("profile") },
                    onNavigateToTransactionHistory = { navigateTo("transaction_history") },
                    onNavigateToAlerts = { navigateTo("alerts") }
                )
                "analytics" -> AnalyticsScreen()
                "budget"    -> BudgetScreen()
                "tools"     -> ToolsScreen(onNavigate = { navigateTo(it) })
                "settings"  -> SettingsScreen(onNavigate = { navigateTo(it) })
                "profile"   -> ProfileScreen(onBack = { currentRoute = previousRoute })
                "transaction_history" -> TransactionHistoryScreen(onBack = { currentRoute = previousRoute })
                "alerts"    -> AlertsScreen(onBack = { currentRoute = previousRoute })
                "security"  -> SecurityScreen(onBack = { currentRoute = previousRoute })
                "payment_reminders" -> PaymentRemindersScreen(onBack = { currentRoute = previousRoute })
                "export_data" -> ExportDataScreen(onBack = { currentRoute = previousRoute })
                "net_worth" -> NetWorthScreen(onBack = { currentRoute = previousRoute })
                "notification_capture" -> NotificationCaptureScreen(onBack = { currentRoute = previousRoute })
                "achievement_certificates" -> AchievementCertificatesScreen(onBack = { currentRoute = previousRoute })
                "support"   -> SupportScreen(onBack = { currentRoute = previousRoute })
            }
        }
    }
}