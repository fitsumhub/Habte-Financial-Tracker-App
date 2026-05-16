package com.mobile.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Data utility functions — validates balance formatting,
 * total calculations, and preset bank data integrity.
 */
class DataTest {

    // ── getTotalBalance ──────────────────────────────────────────────────

    @Test
    fun `getTotalBalance returns zero for empty list`() {
        assertEquals(0.0, Data.getTotalBalance(emptyList()), 0.01)
    }

    @Test
    fun `getTotalBalance sums all accounts across all banks`() {
        val banks = listOf(
            Bank("b1", "Bank One", "BK1",
                listOf(
                    Account("a1", "b1", "1234", "Main", 5000.0, "ETB", AccountType.SAVINGS),
                    Account("a2", "b1", "5678", "Savings", 3000.0, "ETB", AccountType.SAVINGS)
                ),
                "#000", "#111", "BK1"
            ),
            Bank("b2", "Bank Two", "BK2",
                listOf(
                    Account("a3", "b2", "9012", "Current", 2000.0, "ETB", AccountType.CURRENT)
                ),
                "#222", "#333", "BK2"
            )
        )
        assertEquals(10000.0, Data.getTotalBalance(banks), 0.01)
    }

    @Test
    fun `getTotalBalance handles bank with no accounts`() {
        val banks = listOf(
            Bank("b1", "Empty Bank", "EMP", emptyList(), "#000", "#111", "EMP")
        )
        assertEquals(0.0, Data.getTotalBalance(banks), 0.01)
    }

    // ── getBankTotal ─────────────────────────────────────────────────────

    @Test
    fun `getBankTotal sums accounts in a single bank`() {
        val bank = Bank("b1", "Bank", "BNK",
            listOf(
                Account("a1", "b1", "1234", "Main", 7500.0, "ETB", AccountType.SAVINGS),
                Account("a2", "b1", "5678", "Savings", 2500.0, "ETB", AccountType.SAVINGS)
            ),
            "#000", "#111", "BNK"
        )
        assertEquals(10000.0, Data.getBankTotal(bank), 0.01)
    }

    @Test
    fun `getBankTotal returns zero for bank with no accounts`() {
        val bank = Bank("b1", "Empty", "EMP", emptyList(), "#000", "#111", "EMP")
        assertEquals(0.0, Data.getBankTotal(bank), 0.01)
    }

    // ── formatBalance ────────────────────────────────────────────────────

    @Test
    fun `formatBalance formats with two decimal places`() {
        assertEquals("1,000.00", Data.formatBalance(1000.0))
    }

    @Test
    fun `formatBalance formats large number with commas`() {
        assertEquals("1,234,567.89", Data.formatBalance(1234567.89))
    }

    @Test
    fun `formatBalance formats zero`() {
        assertEquals("0.00", Data.formatBalance(0.0))
    }

    @Test
    fun `formatBalance short mode for values above 1000`() {
        assertEquals("5.0k", Data.formatBalance(5000.0, short = true))
    }

    @Test
    fun `formatBalance short mode for values below 1000`() {
        // Below 1000, short mode should still show full format
        val result = Data.formatBalance(500.0, short = true)
        assertEquals("500.00", result)
    }

    @Test
    fun `formatBalance short mode for exact 1000`() {
        assertEquals("1.0k", Data.formatBalance(1000.0, short = true))
    }

    @Test
    fun `formatBalance handles decimal precision`() {
        assertEquals("99.99", Data.formatBalance(99.99))
    }

    // ── Preset Banks Data Integrity ──────────────────────────────────────

    @Test
    fun `PRESET_BANKS contains 16 banks`() {
        assertEquals(16, Data.PRESET_BANKS.size)
    }

    @Test
    fun `all preset banks have unique IDs`() {
        val ids = Data.PRESET_BANKS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all preset banks have unique short names`() {
        val shortNames = Data.PRESET_BANKS.map { it.shortName }
        assertEquals(shortNames.size, shortNames.toSet().size)
    }

    @Test
    fun `all preset banks have valid color hex strings`() {
        val hexRegex = Regex("^#[0-9A-Fa-f]{6}$")
        Data.PRESET_BANKS.forEach { bank ->
            assertTrue("${bank.name} colorFrom invalid: ${bank.colorFrom}", hexRegex.matches(bank.colorFrom))
            assertTrue("${bank.name} colorTo invalid: ${bank.colorTo}", hexRegex.matches(bank.colorTo))
        }
    }

    @Test
    fun `all preset banks have non-empty logoText`() {
        Data.PRESET_BANKS.forEach { bank ->
            assertTrue("${bank.name} logoText is empty", bank.logoText.isNotEmpty())
        }
    }

    @Test
    fun `CBE preset bank exists with correct data`() {
        val cbe = Data.PRESET_BANKS.find { it.id == "cbe" }
        assertNotNull(cbe)
        assertEquals("Commercial Bank of Ethiopia", cbe!!.name)
        assertEquals("CBE", cbe.shortName)
    }

    @Test
    fun `Telebirr preset bank exists`() {
        val tele = Data.PRESET_BANKS.find { it.id == "tele" }
        assertNotNull(tele)
        assertEquals("Telebirr", tele!!.name)
        assertEquals("TEL", tele.shortName)
    }

    // ── Account Types ────────────────────────────────────────────────────

    @Test
    fun `AccountType enum has three values`() {
        assertEquals(3, AccountType.values().size)
        assertNotNull(AccountType.SAVINGS)
        assertNotNull(AccountType.CURRENT)
        assertNotNull(AccountType.MOBILE)
    }

    // ── BANKS and MOCK data are initially empty ──────────────────────────

    @Test
    fun `BANKS list is initially empty`() {
        assertTrue(Data.BANKS.isEmpty())
    }

    @Test
    fun `MOCK_TRANSACTIONS is initially empty`() {
        assertTrue(Data.MOCK_TRANSACTIONS.isEmpty())
    }

    @Test
    fun `MOCK_TREND_DATA is initially empty`() {
        assertTrue(Data.MOCK_TREND_DATA.isEmpty())
    }
}
