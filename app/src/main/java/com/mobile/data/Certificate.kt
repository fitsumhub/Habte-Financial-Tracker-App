package com.mobile.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CertificatePeriod(val label: String) {
    WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly")
}

/** Visual style a certificate is rendered in — see CertificateRenderer for the actual drawing per template. */
enum class CertificateTemplate(val label: String, val subtitle: String = "") {
    CLASSIC_GOLD("Classic Gold", "Royal Navy & 24K Gold"),
    ROYAL_EMERALD("Executive Emerald", "Ethiopian Emerald & Mint"),
    ETHIOPIAN_HERITAGE("Axumite Heritage", "Tricolor Tibeb & Maroon"),
    BIRR_BANKNOTE("National Banknote", "Engraved Rosette & Watermark"),
    PLATINUM_TITANIUM("Platinum Sovereign", "Obsidian & Brushed Silver"),
    SOLAR_GOLD("Addis Sunrise", "Warm Amber & Golden Bronze")
}

/** Achievement milestone focus / category for customized certificate themes. */
enum class CertificateCategory(val label: String, val iconDesc: String) {
    OVERALL_MASTERY("Executive Mastery", "Comprehensive Financial Health"),
    SAVINGS_CHAMPION("Savings Velocity", "Capital Accumulation & Growth"),
    DISCIPLINED_BUDGET("Budget Discipline", "Expense Control & Solvency"),
    TRANSACTION_VANGUARD("Ledger Consistency", "Complete Transaction Tracking")
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
    val solvencyRatio: Double,
    val transactionCount: Int,
    val disciplineScore: Int,
    val title: String,
    val subtitle: String,
    val category: CertificateCategory = CertificateCategory.OVERALL_MASTERY,
    val largestTransactionAmount: Double = 0.0,
    val topCategory: String = "General",
    val solvencyTier: String = "Solvent"
)

private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
private val monthDayYearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

/**
 * Multi-factored recognition algorithm considering:
 * 1. Net Savings Volume (ETB)
 * 2. Savings Rate Percentage (%)
 * 3. Solvency Ratio (Income / Expense)
 * 4. Tracking Consistency (Transaction count)
 * 5. Milestone Focus Category
 */
private fun achievementTitleFor(
    category: CertificateCategory,
    savingsRate: Double?,
    netSaved: Double,
    solvencyRatio: Double,
    transactionCount: Int
): String {
    if (transactionCount == 0) return "Silent Ghost of the Ledger 👻"
    val rate = savingsRate ?: 0.0

    return when (category) {
        CertificateCategory.SAVINGS_CHAMPION -> when {
            rate >= 50.0 && netSaved >= 10000.0 -> "Apex Vault Dragon 🐉💰"
            rate >= 35.0 || netSaved >= 5000.0  -> "Certified Wealth Goblin 🧌💎"
            rate >= 20.0 || netSaved > 0.0       -> "Professional Coin Stasher 🐿️"
            rate > 0.0                          -> "Cushion Builder 1st Class 🛋️"
            else                                -> "Certified Savings Evaporator 💨"
        }

        CertificateCategory.DISCIPLINED_BUDGET -> when {
            solvencyRatio >= 2.5 && rate >= 40.0 -> "CEO of 'We Have Food At Home' 🍲"
            solvencyRatio >= 1.5 && rate >= 20.0 -> "Impulse Control Grandmaster 🧘‍♂️"
            solvencyRatio >= 1.0                 -> "Tightrope Walking Accountant 🎪"
            else                                 -> "Budget? What Budget? 🙈💸"
        }

        CertificateCategory.TRANSACTION_VANGUARD -> when {
            transactionCount >= 25 -> "Card Tap Olympic Gold 💳⚡"
            transactionCount >= 10 -> "Telebirr Speed Demon 🏃‍♂️💨"
            transactionCount >= 5  -> "Diligent Expense Inspector 🔍"
            else                   -> "Stealth Mode Wallet Monk 🧘"
        }

        CertificateCategory.OVERALL_MASTERY -> when {
            rate >= 45.0 && netSaved >= 8000.0 && transactionCount >= 10 -> "Imperial Sovereign of Money Magic 🪄👑"
            rate >= 30.0 && netSaved > 0.0                                -> "Executive Wealth Whisperer 💼✨"
            rate >= 15.0                                                  -> "Wallet Survival Champion 🛡️"
            rate > 0.0                                                    -> "Narrowly Escaped Broke 😅"
            netSaved == 0.0                                               -> "Master of Perfect Zero ⚖️"
            else                                                          -> "Hero of the Ethiopian Economy 📈"
        }
    }
}

private fun achievementSubtitleFor(
    period: CertificatePeriod,
    category: CertificateCategory,
    netSaved: Double,
    savingsRate: Double?,
    transactionCount: Int
): String {
    val periodWord = period.label.lowercase()
    if (transactionCount == 0) {
        return "Conferred for supreme stealth mode: 0 transactions logged this $periodWord. Either your wallet is locked inside an impenetrable titanium safe, or you have learned to survive on pure sunlight and good vibes."
    }

    val savedText = "${Data.formatBalance(kotlin.math.abs(netSaved))} ETB"
    val rateText = savingsRate?.let { " (${String.format(Locale.getDefault(), "%.1f", it)}% savings efficiency)" } ?: ""

    return when {
        netSaved > 0 -> when (category) {
            CertificateCategory.SAVINGS_CHAMPION ->
                if ((savingsRate ?: 0.0) >= 40.0)
                    "Conferred for fiercely hoarding $savedText from spontaneous online shopping and high-end macchiatos this $periodWord. With an epic$rateText savings rate, your bank account is throwing a party!"
                else
                    "Conferred for successfully stashing away $savedText into the secret vault this $periodWord across $transactionCount transactions. Every single Birr saved is another step away from payday panic!"

            CertificateCategory.DISCIPLINED_BUDGET ->
                if ((savingsRate ?: 0.0) >= 40.0)
                    "Officially certified for walking past 100 things you wanted to buy and keeping $savedText safe in positive operating liquidity. A true master of 'we have food at home' across $transactionCount transactions!"
                else
                    "Recognized for keeping your financial ship afloat with $savedText in net retained liquidity across $transactionCount transactions. The budget survived the storm with flying colors!"

            CertificateCategory.TRANSACTION_VANGUARD ->
                "Conferred for relentless financial surveillance across $transactionCount transactions, while calmly capturing $savedText into the vault without dropping a single receipt."

            CertificateCategory.OVERALL_MASTERY ->
                if ((savingsRate ?: 0.0) >= 30.0)
                    "Conferred for outstanding financial wizardry this $periodWord: Successfully defended $savedText$rateText across $transactionCount transactions. Warren Buffett is nervously taking notes!"
                else
                    "Conferred for honorable financial juggling this $periodWord: Managed to retain $savedText amidst temptations and $transactionCount transactions. Your wallet lives to fight another day!"
        }

        netSaved == 0.0 ->
            "Conferred for achieving supernatural mathematical precision: Exactly 0.00 ETB left over across $transactionCount transactions this $periodWord. Money came in, money went out, absolute zero drama!"

        else -> when (category) {
            CertificateCategory.SAVINGS_CHAMPION ->
                "Conferred for fearlessly funding life's adventures. Deployed $savedText past income across $transactionCount transactions — savings took a vacation, but the spirit of prosperity remains high!"

            CertificateCategory.DISCIPLINED_BUDGET ->
                "The budget made a valiant stand, but the checkout button won this $periodWord. Tracked $savedText in heroic outflow across $transactionCount entries. Tomorrow we rebuild the empire!"

            CertificateCategory.TRANSACTION_VANGUARD ->
                "Conferred for logging every single Birr of the spending spree across $transactionCount transactions. Wallet may be crying, but the ledger integrity is 100% crystal clear!"

            CertificateCategory.OVERALL_MASTERY ->
                "Single-handedly stimulated the Ethiopian economy this $periodWord by deploying $savedText across $transactionCount power moves. Money is temporary, but legendary spending receipts are forever!"
        }
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

/** Computes the multi-factor stats + motivational title/subtitle a certificate for [period] and [category] should show. */
fun computeCertificateAchievement(
    transactions: List<Transaction>,
    period: CertificatePeriod,
    category: CertificateCategory = CertificateCategory.OVERALL_MASTERY,
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
    val solvency = if (expense > 0) (income / expense) else if (income > 0) 5.0 else 1.0

    val largestTx = inRange.maxOfOrNull { it.amount } ?: 0.0
    val topCat = inRange.filter { it.type == "debit" }
        .groupBy { it.category }
        .maxByOrNull { it.value.sumOf { tx -> tx.amount } }
        ?.key ?: if (inRange.isNotEmpty()) "General" else "No Outflow"

    val solvencyTier = when {
        solvency >= 3.0 -> "Prime Solvency"
        solvency >= 2.0 -> "Strong Liquidity"
        solvency >= 1.2 -> "Balanced Reserve"
        solvency >= 1.0 -> "Solvent Buffer"
        else -> "Monitored Cashflow"
    }

    val disciplineScore = (((rate ?: 0.0).coerceIn(0.0, 100.0) * 0.5) +
            ((solvency * 20.0).coerceIn(0.0, 30.0)) +
            ((inRange.size * 2.0).coerceIn(0.0, 20.0))).toInt().coerceIn(10, 100)

    return CertificateAchievement(
        periodLabel = label,
        totalIncome = income,
        totalExpense = expense,
        netSaved = net,
        savingsRatePercent = rate,
        solvencyRatio = solvency,
        transactionCount = inRange.size,
        disciplineScore = disciplineScore,
        title = achievementTitleFor(category, rate, net, solvency, inRange.size),
        subtitle = achievementSubtitleFor(period, category, net, rate, inRange.size),
        category = category,
        largestTransactionAmount = largestTx,
        topCategory = topCat,
        solvencyTier = solvencyTier
    )
}

/** Convenience overload for callers passing [now] without an explicit category. */
fun computeCertificateAchievement(
    transactions: List<Transaction>,
    period: CertificatePeriod,
    now: Calendar
): CertificateAchievement = computeCertificateAchievement(transactions, period, CertificateCategory.OVERALL_MASTERY, now)
