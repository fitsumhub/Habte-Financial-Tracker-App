package com.mobile.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.data.FinanceRepository
import java.io.OutputStreamWriter
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(onBack: () -> Unit) {
    val context = LocalContext.current
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
                        obj.put("category", t.category)
                        obj.put("bankShortName", t.bankShortName)
                        jsonArray.put(obj)
                    }
                    writer.write(jsonArray.toString(4))
                    writer.flush()
                }
                Toast.makeText(context, "JSON saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070912))
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Export Data",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Download your financial data for backup or external analysis. All files are encrypted before export.",
                color = Color(0xFF64748B),
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
        }
    }
}

@Composable
fun ExportOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0E1527), Color(0xFF161E36))))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF64748B), fontSize = 13.sp)
        }
        Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF6366F1))
    }
}
