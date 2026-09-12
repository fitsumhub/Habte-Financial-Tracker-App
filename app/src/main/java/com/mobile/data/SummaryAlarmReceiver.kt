package com.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mobile.data.db.AppDatabase
import com.mobile.data.db.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fires on the AlarmManager schedule set by SummaryScheduler.
 * Fast execution: reads cached Room database first in <1ms, falling back to SMS inbox only if empty.
 */
class SummaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val frequency = intent.getStringExtra(SummaryScheduler.EXTRA_FREQUENCY) ?: "Daily"
        val appContext = context.applicationContext

        SettingsRepository.init(appContext)
        FinanceRepository.init(appContext)

        if (!SettingsRepository.notificationsEnabled.value) return
        val windowMillis = SummaryScheduler.intervalMillis(frequency) ?: (24 * 60 * 60 * 1000L)

        // Re-arm next alarm for this recurring frequency
        if (frequency in SettingsRepository.summaryFrequencies.value) {
            SummaryScheduler.schedule(appContext, frequency)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val since = System.currentTimeMillis() - windowMillis
                val transactions = loadTransactionsSince(appContext, since)
                SummaryNotifier.notify(appContext, frequency, transactions)
            } catch (e: Exception) {
                // Log and gracefully finish
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun loadTransactionsSince(context: Context, sinceMillis: Long): List<Transaction> {
        // Fast path: Query Room DB directly (instantaneous)
        try {
            val entities = AppDatabase.getInstance(context).transactionDao().getAll()
            if (entities.isNotEmpty()) {
                val domainTxs = entities.map { it.toDomain() }
                return domainTxs.filter { tx ->
                    val txTimeMillis = transactionTimestampMillis(tx) ?: parseTransactionDate(tx.date)?.timeInMillis
                    txTimeMillis != null && txTimeMillis >= sinceMillis
                }
            }
        } catch (e: Exception) {
            // fallback below
        }

        // Secondary fallback: Read SMS inbox
        return readTransactionsFromSms(context, sinceMillis)
    }

    private fun readTransactionsFromSms(context: Context, sinceMillis: Long): List<Transaction> {
        val results = mutableListOf<Transaction>()
        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                "date >= ?",
                arrayOf(sinceMillis.toString()),
                "date DESC"
            )
            cursor?.use {
                val addressIndex = it.getColumnIndex("address")
                val bodyIndex = it.getColumnIndex("body")
                val dateIndex = it.getColumnIndex("date")
                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: continue
                    val body = it.getString(bodyIndex) ?: continue
                    val date = it.getLong(dateIndex)
                    SmsParser.parseMessage(address, body, date)?.let(results::add)
                }
            }
        } catch (e: Exception) {
            // Permission or content provider error
        }
        return results
    }
}
