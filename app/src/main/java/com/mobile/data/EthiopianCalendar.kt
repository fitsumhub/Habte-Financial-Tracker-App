package com.mobile.data

import java.util.Calendar

/** Which calendar system a screen should render dates in — user-selectable in Settings. */
enum class CalendarSystem { GREGORIAN, ETHIOPIAN }

data class EthiopianDate(val year: Int, val month: Int, val day: Int)

/**
 * Gregorian <-> Ethiopian date conversion via Julian Day Number, using the standard
 * Amete Mihret epoch offset (matches ICU4J's EthiopicCalendar and the algorithm in
 * Reingold & Dershowitz, "Calendrical Calculations"). Verified against known reference
 * dates: 2024-09-11 Gregorian = Meskerem 1, 2017 E.C.; the Ethiopian Millennium
 * (2007-09-12 Gregorian, since 2008 is a Gregorian leap year) = Meskerem 1, 2000 E.C.
 */
object EthiopianCalendar {
    private const val JD_EPOCH_OFFSET_AMETE_MIHRET = 1723856

    val MONTH_NAMES = listOf(
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit",
        "Megabit", "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
    )

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + Math.floorDiv(y, 4) - Math.floorDiv(y, 100) + Math.floorDiv(y, 400) - 32045
    }

    private fun jdnToGregorian(jdn: Int): Triple<Int, Int, Int> {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - 146097 * b / 4
        val d = (4 * c + 3) / 1461
        val e = c - 1461 * d / 4
        val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = 100 * b + d - 4800 + m / 10
        return Triple(year, month, day)
    }

    private fun ethiopianToJdn(year: Int, month: Int, day: Int): Int =
        JD_EPOCH_OFFSET_AMETE_MIHRET + 365 * year + Math.floorDiv(year, 4) + 30 * month + day - 31

    private fun jdnToEthiopian(jdn: Int): EthiopianDate {
        val diff = jdn - JD_EPOCH_OFFSET_AMETE_MIHRET
        val r = Math.floorMod(diff, 1461)
        val n = r % 365 + 365 * (r / 1460)
        val year = 4 * Math.floorDiv(diff, 1461) + r / 365 - r / 1460
        val month = n / 30 + 1
        val day = n % 30 + 1
        return EthiopianDate(year, month, day)
    }

    fun fromGregorian(cal: Calendar): EthiopianDate =
        jdnToEthiopian(gregorianToJdn(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))

    /** Returns a Gregorian Calendar set to midnight on the given Ethiopian date. */
    fun toGregorian(date: EthiopianDate): Calendar {
        val (y, m, d) = jdnToGregorian(ethiopianToJdn(date.year, date.month, date.day))
        return Calendar.getInstance().apply {
            clear()
            set(y, m - 1, d, 0, 0, 0)
        }
    }

    fun monthName(month: Int): String = MONTH_NAMES.getOrElse(month - 1) { "" }

    /** Days in [month] of Ethiopian [year] — 30 for months 1-12, 5 or 6 (leap) for Pagume (13). */
    fun daysInMonth(year: Int, month: Int): Int {
        if (month != 13) return 30
        return ethiopianToJdn(year + 1, 1, 1) - ethiopianToJdn(year, 1, 1) - 360
    }

    /** Gregorian [start, end] Calendar range (inclusive, end = 23:59:59.999) spanning the given Ethiopian month. */
    fun monthRange(year: Int, month: Int): Pair<Calendar, Calendar> {
        val start = toGregorian(EthiopianDate(year, month, 1))
        val end = toGregorian(EthiopianDate(year, month, daysInMonth(year, month))).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }
        return start to end
    }

    /** Gregorian [start, end] Calendar range (inclusive) spanning the given Ethiopian year (Meskerem 1 - Pagume end). */
    fun yearRange(year: Int): Pair<Calendar, Calendar> {
        val start = toGregorian(EthiopianDate(year, 1, 1))
        val end = toGregorian(EthiopianDate(year, 13, daysInMonth(year, 13))).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }
        return start to end
    }

    fun formatFull(cal: Calendar): String {
        val e = fromGregorian(cal)
        return "${monthName(e.month)} ${e.day}, ${e.year}"
    }

    fun formatMonthYear(cal: Calendar): String {
        val e = fromGregorian(cal)
        return "${monthName(e.month)} ${e.year}"
    }

    fun formatMonthShort(cal: Calendar): String {
        val e = fromGregorian(cal)
        return monthName(e.month).take(3)
    }
}

/** Formats [cal] as a full human-readable date honoring the user's chosen calendar system. */
fun formatCalendarDate(cal: Calendar, system: CalendarSystem): String = when (system) {
    CalendarSystem.GREGORIAN -> java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(cal.time)
    CalendarSystem.ETHIOPIAN -> EthiopianCalendar.formatFull(cal)
}

/** Formats [cal] as "Month Year" honoring the user's chosen calendar system. */
fun formatCalendarMonthYear(cal: Calendar, system: CalendarSystem): String = when (system) {
    CalendarSystem.GREGORIAN -> java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
    CalendarSystem.ETHIOPIAN -> EthiopianCalendar.formatMonthYear(cal)
}
