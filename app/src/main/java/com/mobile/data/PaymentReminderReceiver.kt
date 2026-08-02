package com.mobile.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobile.data.db.AppDatabase
import com.mobile.data.db.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fires on the alarm armed by PaymentReminderScheduler. Reads the reminder straight from
 * Room — this can run in a fresh process after the app was fully killed, so it can't rely
 * on PaymentReminderRepository's in-memory StateFlow being populated yet. Notifies unless
 * this due date was already marked paid, then either re-arms for the next occurrence
 * (recurring reminders) or disables itself (one-time reminders, mirroring how a one-time
 * alarm clock alarm turns itself off after ringing).
 */
class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(PaymentReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0) return
        val appContext = context.applicationContext
        SettingsRepository.init(appContext)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).paymentReminderDao()
                val entity = dao.getById(reminderId) ?: return@launch
                if (!entity.enabled) return@launch
                val reminder = entity.toDomain()

                if (reminder.lastPaidCycle != cycleKeyFor(reminder.dueDateMillis)) {
                    PaymentReminderNotifier.notify(appContext, reminder)
                }

                if (reminder.repeat == ReminderRepeat.ONE_TIME) {
                    dao.setEnabled(reminderId, false)
                } else {
                    val anchor = Calendar.getInstance().apply { timeInMillis = reminder.dueDateMillis }
                    val next = PaymentReminderScheduler.nextOccurrence(anchor, reminder.repeat)
                    dao.updateDueDate(reminderId, next.timeInMillis)
                    PaymentReminderScheduler.scheduleFor(appContext, reminderId, next.timeInMillis, reminder.daysBefore)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
