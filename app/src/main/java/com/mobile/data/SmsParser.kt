package com.mobile.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsParser {

    fun parseMessage(sender: String, body: String, timestamp: Long): Transaction? {
        val lowerSender = sender.lowercase().trim()
        val lowerBody = body.lowercase()

        // BUG FIX: Expanded bank detection to include all PRESET_BANKS + common sender IDs
        val bankShortName = when {
            lowerSender.contains("cbe") || lowerSender == "1000" || lowerSender == "8008" -> "CBE"
            lowerSender.contains("boa") || lowerSender.contains("abyssinia") -> "BOA"
            lowerSender.contains("telebirr") || lowerSender == "tele" || lowerSender == "127" || lowerSender == "*127#" -> "TEL"
            lowerSender.contains("awash") || lowerSender == "awash" -> "AWA"
            lowerSender.contains("dashen") -> "DAS"
            lowerSender.contains("hibret") || lowerSender.contains("united") -> "HIB"
            lowerSender.contains("zemen") -> "ZEM"
            lowerSender.contains("nib") -> "NIB"
            lowerSender.contains("coop") || lowerSender.contains("coopbank") -> "COO"
            lowerSender.contains("abay") -> "ABY"
            lowerSender.contains("berhan") -> "BER"
            lowerSender.contains("bunna") -> "BUN"
            lowerSender.contains("wegagen") -> "WEG"
            lowerSender.contains("oromia") -> "ORO"
            lowerSender.contains("lion") -> "LIO"
            lowerSender.contains("enat") -> "ENA"
            else -> return null
        }

        // Determine transaction type
        val type = when {
            lowerBody.contains("credited") || lowerBody.contains("received") ||
            lowerBody.contains("deposited") || lowerBody.contains("incoming") -> "credit"
            lowerBody.contains("debited") || lowerBody.contains("paid") ||
            lowerBody.contains("transferred") || lowerBody.contains("withdrawn") ||
            lowerBody.contains("purchase") || lowerBody.contains("charged") -> "debit"
            else -> return null
        }

        // BUG FIX: Improved amount regex — handles "ETB 1,234.56", "Birr 500", "amount 100.00"
        val amountRegex = Regex(
            """(?:etb|birr|amount[:\s]?of|amount:?|credited\s+with|debited\s+with|transfer\s+of)\s*([0-9,]+\.?[0-9]*)""",
            RegexOption.IGNORE_CASE
        )
        val amountRegex2 = Regex(
            """([0-9,]+\.?[0-9]*)\s*(?:etb|birr|br\.)""",
            RegexOption.IGNORE_CASE
        )
        val matchResult = amountRegex.find(lowerBody) ?: amountRegex2.find(lowerBody)

        val amount = matchResult?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null  // BUG FIX: return null instead of 0.0 amount to avoid garbage data

        if (amount <= 0.0) return null

        // Extract remaining balance
        val balanceRegex = Regex(
            """(?:balance\s*is|balance:?|your\s+balance|available\s+balance|current\s+balance)\s*(?:etb|birr|br\.)?\s*([0-9,]+\.?[0-9]*)""",
            RegexOption.IGNORE_CASE
        )
        val balanceMatch = balanceRegex.find(lowerBody)
        val balance = balanceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        // Extract Account Suffix (The user's account)
        var accountSuffix: String? = null
        val myAccountRegex = Regex(
            "(?:from\\s+your(?:\\s+[a-zA-Z\\-]+)?\\s+account|your(?:\\s+[a-zA-Z\\-]+)?\\s+account|account\\s+ending\\s+in|credited\\s+to\\s+account)\\s*(?:no\\.?|number|:)?\\s*[a-z0-9*#]*([0-9]{4,})",
            RegexOption.IGNORE_CASE
        )
        val myMatch = myAccountRegex.find(lowerBody)
        if (myMatch != null) {
            accountSuffix = myMatch.groupValues[1].takeLast(4)
        } else {
            val genericAccountRegex = Regex(
                "(?:a/c|account|acc|wallet)\\s*(?:no\\.?|number|:)?\\s*(?:\\.{2,}|\\*+|#+)?([0-9]{4,})",
                RegexOption.IGNORE_CASE
            )
            val allMatches = genericAccountRegex.findAll(lowerBody).toList()
            for (match in allMatches) {
                val index = match.range.first
                val precedingText = lowerBody.substring(kotlin.math.max(0, index - 30), index)
                
                // If it's a debit and preceded by "to", it's likely a destination account, so skip
                if (type == "debit" && precedingText.contains(Regex("\\bto\\b"))) {
                    continue
                }
                
                accountSuffix = match.groupValues[1].takeLast(4)
                break
            }
        }

        // BUG FIX: For Telebirr, force all transactions to the same single account 
        // because it is a phone-number based wallet and doesn't use multiple distinct sub-accounts.
        // This prevents duplicate accounts like "TEL Main Account" and "TEL Account (*9503)".
        if (bankShortName == "TEL") {
            accountSuffix = null
        }

        val title = categorizeTitle(lowerBody)
        val category = categorizeExpense(lowerBody, title, type)

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(timestamp))

        return Transaction(
            id = "sms-$bankShortName-$timestamp",  // BUG FIX: unique ID includes bank name to avoid collisions
            title = title,
            amount = amount,  // BUG FIX: Store raw positive amount; UI determines sign via type
            date = dateStr,
            type = type,
            bankShortName = bankShortName,
            category = category,
            balance = balance,
            accountSuffix = accountSuffix
        )
    }

    private fun categorizeTitle(body: String): String {
        return when {
            body.contains("airtime") || body.contains("recharge") -> "Airtime Recharge"
            body.contains("electric") || body.contains("eepco") -> "Electricity Bill"
            body.contains("water") -> "Water Bill"
            body.contains("utility") -> "Utility Bill"
            body.contains("supermarket") || body.contains("market") || body.contains("grocery") -> "Groceries"
            body.contains("restaurant") || body.contains("cafe") || body.contains("hotel") -> "Dining"
            body.contains("fuel") || body.contains("petrol") -> "Fuel"
            body.contains("atm") || body.contains("withdraw") -> "ATM Withdrawal"
            body.contains("salary") || body.contains("payroll") || body.contains("pay") -> "Salary Deposit"
            body.contains("transfer") -> "Bank Transfer"
            body.contains("purchase") -> "Purchase"
            else -> "Bank Transaction"
        }
    }

    private fun categorizeExpense(body: String, title: String, type: String): String {
        if (type == "credit") return "Income"
        return when (title) {
            "Airtime Recharge", "Electricity Bill", "Water Bill", "Utility Bill" -> "Bills & Utilities"
            "Groceries", "Dining" -> "Food & Dining"
            "Fuel" -> "Transport"
            "ATM Withdrawal" -> "Cash"
            "Bank Transfer" -> "Transfers"
            "Purchase" -> "Shopping"
            else -> "General"
        }
    }
}
