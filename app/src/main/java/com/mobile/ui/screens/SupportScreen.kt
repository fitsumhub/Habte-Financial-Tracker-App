package com.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var expandedFaq by remember { mutableStateOf(-1) }

    val faqs = listOf(
        "How do I add a bank account?" to "Go to the Home tab, tap the + button, select your bank from the list, and enter your account details.",
        "How does SMS sync work?" to "The app reads incoming bank SMS messages to automatically detect and log your transactions. Grant SMS permission when prompted.",
        "Is my data secure?" to "Yes! All data is stored locally on your device. We never upload your financial information to any server.",
        "How do I export my data?" to "Navigate to Tools > Export Data and choose CSV or JSON format to download your transaction history.",
        "How do I change my PIN?" to "Go to Tools > Security > Change App PIN to update your 4-digit security code."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Support", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp)
        ) {
            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("We're here to help!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("24/7 support for all your needs", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Options
            Text("Contact Us", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

            SupportContactRow(
                icon = Icons.Default.Phone,
                title = "Call Support",
                subtitle = "+251925709550",
                color = Color(0xFF059669),
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+251925709550"))
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SupportContactRow(
                icon = Icons.Default.Email,
                title = "Email Us",
                subtitle = "Fitsumenunu21@gmail.com",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:Fitsumenunu21@gmail.com"))
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SupportContactRow(
                icon = Icons.Default.Send,
                title = "Telegram",
                subtitle = "@AplusHustler",
                color = Color(0xFF0EA5E9),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/AplusHustler"))
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Rewarded ad demo — a purely optional, non-critical action (never gates any
            // real feature), which is exactly what rewarded ads are for. Habte has no
            // premium tier today, so this just says thanks; a future paid feature could
            // reuse the same AdMobService.showRewardedIfLoaded call as its unlock trigger.
            SupportTheAppRow(context = context)

            Spacer(modifier = Modifier.height(28.dp))

            // FAQ
            Text("Frequently Asked Questions", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

            faqs.forEachIndexed { index, (question, answer) ->
                val isExpanded = expandedFaq == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { expandedFaq = if (isExpanded) -1 else index }
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(question, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(answer, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Optional, non-critical rewarded-ad action — never gates a real feature, just says thanks. */
@Composable
private fun SupportTheAppRow(context: android.content.Context) {
    var isShowingAd by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(enabled = !isShowingAd) {
                val activity = context as? android.app.Activity ?: return@clickable
                isShowingAd = true
                com.mobile.ads.AdMobService.showRewardedIfLoaded(
                    activity = activity,
                    onReward = {
                        Toast.makeText(context, "Thanks for supporting Habte!", Toast.LENGTH_LONG).show()
                    },
                    onClosed = { isShowingAd = false }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Support Habte", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Watch a short ad — totally optional", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        if (isShowingAd) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SupportContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
