package com.mobile.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReminderRepeat { ONE_TIME, WEEKLY, MONTHLY, YEARLY }

/** A single scheduled bill/payment reminder — the domain-layer sibling of com.mobile.data.db.PaymentReminderEntity. */
data class PaymentReminder(
    val id: Long = 0,
    val label: String,
    val category: String,
    val amount: Double,
    val payee: String,
    val dueDateMillis: Long,
    val repeat: ReminderRepeat,
    val daysBefore: Int,
    val enabled: Boolean,
    val lastPaidCycle: String
)

/** "yyyy-MM-dd" of a due date — keys lastPaidCycle uniformly across every repeat type (monthly, weekly, ...). */
fun cycleKeyFor(dueDateMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(dueDateMillis))
