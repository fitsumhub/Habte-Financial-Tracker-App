package com.mobile.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.Data
import com.mobile.ui.theme.LocalEthiopianColors

@Composable
fun BalanceCard(
    totalBalance: Double,
    bankCount: Int,
    accountCount: Int,
    trendData: List<Float>,
    modifier: Modifier = Modifier
) {
    val autoHide by com.mobile.data.SettingsRepository.autoHideBalances.collectAsState()
    var hidden by remember(autoHide) { mutableStateOf(autoHide) }
    val colors = LocalEthiopianColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x33000000),
                spotColor = colors.emeraldPrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header Row: Label + Eye Privacy Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable { hidden = !hidden },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hidden) "Show balance" else "Hide balance",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Balance Amount
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.animateContentSize()
            ) {
                if (hidden) {
                    Text(
                        text = "••••••••",
                        color = colors.textPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                } else {
                    Text(
                        text = "ETB ",
                        color = colors.emeraldPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = Data.formatBalance(totalBalance),
                        color = colors.textPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Growth Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.emeraldPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = colors.emeraldPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "+8.4% this month",
                    color = colors.emeraldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Minimalist Sparkline Chart
            Sparkline(
                data = trendData,
                lineColor = colors.emeraldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Footer
            Text(
                text = "$bankCount Institutions · $accountCount Accounts",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun Sparkline(
    data: List<Float>,
    lineColor: Color = Color(0xFF00C853),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val step = if (data.size > 1) width / (data.size - 1) else width

        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * step
                val y = height - (value * height).coerceIn(2f, height - 2f)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.18f), Color.Transparent)
            )
        )
    }
}
