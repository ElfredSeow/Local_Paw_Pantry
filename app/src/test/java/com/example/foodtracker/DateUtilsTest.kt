package com.example.foodtracker

import com.example.foodtracker.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Covers the app's trickiest pure logic — expiry day math and timeline grouping (H2).
 * Dates are computed relative to today so the tests stay valid over time.
 */
class DateUtilsTest {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private fun daysFromNow(n: Long): String = LocalDate.now().plusDays(n).format(fmt)

    @Test
    fun getDaysUntil_futureDate_returnsPositiveCount() {
        assertEquals(5L, DateUtils.getDaysUntil(daysFromNow(5)))
    }

    @Test
    fun getDaysUntil_pastDate_returnsNegativeCount() {
        assertEquals(-3L, DateUtils.getDaysUntil(daysFromNow(-3)))
    }

    @Test
    fun getDaysUntil_invalidString_returnsZero() {
        assertEquals(0L, DateUtils.getDaysUntil("not-a-date"))
    }

    @Test
    fun formatDate_validDate_isReformatted() {
        val result = DateUtils.formatDate("2026-01-05")
        // Month name is locale-dependent; assert the stable parts instead.
        assertTrue(result.contains("2026"))
        assertTrue(result.contains("5"))
    }

    @Test
    fun formatDate_invalidDate_returnsInputUnchanged() {
        assertEquals("garbage", DateUtils.formatDate("garbage"))
    }

    @Test
    fun getGroupKey_pastDate_isExpired() {
        assertEquals("Expired", DateUtils.getGroupKey(daysFromNow(-1)))
    }

    @Test
    fun getGroupKey_today_isToday() {
        assertTrue(DateUtils.getGroupKey(daysFromNow(0)).startsWith("Today"))
    }

    @Test
    fun getGroupKey_tomorrow_isTomorrow() {
        assertTrue(DateUtils.getGroupKey(daysFromNow(1)).startsWith("Tomorrow"))
    }

    @Test
    fun getGroupKey_withinWeek_isNextWeek() {
        assertEquals("Next Week", DateUtils.getGroupKey(daysFromNow(5)))
    }

    @Test
    fun getGroupKey_beyondWeek_isLater() {
        assertEquals("Later", DateUtils.getGroupKey(daysFromNow(20)))
    }
}
