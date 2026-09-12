package com.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobile.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-registers every active alarm after a device reboot — AlarmManager alarms don't
 * survive a restart, so without this the "Spending Summary" schedule and any enabled
 * payment reminders would silently stop firing until the user happened to reopen the
 * relevant screen.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        try {
            SettingsRepository.init(appContext)
            FinanceRepository.init(appContext)
            SummaryScheduler.rescheduleAll(appContext, SettingsRepository.summaryFrequencies.value)
        } catch (t: Throwable) {
            android.util.Log.e("BootReceiver", "Error synchronizing state on boot", t)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(appContext).paymentReminderDao().getEnabled().forEach { entity ->
                    PaymentReminderScheduler.scheduleFor(appContext, entity.id, entity.dueDateMillis, entity.daysBefore)
                }
            } catch (t: Throwable) {
                android.util.Log.e("BootReceiver", "Error rescheduling payment reminders on boot", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
