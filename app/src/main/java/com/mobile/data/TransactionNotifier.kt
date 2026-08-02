package com.mobile.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mobile.MainActivity
import com.mobile.R

/**
 * Posts a system notification for a newly parsed SMS transaction, styled as
 * "{Bank} • Money In/Out" / "{amount} • {title} • Tap to categorize" so tapping
 * it jumps straight to that transaction's categorize sheet (see MainActivity's
 * EXTRA_TRANSACTION_ID handling and HomeScreen's pendingTransactionId effect).
 */
object TransactionNotifier {
    private const val CHANNEL_ID = "transaction_alerts"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"

    private const val CREDIT_COLOR = 0xFF059669.toInt()
    private const val DEBIT_COLOR = 0xFFDC2626.toInt()

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New transactions detected from bank and wallet SMS"
            }
        )
    }

    fun notify(context: Context, transaction: Transaction) {
        if (!SettingsRepository.notificationsEnabled.value) return
        if (!SettingsRepository.smsAlerts.value) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) return

        ensureChannel(context)

        val institutionName = InstitutionCatalog.ALL
            .find { it.shortName == transaction.bankShortName }?.name
            ?: transaction.bankShortName
        val isCredit = transaction.type == "credit"
        val direction = if (isCredit) "Money In" else "Money Out"
        val sign = if (isCredit) "+" else "-"
        val amountText = "$sign${Data.formatBalance(transaction.amount)}"
        val accentColor = if (isCredit) CREDIT_COLOR else DEBIT_COLOR
        val categoryLabel = if (transaction.category == "Other") "Uncategorized" else transaction.category

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transaction.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transaction.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(NotificationIcons.build(accentColor, if (isCredit) NotificationGlyph.UP else NotificationGlyph.DOWN))
            .setColor(accentColor)
            .setContentTitle("$institutionName • $direction")
            .setContentText("$amountText • ${transaction.title} • Tap to categorize")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$amountText ETB • ${transaction.title}\n$categoryLabel • Tap to categorize this transaction")
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(transaction.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted — silently skip, transaction is still recorded.
        }
    }
}
