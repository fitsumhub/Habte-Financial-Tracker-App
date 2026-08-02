package com.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val address = sms.displayOriginatingAddress ?: continue
                val body = sms.displayMessageBody ?: continue
                val timestamp = sms.timestampMillis

                val parsedTx = SmsParser.parseMessage(address, body, timestamp)
                if (parsedTx != null) {
                    FinanceRepository.addTransaction(parsedTx)
                    TransactionNotifier.notify(context, parsedTx)
                }
            }
        }
    }
}
