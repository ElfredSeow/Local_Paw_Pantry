package com.example.foodtracker

import com.example.foodtracker.data.Converters
import com.example.foodtracker.data.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JUnit test for [Converters] - no Android/Robolectric dependency needed since
 * Gson-based (de)serialization runs on the plain JVM.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `valid json round-trips to an equal reminder list`() {
        val reminders = listOf(
            Reminder(id = "r1", daysBefore = 3, isNotified = false),
            Reminder(id = "r2", daysBefore = 1, isNotified = true)
        )

        val json = converters.fromReminderList(reminders)
        val result = converters.toReminderList(json)

        assertEquals(reminders, result)
    }

    @Test
    fun `malformed json returns an empty list instead of throwing`() {
        val result = converters.toReminderList("{ this is not valid json ]]]")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty string returns an empty list instead of throwing`() {
        // Gson.fromJson returns null (rather than throwing) for an empty/blank input string,
        // so this exercises the `?: emptyList()` fallback specifically, not the catch block.
        val result = converters.toReminderList("")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty list round-trips to an empty list`() {
        val json = converters.fromReminderList(emptyList())
        val result = converters.toReminderList(json)

        assertEquals(emptyList<Reminder>(), result)
    }
}
