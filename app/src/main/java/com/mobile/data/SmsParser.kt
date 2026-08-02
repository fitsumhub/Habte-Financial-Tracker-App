package com.mobile.data

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsParser {

    // Messages describing a transaction that didn't actually complete (rejected/reversed
    // *attempts*, not to be confused with a reversal *refund*, which is a real credit and
    // is left to the normal credit-keyword path below). Checked before type classification
    // so a message like "...has been debited... Transaction failed, will be reversed" can't
    // slip through just because "debited" also appears in it.
    private val failedTransactionRegex = Regex(
        "\\b(?:failed|unsuccessful|declined|not\\s+successful|could\\s+not\\s+be\\s+completed|insufficient\\s+(?:balance|funds?))\\b",
        RegexOption.IGNORE_CASE
    )

    // Messages announcing a transaction that hasn't happened yet (a future-dated standing
    // order/subscription notice). These use the same "debited"/"credited" vocabulary as a
    // completed transaction but in the future tense, so they need their own guard rather
    // than relying on the type-keyword check below. Deliberately narrow to unambiguous
    // future-tense phrasing only — a message like "Your scheduled payment of ETB 250.00
    // has been debited" describes a *completed* instance of a standing order ("scheduled"
    // is just the plan's name there), so a broader "scheduled" keyword match would wrongly
    // drop a real transaction, which is worse than the rare false-positive this narrower
    // form might miss.
    private val pendingTransactionRegex = Regex(
        "\\bwill\\s+be\\s+(?:debited|credited|charged)\\b|\\bis\\s+scheduled\\s+to\\s+be\\b",
        RegexOption.IGNORE_CASE
    )

    fun parseMessage(sender: String, body: String, timestamp: Long): Transaction? {
        // Data-driven institution lookup — see InstitutionCatalog.ALL. Adding a new
        // bank/wallet there is enough to make it detectable here, no changes needed.
        val institution = InstitutionCatalog.findBySmsSender(sender) ?: return null
        return parseBody(institution, body, timestamp, idPrefix = "sms", sourceKey = sender)
    }

    /**
     * The actual transaction-detection logic (type, amount, balance, account, counterparty,
     * category), shared by the SMS path above and NotificationCaptureListenerService's
     * bank-app-notification path — neither the regexes below nor the categorization rules
     * care whether the text came from an SMS body or a notification's title+text, only
     * [institution] (who the message is from) differs by source. [idPrefix] and [sourceKey]
     * exist so each source can keep its own stable, non-colliding id scheme (see
     * stableTransactionKey) without this function needing to know which source it's in.
     */
    fun parseBody(institution: InstitutionProfile, body: String, timestamp: Long, idPrefix: String, sourceKey: String): Transaction? {
        val lowerBody = body.lowercase()
        val bankShortName = institution.shortName

        if (failedTransactionRegex.containsMatchIn(lowerBody) || pendingTransactionRegex.containsMatchIn(lowerBody)) {
            return null
        }

        // Determine transaction type — "money in"/"money out" is how several bank apps'
        // own notifications (as opposed to their SMS templates) phrase this directly.
        val type = when {
            lowerBody.contains("credited") || lowerBody.contains("received") ||
            lowerBody.contains("deposited") || lowerBody.contains("incoming") ||
            lowerBody.contains("money in") -> "credit"
            lowerBody.contains("debited") || lowerBody.contains("paid") ||
            lowerBody.contains("transferred") || lowerBody.contains("withdrawn") ||
            lowerBody.contains("purchase") || lowerBody.contains("charged") ||
            lowerBody.contains("sent") || lowerBody.contains("money out") -> "debit"
            else -> return null
        }

        // BUG FIX: Improved amount regex — handles "ETB 1,234.56", "Birr 500", "amount 100.00"
        val amountRegex = Regex(
            """(?:etb|birr|amount[:\s]?of|amount:?|credited\s+with|debited\s+with|transfer\s+of)\s*([0-9,]+\.?[0-9]*)""",
            RegexOption.IGNORE_CASE
        )
        val amountRegex2 = Regex(
            """([0-9,]+\.?[0-9]*)\s*(?:etb|birr|br\.?)""",
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

        // Digital wallets (Telebirr, and any future ones) are phone-number based and
        // don't use multiple distinct sub-accounts, so force a single account per wallet.
        // This prevents duplicate accounts like "TEL Main Account" and "TEL Account (*9503)".
        if (institution.type == InstitutionType.DIGITAL_WALLET) {
            accountSuffix = null
        }

        val title = categorizeTitle(lowerBody, body, type, institution.name)
        val category = categorizeExpense(title, type)

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(timestamp))
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

        return Transaction(
            // Derived from the message content (bank reference number when the message
            // states one, otherwise a hash of sourceKey+body) rather than the delivery
            // timestamp. For SMS specifically: the live SmsReceiver path and the historical
            // inbox-resync path in FinanceRepository can each observe a slightly different
            // timestamp for the exact same real SMS (PDU delivery time vs. the inbox's
            // stored `date` column), which let the old timestamp-based ID create duplicate
            // transactions when both paths saw the same message. A content-derived ID is
            // identical either way, so Room's insert-ignore-on-conflict correctly dedupes it.
            id = "$idPrefix-$bankShortName-${stableTransactionKey(sourceKey, body)}",
            title = title,
            amount = amount,  // BUG FIX: Store raw positive amount; UI determines sign via type
            date = dateStr,
            time = timeStr,
            type = type,
            bankShortName = bankShortName,
            category = category,
            balance = balance,
            accountSuffix = accountSuffix
        )
    }

    // BUG FIX: A real sender/receiver name (or stated purpose) in the SMS body always
    // takes priority over generic keyword buckets like "Salary Deposit"/"Bank Transfer" —
    // those are only a fallback for messages that don't name who or what the money was for.
    private fun categorizeTitle(lowerBody: String, body: String, type: String, institutionName: String): String {
        extractCounterpartyName(body, type)?.let { return it }
        return when {
            lowerBody.contains("airtime") || lowerBody.contains("recharge") -> "Airtime Recharge"
            lowerBody.contains("electric") || lowerBody.contains("eepco") -> "Electricity Bill"
            lowerBody.contains("water") -> "Water Bill"
            lowerBody.contains("utility") -> "Utility Bill"
            lowerBody.contains("supermarket") || lowerBody.contains("market") || lowerBody.contains("grocery") -> "Groceries"
            lowerBody.contains("restaurant") || lowerBody.contains("cafe") || lowerBody.contains("hotel") -> "Dining"
            lowerBody.contains("fuel") || lowerBody.contains("petrol") -> "Fuel"
            lowerBody.contains("atm") || lowerBody.contains("withdraw") -> "ATM Withdrawal"
            lowerBody.contains("loan") -> "Loan Payment"
            lowerBody.contains("salary") || lowerBody.contains("payroll") || Regex("\\bpay\\b").containsMatchIn(lowerBody) -> "Salary Deposit"
            lowerBody.contains("transfer") -> "Bank Transfer"
            lowerBody.contains("purchase") -> "Purchase"
            // Fall back to the institution's real name instead of a generic
            // "Bank Transaction" placeholder when nothing else could be determined.
            else -> institutionName
        }
    }

    // BUG FIX: Instead of a generic "Bank Transaction" fallback, pull the sender/receiver
    // name out of the SMS body (e.g. "...received from ABEBE KEBEDE", "...transferred to Selam PLC",
    // "...credited with ETB 500 by ABEBE KEBEDE", "...to account 1**7019 (KIDUS TESSEMA)")
    // so the transaction list shows who the money moved with.
    private fun extractCounterpartyName(body: String, type: String): String? {
        // credit messages name the sender after either "from" or "by" depending on the bank
        // (e.g. BOA: "credited with ETB 500 by Abrham Tefera Mengstie").
        val keyword = if (type == "credit") "(?:from|by)" else "to"
        val nameRegex = Regex(
            "\\b$keyword\\s+([A-Za-z][A-Za-z .'\\-]{1,40}?)(?=\\s*(?:[.,;:(]|\\bon\\b|\\bat\\b|\\busing\\b|\\bvia\\b|\\bthrough\\b|\\baccount\\b|\\bacc\\b|\\bwallet\\b|\\bbalance\\b|\\d)|$)",
            RegexOption.IGNORE_CASE
        )
        nameRegex.find(body)?.groupValues?.get(1)?.trim()?.let { cleanCounterpartyName(it)?.let { name -> return name } }

        // Fallback: some banks (e.g. CBE) put the counterparty's name in parentheses after
        // the destination account number instead — "...to account 1**7019 (Kidus Tessema Girma)."
        val parenRegex = Regex("\\(([A-Za-z][A-Za-z .'\\-]{1,40})\\)")
        val parenName = parenRegex.find(body)?.groupValues?.get(1)?.trim() ?: return null
        return cleanCounterpartyName(parenName)
    }

    // Matches the bank's own transaction reference when the SMS states one explicitly
    // (e.g. "Ref: FT23198ABCDE", "Transaction ID: 123456789012") — the most reliable
    // dedup key available, since it's literally what the bank uses to identify the
    // transaction on their side.
    private val referenceRegex = Regex(
        "(?:ref(?:erence)?(?:\\s*(?:no|number))?|txn\\s*id|transaction\\s*(?:id|no|number))\\s*[:#]?\\s*([A-Za-z0-9]{6,20})",
        RegexOption.IGNORE_CASE
    )

    // Produces a stable identifier for a given message: the bank's own reference number if
    // it states one, otherwise a hash of sourceKey+body (sourceKey is the SMS sender
    // address, or a notification's package name — see parseBody). Either way this is
    // deterministic for the exact same message regardless of which timestamp the caller
    // happened to observe — see the comment on Transaction.id in parseBody() for why
    // that matters.
    private fun stableTransactionKey(sourceKey: String, body: String): String {
        referenceRegex.find(body)?.groupValues?.get(1)?.let { return it }

        val normalized = body.trim().lowercase().replace(Regex("\\s+"), " ")
        val digest = MessageDigest.getInstance("SHA-256").digest("$sourceKey|$normalized".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun cleanCounterpartyName(rawName: String): String? {
        if (rawName.length < 2 || rawName.any { it.isDigit() }) return null

        val stopWords = setOf("your", "the", "a", "an", "account", "acc", "wallet", "balance", "you")
        val words = rawName.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty() || words.first().lowercase() in stopWords) return null

        return words.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun categorizeExpense(title: String, type: String): String {
        if (type == "credit") return "Income"
        return when (title) {
            "Airtime Recharge", "Electricity Bill", "Water Bill", "Utility Bill" -> "Bills & Utilities"
            "Groceries", "Dining" -> "Food & Dining"
            "Fuel" -> "Transport"
            "ATM Withdrawal" -> "Cash"
            "Loan Payment" -> "Loan"
            "Bank Transfer" -> "Transfers"
            "Purchase" -> "Shopping"
            else -> "Other" // BUG FIX: matches the "Other" == Uncategorized convention used in AnalyticsScreen/WrappedStoryScreen
        }
    }
}
