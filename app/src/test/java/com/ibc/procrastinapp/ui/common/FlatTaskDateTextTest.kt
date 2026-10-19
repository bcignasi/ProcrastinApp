package com.ibc.procrastinapp.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FlatTaskDateTextTest {
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun todayShowsOnlyTheTimeIncludingMidnight() {
        assertEquals("16:30", flatTaskDateText("2026-10-19 16:30", today, Locale.US))
        assertEquals("00:00", flatTaskDateText("2026-10-19 00:00", today, Locale.US))
    }

    @Test
    fun otherDaysKeepTheirDateAndTime() {
        assertEquals("Oct 20, 2026 · 17:00", flatTaskDateText("2026-10-20 17:00", today, Locale.US))
        assertEquals("Oct 18, 2026 · 00:00", flatTaskDateText("2026-10-18 00:00", today, Locale.US))
    }

    @Test
    fun dateOnlyDoesNotInventAMidnightDeadline() {
        assertEquals("Oct 19, 2026", flatTaskDateText("2026-10-19", today, Locale.US))
    }

    @Test
    fun changingTheReferenceDayRestoresTheDateOnYesterday() {
        assertEquals("Oct 19, 2026 · 16:30", flatTaskDateText("2026-10-19 16:30", today.plusDays(1), Locale.US))
    }

    @Test
    fun missingOrInvalidDatesAreNotReplacedByTheCurrentTime() {
        assertNull(flatTaskDateText("", today, Locale.US))
        assertNull(flatTaskDateText("  ", today, Locale.US))
        assertEquals("2026-02-31 16:30", flatTaskDateText("2026-02-31 16:30", today, Locale.US))
        assertEquals("fecha incorrecta", flatTaskDateText("fecha incorrecta", today, Locale.US))
    }
}
