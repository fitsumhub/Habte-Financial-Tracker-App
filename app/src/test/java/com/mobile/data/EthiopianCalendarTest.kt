package com.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

private fun calendarOf(year: Int, month: Int, day: Int): Calendar =
    Calendar.getInstance().apply {
        clear()
        set(year, month, day)
    }

class EthiopianCalendarTest {

    @Test
    fun `Ethiopian New Year 2017 falls on Sept 11, 2024`() {
        val e = EthiopianCalendar.fromGregorian(calendarOf(2024, Calendar.SEPTEMBER, 11))
        assertEquals(EthiopianDate(2017, 1, 1), e)
    }

    @Test
    fun `Ethiopian Millennium falls on Sept 12, 2007 since 2008 is a Gregorian leap year`() {
        val e = EthiopianCalendar.fromGregorian(calendarOf(2007, Calendar.SEPTEMBER, 12))
        assertEquals(EthiopianDate(2000, 1, 1), e)
    }

    @Test
    fun `New Year shifts a day earlier the year before a Gregorian leap year`() {
        // 2016 is a Gregorian leap year, so the preceding Ethiopian New Year lands Sept 12, not 11.
        assertEquals(EthiopianDate(2008, 1, 1), EthiopianCalendar.fromGregorian(calendarOf(2015, Calendar.SEPTEMBER, 12)))
        assertEquals(EthiopianDate(2007, 13, 6), EthiopianCalendar.fromGregorian(calendarOf(2015, Calendar.SEPTEMBER, 11)))
    }

    @Test
    fun `round trip through Gregorian and back is stable`() {
        val original = calendarOf(2026, Calendar.JULY, 31)
        val ethiopian = EthiopianCalendar.fromGregorian(original)
        val back = EthiopianCalendar.toGregorian(ethiopian)
        assertEquals(original.get(Calendar.YEAR), back.get(Calendar.YEAR))
        assertEquals(original.get(Calendar.MONTH), back.get(Calendar.MONTH))
        assertEquals(original.get(Calendar.DAY_OF_MONTH), back.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `Pagume has 6 days in an Ethiopian leap year, 5 otherwise`() {
        assertEquals(6, EthiopianCalendar.daysInMonth(2015, 13))
        assertEquals(5, EthiopianCalendar.daysInMonth(2016, 13))
        assertEquals(6, EthiopianCalendar.daysInMonth(2019, 13))
    }

    @Test
    fun `regular Ethiopian months always have 30 days`() {
        for (month in 1..12) {
            assertEquals(30, EthiopianCalendar.daysInMonth(2017, month))
        }
    }

    @Test
    fun `monthRange spans exactly the days in that Ethiopian month`() {
        val (start, end) = EthiopianCalendar.monthRange(2017, 1)
        assertEquals(EthiopianDate(2017, 1, 1), EthiopianCalendar.fromGregorian(start))
        assertEquals(EthiopianDate(2017, 1, 30), EthiopianCalendar.fromGregorian(end))
    }

    @Test
    fun `yearRange spans Meskerem 1 through the end of Pagume`() {
        val (start, end) = EthiopianCalendar.yearRange(2017)
        assertEquals(EthiopianDate(2017, 1, 1), EthiopianCalendar.fromGregorian(start))
        assertEquals(EthiopianDate(2017, 13, EthiopianCalendar.daysInMonth(2017, 13)), EthiopianCalendar.fromGregorian(end))
    }

    @Test
    fun `formatMonthYear uses the Ethiopian month name`() {
        assertEquals("Meskerem 2017", EthiopianCalendar.formatMonthYear(calendarOf(2024, Calendar.SEPTEMBER, 11)))
    }
}
