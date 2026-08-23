package com.example.foodtracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getDaysUntil(expiryDate: String): Long {
        return try {
            val expiry = LocalDate.parse(expiryDate, formatter)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, expiry)
        } catch (e: Exception) {
            0
        }
    }

    fun formatDate(date: String): String {
        return try {
            val d = LocalDate.parse(date, formatter)
            // Explicit Locale.US: this is a display formatter (month names), and without an
            // explicit Locale, DateTimeFormatter.ofPattern falls back to the device's default
            // locale - making the rendered string non-deterministic across devices (and across
            // test/CI environments) even though the underlying stored date is fixed ISO.
            d.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        } catch (e: Exception) {
            date
        }
    }

    fun getGroupKey(expiryDate: String): String {
        val days = getDaysUntil(expiryDate)
        return when {
            days < 0 -> "Expired"
            days == 0L -> "Today, ${formatDateFull(expiryDate)}"
            days == 1L -> "Tomorrow, ${formatDateFull(expiryDate)}"
            days <= 7 -> "Next Week"
            else -> "Later"
        }
    }

    private fun formatDateFull(date: String): String {
        return try {
            val d = LocalDate.parse(date, formatter)
            // See the note in formatDate() above: explicit Locale.US for the same reason.
            d.format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))
        } catch (e: Exception) {
            date
        }
    }
}
