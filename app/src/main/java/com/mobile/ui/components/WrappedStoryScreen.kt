package com.mobile.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobile.data.Data
import com.mobile.data.Transaction
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ── Stats ─────────────────────────────────────────────────────────────────────

private data class WrappedStats(
    val txCount: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val savingsRate: Double?,
    val topBank: String?,
    val topBankCount: Int,
    val avgTransaction: Double,
    val topCategories: List<Pair<String, Double>>,
    val monthCounts: List<Int>,
    val busiestMonthLabel: String,
    val busiestMonthCount: Int,
    val biggestTx: Transaction,
    val moneyPersona: MoneyPersona?
)

// A light, non-judgmental personality read on the year's savings rate — the kind of
// fun, shareable payoff Spotify Wrapped built its reputation on. Tiers are informational
// framing only, not a score; null when there's no income to measure a rate against.
private data class MoneyPersona(val title: String, val tagline: String, val icon: ImageVector)

private fun moneyPersonaFor(savingsRate: Double?): MoneyPersona? = when {
    savingsRate == null -> null
    savingsRate >= 30 -> MoneyPersona("The Vault", "You barely touch what comes in — elite-level restraint.", Icons.Filled.Savings)
    savingsRate >= 15 -> MoneyPersona("The Planner", "Steady and deliberate — always a little ahead of yourself.", Icons.Filled.FactCheck)
    savingsRate >= 0 -> MoneyPersona("The Balancer", "Income and spending move together — you make it work.", Icons.Filled.Balance)
    else -> MoneyPersona("The Spender", "You live in the moment — every birr finds a purpose.", Icons.Filled.Whatshot)
}

private val wrappedDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
private val monthNames = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val monthAbbrev = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun computeWrappedStats(transactions: List<Transaction>, year: Int): WrappedStats? {
    val yearTx = transactions.mapNotNull { tx ->
        val date = try { wrappedDateFormat.parse(tx.date) } catch (e: Exception) { null } ?: return@mapNotNull null
        val cal = Calendar.getInstance().apply { time = date }
        if (cal.get(Calendar.YEAR) == year) tx to cal else null
    }
    if (yearTx.isEmpty()) return null

    val income = yearTx.filter { it.first.type == "credit" }.sumOf { it.first.amount }
    val expense = yearTx.filter { it.first.type == "debit" }.sumOf { it.first.amount }
    val savingsRate = if (income > 0) ((income - expense) / income * 100) else null

    val topBankEntry = yearTx.groupingBy { it.first.bankShortName }.eachCount().maxByOrNull { it.value }

    val topCategories = yearTx.filter { it.first.type == "debit" }
        .groupBy { if (it.first.category == "Other") "Uncategorized" else it.first.category }
        .mapValues { entry -> entry.value.sumOf { it.first.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    val monthCounts = IntArray(12)
    yearTx.forEach { (_, cal) -> monthCounts[cal.get(Calendar.MONTH)]++ }
    val busiestMonthIndex = monthCounts.indices.maxByOrNull { monthCounts[it] } ?: 0

    val biggestTx = yearTx.maxByOrNull { it.first.amount }!!.first

    return WrappedStats(
        txCount = yearTx.size,
        totalIncome = income,
        totalExpense = expense,
        savingsRate = savingsRate,
        topBank = topBankEntry?.key,
        topBankCount = topBankEntry?.value ?: 0,
        avgTransaction = (income + expense) / yearTx.size,
        topCategories = topCategories,
        monthCounts = monthCounts.toList(),
        busiestMonthLabel = monthNames[busiestMonthIndex],
        busiestMonthCount = monthCounts[busiestMonthIndex],
        biggestTx = biggestTx,
        moneyPersona = moneyPersonaFor(savingsRate)
    )
}

private fun shareWrapped(context: Context, stats: WrappedStats, year: Int) {
    val text = buildString {
        appendLine("My $year Wrapped on Habte")
        appendLine("${stats.txCount} transactions tracked")
        appendLine("Income: ETB ${Data.formatBalance(stats.totalIncome)}")
        appendLine("Expense: ETB ${Data.formatBalance(stats.totalExpense)}")
        stats.savingsRate?.let { appendLine("Savings rate: ${String.format("%.1f", it)}%") }
        stats.moneyPersona?.let { appendLine("Money persona: ${it.title}") }
        stats.topCategories.firstOrNull()?.let { appendLine("Top category: ${it.first}") }
        appendLine("Busiest month: ${stats.busiestMonthLabel} (${stats.busiestMonthCount} transactions)")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share your Wrapped"))
}

// ── Slide spec ────────────────────────────────────────────────────────────────

private data class SlideSpec(val gradient: List<Color>, val content: @Composable () -> Unit)

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun WrappedStoryScreen(
    transactions: List<Transaction>,
    year: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember(transactions, year) { computeWrappedStats(transactions, year) }

    if (stats == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val slides = remember(stats) {
        buildList {
            add(SlideSpec(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))) { IntroSlide(stats, year) })
            add(SlideSpec(listOf(Color(0xFF059669), Color(0xFF047857))) {
                BigStatSlide(
                    icon = Icons.Filled.TrendingUp,
                    eyebrow = "Money coming in",
                    amount = stats.totalIncome,
                    caption = "flowed into your accounts this year.",
                    extraCaption = stats.topBank?.let { "$it saw the most action — ${stats.topBankCount} transactions." }
                )
            })
            add(SlideSpec(listOf(Color(0xFFDC2626), Color(0xFF991B1B))) {
                BigStatSlide(
                    icon = Icons.Filled.TrendingDown,
                    eyebrow = "Money going out",
                    amount = stats.totalExpense,
                    caption = "left your accounts this year.",
                    extraCaption = "Averaging ETB ${Data.formatBalance(stats.avgTransaction)} per transaction."
                )
            })
            if (stats.topCategories.isNotEmpty()) {
                add(SlideSpec(listOf(Color(0xFFDB2777), Color(0xFF9D174D))) { TopCategorySlide(stats) })
            }
            add(SlideSpec(listOf(Color(0xFF2563EB), Color(0xFF1E3A8A))) { BusiestMonthSlide(stats) })
            add(SlideSpec(listOf(Color(0xFFF59E0B), Color(0xFFB45309))) { BiggestTransactionSlide(stats.biggestTx) })
            stats.moneyPersona?.let { persona ->
                add(SlideSpec(listOf(Color(0xFF7C3AED), Color(0xFF4C1D95))) { PersonaSlide(persona) })
            }
            add(SlideSpec(listOf(Color(0xFF4F46E5), Color(0xFF312E81))) {
                RecapSlide(stats, year, onShare = { shareWrapped(context, stats, year) }, onDone = onDismiss)
            })
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val slideDurationNanos = 5_000_000_000L

    LaunchedEffect(currentIndex, slides.size) {
        progress.snapTo(0f)
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = frameTime - lastFrame
            lastFrame = frameTime
            if (!isPaused) {
                val next = progress.value + dt.toFloat() / slideDurationNanos.toFloat()
                if (next >= 1f) {
                    progress.snapTo(1f)
                    break
                }
                progress.snapTo(next)
            }
        }
        if (currentIndex < slides.lastIndex) currentIndex++ else onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed animated background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(slides[currentIndex].gradient))
            )

            // Safe-area slide content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Crossfade(targetState = currentIndex, animationSpec = tween(300), label = "wrappedSlide") { index ->
                    slides[index].content()
                }
            }

            // Tap zones: left = back, right = next, hold = pause
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slides.size) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onLongPress = { /* pause is already engaged via onPress */ },
                            onTap = { offset ->
                                if (offset.x < size.width / 2f) {
                                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                } else if (currentIndex < slides.lastIndex) {
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        )
                    }
            )

            // Progress bar + branding + close
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    slides.indices.forEach { i ->
                        val fill = when {
                            i < currentIndex -> 1f
                            i == currentIndex -> progress.value
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fill)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Habte Wrapped", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ── Reveal helpers ────────────────────────────────────────────────────────────

@Composable
private fun RevealUp(delayMillis: Long, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMillis); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
    ) { content() }
}

@Composable
private fun RevealScale(delayMillis: Long, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMillis); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.5f)
    ) { content() }
}

@Composable
private fun CountUpAmount(target: Double, fontSize: TextUnit, color: Color = Color.White) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target.toFloat(), animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }
    val display = if (anim.value >= target.toFloat() - 1f) target else anim.value.toDouble()
    Text(Data.formatBalance(display), color = color, fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun IconBubble(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 72.dp, iconSize: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

// ── Slides ────────────────────────────────────────────────────────────────────

@Composable
private fun IntroSlide(stats: WrappedStats, year: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RevealScale(0) { IconBubble(Icons.Filled.AutoAwesome, size = 88.dp, iconSize = 44.dp) }
        Spacer(Modifier.height(28.dp))
        RevealUp(150) {
            Text("Your $year", color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        RevealUp(250) {
            Text("Wrapped", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(20.dp))
        RevealUp(500) {
            Text(
                "${stats.txCount} transactions told your money story this year.",
                color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium
            )
        }
        RevealUp(700) {
            Text("Tap to begin →", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun BigStatSlide(
    icon: ImageVector,
    eyebrow: String,
    amount: Double,
    caption: String,
    extraCaption: String?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RevealScale(0) { IconBubble(icon) }
        Spacer(Modifier.height(24.dp))
        RevealUp(150) {
            Text(eyebrow, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        RevealUp(300) {
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpAmount(target = amount, fontSize = 42.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "ETB", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        RevealUp(600) {
            Text(caption, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
        if (extraCaption != null) {
            RevealUp(750) {
                Text(
                    extraCaption, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBarRow(rank: Int, category: String, amount: Double, maxAmount: Double) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val fraction by animateFloatAsState(
        targetValue = if (animated) (amount / maxAmount).toFloat() else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "categoryBar"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#$rank $category", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("ETB ${Data.formatBalance(amount)}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun TopCategorySlide(stats: WrappedStats) {
    val maxAmount = stats.topCategories.maxOf { it.second }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RevealScale(0) { IconBubble(Icons.Filled.Category) }
        Spacer(Modifier.height(24.dp))
        RevealUp(150) {
            Text("Where your money went", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth()) {
            stats.topCategories.forEachIndexed { index, (category, amount) ->
                RevealUp(300L + index * 150L) {
                    CategoryBarRow(rank = index + 1, category = category, amount = amount, maxAmount = maxAmount)
                }
            }
        }
    }
}

@Composable
private fun MonthBar(month: String, count: Int, maxCount: Int, isHighlighted: Boolean, delayMillis: Long, modifier: Modifier = Modifier) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMillis); animated = true }
    val targetFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val heightFraction by animateFloatAsState(
        targetValue = if (animated) targetFraction else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "monthBar"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.width(14.dp).height(90.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction.coerceIn(0.03f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isHighlighted) Color.White else Color.White.copy(alpha = 0.35f))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            month,
            color = Color.White.copy(alpha = if (isHighlighted) 1f else 0.6f),
            fontSize = 9.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun BusiestMonthSlide(stats: WrappedStats) {
    val maxCount = stats.monthCounts.maxOrNull() ?: 1
    val busiestIndex = stats.monthCounts.indices.maxByOrNull { stats.monthCounts[it] } ?: 0
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RevealScale(0) { IconBubble(Icons.Filled.CalendarMonth) }
        Spacer(Modifier.height(24.dp))
        RevealUp(150) {
            Text("Your busiest month", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        RevealUp(280) {
            Text(stats.busiestMonthLabel, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
        RevealUp(400) {
            Text("${stats.busiestMonthCount} transactions", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            monthAbbrev.forEachIndexed { index, label ->
                MonthBar(
                    month = label,
                    count = stats.monthCounts[index],
                    maxCount = maxCount,
                    isHighlighted = index == busiestIndex,
                    delayMillis = 500L + index * 40L,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BiggestTransactionSlide(tx: Transaction) {
    val dateFormat by com.mobile.data.SettingsRepository.dateFormat.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RevealScale(0) { IconBubble(Icons.Filled.Bolt) }
        Spacer(Modifier.height(24.dp))
        RevealUp(150) {
            Text("Your biggest single move", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        RevealUp(320) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (tx.type == "credit") "+" else "-", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                CountUpAmount(target = tx.amount, fontSize = 40.sp)
            }
        }
        RevealUp(420) {
            Text("ETB", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        RevealUp(600) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tx.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("${tx.bankShortName} • ${com.mobile.data.formatDisplayDate(tx.date, dateFormat)}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PersonaSlide(persona: MoneyPersona) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RevealScale(0) { IconBubble(persona.icon, size = 88.dp, iconSize = 44.dp) }
        Spacer(Modifier.height(24.dp))
        RevealUp(150) {
            Text("Your money persona is", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        RevealUp(300) {
            Text(persona.title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        RevealUp(500) {
            Text(
                persona.tagline, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun RecapSlide(stats: WrappedStats, year: Int, onShare: () -> Unit, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ConfettiOverlay()
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RevealScale(0) { IconBubble(Icons.Filled.EmojiEvents, size = 88.dp, iconSize = 44.dp) }
            Spacer(Modifier.height(24.dp))
            RevealUp(150) {
                Text("That's a wrap on $year", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            if (stats.savingsRate != null) {
                RevealUp(320) {
                    Text(
                        "You saved ${String.format("%.1f", stats.savingsRate)}% of what came in.",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            RevealUp(450) {
                Text("Here's to an even sharper ${year + 1}.", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(36.dp))
            RevealUp(650) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onShare,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF312E81)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Confetti ──────────────────────────────────────────────────────────────────

private data class ConfettiPiece(
    val startX: Float,
    val phase: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float
)

@Composable
private fun ConfettiOverlay() {
    val colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFD700), Color(0xFFFF6FCF), Color(0xFF7DD3FC), Color(0xFFBEF264))
    val pieces = remember {
        List(26) {
            ConfettiPiece(
                startX = Random.nextFloat(),
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.6f + Random.nextFloat() * 0.6f,
                size = 6f + Random.nextFloat() * 6f,
                color = colors[Random.nextInt(colors.size)],
                spin = if (Random.nextBoolean()) 1f else -1f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "confettiClock")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "confettiTime"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val progress = (time * piece.speed + piece.startX) % 1f
            val y = size.height * progress
            val x = (piece.startX * size.width) + sin(progress * 2f * PI.toFloat() * 2f + piece.phase) * 24f
            rotate(degrees = progress * 360f * piece.spin, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = 0.85f),
                    topLeft = Offset(x - piece.size / 2f, y - piece.size / 2f),
                    size = Size(piece.size, piece.size * 1.6f)
                )
            }
        }
    }
}
