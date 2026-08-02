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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Posts the "payment due soon" notification for one reminder (see PaymentReminderScheduler/Receiver). */
object PaymentReminderNotifier {
    private const val CHANNEL_ID = "payment_reminders"
    private const val BASE_NOTIFICATION_ID = 9300
    private const val ACCENT_COLOR = 0xFF8B5CF6.toInt() // matches the Payment Reminders tile color in Tools

    /** Shared with PaymentReminderActionReceiver so "Mark as Paid" dismisses the exact notification it came from. */
    fun notificationIdFor(reminderId: Long): Int = (BASE_NOTIFICATION_ID + reminderId).toInt()

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Payment Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders ahead of bills and recurring payments you've set up"
            }
        )
    }

    fun notify(context: Context, reminder: PaymentReminder) {
        if (!SettingsRepository.notificationsEnabled.value) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) return

        ensureChannel(context)

        val daysLeft = ((reminder.dueDateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L))
            .toInt().coerceAtLeast(0)
        val dueDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(reminder.dueDateMillis))

        val title = if (daysLeft == 0) {
            "${reminder.label} is due today"
        } else {
            "${reminder.label} due in $daysLeft day${if (daysLeft == 1) "" else "s"}"
        }
        val body = buildString {
            if (reminder.amount > 0) append("ETB ${Data.formatBalance(reminder.amount)}")
            if (reminder.payee.isNotBlank()) {
                if (isNotEmpty()) append(" to ${reminder.payee}") else append("Due to ${reminder.payee}")
            }
            if (isNotEmpty()) append(" • ")
            append(dueDateStr)
        }

        val notificationId = notificationIdFor(reminder.id)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markPaidIntent = Intent(context, PaymentReminderActionReceiver::class.java).apply {
            putExtra(PaymentReminderScheduler.EXTRA_REMINDER_ID, reminder.id)
        }
        val markPaidPendingIntent = PendingIntent.getBroadcast(
            context, notificationId, markPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(NotificationIcons.build(ACCENT_COLOR, NotificationGlyph.DOT))
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n${reminder.category}"))
            .addAction(0, "Mark as Paid", markPaidPendingIntent)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted — nothing else to do here.
        }
    }
}
