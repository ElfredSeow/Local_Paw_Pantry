package com.example.foodtracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
            d.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
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
            d.format(DateTimeFormatter.ofPattern("MMMM d"))
        } catch (e: Exception) {
            date
        }
    }
}
