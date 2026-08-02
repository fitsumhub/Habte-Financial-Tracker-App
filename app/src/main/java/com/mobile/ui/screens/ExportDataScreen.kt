package com.mobile.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.mobile.data.FinanceRepository
import com.mobile.data.Transaction
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by FinanceRepository.transactions.collectAsState()

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    val csv = java.lang.StringBuilder("Date,Title,Amount,Type,Category,Bank\n")
                    transactions.forEach { t ->
                        csv.append("${t.date},${t.title},${t.amount},${t.type},${t.category},${t.bankShortName}\n")
                    }
                    writer.write(csv.toString())
                    writer.flush()
                }
                Toast.makeText(context, "CSV saved successfully!", Toast.LENGTH_LONG).show()
                // Interstitials belong after a non-critical action completes — exporting
                // data (not a financial task itself) is exactly that.
                (context as? android.app.Activity)?.let { activity ->
                    com.mobile.ads.AdMobService.showInterstitialIfLoaded(activity)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    val jsonArray = JSONArray()
                    transactions.forEach { t ->
                        val obj = JSONObject()
                        obj.put("id", t.id)
                        obj.put("title", t.title)
                        obj.put("amount", t.amount)
                        obj.put("type", t.type)
                        obj.put("date", t.date)
                        obj.put("time", t.time)
                        obj.put("category", t.category)
                        obj.put("bankShortName", t.bankShortName)
                        obj.put("balance", t.balance)
                        obj.put("accountSuffix", t.accountSuffix)
                        obj.put("reason", t.reason)
                        jsonArray.put(obj)
                    }
                    writer.write(jsonArray.toString(4))
                    writer.flush()
                }
                Toast.makeText(context, "JSON saved successfully!", Toast.LENGTH_LONG).show()
                // Interstitials belong after a non-critical action completes — exporting
                // data (not a financial task itself) is exactly that.
                (context as? android.app.Activity)?.let { activity ->
                    com.mobile.ads.AdMobService.showInterstitialIfLoaded(activity)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isRestoring by remember { mutableStateOf(false) }
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isRestoring = true
            scope.launch {
                try {
                    val text = context.contentResolver.openInputStream(it)?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    }
                    if (text.isNullOrBlank()) {
                        Toast.makeText(context, "That file is empty.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val array = JSONArray(text)
                    val restored = mutableListOf<Transaction>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        restored += Transaction(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            date = obj.getString("date"),
                            type = obj.getString("type"),
                            bankShortName = obj.getString("bankShortName"),
                            category = obj.optString("category", "Other"),
                            balance = if (obj.isNull("balance")) null else obj.optDouble("balance"),
                            accountSuffix = if (obj.isNull("accountSuffix")) null else obj.optString("accountSuffix"),
                            time = obj.optString("time", ""),
                            reason = obj.optString("reason", "")
                        )
                    }
                    FinanceRepository.restoreTransactions(restored)
                    Toast.makeText(context, "Restored ${restored.size} transaction(s).", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Couldn't read backup: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Export Data",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Download your financial data for backup or external analysis. Files are saved wherever you choose on your device — keep them somewhere private.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Export as CSV
            ExportOptionRow(
                title = "Export as CSV",
                subtitle = "Best for Excel or Google Sheets",
                icon = Icons.Default.Description,
                onClick = {
                    if (transactions.isEmpty()) {
                        Toast.makeText(context, "No transactions to export.", Toast.LENGTH_SHORT).show()
                    } else {
                        createCsvLauncher.launch("Habte_Export_${System.currentTimeMillis()}.csv")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Export as JSON
            ExportOptionRow(
                title = "Export as JSON",
                subtitle = "Raw data for developers or backup",
                icon = Icons.Default.Code,
                onClick = {
                    if (transactions.isEmpty()) {
                        Toast.makeText(context, "No transactions to export.", Toast.LENGTH_SHORT).show()
                    } else {
                        createJsonLauncher.launch("Habte_Export_${System.currentTimeMillis()}.json")
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Restore Backup",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Bring transactions back from a JSON file exported by Habte. Existing transactions are never overwritten.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ExportOptionRow(
                title = if (isRestoring) "Restoring…" else "Import from JSON",
                subtitle = "Select a previously exported .json backup",
                icon = Icons.Default.Upload,
                loading = isRestoring,
                onClick = { openJsonLauncher.launch(arrayOf("application/json")) }
            )
        }
    }
}

@Composable
fun ExportOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(enabled = !loading) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
