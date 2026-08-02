package com.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

// ── Bespoke onboarding palette ──────────────────────────────────────────────────
// Deliberately its own dark/premium look, separate from AppTheme's light corporate
// scheme (see project_design_direction memory) — this screen never reads
// MaterialTheme.colorScheme since it's a one-time pre-login moment, not part of the
// app's ongoing chrome.
private val OnboardingBg = Color(0xFF0B0B0B)
private val EmeraldPrimary = Color(0xFF00C853)
private val GoldAccent = Color(0xFFFFD54F)

// Ethiopian flag red — used only in the tiny tricolor flourish below, never on
// interactive/status elements, so it never reads as an error or a negative amount.
private val EthiopianRed = Color(0xFFDA121A)

private val OnboardingTextPrimary = Color.White
private val OnboardingTextMuted = Color(0xFFA6A8AE)
private val ButtonTextColor = Color(0xFF06210F)

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    // Best-effort Amharic rendering of the subtitle — worth a native speaker's pass
    // before shipping, but gives the flow authentic local-language warmth in the
    // meantime rather than being purely an English/transliteration exercise.
    val amharicTagline: String,
    val heroIcon: ImageVector,
    val satelliteIcons: List<ImageVector>
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        title = "Welcome to Habte (ሀብቴ)",
        subtitle = "Your smart personal finance companion",
        amharicTagline = "ብልህ የግል ገንዘብ ጓደኛዎ",
        heroIcon = Icons.Default.AccountBalanceWallet,
        satelliteIcons = listOf(Icons.Default.PhoneAndroid, Icons.Default.TrendingUp, Icons.Default.Savings)
    ),
    OnboardingPage(
        title = "Track Your Money Easily",
        subtitle = "Monitor income, expenses, and savings in one place",
        amharicTagline = "ገቢ፣ ወጪ እና ቁጠባዎን በአንድ ቦታ ይከታተሉ",
        heroIcon = Icons.Default.BarChart,
        satelliteIcons = listOf(Icons.Default.PieChart, Icons.Default.Receipt, Icons.Default.ShowChart)
    ),
    OnboardingPage(
        title = "Connect Your Financial Life",
        subtitle = "Manage banks, digital wallets, and payments securely",
        amharicTagline = "ባንክ፣ ዲጂታል ዋሌት እና ክፍያዎችን በደህንነት ያስተዳድሩ",
        heroIcon = Icons.Default.Shield,
        satelliteIcons = listOf(Icons.Default.AccountBalance, Icons.Default.CreditCard, Icons.Default.PhoneAndroid)
    ),
    OnboardingPage(
        title = "Build Your Financial Future",
        subtitle = "Take control of your wealth and achieve your goals",
        amharicTagline = "ሃብትዎን ይቆጣጠሩ፣ ግቦችዎንም ያሳኩ",
        heroIcon = Icons.Default.RocketLaunch,
        satelliteIcons = listOf(Icons.Default.TrendingUp, Icons.Default.EmojiEvents, Icons.Default.Star)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { ONBOARDING_PAGES.size }
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.lastIndex

    // One-time entrance: the whole screen settles in on first composition instead of
    // popping in fully-formed — the polish an FTUE's very first frame is expected to have.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBg)
            .graphicsLayer {
                alpha = entrance.value
                translationY = (1f - entrance.value) * 40f
            }
    ) {
        // Ambient glow blobs behind everything — the "soft gradient" backdrop.
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-140).dp)
                .background(
                    Brush.radialGradient(listOf(EmeraldPrimary.copy(alpha = 0.30f), Color.Transparent)),
                    CircleShape
                )
                .blur(110.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 90.dp)
                .background(
                    Brush.radialGradient(listOf(GoldAccent.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape
                )
                .blur(100.dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TricolorFlourish()
                    Spacer(modifier = Modifier.width(10.dp))
                    Crossfade(targetState = pagerState.currentPage, label = "stepCounter") { page ->
                        Text(
                            text = "STEP ${page + 1} OF ${ONBOARDING_PAGES.size}",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
                AnimatedVisibility(visible = !isLastPage) {
                    TextButton(onClick = onFinished) {
                        Text("Skip", color = OnboardingTextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                OnboardingPageContent(page = ONBOARDING_PAGES[page], pageOffset = pageOffset)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ONBOARDING_PAGES.indices.forEach { index ->
                    val isActive = index == pagerState.currentPage
                    val width by animateDpAsState(if (isActive) 26.dp else 8.dp, label = "dotWidth")
                    val color by animateColorAsState(
                        if (isActive) EmeraldPrimary else Color.White.copy(alpha = 0.18f),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                // Kept plain — no custom press-scale graphicsLayer. Material3's own default
                // press/ripple feedback is enough for this button; verified on-device.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ButtonTextColor)
            ) {
                Crossfade(targetState = isLastPage, label = "ctaLabel") { last ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (last) "Get Started" else "Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// Small three-band flourish echoing the Ethiopian flag's green/gold/red — purely
// decorative, kept away from any interactive or status color role so it never gets
// read as a semantic (success/warning/error) cue elsewhere in the screen.
@Composable
private fun TricolorFlourish() {
    Row(
        modifier = Modifier
            .height(10.dp)
            .width(20.dp)
            .clip(RoundedCornerShape(2.dp))
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(EmeraldPrimary))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoldAccent))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(EthiopianRed))
    }
}

// A thin repeating diamond motif, the kind of geometric border found on habesha tibeb
// weave — a subtle nod to Ethiopian textile pattern language rather than a literal
// illustration, sitting between the hero and the copy as a small cultural signature.
@Composable
private fun EthiopianPatternDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(12.dp)) {
        val slots = 5
        val step = size.width / slots
        val strokeWidth = 1.2.dp.toPx()
        for (i in 0 until slots) {
            val cx = step * i + step / 2f
            val cy = size.height / 2f
            val r = step * 0.3f
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(path, color = GoldAccent.copy(alpha = 0.4f), style = Stroke(width = strokeWidth))
            drawCircle(color = GoldAccent.copy(alpha = 0.55f), radius = strokeWidth * 0.8f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageOffset: Float) {
    val clamped = pageOffset.coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingHero(page = page, pageOffset = pageOffset)
        Spacer(modifier = Modifier.height(28.dp))
        EthiopianPatternDivider(
            modifier = Modifier
                .width(140.dp)
                .graphicsLayer { alpha = 1f - clamped }
        )
        Spacer(modifier = Modifier.height(20.dp))
        // Text block drifts/fades at a slightly different rate than the hero above it —
        // a light parallax layer rather than everything moving in lockstep.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - clamped * 0.85f
                translationY = clamped * 22f
            }
        ) {
            Text(
                text = page.title,
                color = OnboardingTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = page.subtitle,
                color = OnboardingTextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = page.amharicTagline,
                color = GoldAccent.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// A layered glass-circle "hero" with small orbiting icon chips, standing in for a
// custom illustration — no image-generation tool is available here, so the premium
// feel comes from glassmorphism (translucent + blurred glow) and motion instead of
// character art.
@Composable
private fun OnboardingHero(page: OnboardingPage, pageOffset: Float) {
    val infinite = rememberInfiniteTransition(label = "heroFloat")
    val floatOffset by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatOffset"
    )
    val clampedOffset = pageOffset.coerceIn(0f, 1f)
    val scale = 1f - (clampedOffset * 0.15f)
    val fade = 1f - (clampedOffset * 0.6f)

    Box(
        modifier = Modifier
            .size(220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = fade
                translationY = floatOffset
            },
        contentAlignment = Alignment.Center
    ) {
        page.satelliteIcons.forEachIndexed { index, icon ->
            val angle = Math.toRadians((360.0 / page.satelliteIcons.size) * index + 30.0)
            val radius = 96f
            val x = (radius * cos(angle)).toFloat().dp
            val y = (radius * sin(angle)).toFloat().dp
            val chipColor = if (index % 2 == 0) GoldAccent else EmeraldPrimary
            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, chipColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = chipColor, modifier = Modifier.size(18.dp))
            }
        }

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(
                    Brush.radialGradient(listOf(EmeraldPrimary.copy(alpha = 0.45f), Color.Transparent)),
                    CircleShape
                )
                .blur(36.dp)
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(GoldAccent.copy(alpha = 0.6f), EmeraldPrimary.copy(alpha = 0.35f))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.heroIcon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}
