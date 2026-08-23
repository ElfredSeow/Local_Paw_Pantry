package com.example.foodtracker

import com.example.foodtracker.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Plain JUnit tests for [DateUtils] - no Android dependencies, runs on the host JVM.
 *
 * [DateUtils.getDaysUntil] and [DateUtils.getGroupKey] both call `LocalDate.now()` internally,
 * so every date used here is built relative to "today" (via [isoDate]) rather than hardcoded.
 * A test that hardcoded e.g. "2026-08-23" as "today" would start failing the very next day.
 */
class DateUtilsTest {

    // Matches DateUtils' private storage formatter ("yyyy-MM-dd").
    private val storageFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Matches DateUtils.formatDateFull's pattern + Locale, used to predict getGroupKey's
    // "Today, <full date>" / "Tomorrow, <full date>" output.
    private val fullDisplayFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.US)

    /** Today + [offsetDays], formatted the same way DateUtils expects to parse it. */
    private fun isoDate(offsetDays: Long): String =
        LocalDate.now().plusDays(offsetDays).format(storageFormatter)

    private fun fullDate(offsetDays: Long): String =
        LocalDate.now().plusDays(offsetDays).format(fullDisplayFormatter)

    // ------------------------------------------------------------------
    // getDaysUntil
    // ------------------------------------------------------------------

    @Test
    fun getDaysUntil_today_isZero() {
        assertEquals(0L, DateUtils.getDaysUntil(isoDate(0)))
    }

    @Test
    fun getDaysUntil_tomorrow_isOne() {
        assertEquals(1L, DateUtils.getDaysUntil(isoDate(1)))
    }

    @Test
    fun getDaysUntil_oneWeekOut_isSeven() {
        assertEquals(7L, DateUtils.getDaysUntil(isoDate(7)))
    }

    @Test
    fun getDaysUntil_twoWeeksOut_isFourteen() {
        assertEquals(14L, DateUtils.getDaysUntil(isoDate(14)))
    }

    @Test
    fun getDaysUntil_pastDate_isNegative() {
        assertEquals(-1L, DateUtils.getDaysUntil(isoDate(-1)))
        assertEquals(-30L, DateUtils.getDaysUntil(isoDate(-30)))
    }

    @Test
    fun getDaysUntil_unparseableInput_fallsBackToZero() {
        assertEquals(0L, DateUtils.getDaysUntil("not-a-date"))
        assertEquals(0L, DateUtils.getDaysUntil(""))
        assertEquals(0L, DateUtils.getDaysUntil("2026-13-40"))
    }

    // ------------------------------------------------------------------
    // getGroupKey
    // ------------------------------------------------------------------

    @Test
    fun getGroupKey_today_isLabeledToday() {
        assertEquals("Today, ${fullDate(0)}", DateUtils.getGroupKey(isoDate(0)))
    }

    @Test
    fun getGroupKey_tomorrow_isLabeledTomorrow() {
        assertEquals("Tomorrow, ${fullDate(1)}", DateUtils.getGroupKey(isoDate(1)))
    }

    @Test
    fun getGroupKey_twoDaysOut_isNextWeek() {
        // First day inside the "Next Week" band (day 2, since 0 and 1 have their own labels).
        assertEquals("Next Week", DateUtils.getGroupKey(isoDate(2)))
    }

    @Test
    fun getGroupKey_sevenDaysOut_isStillNextWeek() {
        // Upper boundary: getGroupKey uses `days <= 7`, so day 7 itself is the last "Next Week" day.
        assertEquals("Next Week", DateUtils.getGroupKey(isoDate(7)))
    }

    @Test
    fun getGroupKey_eightDaysOut_isLater() {
        // First day past the "Next Week" boundary.
        assertEquals("Later", DateUtils.getGroupKey(isoDate(8)))
    }

    @Test
    fun getGroupKey_fourteenDaysOut_isLater() {
        assertEquals("Later", DateUtils.getGroupKey(isoDate(14)))
    }

    @Test
    fun getGroupKey_pastDate_isExpired() {
        assertEquals("Expired", DateUtils.getGroupKey(isoDate(-1)))
        assertEquals("Expired", DateUtils.getGroupKey(isoDate(-14)))
    }

    @Test
    fun getGroupKey_unparseableInput_isTreatedAsTodayWithRawString() {
        // getDaysUntil("garbage") catches the parse failure and returns 0, which getGroupKey
        // treats as "today"; formatDateFull("garbage") independently fails to parse the same
        // input and falls back to returning it unchanged. Net result: "Today, garbage".
        assertEquals("Today, garbage", DateUtils.getGroupKey("garbage"))
    }
}
