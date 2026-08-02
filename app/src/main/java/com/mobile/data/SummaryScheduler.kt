package com.mobile.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Schedules the periodic "spending summary" notification via AlarmManager —
 * deliberately not WorkManager, since this project has no WorkManager dependency
 * yet and adding one requires resolving a new Gradle artifact, which isn't
 * guaranteed to be available in every build environment. Inexact repeating
 * alarms need no special permission and are perfectly adequate for a summary
 * that's fine landing a little early/late.
 *
 * Multiple frequencies can be active at once (e.g. Daily + Weekly) — each gets
 * its own independent alarm, keyed by its own PendingIntent request code, so
 * enabling/disabling one never disturbs another's schedule.
 */
object SummaryScheduler {
    const val EXTRA_FREQUENCY = "extra_frequency"

    val ALL_FREQUENCIES = listOf("Every 12 Hours", "Daily", "Weekly", "Every 15 Days", "Monthly")

    private const val BASE_REQUEST_CODE = 9000

    /** Also used by SummaryNotifier so each frequency's notification has a stable, distinct ID. */
    fun idFor(frequency: String): Int = BASE_REQUEST_CODE + ALL_FREQUENCIES.indexOf(frequency) + 1

    fun intervalMillis(frequency: String): Long? = when (frequency) {
        "Every 12 Hours" -> 12 * 60 * 60 * 1000L
        "Daily" -> 24 * 60 * 60 * 1000L
        "Weekly" -> 7 * 24 * 60 * 60 * 1000L
        "Every 15 Days" -> 15 * 24 * 60 * 60 * 1000L
        "Monthly" -> 30 * 24 * 60 * 60 * 1000L
        else -> null
    }

    private fun pendingIntent(context: Context, frequency: String, createIfMissing: Boolean): PendingIntent? {
        val intent = Intent(context, SummaryAlarmReceiver::class.java).putExtra(EXTRA_FREQUENCY, frequency)
        val flags = (if (createIfMissing) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or
            PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, idFor(frequency), intent, flags)
    }

    /** Cancels [frequency]'s alarm, if any. */
    fun cancel(context: Context, frequency: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        pendingIntent(context, frequency, createIfMissing = false)?.let { alarmManager.cancel(it) }
    }

    /** Cancels any existing alarm for [frequency] and starts a fresh one from now. */
    fun schedule(context: Context, frequency: String) {
        val interval = intervalMillis(frequency) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancel(context, frequency)
        val pending = pendingIntent(context, frequency, createIfMissing = true) ?: return
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + interval,
            interval,
            pending
        )
    }

    /** Enables or disables one frequency without touching any of the others. */
    fun toggle(context: Context, frequency: String, enabled: Boolean) {
        if (enabled) schedule(context, frequency) else cancel(context, frequency)
    }

    /**
     * Reconciles all alarms against [activeFrequencies] from scratch — used after a
     * reboot (every alarm is guaranteed gone, so a fresh schedule is always correct)
     * and to clean up any frequency that's no longer selected.
     */
    fun rescheduleAll(context: Context, activeFrequencies: Set<String>) {
        ALL_FREQUENCIES.forEach { frequency ->
            if (frequency in activeFrequencies) schedule(context, frequency) else cancel(context, frequency)
        }
    }

    /** Schedules only the frequencies in [activeFrequencies] that aren't already running — avoids resetting an existing cadence on every app open. */
    fun ensureScheduled(context: Context, activeFrequencies: Set<String>) {
        activeFrequencies.forEach { frequency ->
            if (pendingIntent(context, frequency, createIfMissing = false) == null) {
                schedule(context, frequency)
            }
        }
    }
}
