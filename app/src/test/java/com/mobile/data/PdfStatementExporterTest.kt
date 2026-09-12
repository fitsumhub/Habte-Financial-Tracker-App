package com.mobile.data

import org.junit.Assert.*
import org.junit.Test

class PdfStatementExporterTest {

    @Test
    fun `test transaction date and date-time parsing with Locale US`() {
        val tx1 = Transaction(
            id = "tx1",
            title = "CBE Transfer",
            amount = 500.0,
            date = "Nov 14, 2023",
            time = "02:30 PM",
            type = "credit",
            bankShortName = "CBE",
            category = "Income",
            balance = 5500.0,
            accountSuffix = "1234"
        )

        val cal = parseTransactionDate(tx1.date)
        assertNotNull(cal)

        val millis = transactionTimestampMillis(tx1)
        assertNotNull(millis)
        assertTrue(millis!! > 0L)
    }

    @Test
    fun `test mathematical consistency of opening and closing balance formula`() {
        // Scenario:
        // Tx1: Debit 100 ETB, Balance after = 900. (Opening balance before Tx1 was 1000)
        // Tx2: Credit 500 ETB, Balance after = 1400.
        val tx1 = Transaction(
            id = "tx1",
            title = "Debit",
            amount = 100.0,
            date = "Nov 14, 2023",
            type = "debit",
            bankShortName = "CBE",
            category = "Shopping",
            balance = 900.0,
            accountSuffix = "1234",
            time = "02:30 PM"
        )
        val tx2 = Transaction(
            id = "tx2",
            title = "Credit",
            amount = 500.0,
            date = "Nov 15, 2023",
            type = "credit",
            bankShortName = "CBE",
            category = "Income",
            balance = 1400.0,
            accountSuffix = "1234",
            time = "10:15 AM"
        )

        val transactions = listOf(tx1, tx2)
        val totalIncome = transactions.filter { it.type == "credit" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "debit" }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense

        val newestWithBal = transactions.lastOrNull { it.balance != null }
        val closingBalance = newestWithBal!!.balance!!

        val oldestWithBal = transactions.firstOrNull { it.balance != null }!!
        val openingBalance = if (oldestWithBal.type == "credit") {
            oldestWithBal.balance!! - oldestWithBal.amount
        } else {
            oldestWithBal.balance!! + oldestWithBal.amount
        }

        assertEquals(500.0, totalIncome, 0.01)
        assertEquals(100.0, totalExpense, 0.01)
        assertEquals(400.0, netCashFlow, 0.01)
        assertEquals(1000.0, openingBalance, 0.01)
        assertEquals(1400.0, closingBalance, 0.01)
        assertEquals(closingBalance, openingBalance + netCashFlow, 0.01)
    }
}
