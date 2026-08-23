package com.example.foodtracker

import com.example.foodtracker.data.Reminder
import com.example.foodtracker.util.CsvHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JUnit tests for CsvHelper's pure functions - no Android/Context dependency, so
 * these run as local JVM unit tests.
 */
class CsvHelperTest {

    // ---- escapeCsv / parseCsvRow round-trips ----

    @Test
    fun `name containing a comma survives escape and parse`() {
        val original = "Peanut Butter, Chunky"
        val escaped = CsvHelper.escapeCsv(original)
        val cols = CsvHelper.parseCsvRow("$escaped,Pantry")
        assertEquals(listOf(original, "Pantry"), cols)
    }

    @Test
    fun `name containing quotes survives escape and parse`() {
        val original = "Ben & Jerry's \"Chunky Monkey\""
        val escaped = CsvHelper.escapeCsv(original)
        val cols = CsvHelper.parseCsvRow("$escaped,Freezer")
        assertEquals(original, cols[0])
        assertEquals("Freezer", cols[1])
    }

    @Test
    fun `name containing embedded newlines survives a multi-row file without corrupting later rows`() {
        val original = "Multi\nLine\r\nName"
        val escapedName = CsvHelper.escapeCsv(original)
        // Two data rows in one file; the first row's quoted name embeds real newlines,
        // which used to desync a line-by-line reader and shift every column after it.
        val text = buildString {
            append("id,name,category,quantity,expiryDate,reminders,image,icon\n")
            append("1,$escapedName,Pantry,2,2026-01-01,\"[]\",,general\n")
            append("2,Second Item,Pantry,3,2026-02-02,\"[]\",,general\n")
        }

        val records = CsvHelper.splitCsvRecords(text)
        // header + 2 data rows - the embedded newlines must not be miscounted as extra records
        assertEquals(3, records.size)

        val firstRowCols = CsvHelper.parseCsvRow(records[1])
        assertEquals(original, firstRowCols[1])
        assertEquals("Pantry", firstRowCols[2])

        val secondRowCols = CsvHelper.parseCsvRow(records[2])
        assertEquals("Second Item", secondRowCols[1])
        assertEquals("3", secondRowCols[3])
    }

    // ---- malformed rows ----

    @Test
    fun `row with too few columns is rejected instead of silently misreading fields`() {
        val cols = CsvHelper.parseCsvRow("1,OnlyName,Pantry")
        assertTrue(cols.size < 6)
        assertThrows(IllegalArgumentException::class.java) {
            CsvHelper.parseFoodItemRow(cols)
        }
    }

    @Test
    fun `completely blank row is rejected`() {
        val cols = CsvHelper.parseCsvRow("")
        assertThrows(IllegalArgumentException::class.java) {
            CsvHelper.parseFoodItemRow(cols)
        }
    }

    // ---- empty reminders ----

    @Test
    fun `empty reminders cell does not crash and yields an empty list`() {
        val cols = listOf("1", "Milk", "Dairy", "1", "2026-01-01", "", "", "general")
        val item = CsvHelper.parseFoodItemRow(cols)
        assertEquals(emptyList<Reminder>(), item.reminders)
    }

    @Test
    fun `blank whitespace reminders cell does not crash and yields an empty list`() {
        val cols = listOf("1", "Milk", "Dairy", "1", "2026-01-01", "   ", "", "general")
        val item = CsvHelper.parseFoodItemRow(cols)
        assertEquals(emptyList<Reminder>(), item.reminders)
    }

    @Test
    fun `malformed reminders json falls back to an empty list instead of throwing`() {
        val cols = listOf("1", "Milk", "Dairy", "1", "2026-01-01", "{not valid json", "", "general")
        val item = CsvHelper.parseFoodItemRow(cols)
        assertEquals(emptyList<Reminder>(), item.reminders)
    }

    // ---- quantity clamping ----

    @Test
    fun `negative quantity is clamped to zero`() {
        val cols = listOf("1", "Milk", "Dairy", "-5", "2026-01-01", "", "", "general")
        val item = CsvHelper.parseFoodItemRow(cols)
        assertEquals(0, item.quantity)
    }

    @Test
    fun `non numeric quantity falls back to one`() {
        val cols = listOf("1", "Milk", "Dairy", "not-a-number", "2026-01-01", "", "", "general")
        val item = CsvHelper.parseFoodItemRow(cols)
        assertEquals(1, item.quantity)
    }
}
