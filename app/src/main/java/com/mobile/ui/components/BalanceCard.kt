package com.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(bottom = 18.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFF1E1B4B))
                )
            )
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {

        // Glow dot — top-right decorative element
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 160.dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color(0x0FFFFFFF))
        )

        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    color = Color(0xA6FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1EFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SIM chip decoration
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFCD34D), Color(0xFFF59E0B), Color(0xFFD97706))
                        )
                    )
                    .border(1.dp, Color(0x33000000), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0x40000000))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))


            // Balance amount row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text = if (hidden) "••••••••" else Data.formatBalance(totalBalance),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = " ETB",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }


            Spacer(modifier = Modifier.height(20.dp))

            // Sparkline Trend Chart
            Sparkline(
                data = trendData,
                modifier = Modifier
                    .fillMaxWidth()

                    .height(40.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))


            // Footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$bankCount Banks · $accountCount Accounts",
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { hidden = !hidden },
                        modifier = Modifier
                            .size(30.dp)
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
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x1EFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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

