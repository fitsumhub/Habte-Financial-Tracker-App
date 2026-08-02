package com.mobile.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules a one-shot AlarmManager alarm ahead of a payment reminder's due date, keyed
 * by the reminder's own row id so any number of independent reminders (rent, loans,
 * subscriptions, ...) can be armed at once — see PaymentReminderReceiver, which re-arms
 * recurring reminders for their next occurrence each time one fires. Uses
 * AlarmManager.set() — an inexact one-shot alarm needs no special permission, and being
 * off by a few minutes is fine for a days-ahead reminder.
 */
object PaymentReminderScheduler {
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    private const val BASE_REQUEST_CODE = 9200

    /** Advances [from] to its next occurrence for [repeat], clamping day-of-month for MONTHLY (e.g. Jan 31 → Feb 28). */
    fun nextOccurrence(from: Calendar, repeat: ReminderRepeat): Calendar {
        val cal = from.clone() as Calendar
        when (repeat) {
            ReminderRepeat.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            ReminderRepeat.MONTHLY -> {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            ReminderRepeat.YEARLY -> cal.add(Calendar.YEAR, 1)
            ReminderRepeat.ONE_TIME -> Unit
        }
        return cal
    }

    private fun pendingIntent(context: Context, reminderId: Long, createIfMissing: Boolean): PendingIntent? {
        val intent = Intent(context, PaymentReminderReceiver::class.java).putExtra(EXTRA_REMINDER_ID, reminderId)
        val flags = (if (createIfMissing) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or
            PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, (BASE_REQUEST_CODE + reminderId).toInt(), intent, flags)
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        pendingIntent(context, reminderId, createIfMissing = false)?.let { alarmManager.cancel(it) }
    }

    /** Cancels any existing alarm for [reminderId] and arms a fresh one, [daysBefore] days ahead of [dueDateMillis]. */
    fun scheduleFor(context: Context, reminderId: Long, dueDateMillis: Long, daysBefore: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancel(context, reminderId)
        val trigger = (dueDateMillis - daysBefore * 24 * 60 * 60 * 1000L)
            .coerceAtLeast(System.currentTimeMillis() + 1_000L)
        val pending = pendingIntent(context, reminderId, createIfMissing = true) ?: return
        alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pending)
    }
}
