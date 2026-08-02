package com.mobile.data

import android.content.Context
import com.mobile.data.db.AppDatabase
import com.mobile.data.db.toDomain
import com.mobile.data.db.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Persists payment reminders (Room-backed) and keeps their AlarmManager schedules in sync with the stored state. */
object PaymentReminderRepository {
    private lateinit var db: AppDatabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false

    private val _reminders = MutableStateFlow<List<PaymentReminder>>(emptyList())
    val reminders: StateFlow<List<PaymentReminder>> = _reminders.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        db = AppDatabase.getInstance(context)
        scope.launch {
            db.paymentReminderDao().observeAll().collect { entities ->
                _reminders.value = entities.map { it.toDomain() }
            }
        }
    }

    /** Inserts a new reminder or replaces an existing one (by id), then (re)arms or cancels its alarm to match [reminder]'s enabled state. */
    fun save(context: Context, reminder: PaymentReminder) {
        val appContext = context.applicationContext
        scope.launch {
            val newId = db.paymentReminderDao().upsert(reminder.toEntity())
            val resolvedId = if (reminder.id == 0L) newId else reminder.id
            if (reminder.enabled) {
                PaymentReminderScheduler.scheduleFor(appContext, resolvedId, reminder.dueDateMillis, reminder.daysBefore)
            } else {
                PaymentReminderScheduler.cancel(appContext, resolvedId)
            }
        }
    }

    fun setEnabled(context: Context, reminder: PaymentReminder, enabled: Boolean) {
        val appContext = context.applicationContext
        scope.launch {
            db.paymentReminderDao().setEnabled(reminder.id, enabled)
            if (enabled) {
                PaymentReminderScheduler.scheduleFor(appContext, reminder.id, reminder.dueDateMillis, reminder.daysBefore)
            } else {
                PaymentReminderScheduler.cancel(appContext, reminder.id)
            }
        }
    }

    /** Suppresses this cycle's notification without disturbing the recurring schedule itself. */
    fun markPaid(reminder: PaymentReminder) {
        scope.launch { db.paymentReminderDao().markPaid(reminder.id, cycleKeyFor(reminder.dueDateMillis)) }
    }

    fun delete(context: Context, reminder: PaymentReminder) {
        val appContext = context.applicationContext
        scope.launch {
            PaymentReminderScheduler.cancel(appContext, reminder.id)
            db.paymentReminderDao().delete(reminder.id)
        }
    }
}
