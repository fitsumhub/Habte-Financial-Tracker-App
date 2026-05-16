package com.mobile.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FinanceRepository — validates bank and transaction
 * state management, add/remove, and data integrity.
 */
class FinanceRepositoryTest {

    @Before
    fun setup() {
        // Clear all data before each test to ensure isolation
        FinanceRepository.clearAll()
    }

    // ── Bank Operations ──────────────────────────────────────────────────

    @Test
    fun `addBank adds a bank to the list`() {
        val bank = createTestBank("test1", "Test Bank", "TST")
        FinanceRepository.addBank(bank)
        
        assertEquals(1, FinanceRepository.banks.value.size)
        assertEquals("test1", FinanceRepository.banks.value[0].id)
    }

    @Test
    fun `addBank appends multiple banks`() {
        FinanceRepository.addBank(createTestBank("b1", "Bank One", "BK1"))
        FinanceRepository.addBank(createTestBank("b2", "Bank Two", "BK2"))
        
        assertEquals(2, FinanceRepository.banks.value.size)
    }

    @Test
    fun `removeBank removes the correct bank`() {
        FinanceRepository.addBank(createTestBank("b1", "Bank One", "BK1"))
        FinanceRepository.addBank(createTestBank("b2", "Bank Two", "BK2"))
        
        FinanceRepository.removeBank("b1")
        
        assertEquals(1, FinanceRepository.banks.value.size)
        assertEquals("b2", FinanceRepository.banks.value[0].id)
    }

    @Test
    fun `removeBank also removes associated transactions`() {
        FinanceRepository.addBank(createTestBank("b1", "Bank One", "BK1"))
        FinanceRepository.addTransaction(createTestTransaction("t1", "BK1", 100.0))
        FinanceRepository.addTransaction(createTestTransaction("t2", "BK2", 200.0))
        
        FinanceRepository.removeBank("b1")
        
        // Only BK2 transaction should remain
        assertEquals(1, FinanceRepository.transactions.value.size)
        assertEquals("BK2", FinanceRepository.transactions.value[0].bankShortName)
    }

    @Test
    fun `removeBank with nonexistent ID does nothing`() {
        FinanceRepository.addBank(createTestBank("b1", "Bank One", "BK1"))
        FinanceRepository.removeBank("nonexistent")
        assertEquals(1, FinanceRepository.banks.value.size)
    }

    @Test
    fun `updateBankColors updates only the target bank`() {
        FinanceRepository.addBank(createTestBank("b1", "Bank One", "BK1"))
        FinanceRepository.addBank(createTestBank("b2", "Bank Two", "BK2"))
        
        FinanceRepository.updateBankColors("b1", "#FF0000", "#00FF00")
        
        val updatedBank = FinanceRepository.banks.value.find { it.id == "b1" }
        assertEquals("#FF0000", updatedBank?.colorFrom)
        assertEquals("#00FF00", updatedBank?.colorTo)
        
        // Other bank should be unchanged
        val otherBank = FinanceRepository.banks.value.find { it.id == "b2" }
        assertEquals("#3730A3", otherBank?.colorFrom)
    }

    // ── Transaction Operations ───────────────────────────────────────────

    @Test
    fun `addTransaction inserts and sorts by date descending`() {
        // Use dates that sort correctly descending by string comparison
        FinanceRepository.addTransaction(createTestTransaction("t1", "BK1", 100.0, "May 01, 2024"))
        FinanceRepository.addTransaction(createTestTransaction("t2", "BK1", 200.0, "May 15, 2024"))
        
        val txs = FinanceRepository.transactions.value
        assertEquals(2, txs.size)
        // "May 15" > "May 01" lexicographically, so t2 first
        assertEquals("t2", txs[0].id)
        assertEquals("t1", txs[1].id)
    }

    @Test
    fun `updateTransactionCategory updates correctly`() {
        FinanceRepository.addTransaction(createTestTransaction("t1", "BK1", 100.0, category = "Other"))
        
        FinanceRepository.updateTransactionCategory("t1", "Food & Dining")
        
        assertEquals("Food & Dining", FinanceRepository.transactions.value[0].category)
    }

    @Test
    fun `updateTransactionCategory with nonexistent ID does nothing`() {
        FinanceRepository.addTransaction(createTestTransaction("t1", "BK1", 100.0, category = "Other"))
        
        FinanceRepository.updateTransactionCategory("nonexistent", "Food & Dining")
        
        assertEquals("Other", FinanceRepository.transactions.value[0].category)
    }

    // ── Clear All ────────────────────────────────────────────────────────

    @Test
    fun `clearAll removes all banks and transactions`() {
        FinanceRepository.addBank(createTestBank("b1", "Bank One", "BK1"))
        FinanceRepository.addTransaction(createTestTransaction("t1", "BK1", 100.0))
        
        FinanceRepository.clearAll()
        
        assertEquals(0, FinanceRepository.banks.value.size)
        assertEquals(0, FinanceRepository.transactions.value.size)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun createTestBank(
        id: String,
        name: String,
        shortName: String,
        accounts: List<Account> = listOf(
            Account("acc1", id, "1234567890", "Main", 10000.0, "ETB", AccountType.SAVINGS)
        )
    ) = Bank(
        id = id,
        name = name,
        shortName = shortName,
        accounts = accounts,
        colorFrom = "#3730A3",
        colorTo = "#1E1B4B",
        logoText = shortName
    )

    private fun createTestTransaction(
        id: String,
        bankShortName: String,
        amount: Double,
        date: String = "Jan 01, 2024",
        type: String = "debit",
        category: String = "Other"
    ) = Transaction(
        id = id,
        title = "Test Transaction",
        amount = amount,
        date = date,
        type = type,
        bankShortName = bankShortName,
        category = category
    )
}
