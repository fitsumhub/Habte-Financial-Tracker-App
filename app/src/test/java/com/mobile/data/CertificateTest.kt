package com.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun calendarOn(year: Int, month: Int, day: Int): Calendar =
    Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
    }

private val certTxDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

private fun certTx(type: String, amount: Double, date: Calendar): Transaction =
    Transaction(
        id = "id-$type-$amount-${date.timeInMillis}",
        title = "t",
        amount = amount,
        date = certTxDateFormat.format(date.time),
        type = type,
        bankShortName = "CBE"
    )

class CertificateTest {

    @Test
    fun `monthly achievement only counts transactions in the displayed month`() {
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val transactions = listOf(
            certTx("credit", 1000.0, calendarOn(2026, Calendar.AUGUST, 3)),
            certTx("debit", 400.0, calendarOn(2026, Calendar.AUGUST, 10)),
            certTx("credit", 5000.0, calendarOn(2026, Calendar.JULY, 20)), // outside the month
            certTx("debit", 5000.0, calendarOn(2026, Calendar.SEPTEMBER, 1)) // outside the month
        )

        val result = computeCertificateAchievement(transactions, CertificatePeriod.MONTHLY, now)

        assertEquals(1000.0, result.totalIncome, 0.0001)
        assertEquals(400.0, result.totalExpense, 0.0001)
        assertEquals(600.0, result.netSaved, 0.0001)
        assertEquals(2, result.transactionCount)
        assertEquals("August 2026", result.periodLabel)
    }

    @Test
    fun `yearly achievement only counts transactions in the displayed year`() {
        val now = calendarOn(2026, Calendar.DECEMBER, 31)
        val transactions = listOf(
            certTx("credit", 10000.0, calendarOn(2026, Calendar.JANUARY, 1)),
            certTx("debit", 3000.0, calendarOn(2026, Calendar.JUNE, 1)),
            certTx("credit", 9999.0, calendarOn(2025, Calendar.DECEMBER, 31)) // outside the year
        )

        val result = computeCertificateAchievement(transactions, CertificatePeriod.YEARLY, now)

        assertEquals(10000.0, result.totalIncome, 0.0001)
        assertEquals(3000.0, result.totalExpense, 0.0001)
        assertEquals(2, result.transactionCount)
        assertEquals("2026", result.periodLabel)
    }

    @Test
    fun `weekly achievement covers the rolling Monday-to-now window, not other days`() {
        // 2026-08-15 is a Saturday — its Monday is 2026-08-10.
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val transactions = listOf(
            certTx("credit", 500.0, calendarOn(2026, Calendar.AUGUST, 11)), // this week
            certTx("debit", 100.0, calendarOn(2026, Calendar.AUGUST, 9)) // last week, excluded
        )

        val result = computeCertificateAchievement(transactions, CertificatePeriod.WEEKLY, now)

        assertEquals(500.0, result.totalIncome, 0.0001)
        assertEquals(0.0, result.totalExpense, 0.0001)
        assertEquals(1, result.transactionCount)
    }

    @Test
    fun `title is Savings Champion at or above a 30 percent savings rate`() {
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val transactions = listOf(
            certTx("credit", 1000.0, calendarOn(2026, Calendar.AUGUST, 1)),
            certTx("debit", 600.0, calendarOn(2026, Calendar.AUGUST, 2))
        )
        val result = computeCertificateAchievement(transactions, CertificatePeriod.MONTHLY, now)
        assertEquals("Savings Champion", result.title)
    }

    @Test
    fun `title is Steady Saver between 10 and 30 percent savings rate`() {
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val transactions = listOf(
            certTx("credit", 1000.0, calendarOn(2026, Calendar.AUGUST, 1)),
            certTx("debit", 850.0, calendarOn(2026, Calendar.AUGUST, 2))
        )
        val result = computeCertificateAchievement(transactions, CertificatePeriod.MONTHLY, now)
        assertEquals("Steady Saver", result.title)
    }

    @Test
    fun `title is Building Momentum for a small positive savings rate`() {
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val transactions = listOf(
            certTx("credit", 1000.0, calendarOn(2026, Calendar.AUGUST, 1)),
            certTx("debit", 990.0, calendarOn(2026, Calendar.AUGUST, 2))
        )
        val result = computeCertificateAchievement(transactions, CertificatePeriod.MONTHLY, now)
        assertEquals("Building Momentum", result.title)
    }

    @Test
    fun `title is Financial Tracker when spending exceeds income, not a shaming label`() {
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val transactions = listOf(
            certTx("credit", 500.0, calendarOn(2026, Calendar.AUGUST, 1)),
            certTx("debit", 800.0, calendarOn(2026, Calendar.AUGUST, 2))
        )
        val result = computeCertificateAchievement(transactions, CertificatePeriod.MONTHLY, now)
        assertEquals("Financial Tracker", result.title)
        assertTrue(result.subtitle.contains("tracking", ignoreCase = true))
    }

    @Test
    fun `title is Tracking Milestone and subtitle is still positive when there are no transactions`() {
        val now = calendarOn(2026, Calendar.AUGUST, 15)
        val result = computeCertificateAchievement(emptyList(), CertificatePeriod.MONTHLY, now)
        assertEquals("Tracking Milestone", result.title)
        assertEquals(0, result.transactionCount)
        assertTrue(result.subtitle.isNotBlank())
    }
}
