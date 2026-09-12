package com.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.ui.components.EthiopianTricolorBar
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

// ─── Habte Executive Dark Fintech Palette ──────────────────────────────────────
private val OnboardingBg = Color(0xFF07120D)
private val OnboardingSurface = Color(0xFF0D1B14)
private val OnboardingElevated = Color(0xFF12261B)
private val OnboardingBorder = Color(0xFF1A3828)
private val EmeraldPrimary = Color(0xFF00C853)
private val EmeraldDeep = Color(0xFF008F3C)
private val EmeraldDark = Color(0xFF064E3B)
private val GoldAccent = Color(0xFFFFD54F)
private val SoftGold = Color(0xFFFFF3C4)
private val GoldMuted = Color(0xFFB3923B)

private val OnboardingTextPrimary = Color(0xFFF5FFF8)
private val OnboardingTextMuted = Color(0xFF7F9187)
private val OnboardingTextSubtle = Color(0xFF4D6156)
private val ButtonTextColor = Color(0xFF031A0C)

private data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val amharicTagline: String,
    val heroIcon: ImageVector,
    val satelliteIcons: List<ImageVector>
)

private val ONBOARDING_PAGES = listOf(
    OnboardingPageData(
        title = "Welcome to Habte",
        subtitle = "Your intelligent companion for personal wealth and financial clarity.",
        amharicTagline = "እንኳን ወደ ሀብተ በደህና መጡ — አስተማማኝ የፋይናንስ አጋርዎ",
        heroIcon = Icons.Default.AccountBalanceWallet,
        satelliteIcons = listOf(
            Icons.Default.AccountBalance,
            Icons.Default.CreditCard,
            Icons.Default.Savings,
            Icons.Default.Payments
        )
    ),
    OnboardingPageData(
        title = "Understand Your Money",
        subtitle = "Automatic tracking of income, expenses, and savings in real time.",
        amharicTagline = "ገቢና ወጪዎን በቀላሉ እና በትክክል ይከታተሉ",
        heroIcon = Icons.Default.Analytics,
        satelliteIcons = listOf(
            Icons.AutoMirrored.Filled.ReceiptLong,
            Icons.Default.PieChart,
            Icons.Default.AutoGraph,
            Icons.Default.Smartphone
        )
    ),
    OnboardingPageData(
        title = "Bank-Grade Privacy",
        subtitle = "Your SMS and financial records stay strictly on your device.",
        amharicTagline = "የፋይናንስ መረጃዎ በከፍተኛ ጥበቃ እና ሚስጥራዊነት የተጠበቀ ነው",
        heroIcon = Icons.Default.Security,
        satelliteIcons = listOf(
            Icons.Default.Fingerprint,
            Icons.Default.Lock,
            Icons.Default.VpnKey,
            Icons.Default.VerifiedUser
        )
    ),
    OnboardingPageData(
        title = "Build Your Future",
        subtitle = "Set actionable budgets, meet your targets, and achieve financial freedom.",
        amharicTagline = "የወደፊት የሀብት ግቦችዎን ያቅዱ እና ያሳኩ",
        heroIcon = Icons.Default.RocketLaunch,
        satelliteIcons = listOf(
            Icons.AutoMirrored.Filled.TrendingUp,
            Icons.Default.TrackChanges,
            Icons.Default.EmojiEvents,
            Icons.Default.WorkspacePremium
        )
    )
)

/**
 * Premium Ethiopian Fintech Onboarding Screen.
 * Provides a structured, responsive, and beautifully choreographed introduction to Habte.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { ONBOARDING_PAGES.size }
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.lastIndex
    val haptic = LocalHapticFeedback.current

    // Smooth entry fade-in
    val entranceAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceAnim.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBg)
            .graphicsLayer {
                alpha = entranceAnim.value
                translationY = (1f - entranceAnim.value) * 20f
            }
    ) {
        // Subtle Atmospheric Glow Layer
        AmbientGlowBackground(currentPage = pagerState.currentPage)

        // Cultural Tricolor top accent
        EthiopianTricolorBar(modifier = Modifier.align(Alignment.TopCenter))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val isCompactScreen = maxHeight < 680.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top Bar: Step Indicator & Skip Action ────────────────────────
                TopHeaderBar(
                    currentPage = pagerState.currentPage,
                    totalPages = ONBOARDING_PAGES.size,
                    isLastPage = isLastPage,
                    onSkip = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFinished()
                    }
                )

                // ── Horizontal Pager (Hero, Titles, Cultural Divider) ────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIdx ->
                    val pageOffset = ((pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction)
                    PremiumPageContent(
                        page = ONBOARDING_PAGES[pageIdx],
                        pageOffset = pageOffset,
                        pageIdx = pageIdx,
                        isCompact = isCompactScreen
                    )
                }

                // ── Bottom Section: Refined Progress Indicator & CTA Button ──────
                BottomControlSection(
                    currentPage = pagerState.currentPage,
                    totalPages = ONBOARDING_PAGES.size,
                    isLastPage = isLastPage,
                    onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isLastPage) {
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

// ── Top Navigation & Brand Header ───────────────────────────────────────────────

@Composable
private fun TopHeaderBar(
    currentPage: Int,
    totalPages: Int,
    isLastPage: Boolean,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Habte Brand Chip + Step Metadata
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OnboardingSurface.copy(alpha = 0.75f))
                .border(1.dp, OnboardingBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            TricolorFlourish()
            Spacer(modifier = Modifier.width(8.dp))
            Crossfade(targetState = currentPage, label = "stepBadge") { page ->
                Text(
                    text = "STEP ${page + 1} OF $totalPages",
                    color = EmeraldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // Secondary Skip Action (48dp Touch Target)
        AnimatedVisibility(
            visible = !isLastPage,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            PremiumSkipButton(onClick = onSkip)
        }
    }
}

@Composable
private fun PremiumSkipButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "skipScale"
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Skip",
            color = OnboardingTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { role = Role.Button }
        )
    }
}

// ── Page Content Container with Parallax & Typography ─────────────────────────

@Composable
private fun PremiumPageContent(
    page: OnboardingPageData,
    pageOffset: Float,
    pageIdx: Int,
    isCompact: Boolean
) {
    val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

    // Subtle, disciplined scale and parallax (No harsh rotations)
    val scale = 1f - (absOffset * 0.08f)
    val alpha = (1f - (absOffset * 1.2f)).coerceIn(0f, 1f)
    val heroParallax = pageOffset * 30f
    val textParallax = pageOffset * 15f
    val subtleTilt = (pageOffset * -1.5f).coerceIn(-2f, 2f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── 1. Hero Illustration Container ─────────────────────────────────────
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    translationX = heroParallax
                    rotationZ = subtleTilt
                }
        ) {
            PremiumHeroContainer(
                page = page,
                pageIdx = pageIdx,
                pageOffset = pageOffset,
                isCompact = isCompact
            )
        }

        Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

        // ── 2. Ethiopian Cultural Gold Divider Accent ──────────────────────────
        RefinedEthiopianDivider(
            modifier = Modifier
                .width(100.dp)
                .graphicsLayer {
                    this.alpha = (alpha * 0.85f).coerceIn(0f, 1f)
                    scaleX = 1f - absOffset
                }
        )

        Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 18.dp))

        // ── 3. Structured Text Group ───────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha
                    translationX = textParallax
                }
        ) {
            Text(
                text = page.title,
                color = OnboardingTextPrimary,
                fontSize = if (isCompact) 24.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = if (isCompact) 30.sp else 34.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = page.subtitle,
                color = OnboardingTextMuted,
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = if (isCompact) 18.sp else 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Authentic, localized Amharic phrase
            Text(
                text = page.amharicTagline,
                color = SoftGold.copy(alpha = 0.90f),
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

// ── Multi-Layer Hero Graphics ──────────────────────────────────────────────────

@Composable
private fun PremiumHeroContainer(
    page: OnboardingPageData,
    pageIdx: Int,
    pageOffset: Float,
    isCompact: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroMotion")

    // Gentle vertical float (calm, breathing 3.6s cycle)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroFloat"
    )

    // Slow ambient hero radial glow
    val ambientGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroAmbientGlow"
    )

    // Very slow orbital rotation (20s full circle)
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    val heroContainerSize = if (isCompact) 180.dp else 210.dp
    val glassCenterSize = if (isCompact) 116.dp else 132.dp

    Box(
        modifier = Modifier
            .size(heroContainerSize)
            .graphicsLayer { translationY = floatOffset },
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Subtle Radial Ambient Glow
        Box(
            modifier = Modifier
                .size(heroContainerSize)
                .graphicsLayer {
                    scaleX = ambientGlowScale
                    scaleY = ambientGlowScale
                }
                .background(
                    Brush.radialGradient(
                        listOf(EmeraldPrimary.copy(alpha = 0.20f), Color.Transparent)
                    ),
                    CircleShape
                )
                .blur(28.dp)
        )

        // Layer 2: Fine Decorative Orbital Ring with Gold Node
        Canvas(
            modifier = Modifier
                .size(heroContainerSize - 20.dp)
                .graphicsLayer { rotationZ = orbitAngle }
        ) {
            val radius = size.minDimension / 2f
            drawCircle(
                color = EmeraldPrimary.copy(alpha = 0.18f),
                radius = radius,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
                )
            )
            // Orbiting gold satellite node
            drawCircle(
                color = GoldAccent,
                radius = 3.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f - radius)
            )
        }

        // Layer 3: Satellites positioned harmoniously
        page.satelliteIcons.forEachIndexed { index, icon ->
            val baseAngle = Math.toRadians((360.0 / page.satelliteIcons.size) * index + 40.0)
            val orbitRadius = if (isCompact) 82f else 95f

            val satPulse by infiniteTransition.animateFloat(
                initialValue = -2f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2800 + (index * 400), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "satPulse_$index"
            )

            val x = (orbitRadius * cos(baseAngle)).toFloat().dp
            val y = (orbitRadius * sin(baseAngle)).toFloat().dp
            val accentColor = if (index % 2 == 0) GoldAccent else EmeraldPrimary

            Box(
                modifier = Modifier
                    .offset(x = x, y = y + satPulse.dp)
                    .size(if (isCompact) 36.dp else 40.dp)
                    .clip(CircleShape)
                    .background(OnboardingSurface.copy(alpha = 0.90f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                )
            }
        }

        // Layer 4: Central Glassmorphism Container with Page-Specific Artwork
        Box(
            modifier = Modifier
                .size(glassCenterSize)
                .clip(CircleShape)
                .background(OnboardingElevated.copy(alpha = 0.92f))
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            GoldAccent.copy(alpha = 0.55f),
                            EmeraldPrimary.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (pageIdx) {
                0 -> WalletHeroVisual(infiniteTransition = infiniteTransition)
                1 -> DashboardHeroVisual(infiniteTransition = infiniteTransition)
                2 -> SecurityHeroVisual(infiniteTransition = infiniteTransition)
                3 -> FutureHeroVisual(infiniteTransition = infiniteTransition)
            }
        }

        // Layer 5: Intentional Habte "ሀ" Branding Signature Badge
        HabteBrandSignatureBadge(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (-4).dp)
        )
    }
}

// ── Specific Visuals per Page ──────────────────────────────────────────────────

@Composable
private fun WalletHeroVisual(infiniteTransition: androidx.compose.animation.core.InfiniteTransition) {
    val highlightSweep by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walletHighlight"
    )
    val coinOrbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coinOrbit"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Inner depth circle
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    Brush.radialGradient(listOf(EmeraldPrimary.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                )
        )

        // 2 Elliptical Orbiting Gold/Emerald Micro-Tokens
        repeat(2) { i ->
            val rad = Math.toRadians((coinOrbit + (i * 180)).toDouble())
            val cx = (36 * cos(rad)).toFloat().dp
            val cy = (14 * sin(rad)).toFloat().dp
            val tokenColor = if (i == 0) GoldAccent else EmeraldPrimary

            Box(
                modifier = Modifier
                    .offset(x = cx, y = cy)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(tokenColor)
                    .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
        }

        // Central Wallet Icon with Light Sweep
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier.size(46.dp)
        )

        // Gentle light accent line
        Canvas(modifier = Modifier.size(46.dp)) {
            drawLine(
                color = GoldAccent.copy(alpha = highlightSweep),
                start = Offset(size.width * 0.15f, size.height * 0.35f),
                end = Offset(size.width * 0.85f, size.height * 0.65f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

@Composable
private fun DashboardHeroVisual(infiniteTransition: androidx.compose.animation.core.InfiniteTransition) {
    val bar1 by infiniteTransition.animateFloat(0.55f, 0.95f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(0.40f, 0.85f, infiniteRepeatable(tween(2900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(0.70f, 1.05f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b3")
    val bar4 by infiniteTransition.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(3100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b4")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Growth percentage badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GoldAccent.copy(alpha = 0.16f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("↗ +24%", color = GoldAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val usableW = size.width - 16f
            val usableH = size.height - 20f
            val startX = 8f
            val startY = size.height - 8f
            val step = usableW / 3f

            val barHeights = floatArrayOf(bar1, bar2, bar3, bar4)
            val points = mutableListOf<Offset>()

            // Draw subtle horizontal grid lines
            repeat(3) { g ->
                val gy = startY - (usableH * 0.33f * (g + 1))
                drawLine(
                    color = OnboardingBorder.copy(alpha = 0.4f),
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 0.8.dp.toPx()
                )
            }

            // Draw Bars
            for (i in 0..3) {
                val bx = startX + i * step
                val bh = barHeights[i] * usableH * 0.65f
                val by = startY - bh

                drawRoundRect(
                    color = if (i % 2 == 0) EmeraldPrimary.copy(alpha = 0.85f) else EmeraldDeep.copy(alpha = 0.90f),
                    topLeft = Offset(bx - 5f, by),
                    size = Size(10f, bh),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                points.add(Offset(bx, by))
            }

            // Draw Growth Trajectory Line
            val path = Path()
            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }
                drawPath(
                    path = path,
                    color = GoldAccent,
                    style = Stroke(width = 1.6.dp.toPx())
                )

                // Draw Data Points
                points.forEach { pt ->
                    drawCircle(color = GoldAccent, radius = 2.5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 4.dp.toPx(), center = pt, style = Stroke(0.6.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun SecurityHeroVisual(infiniteTransition: androidx.compose.animation.core.InfiniteTransition) {
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing)),
        label = "secRingPulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing)),
        label = "secRingAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Expanding protection ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = EmeraldPrimary.copy(alpha = ringAlpha),
                radius = (54f * ringPulse).dp.toPx(),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Glass Shield
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(EmeraldPrimary.copy(alpha = 0.85f), EmeraldDark.copy(alpha = 0.95f))
                    )
                )
                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = OnboardingTextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Checkmark Verification Badge
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = 18.dp, y = (-18).dp)
                .clip(CircleShape)
                .background(GoldAccent)
                .border(1.dp, OnboardingSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ButtonTextColor,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
private fun FutureHeroVisual(infiniteTransition: androidx.compose.animation.core.InfiniteTransition) {
    val rocketFloat by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "futureRocketFloat"
    )
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "starTwinkle"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Curved Trajectory
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            path.moveTo(size.width * 0.2f, size.height * 0.8f)
            path.quadraticBezierTo(
                size.width * 0.45f, size.height * 0.55f,
                size.width * 0.8f, size.height * 0.2f
            )
            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(EmeraldPrimary, GoldAccent)),
                style = Stroke(width = 1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
            )
        }

        // Goal Star Beacon
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 10.dp)
                .size(12.dp)
                .graphicsLayer { alpha = starTwinkle }
        )

        // Rocket Icon with micro-float
        Icon(
            imageVector = Icons.Default.RocketLaunch,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { translationY = rocketFloat.dp.toPx() }
        )
    }
}

// ── Habte Brand Signature Badge ────────────────────────────────────────────────

@Composable
private fun HabteBrandSignatureBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(EmeraldPrimary, EmeraldDeep)
                )
            )
            .border(1.2.dp, GoldAccent.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ሀ",
            color = OnboardingTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Bottom Progress Indicator & Action CTA ──────────────────────────────────────

@Composable
private fun BottomControlSection(
    currentPage: Int,
    totalPages: Int,
    isLastPage: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Refined Segmented Progress Indicator ───────────────────────────────
        PremiumProgressIndicator(
            currentPage = currentPage,
            totalPages = totalPages
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Primary Action CTA Button ──────────────────────────────────────────
        PremiumPrimaryButton(
            isLastPage = isLastPage,
            onClick = onNext
        )
    }
}

@Composable
private fun PremiumProgressIndicator(
    currentPage: Int,
    totalPages: Int
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { idx ->
            val isActive = idx == currentPage
            val width by animateDpAsState(
                targetValue = if (isActive) 28.dp else 8.dp,
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                label = "progressWidth"
            )

            val bgBrush = if (isActive) {
                Brush.horizontalGradient(listOf(EmeraldPrimary, GoldAccent))
            } else {
                Brush.horizontalGradient(listOf(OnboardingElevated, OnboardingElevated))
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.5.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(99.dp))
                    .background(bgBrush)
                    .border(
                        width = 0.6.dp,
                        color = if (isActive) Color.Transparent else OnboardingBorder.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(99.dp)
                    )
            )
        }
    }
}

@Composable
private fun PremiumPrimaryButton(
    isLastPage: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ctaShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -350f,
        targetValue = 650f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "btnShimmer"
    )

    val buttonText = if (isLastPage) "Get Started" else "Continue"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(EmeraldPrimary, EmeraldDeep)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(SoftGold.copy(alpha = 0.6f), EmeraldPrimary.copy(alpha = 0.4f))),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = buttonText
            }
    ) {
        // Very subtle luxury shimmer highlight sweep
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .offset(x = shimmerOffset.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(targetState = buttonText, label = "btnLabel") { label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        color = ButtonTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ButtonTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Ethiopian Cultural Accents & Ambient Effects ────────────────────────────────

@Composable
private fun TricolorFlourish() {
    Row(
        modifier = Modifier
            .height(8.dp)
            .width(18.dp)
            .clip(RoundedCornerShape(2.dp))
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(EmeraldPrimary))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GoldAccent))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFDA121A)))
    }
}

@Composable
private fun RefinedEthiopianDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(10.dp)) {
        val count = 4
        val step = size.width / count
        val strokeWidth = 1.dp.toPx()
        for (i in 0 until count) {
            val cx = step * i + step / 2f
            val cy = size.height / 2f
            val r = step * 0.28f

            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(path, color = GoldMuted.copy(alpha = 0.35f), style = Stroke(width = strokeWidth))
            drawCircle(color = GoldAccent.copy(alpha = 0.6f), radius = strokeWidth * 0.7f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun AmbientGlowBackground(currentPage: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientBg")

    val glowBreath by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowBreath"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Top Ambient Emerald Light
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-140).dp)
                .graphicsLayer {
                    scaleX = glowBreath
                    scaleY = glowBreath
                    alpha = 0.22f
                }
                .background(
                    Brush.radialGradient(listOf(EmeraldPrimary, Color.Transparent)),
                    CircleShape
                )
                .blur(120.dp)
        )

        // Bottom Soft Gold Accent
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 90.dp)
                .graphicsLayer { alpha = 0.12f }
                .background(
                    Brush.radialGradient(listOf(GoldAccent, Color.Transparent)),
                    CircleShape
                )
                .blur(100.dp)
        )
    }
}

