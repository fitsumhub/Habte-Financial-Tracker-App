package com.mobile.data

import java.text.DecimalFormat
import androidx.compose.runtime.Stable
import com.mobile.R

// ── Account types (mirrors "savings" | "current" | "mobile" union in data.ts) ─
enum class AccountType { SAVINGS, CURRENT, MOBILE }

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
    val logoResId: Int? = null
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
    val accountSuffix: String? = null // Last 4 digits of the account
)




// ── Seed data (mirrors BANKS in data.ts) ─────────────────────────────────────
object Data {

    val PRESET_BANKS = listOf(
        Bank("cbe",     "Commercial Bank of Ethiopia", "CBE",  listOf(), "#3730A3", "#1E1B4B", "CBE", R.drawable.logo_cbe),
        Bank("boa",     "Bank of Abyssinia",          "BOA",  listOf(), "#F59E0B", "#B45309", "BOA", R.drawable.logo_boa),
        Bank("awash",   "Awash Bank",                 "AWA",  listOf(), "#1E40AF", "#1D4ED8", "AWB", R.drawable.logo_awash),
        Bank("dashen",  "Dashen Bank",                "DAS",  listOf(), "#1E3A8A", "#EF4444", "DSB", R.drawable.logo_dashen),
        Bank("hibret",  "Hibret Bank",                "HIB",  listOf(), "#059669", "#D97706", "HBT", R.drawable.logo_hibret),
        Bank("zemen",   "Zemen Bank",                 "ZEM",  listOf(), "#111827", "#D97706", "ZMN", R.drawable.logo_zemen),
        Bank("nib",     "Nib International Bank",     "NIB",  listOf(), "#1E40AF", "#FACC15", "NIB", R.drawable.logo_nib),
        Bank("coop",    "Cooperative Bank of Oromia", "COO",  listOf(), "#047857", "#F59E0B", "CPB", R.drawable.logo_coop),
        Bank("abay",    "Abay Bank",                  "ABY",  listOf(), "#1D4ED8", "#059669", "ABY", R.drawable.logo_abay),
        Bank("berhan",  "Berhan Bank",                "BER",  listOf(), "#B91C1C", "#F59E0B", "BRH", R.drawable.logo_berhan),
        Bank("bunna",   "Bunna Bank",                 "BUN",  listOf(), "#451A03", "#D97706", "BNA", R.drawable.logo_bunna),
        Bank("wegagen", "Wegagen Bank",               "WEG",  listOf(), "#1D4ED8", "#F59E0B", "WGN", R.drawable.logo_wegagen),
        Bank("oromia",  "Oromia Bank",                "ORO",  listOf(), "#059669", "#B91C1C", "ORB", R.drawable.logo_oromia),
        Bank("lion",    "Lion Bank",                  "LIO",  listOf(), "#F59E0B", "#B91C1C", "LIB"),
        Bank("enat",    "Enat Bank",                  "ENA",  listOf(), "#DB2777", "#1D4ED8", "ENB", R.drawable.logo_enat),
        Bank("tele",    "Telebirr",                   "TEL",  listOf(), "#0E7490", "#164E63", "TEL")
    )

    val BANKS: List<Bank> = emptyList()


    val MOCK_TRANSACTIONS = emptyList<Transaction>()
    val MOCK_TREND_DATA = emptyList<Float>()



    // ── Utility functions (mirrors data.ts) ───────────────────────────────────
    fun getTotalBalance(banks: List<Bank>): Double =
        banks.sumOf { bank -> bank.accounts.sumOf { it.balance } }

    fun formatBalance(amount: Double, short: Boolean = false): String {
        if (short && amount >= 1000) return String.format("%.1fk", amount / 1000)
        return DecimalFormat("#,##0.00").format(amount)
    }

    fun getBankTotal(bank: Bank): Double =
        bank.accounts.sumOf { it.balance }
}
