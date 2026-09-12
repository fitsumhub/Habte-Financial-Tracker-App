package com.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Thin three-stripe bar: Green | Gold | Red (Ethiopian flag colours). */
@Composable
fun EthiopianTricolorBar(modifier: Modifier = Modifier, height: Dp = 3.dp) {
    Row(modifier = modifier.fillMaxWidth().height(height)) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2E8B57)))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD4A017)))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFC62828)))
    }
}

/** Habesha kemis-embroidery dot-grid at very low alpha. */
@Composable
fun HabeshaDotGrid(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFFD4A017),
    alpha: Float = 0.07f,
    spacing: Float = 22f
) {
    Canvas(modifier = modifier) {
        val cols = (size.width / spacing).toInt() + 2
        val rows = (size.height / spacing).toInt() + 2
        for (row in 0..rows) {
            for (col in 0..cols) {
                val offset = if (row % 2 == 0) 0f else spacing / 2f
                drawCircle(
                    color = dotColor.copy(alpha = alpha),
                    radius = 1.5.dp.toPx(),
                    center = Offset(col * spacing + offset, row * spacing)
                )
            }
        }
    }
}

/** Faint Axumite-inspired cross watermark for card backgrounds. */
@Composable
fun AxumiteCrossWatermark(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFD4A017),
    alpha: Float = 0.05f
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val armLen = minOf(size.width, size.height) * 0.38f
        val armW   = armLen * 0.22f
        val c = color.copy(alpha = alpha)

        // Vertical arm
        drawRect(
            color = c,
            topLeft = Offset(cx - armW / 2f, cy - armLen),
            size = Size(armW, armLen * 2f)
        )
        // Horizontal arm
        drawRect(
            color = c,
            topLeft = Offset(cx - armLen, cy - armW / 2f),
            size = Size(armLen * 2f, armW)
        )
        // Corner squares (Axumite terminus ornaments)
        val cs = armW * 0.5f
        listOf(
            Pair(-armLen + armW * 0.5f, -armLen + armW * 0.5f),
            Pair( armLen - armW * 1.5f, -armLen + armW * 0.5f),
            Pair(-armLen + armW * 0.5f,  armLen - armW * 1.5f),
            Pair( armLen - armW * 1.5f,  armLen - armW * 1.5f),
        ).forEach { (dx, dy) ->
            drawRect(
                color = c,
                topLeft = Offset(cx + dx, cy + dy),
                size = Size(cs, cs)
            )
        }
    }
}

/** Animated gold shimmer brush — sweep it across any background box. */
@Composable
fun goldShimmerBrush(
    shimmerColor: Color = Color(0xFFD4A017),
    baseColor: Color    = Color(0xFF1E1812)
): Brush {
    val transition = rememberInfiniteTransition(label = "goldShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerP"
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, shimmerColor.copy(alpha = 0.35f), shimmerColor.copy(alpha = 0.65f), shimmerColor.copy(alpha = 0.35f), baseColor),
        start = Offset(progress * 2000f - 500f, 0f),
        end   = Offset(progress * 2000f + 500f, 200f)
    )
}

/** The diagonal balance-card gradient (Addis night → warm gold → Axumite green). */
val BalanceCardGradient: Brush
    get() = Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF1A120A),
            0.45f to Color(0xFF2C1E0F),
            0.75f to Color(0xFF0F2318),
            1.00f to Color(0xFF0A0F0D),
        ),
        start = Offset(0f, 0f),
        end   = Offset(1200f, 800f)
    )
