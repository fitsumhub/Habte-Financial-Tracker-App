package com.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import com.mobile.data.Data


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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x664F46E5),
                spotColor = Color(0x664F46E5)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(22.dp)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    color = Color(0xB3FFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = { hidden = !hidden },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x1EFFFFFF))
                ) {
                    Icon(
                        imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hidden) "Show balance" else "Hide balance",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Balance amount row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text = if (hidden) "••••••••" else Data.formatBalance(totalBalance),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = " ETB",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Sparkline Trend Chart
            Sparkline(
                data = trendData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Footer row
            Text(
                text = "$bankCount Banks · $accountCount Accounts",
                color = Color(0x99FFFFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun Sparkline(data: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val step = width / (data.size - 1)

        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * step
                val y = height - (value * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Fill gradient below path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
            )
        )

    }
}
