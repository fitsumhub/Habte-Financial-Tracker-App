package com.mobile.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CertificatePeriod(val label: String) {
    WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly")
}

/** Visual style a certificate is rendered in — see CertificateRenderer for the actual drawing per template. */
enum class CertificateTemplate(val label: String) {
    CLASSIC_GOLD("Classic Gold"),
    ETHIOPIAN_HERITAGE("Ethiopian Heritage"),
    BIRR_BANKNOTE("Birr Banknote")
}

/** A generated Achievement Certificate, persisted so the user can re-download or re-share it later. */
data class Certificate(
    val id: Long = 0,
    val period: CertificatePeriod,
    val periodLabel: String,
    val userName: String,
    val photoPath: String?,
    val achievementTitle: String,
    val achievementSubtitle: String,
    val generatedAtMillis: Long,
    val imagePath: String
)

/** The real numbers a certificate for [period] is built from — see computeCertificateAchievement. */
data class CertificateAchievement(
    val periodLabel: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSaved: Double,
    val savingsRatePercent: Double?,
    val transactionCount: Int,
    val title: String,
    val subtitle: String
)

private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
private val monthDayYearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

/**
 * Motivational tier from the savings rate alone, so the same tier means the same thing
 * regardless of period — a Weekly "Savings Champion" and a Yearly "Savings Champion" both
 * mean "saved 30%+ of income," not different bars per period length. Framed positively even
 * when nothing was saved, matching the "celebrate financial achievements" brief — a user who
 * tracked every transaction this period still gets a real, non-shaming title.
 */
private fun achievementTitleFor(savingsRate: Double?, transactionCount: Int): String = when {
    transactionCount == 0 || savingsRate == null -> "Tracking Milestone"
    savingsRate >= 30 -> "Savings Champion"
    savingsRate >= 10 -> "Steady Saver"
    savingsRate > 0 -> "Building Momentum"
    else -> "Financial Tracker"
}

private fun achievementSubtitleFor(
    period: CertificatePeriod,
    netSaved: Double,
    savingsRate: Double?,
    transactionCount: Int
): String {
    val periodWord = period.label.lowercase()
    if (transactionCount == 0) return "For staying on top of your finances with Habte this $periodWord."
    val savedText = "${Data.formatBalance(kotlin.math.abs(netSaved))} ETB"
    return if (netSaved >= 0) {
        val rateText = savingsRate?.let { " — a ${String.format("%.0f", it)}% savings rate" } ?: ""
        "For saving $savedText this $periodWord$rateText."
    } else {
        "For tracking every transaction this $periodWord and staying in control of your finances."
    }
}

private fun zeroedOut(cal: Calendar): Calendar = (cal.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

/** [start, end] (inclusive) covering [period] as of [now], plus a human label for it. */
internal fun certificatePeriodRange(period: CertificatePeriod, now: Calendar): Triple<Calendar, Calendar, String> {
    val end = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }
    return when (period) {
        CertificatePeriod.WEEKLY -> {
            val daysSinceMonday = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val start = zeroedOut(now).apply { add(Calendar.DAY_OF_YEAR, -daysSinceMonday) }
            val label = "${monthDayFormat.format(start.time)} - ${monthDayYearFormat.format(now.time)}"
            Triple(start, end, label)
        }
        CertificatePeriod.MONTHLY -> {
            val start = zeroedOut(now).apply { set(Calendar.DAY_OF_MONTH, 1) }
            Triple(start, end, monthYearFormat.format(now.time))
        }
        CertificatePeriod.YEARLY -> {
            val start = zeroedOut(now).apply { set(Calendar.DAY_OF_YEAR, 1) }
            Triple(start, end, now.get(Calendar.YEAR).toString())
        }
    }
}

/** Computes the real stats + motivational title/subtitle a certificate for [period] should show, from [transactions] as of [now]. */
fun computeCertificateAchievement(
    transactions: List<Transaction>,
    period: CertificatePeriod,
    now: Calendar = Calendar.getInstance()
): CertificateAchievement {
    val (start, end, label) = certificatePeriodRange(period, now)
    val inRange = transactions.mapNotNull { tx -> parseTransactionDate(tx.date)?.let { tx to it } }
        .filter { (_, cal) -> !cal.before(start) && !cal.after(end) }
        .map { it.first }

    val income = totalCredit(inRange)
    val expense = totalDebit(inRange)
    val net = income - expense
    val rate = savingsRatePercent(income, expense)

    return CertificateAchievement(
        periodLabel = label,
        totalIncome = income,
        totalExpense = expense,
        netSaved = net,
        savingsRatePercent = rate,
        transactionCount = inRange.size,
        title = achievementTitleFor(rate, inRange.size),
        subtitle = achievementSubtitleFor(period, net, rate, inRange.size)
    )
}
