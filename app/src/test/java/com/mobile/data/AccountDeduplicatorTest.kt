package com.mobile.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AccountDeduplicator — covers all 15 deduplication scenarios
 * specified in the account deduplication architecture requirements.
 */
class AccountDeduplicatorTest {

    // ─── canonicalKey generation ───────────────────────────────────────────────

    @Test
    fun `same suffix produces same canonical key`() {
        val k1 = AccountDeduplicator.canonicalKey("CBE", "6936")
        val k2 = AccountDeduplicator.canonicalKey("CBE", "6936")
        assertEquals(k1, k2)
    }

    @Test
    fun `suffix with leading zeros is padded correctly`() {
        val key = AccountDeduplicator.canonicalKey("CBE", "358")
        assertEquals("CBE:0358", key)
    }

    @Test
    fun `null suffix produces unknown key`() {
        val key = AccountDeduplicator.canonicalKey("CBE", null)
        assertEquals("CBE:unknown", key)
    }

    @Test
    fun `bank name is uppercased in canonical key`() {
        val lower = AccountDeduplicator.canonicalKey("cbe", "6936")
        val upper = AccountDeduplicator.canonicalKey("CBE", "6936")
        assertEquals(lower, upper)
        assertEquals("CBE:6936", lower)
    }

    @Test
    fun `different suffixes produce different keys`() {
        val k6936 = AccountDeduplicator.canonicalKey("CBE", "6936")
        val k0358 = AccountDeduplicator.canonicalKey("CBE", "0358")
        assertNotEquals(k6936, k0358)
    }

    @Test
    fun `different banks with same suffix produce different keys`() {
        val cbe  = AccountDeduplicator.canonicalKey("CBE", "6936")
        val boa  = AccountDeduplicator.canonicalKey("BOA", "6936")
        assertNotEquals(cbe, boa)
    }

    @Test
    fun `non-digit characters in suffix are stripped`() {
        // suffix extracted from "•••• 6936"
        val k1 = AccountDeduplicator.canonicalKey("CBE", "•••• 6936")
        val k2 = AccountDeduplicator.canonicalKey("CBE", "6936")
        assertEquals(k1, k2)
    }

    @Test
    fun `suffix longer than 4 digits uses last 4`() {
        // If a bank somehow exposes more digits, we normalise to last 4
        val key = AccountDeduplicator.canonicalKey("CBE", "1234567890")
        assertEquals("CBE:7890", key)
    }

    // ─── isSameAccount ─────────────────────────────────────────────────────────

    @Test
    fun `same suffix identifies same account`() {
        assertTrue(AccountDeduplicator.isSameAccount("CBE", "6936", "6936"))
    }

    @Test
    fun `both null identifies same account (both unknown)`() {
        assertTrue(AccountDeduplicator.isSameAccount("CBE", null, null))
    }

    @Test
    fun `one null one suffix does NOT identify same account`() {
        // An Unknown placeholder and a known suffix are different canonical identities
        // until we have evidence they are the same — the upgrade path handles merging.
        assertFalse(AccountDeduplicator.isSameAccount("CBE", null, "6936"))
        assertFalse(AccountDeduplicator.isSameAccount("CBE", "6936", null))
    }

    @Test
    fun `different suffixes are different accounts`() {
        assertFalse(AccountDeduplicator.isSameAccount("CBE", "6936", "0358"))
    }

    @Test
    fun `same suffix different banks are different accounts`() {
        // Even though the last 4 digits match, different banks = different institution
        assertFalse(AccountDeduplicator.isSameAccount("CBE", "6936", "6936").not()) // true for same bank
        // for cross-bank check we need to call with different bankShortName
        val cbKey = AccountDeduplicator.canonicalKey("CBE", "6936")
        val boKey = AccountDeduplicator.canonicalKey("BOA", "6936")
        assertNotEquals(cbKey, boKey)
    }

    // ─── canonicalKey format stability ─────────────────────────────────────────

    @Test
    fun `canonical key format is stable for known suffix`() {
        val key = AccountDeduplicator.canonicalKey("BOA", "1234")
        assertEquals("BOA:1234", key)
    }

    @Test
    fun `canonical key format is stable for unknown`() {
        val key = AccountDeduplicator.canonicalKey("BUN", null)
        assertEquals("BUN:unknown", key)
    }

    @Test
    fun `empty string suffix treated as unknown`() {
        val key = AccountDeduplicator.canonicalKey("CBE", "")
        assertEquals("CBE:unknown", key)
    }

    @Test
    fun `suffix with only non-digit chars treated as unknown`() {
        val key = AccountDeduplicator.canonicalKey("CBE", "****")
        assertEquals("CBE:unknown", key)
    }
}
