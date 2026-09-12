package com.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SummarySchedulerTest {
    @Test
    fun summaryScheduler_supportsSixMonthAndYearlyIntervals() {
        assertNotNull(SummaryScheduler.intervalMillis("6 Months"))
        assertNotNull(SummaryScheduler.intervalMillis("Yearly"))
        assertEquals(180L * 24L * 60L * 60L * 1000L, SummaryScheduler.intervalMillis("6 Months"))
        assertEquals(365L * 24L * 60L * 60L * 1000L, SummaryScheduler.intervalMillis("Yearly"))
    }
}
