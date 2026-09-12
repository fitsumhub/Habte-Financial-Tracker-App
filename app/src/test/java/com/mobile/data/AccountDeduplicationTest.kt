package com.mobile.data

import org.junit.Assert.*
import org.junit.Test

class AccountDeduplicationTest {

    @Test
    fun testNormalizeAccountIdentifier() {
        val repo = FinanceRepository
        
        // General bank accounts
        assertEquals("1000123456789", repo.normalizeAccountIdentifier("1000-123456789"))
        assertEquals("1000123456789", repo.normalizeAccountIdentifier("1000 123456789"))
        assertEquals("1000123456789", repo.normalizeAccountIdentifier("1000123456789"))
        assertEquals("Unknown", repo.normalizeAccountIdentifier("Unknown"))
        assertEquals("Unknown", repo.normalizeAccountIdentifier("Main"))
        
        // Mobile money / phone normalizations
        assertEquals("0912345678", repo.normalizeAccountIdentifier("251912345678"))
        assertEquals("0912345678", repo.normalizeAccountIdentifier("0912345678"))
        assertEquals("0912345678", repo.normalizeAccountIdentifier("912345678"))
    }

    @Test
    fun testMatchAccounts() {
        val repo = FinanceRepository
        
        // Exact match
        assertTrue(repo.matchAccounts("1000123456789", "1000-123456789"))
        
        // Suffix matches full account
        assertTrue(repo.matchAccounts("1000123456789", "•••• 6789"))
        assertTrue(repo.matchAccounts("•••• 6789", "1000123456789"))
        assertTrue(repo.matchAccounts("•••• 6789", "6789"))
        
        // Suffix matches suffix
        assertTrue(repo.matchAccounts("•••• 6789", "•••• 6789"))
        
        // Different suffixes
        assertFalse(repo.matchAccounts("•••• 6789", "•••• 1234"))
        
        // Legitimately different accounts sharing same suffix (Full numbers compared)
        assertFalse(repo.matchAccounts("1000123456789", "2000987656789"))
        
        // Unknown matches
        assertTrue(repo.matchAccounts("Unknown", "Unknown"))
        assertFalse(repo.matchAccounts("Unknown", "â€¢â€¢â€¢â€¢ 6789"))
    }
}
