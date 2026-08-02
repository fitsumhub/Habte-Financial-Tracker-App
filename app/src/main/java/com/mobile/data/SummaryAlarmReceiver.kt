package com.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires on the AlarmManager schedule set by SummaryScheduler. Reads the raw SMS
 * inbox directly (rather than FinanceRepository's in-memory state) because this
 * receiver can run in a fresh process with no prior app UI session, so
 * FinanceRepository.transactions may not have been populated yet.
 */
class SummaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val frequency = intent.getStringExtra(SummaryScheduler.EXTRA_FREQUENCY) ?: return
        val appContext = context.applicationContext

        // This receiver can run in a fresh process with no prior app session (e.g.
        // overnight, app fully killed), so SettingsRepository's in-memory defaults
        // may not reflect what's actually persisted — force a reload before reading.
        SettingsRepository.init(appContext)
        if (frequency !in SettingsRepository.summaryFrequencies.value) return // disabled since this alarm was set
        if (!SettingsRepository.notificationsEnabled.value) return
        val windowMillis = SummaryScheduler.intervalMillis(frequency) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val since = System.currentTimeMillis() - windowMillis
                val transactions = readTransactionsSince(appContext, since)
                SummaryNotifier.notify(appContext, frequency, transactions)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun readTransactionsSince(context: Context, sinceMillis: Long): List<Transaction> {
        val results = mutableListOf<Transaction>()
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
        return results
    }
}
