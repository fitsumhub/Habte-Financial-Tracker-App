package com.mobile.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mobile.data.Certificate
import com.mobile.data.CertificateContent
import com.mobile.data.CertificateExporter
import com.mobile.data.CertificatePeriod
import com.mobile.data.CertificateRenderer
import com.mobile.data.CertificateRepository
import com.mobile.data.CertificateTemplate
import com.mobile.data.FinanceRepository
import com.mobile.data.SettingsRepository
import com.mobile.data.computeCertificateAchievement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? = try {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
} catch (e: Exception) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementCertificatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by FinanceRepository.transactions.collectAsState()
    val savedCertificates by CertificateRepository.certificates.collectAsState()
    val userNameSetting by SettingsRepository.userName.collectAsState()

    var selectedPeriod by remember { mutableStateOf(CertificatePeriod.MONTHLY) }
    var selectedTemplate by remember { mutableStateOf(CertificateTemplate.CLASSIC_GOLD) }
    var nameInput by remember { mutableStateOf(userNameSetting) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }
    var showDownloadMenu by remember { mutableStateOf(false) }

    // Tracks which already-rendered Bitmap (by identity) has been persisted, so tapping
    // Download then Share on the same unchanged preview reuses one saved file/gallery entry
    // instead of writing a duplicate every time.
    var savedForBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedFile by remember { mutableStateOf<File?>(null) }

    val dateLabel = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()) }
    val achievement = remember(transactions, selectedPeriod) {
        computeCertificateAchievement(transactions, selectedPeriod)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { decodeBitmapFromUri(context, uri) }
                photoBitmap = bmp
            }
        }
    }

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
            template = selectedTemplate
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

    val pngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = ensureSaved()
            withContext(Dispatchers.IO) { CertificateExporter.copyToUri(context, file, uri) }
            Toast.makeText(context, "Certificate downloaded as PNG", Toast.LENGTH_SHORT).show()
        }
    }
    val jpgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = renderedBitmap
        if (bitmap == null) return@rememberLauncherForActivityResult
        scope.launch {
            ensureSaved()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            }
            Toast.makeText(context, "Certificate downloaded as JPG", Toast.LENGTH_SHORT).show()
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = renderedBitmap
        if (bitmap == null) return@rememberLauncherForActivityResult
        scope.launch {
            ensureSaved()
            withContext(Dispatchers.IO) {
                val temp = File(context.cacheDir, "certificate_export_temp.pdf")
                CertificateExporter.savePdf(bitmap, temp)
                CertificateExporter.copyToUri(context, temp, uri)
            }
            Toast.makeText(context, "Certificate downloaded as PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCurrent() {
        scope.launch {
            val file = ensureSaved()
            context.startActivity(CertificateExporter.shareIntent(context, file, "image/png"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Achievement Certificates", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Celebrate your progress — generate a certificate from your real tracking history and share it anywhere.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Period selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CertificatePeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            period.label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                "Recipient Name",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (photoBitmap != null) "Photo selected" else "Add a profile photo",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Optional — shown on your certificate", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                "Template",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CertificateTemplate.values().forEach { template ->
                    TemplateSwatch(
                        template = template,
                        isSelected = selectedTemplate == template,
                        onClick = { selectedTemplate = template },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                "Preview",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = renderedBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Certificate preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                    )
                }
                if (isRendering) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { showDownloadMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = renderedBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download", fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = showDownloadMenu, onDismissRequest = { showDownloadMenu = false }) {
                        DropdownMenuItem(text = { Text("PNG (best quality)") }, onClick = {
                            showDownloadMenu = false
                            pngLauncher.launch("Habte_Certificate_${selectedPeriod.label}.png")
                        })
                        DropdownMenuItem(text = { Text("JPG") }, onClick = {
                            showDownloadMenu = false
                            jpgLauncher.launch("Habte_Certificate_${selectedPeriod.label}.jpg")
                        })
                        DropdownMenuItem(text = { Text("PDF") }, onClick = {
                            showDownloadMenu = false
                            pdfLauncher.launch("Habte_Certificate_${selectedPeriod.label}.pdf")
                        })
                    }
                }
                OutlinedButton(
                    onClick = { shareCurrent() },
                    modifier = Modifier.weight(1f),
                    enabled = renderedBitmap != null
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }

            if (savedCertificates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Your Certificates",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    savedCertificates.forEach { certificate ->
                        key(certificate.id) {
                            SavedCertificateCard(certificate = certificate)
                        }
                    }
                }
            }
        }
    }
}

/** One selectable template swatch — colors here are a close approximation of CertificateRenderer's actual palette for that template, just enough to preview the vibe before rendering. */
@Composable
private fun TemplateSwatch(template: CertificateTemplate, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gradientColors = when (template) {
        CertificateTemplate.CLASSIC_GOLD -> listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
        CertificateTemplate.ETHIOPIAN_HERITAGE -> listOf(Color(0xFF0C3D24), Color(0xFF081F12))
        CertificateTemplate.BIRR_BANKNOTE -> listOf(Color(0xFF0B4D3A), Color(0xFF041A14))
    }
    val accentColor = when (template) {
        CertificateTemplate.CLASSIC_GOLD -> Color(0xFFFBBF24)
        CertificateTemplate.ETHIOPIAN_HERITAGE -> Color(0xFFFCDD09)
        CertificateTemplate.BIRR_BANKNOTE -> Color(0xFFEFC55E)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.25f))
                .border(1.5.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = accentColor, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            template.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedCertificateCard(certificate: Certificate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) { CertificateExporter.copyToUri(context, File(certificate.imagePath), uri) }
            Toast.makeText(context, "Certificate downloaded", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val file = File(certificate.imagePath)
            if (file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                )
            } else {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(certificate.achievementTitle, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${certificate.period.label} • ${certificate.periodLabel}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(dateFormat.format(Date(certificate.generatedAtMillis)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        IconButton(onClick = {
            downloadLauncher.launch("Habte_Certificate_${certificate.period.label}_${certificate.generatedAtMillis}.png")
        }) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = {
            context.startActivity(CertificateExporter.shareIntent(context, File(certificate.imagePath), "image/png"))
        }) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
