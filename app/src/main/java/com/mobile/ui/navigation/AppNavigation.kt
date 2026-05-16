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

    // Content transitions handled in Scaffold



    Scaffold(
        containerColor = Color(0xFF070912),
        bottomBar = {
            if (currentRoute != "ai_chat") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp) // Slightly taller for labels
                            .graphicsLayer {
                                shadowElevation = 20f
                                shape = RoundedCornerShape(24.dp)
                                clip = false
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0E1527))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                            .padding(horizontal = 8.dp)
                    ) {

                        // We will use a simpler approach for sliding since weight is dynamic.

                        // Instead of a separate indicator box, we'll stick to the per-item background but make it smoother.
                        
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NAV_ITEMS.forEach { item ->
                                val selected = currentRoute == item.route
                                
                                val iconTint by animateColorAsState(
                                    if (selected) Color.White else Color(0xFF64748B),
                                    label = "iconTint"
                                )
                                
                                val boxBgColor by animateColorAsState(
                                    if (selected) Color(0xFF6366F1) else Color.Transparent,
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
                                    modifier = Modifier
                                        .weight(1f)
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
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (selected) 44.dp else 40.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(boxBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.icon else item.icon, // Could use outlined variant here
                                                contentDescription = item.label,
                                                tint = iconTint,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        
                                        AnimatedVisibility(
                                            visible = selected,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Text(
                                                text = item.label,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
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
                "home"      -> HomeScreen(onNavigateToAi = { currentRoute = "ai_chat" }, onNavigateToProfile = { currentRoute = "profile" }, onNavigateToTransactionHistory = { currentRoute = "transaction_history" })
                "analytics" -> AnalyticsScreen(onNavigateToAi = { currentRoute = "ai_chat" })
                "budget"    -> BudgetScreen()
                "tools"     -> ToolsScreen(onNavigate = { currentRoute = it })
                "settings"  -> SettingsScreen()
                "ai_chat"   -> AiChatScreen(onBack = { currentRoute = "home" })
                "profile"   -> ProfileScreen(onBack = { currentRoute = "home" })
                "transaction_history" -> TransactionHistoryScreen(onBack = { currentRoute = "home" })
                "alerts"    -> AlertsScreen(onBack = { currentRoute = "tools" })
                "security"  -> SecurityScreen(onBack = { currentRoute = "tools" })
                "export_data" -> ExportDataScreen(onBack = { currentRoute = "tools" })
                "support"   -> SupportScreen(onBack = { currentRoute = "tools" })
            }
        }
    }
}