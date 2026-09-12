package com.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val pendingIntent = intent

        CoroutineScope(Dispatchers.IO).launch {
            try {
                SettingsRepository.init(context)
                FinanceRepository.init(context)

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(pendingIntent)
                if (!messages.isNullOrEmpty()) {
                    val fullBody = StringBuilder()
                    var originatingAddress: String? = null
                    var timestamp = 0L

                    for (sms in messages) {
                        if (originatingAddress == null) originatingAddress = sms.displayOriginatingAddress
                        if (timestamp == 0L) timestamp = sms.timestampMillis
                        fullBody.append(sms.displayMessageBody)
                    }

                    val address = originatingAddress
                    val body = fullBody.toString()

                    if (!address.isNullOrBlank() && body.isNotBlank()) {
                        val parsedTx = SmsParser.parseMessage(address, body, timestamp)
                        if (parsedTx != null) {
                            FinanceRepository.addTransaction(parsedTx)
                            TransactionNotifier.notify(context, parsedTx)
                            WeeklySpendingWidgetUpdater.updateAllWidgets(context)
                        }
                    }
                }
            } catch (e: Exception) {
                // Log and gracefully finish
            } finally {
                pendingResult.finish()
            }
        }
    }
}
