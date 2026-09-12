package com.mobile.data

/**
 * Generates a stable, bank-scoped canonical key used as the deduplication identity for every
 * bank account record.  All account-creation paths must resolve a canonicalKey FIRST and look
 * it up before inserting — the DB also enforces a UNIQUE constraint on (bankId, canonicalKey).
 *
 * Format:
 *   known suffix  -> "<BANK_SHORT_NAME>:<zero-padded-last-4>"   e.g. "CBE:6936"
 *   unknown       -> "<BANK_SHORT_NAME>:unknown"                 e.g. "CBE:unknown"
 *
 * Rules:
 *  - Strip all non-digit characters from the suffix before padding.
 *  - Pad to at least 4 digits with leading zeros.
 *  - Never use balance, display name, or SMS message ID as part of the key.
 *  - Two accounts with different suffixes are always different records (no last-4 collision).
 */
object AccountDeduplicator {

    const val UNKNOWN_SUFFIX = "unknown"

    /**
     * Build the canonical key for an account.
     *
     * @param bankShortName  The institution short name (e.g. "CBE", "BOA", "BUN").
     * @param accountSuffix  The raw suffix extracted from the SMS (e.g. "6936", "0358"),
     *                       or null if the SMS did not reveal an account number.
     */
    fun canonicalKey(bankShortName: String, accountSuffix: String?): String {
        val bank = bankShortName.uppercase().trim()
        if (accountSuffix == null) return "$bank:$UNKNOWN_SUFFIX"

        // Normalise: keep digits only, then take last 4 (or all if fewer).
        val digits = accountSuffix.filter { it.isDigit() }
        if (digits.isEmpty()) return "$bank:$UNKNOWN_SUFFIX"

        val key = digits.takeLast(4).padStart(4, '0')
        return "$bank:$key"
    }

    /**
     * Returns true when the two raw suffix values (may be null) resolve to the same
     * canonical key for the given bank, i.e. they identify the same physical account.
     */
    fun isSameAccount(bankShortName: String, suffixA: String?, suffixB: String?): Boolean =
        canonicalKey(bankShortName, suffixA) == canonicalKey(bankShortName, suffixB)
}
