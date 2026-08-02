package com.mobile.data

import java.text.DecimalFormat
import androidx.compose.runtime.Stable

// ── Account types — the full set of accounts a financial institution may offer.
enum class AccountType {
    SAVINGS,
    CURRENT,
    SALARY,
    BUSINESS,
    MERCHANT,
    YOUTH,
    STUDENT,
    FIXED_DEPOSIT,
    MOBILE_WALLET,
    DIGITAL_WALLET,
    LOAN,
    INVESTMENT,
    FOREIGN_CURRENCY
}

// ── Data models ───────────────────────────────────────────────────────────────
@Stable
data class Account(
    val id: String,

    val bankId: String,
    val accountNumber: String,
    val label: String,
    val balance: Double,
    val currency: String,
    val type: AccountType
)

@Stable
data class Bank(
    val id: String,
    val name: String,

    val shortName: String,
    val accounts: List<Account>,
    val colorFrom: String,   // hex string, e.g. "#1E3A8A"
    val colorTo: String,
    val logoText: String,
    val logoResId: Int? = null,
    val domain: String? = null
)

// User-configurable spending limit for a period, optionally scoped to one category
// (category == null means an overall limit for that period). Replaces the old
// hardcoded budgetMap in BudgetScreen.
@Stable
data class Budget(
    val id: Long = 0,
    val period: String, // "Daily" | "Weekly" | "Monthly" | "Yearly"
    val category: String?,
    val limit: Double
)

@Stable
data class Transaction(
    val id: String,
    val title: String,

    val amount: Double,
    val date: String,
    val type: String, // "credit" or "debit"
    val bankShortName: String,
    val category: String = "Other",
    val balance: Double? = null,
    val accountSuffix: String? = null, // Last 4 digits of the account
    val time: String = "",
    val reason: String = "" // User-entered note, independent of category — see updateTransactionReason
)




// ── Seed data (mirrors BANKS in data.ts) ─────────────────────────────────────
object Data {

    // Derived from InstitutionCatalog so the "Add Bank" list and SMS detection
    // (SmsParser) both stay in sync with a single source of truth — adding a
    // new institution only means adding an entry to InstitutionCatalog.ALL.
    val PRESET_BANKS: List<Bank> = InstitutionCatalog.ALL.map { it.toBank() }

    val BANKS: List<Bank> = emptyList()


    val MOCK_TRANSACTIONS = emptyList<Transaction>()
    val MOCK_TREND_DATA = emptyList<Float>()



    // ── Utility functions (mirrors data.ts) ───────────────────────────────────
    fun getTotalBalance(banks: List<Bank>): Double =
        banks.sumOf { bank -> bank.accounts.sumOf { it.balance } }

    // DecimalFormat isn't thread-safe, and this is called from both Compose UI
    // (every transaction row) and background notifiers — a ThreadLocal caches one
    // instance per thread instead of re-parsing the pattern string on every call.
    private val balanceFormat = ThreadLocal.withInitial { DecimalFormat("#,##0.00") }

    fun formatBalance(amount: Double, short: Boolean = false): String {
        if (short && amount >= 1000) return String.format("%.1fk", amount / 1000)
        return balanceFormat.get()!!.format(amount)
    }

    fun getBankTotal(bank: Bank): Double =
        bank.accounts.sumOf { it.balance }
}
