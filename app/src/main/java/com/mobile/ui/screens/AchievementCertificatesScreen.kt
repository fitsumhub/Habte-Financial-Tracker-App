package com.mobile.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mobile.data.Certificate
import com.mobile.data.CertificateCategory
import com.mobile.data.CertificateContent
import com.mobile.data.CertificateExporter
import com.mobile.data.CertificatePeriod
import com.mobile.data.CertificateRenderer
import com.mobile.data.CertificateRepository
import com.mobile.data.CertificateTemplate
import com.mobile.data.Data
import com.mobile.data.FinanceRepository
import com.mobile.data.SettingsRepository
import com.mobile.data.computeCertificateAchievement
import com.mobile.ui.theme.LocalEthiopianColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? = try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    }
} catch (_: Exception) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementCertificatesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = LocalEthiopianColors.current

    val transactions by FinanceRepository.transactions.collectAsState()
    val savedCertificates by CertificateRepository.certificates.collectAsState()
    val userNameSetting by SettingsRepository.userName.collectAsState()
    val savedProfilePhotoUri by SettingsRepository.profilePhotoUri.collectAsState()

    var selectedPeriod by remember { mutableStateOf(CertificatePeriod.MONTHLY) }
    var selectedCategory by remember { mutableStateOf(CertificateCategory.OVERALL_MASTERY) }
    var selectedTemplate by remember { mutableStateOf(CertificateTemplate.CLASSIC_GOLD) }
    var nameInput by remember(userNameSetting) { mutableStateOf(userNameSetting) }
    var photoUri by remember { mutableStateOf<Uri?>(savedProfilePhotoUri?.let { Uri.parse(it) }) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }
    var showDownloadMenu by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }
    var showFullscreenPreview by remember { mutableStateOf(false) }

    var savedForBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedFile by remember { mutableStateOf<File?>(null) }

    val dateLabel = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()) }
    val achievement = remember(transactions, selectedPeriod, selectedCategory) {
        computeCertificateAchievement(transactions, selectedPeriod, selectedCategory)
    }

    // Load initial profile photo if available
    LaunchedEffect(savedProfilePhotoUri) {
        if (photoBitmap == null && savedProfilePhotoUri != null) {
            val bmp = withContext(Dispatchers.IO) {
                decodeBitmapFromUri(context, Uri.parse(savedProfilePhotoUri))
            }
            if (bmp != null) photoBitmap = bmp
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { decodeBitmapFromUri(context, uri) }
                photoBitmap = bmp
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Medallion portrait attached", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Document creation launchers for manual download exports
    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { destUri ->
        val bitmapToExport = renderedBitmap
        if (destUri != null && bitmapToExport != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val tempFile = CertificateExporter.savePng(
                        context,
                        bitmapToExport,
                        "Habte_${selectedTemplate.name}_${selectedPeriod.name}.png"
                    )
                    CertificateExporter.copyToUri(context, tempFile, destUri)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "High-Res PNG Certificate Exported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveJpgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destUri ->
        val bitmapToExport = renderedBitmap
        if (destUri != null && bitmapToExport != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val tempFile = CertificateExporter.saveJpg(
                        context,
                        bitmapToExport,
                        "Habte_${selectedTemplate.name}_${selectedPeriod.name}.jpg"
                    )
                    CertificateExporter.copyToUri(context, tempFile, destUri)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "JPEG Certificate Exported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        val bitmapToExport = renderedBitmap
        if (destUri != null && bitmapToExport != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val tempPdf = File(context.cacheDir, "Habte_Certificate_${System.currentTimeMillis()}.pdf")
                    CertificateExporter.savePdf(bitmapToExport, tempPdf)
                    CertificateExporter.copyToUri(context, tempPdf, destUri)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Official PDF Document Exported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic rendering pipeline
    LaunchedEffect(nameInput, photoBitmap, achievement, selectedPeriod, selectedTemplate, dateLabel) {
        isRendering = true
        val content = CertificateContent(
            recipientName = nameInput.ifBlank { "Account Holder" },
            photo = photoBitmap,
            period = selectedPeriod,
            periodLabel = achievement.periodLabel,
            achievementTitle = achievement.title,
            achievementSubtitle = achievement.subtitle,
            dateLabel = dateLabel,
            template = selectedTemplate,
            achievement = achievement
        )
        renderedBitmap = withContext(Dispatchers.Default) { CertificateRenderer.render(content) }
        isRendering = false
    }

    suspend fun ensureSaved(): File {
        val bitmap = renderedBitmap ?: error("Certificate not rendered yet")
        val alreadySaved = savedFile
        if (savedForBitmap === bitmap && alreadySaved != null) return alreadySaved
        val timestamp = System.currentTimeMillis()
        val file = withContext(Dispatchers.IO) {
            val pngFile = CertificateExporter.savePng(context, bitmap, "Habte_Certificate_${selectedPeriod.name}_$timestamp.png")
            val photoPath = photoUri?.let { uri -> CertificateExporter.savePhoto(context, uri, "certificate_photo_$timestamp.jpg") }
            CertificateRepository.save(
                Certificate(
                    period = selectedPeriod,
                    periodLabel = achievement.periodLabel,
                    userName = nameInput.ifBlank { "Account Holder" },
                    photoPath = photoPath,
                    achievementTitle = achievement.title,
                    achievementSubtitle = achievement.subtitle,
                    generatedAtMillis = timestamp,
                    imagePath = pngFile.absolutePath
                )
            )
            pngFile
        }
        savedForBitmap = bitmap
        savedFile = file
        return file
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambientCert")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // ── TOP EXECUTIVE APP BAR ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Honors & Certificates",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = colors.goldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Official Verified Milestone Diplomas",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Vault / Gallery Icon with Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showVaultSheet = true
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Awards Vault",
                        tint = colors.goldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vault (${savedCertificates.size})",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── SCROLLABLE DESIGNER WORKSPACE ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. HERO 3D FLOATING CERTIFICATE PREVIEW ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                colors.surfaceElevated,
                                colors.surface,
                                Color.Black
                            )
                        )
                    )
                    .border(1.2.dp, colors.goldAccent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .clickable {
                        if (renderedBitmap != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFullscreenPreview = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ambient pulsating gold/emerald aura behind preview
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .graphicsLayer {
                            scaleX = glowPulse
                            scaleY = glowPulse
                            alpha = 0.16f
                        }
                        .background(
                            Brush.radialGradient(listOf(colors.goldAccent, Color.Transparent)),
                            CircleShape
                        )
                        .blur(80.dp)
                )

                val previewBitmap = renderedBitmap
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Certificate Preview",
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .aspectRatio(1200f / 1600f)
                            .shadow(16.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.goldAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    )
                }

                if (isRendering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = colors.goldAccent,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Engraving Certificate…",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Floating "Tap to Zoom" Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(0.8.dp, colors.goldAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = colors.goldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fullscreen",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 2. VERIFIED FINANCIAL INTELLIGENCE STRIP ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = colors.emeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verified Ledger Intelligence",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Discipline Score Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.goldAccent.copy(alpha = 0.15f))
                                .border(0.8.dp, colors.goldAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Discipline: ${achievement.disciplineScore}/100",
                                color = colors.goldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4 Key Metric Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Period Inflow",
                            value = "ETB ${Data.formatBalance(achievement.totalIncome)}",
                            color = colors.income
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Period Outflow",
                            value = "ETB ${Data.formatBalance(achievement.totalExpense)}",
                            color = colors.expense
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Net Retained",
                            value = "ETB ${Data.formatBalance(achievement.netSaved)}",
                            color = if (achievement.netSaved >= 0) colors.emeraldPrimary else colors.expense
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Savings Rate",
                            value = "${achievement.savingsRatePercent?.toInt() ?: 0}%",
                            color = colors.goldAccent
                        )
                    }

                    // Capital Retention Visual Progress Bar
                    val savedFraction = ((achievement.savingsRatePercent ?: 0.0) / 100.0).coerceIn(0.0, 1.0).toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Capital Retention Velocity",
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${(savedFraction * 100).toInt()}% Retained",
                                color = colors.emeraldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.expense.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(savedFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(colors.emeraldPrimary, colors.goldAccent)
                                        )
                                    )
                            )
                        }
                    }

                    // Deep Financial Analytics Sub-chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated)
                                .border(0.6.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Solvency Factor",
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", achievement.solvencyRatio)}x (${achievement.solvencyTier})",
                                    color = colors.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated)
                                .border(0.6.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Primary Outflow",
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = achievement.topCategory,
                                    color = colors.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated)
                                .border(0.6.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Audit Depth",
                                    color = colors.textMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${achievement.transactionCount} Verified Txns",
                                    color = colors.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. MILESTONE HONORS FOCUS (CATEGORY SELECTOR) ──────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Honors Milestone Focus",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CertificateCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val catIcon = when (cat) {
                            CertificateCategory.OVERALL_MASTERY -> Icons.Default.AutoAwesome
                            CertificateCategory.SAVINGS_CHAMPION -> Icons.Default.Savings
                            CertificateCategory.DISCIPLINED_BUDGET -> Icons.Default.Shield
                            CertificateCategory.TRANSACTION_VANGUARD -> Icons.Default.ReceiptLong
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.emeraldPrimary else colors.surfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.goldAccent else colors.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else colors.goldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.label,
                                    color = if (isSelected) Color.White else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── 4. PERIOD TIMELINE SELECTOR ────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Accounting Timeframe",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                ) {
                    CertificatePeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.emeraldPrimary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedPeriod = period
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period.label,
                                color = if (isSelected) Color.White else colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── 5. LUXURY EXECUTIVE TEMPLATE STUDIO (6 STYLES) ─────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Executive Certificate Style",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "6 Bespoke Diplomas",
                        color = colors.goldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val templates = CertificateTemplate.values()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in templates.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 0..1) {
                                if (i + j < templates.size) {
                                    val tmpl = templates[i + j]
                                    val isSelected = selectedTemplate == tmpl
                                    TemplateSelectionCard(
                                        modifier = Modifier.weight(1f),
                                        template = tmpl,
                                        isSelected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedTemplate = tmpl
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. RECIPIENT & MEDALLION PHOTO STUDIO ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Award Personalization",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Recipient Name on Certificate") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = colors.goldAccent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated,
                            focusedBorderColor = colors.goldAccent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Photo Medallion Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.2.dp, colors.goldAccent, CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val photo = photoBitmap
                                if (photo != null) {
                                    Image(
                                        bitmap = photo.asImageBitmap(),
                                        contentDescription = "Medallion Portrait",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Upload Photo",
                                        tint = colors.goldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (photoBitmap != null) "Medallion Photo Active" else "Add Portrait Medallion",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Embossed into official wax seal frame",
                                    color = colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (photoBitmap != null) {
                            TextButton(
                                onClick = {
                                    photoUri = null
                                    photoBitmap = null
                                    Toast.makeText(context, "Portrait removed", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Remove", color = colors.expense, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── 7. STICKY BOTTOM ACTION BAR (SHARE & EXPORT) ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Share Award Button
                Button(
                    onClick = {
                        if (renderedBitmap != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                val file = ensureSaved()
                                val shareIntent = CertificateExporter.shareIntent(context, file, "image/png")
                                context.startActivity(shareIntent)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, colors.goldAccent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = colors.goldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Award",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Download / Export Multi-Format Menu Button
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { showDownloadMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export Award",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = showDownloadMenu,
                        onDismissRequest = { showDownloadMenu = false },
                        modifier = Modifier.background(colors.surfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("High-Resolution PNG (Lossless)", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = colors.emeraldPrimary) },
                            onClick = {
                                showDownloadMenu = false
                                savePngLauncher.launch("Habte_${selectedTemplate.name}_${selectedPeriod.name}.png")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("JPEG Image (Fast)", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null, tint = colors.goldAccent) },
                            onClick = {
                                showDownloadMenu = false
                                saveJpgLauncher.launch("Habte_${selectedTemplate.name}_${selectedPeriod.name}.jpg")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Official PDF Document (A4 Printable)", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.expense) },
                            onClick = {
                                showDownloadMenu = false
                                savePdfLauncher.launch("Habte_${selectedTemplate.name}_${selectedPeriod.name}.pdf")
                            }
                        )
                    }
                }
            }
        }
    }

    // ── FULLSCREEN PREVIEW MODAL ───────────────────────────────────────────────
    if (showFullscreenPreview && renderedBitmap != null) {
        Dialog(
            onDismissRequest = { showFullscreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { showFullscreenPreview = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    val fullBitmap = renderedBitmap
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap.asImageBitmap(),
                            contentDescription = "Full Certificate Preview",
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .aspectRatio(1200f / 1600f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, colors.goldAccent, RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showFullscreenPreview = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Fullscreen Preview", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ── SAVED CERTIFICATES VAULT MODAL SHEET ───────────────────────────────────
    if (showVaultSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVaultSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = colors.goldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Honors & Awards Vault",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { showVaultSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textMuted)
                    }
                }

                Text(
                    text = "Historical record of your verified financial achievements.",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (savedCertificates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No saved certificates yet",
                                color = colors.textMuted,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Generate and export an official award to store it in your vault.",
                                color = colors.textMuted.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedCertificates, key = { it.id }) { cert ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = File(cert.imagePath),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = cert.achievementTitle,
                                                color = colors.goldAccent,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${cert.userName} · ${cert.period.label}",
                                                color = colors.textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                                .format(Date(cert.generatedAtMillis))
                                            Text(
                                                text = "Awarded: $dateStr",
                                                color = colors.textMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val file = File(cert.imagePath)
                                                if (file.exists()) {
                                                    val shareIntent = CertificateExporter.shareIntent(context, file, "image/png")
                                                    context.startActivity(shareIntent)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = colors.goldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch { CertificateRepository.delete(cert) }
                                                Toast.makeText(context, "Certificate deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = colors.expense,
                                                modifier = Modifier.size(18.dp)
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
    }
}

// ── HELPER COMPONENT: METRIC TILE ──────────────────────────────────────────────
@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    val colors = LocalEthiopianColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .border(0.6.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Column {
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── HELPER COMPONENT: TEMPLATE SELECTION CARD ──────────────────────────────────
@Composable
private fun TemplateSelectionCard(
    modifier: Modifier = Modifier,
    template: CertificateTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalEthiopianColors.current

    val (bgGradient, accentColor, subtitleText) = when (template) {
        CertificateTemplate.CLASSIC_GOLD -> Triple(
            listOf(Color(0xFF1E1B4B), Color(0xFF030712)),
            Color(0xFFFBBF24),
            "Midnight Navy & Gold"
        )
        CertificateTemplate.ROYAL_EMERALD -> Triple(
            listOf(Color(0xFF064E3B), Color(0xFF021A13)),
            Color(0xFF34D399),
            "Forest Mint Guilloché"
        )
        CertificateTemplate.ETHIOPIAN_HERITAGE -> Triple(
            listOf(Color(0xFF4A0E17), Color(0xFF130507)),
            Color(0xFFFCDD09),
            "Tricolor Tibeb Tapestry"
        )
        CertificateTemplate.BIRR_BANKNOTE -> Triple(
            listOf(Color(0xFF0F3D2E), Color(0xFF03120D)),
            Color(0xFFEFC55E),
            "National Banknote Rosette"
        )
        CertificateTemplate.PLATINUM_TITANIUM -> Triple(
            listOf(Color(0xFF27272A), Color(0xFF09090B)),
            Color(0xFFE2E8F0),
            "Obsidian & Metallic Chrome"
        )
        CertificateTemplate.SOLAR_GOLD -> Triple(
            listOf(Color(0xFF78350F), Color(0xFF1A0701)),
            Color(0xFFF59E0B),
            "Warm Amber & Golden Ochre"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else colors.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            // Gradient swatch preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(bgGradient))
                    .border(0.6.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = template.label,
                color = if (isSelected) accentColor else colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitleText,
                color = colors.textMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
