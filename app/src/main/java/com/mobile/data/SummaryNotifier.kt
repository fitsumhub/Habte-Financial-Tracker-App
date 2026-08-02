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
import kotlin.math.abs

/** Posts the periodic income/expense summary notification (see SummaryScheduler/SummaryAlarmReceiver). */
object SummaryNotifier {
    private const val CHANNEL_ID = "summary_alerts"
    private const val ACCENT_COLOR = 0xFF4F46E5.toInt()

    private fun periodLabel(frequency: String): String = when (frequency) {
        "Every 12 Hours" -> "12-Hour"
        "Daily" -> "Daily"
        "Weekly" -> "Weekly"
        "Every 15 Days" -> "15-Day"
        "Monthly" -> "Monthly"
        else -> "Spending"
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Spending Summaries", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Periodic income vs. expense summary notifications"
            }
        )
    }

    fun notify(context: Context, frequency: String, transactions: List<Transaction>) {
        if (!SettingsRepository.notificationsEnabled.value) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) return
        if (transactions.isEmpty()) return // nothing happened this period — don't nag

        ensureChannel(context)
        val notificationId = SummaryScheduler.idFor(frequency)

        val income = transactions.filter { it.type == "credit" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "debit" }.sumOf { it.amount }
        val net = income - expense
        val netSign = if (net >= 0) "+" else "-"
        val txCount = transactions.size

        val summaryText = "In: ETB ${Data.formatBalance(income)} • Out: ETB ${Data.formatBalance(expense)} • " +
            "Net: $netSign${Data.formatBalance(abs(net))}"

        // Category breakdown for the expanded (BigTextStyle) view only — the collapsed
        // contentText stays a one-line glance, this is what a user pulling the
        // notification open actually wants to see: where the money went, not just the total.
        val topCategories = topSpendingCategories(transactions, limit = 3)
        val categoryLines = topCategories.joinToString("\n") { (category, amount) ->
            val label = if (category == "Other") "Uncategorized" else category
            "• $label — ETB ${Data.formatBalance(amount)}"
        }
        val expandedText = buildString {
            append(summaryText)
            append("\n")
            append("$txCount transaction${if (txCount == 1) "" else "s"}")
            if (categoryLines.isNotEmpty()) {
                append("\n\nTop spending categories:\n")
                append(categoryLines)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(NotificationIcons.build(ACCENT_COLOR, NotificationGlyph.DOT))
            .setColor(ACCENT_COLOR)
            .setContentTitle("Your ${periodLabel(frequency)} Summary")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted — transaction data is unaffected either way.
        }
    }
}
