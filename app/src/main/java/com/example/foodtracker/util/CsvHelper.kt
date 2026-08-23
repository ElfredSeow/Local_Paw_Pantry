package com.example.foodtracker.util

import android.content.Context
import android.net.Uri
import com.example.foodtracker.data.FoodItem
import com.example.foodtracker.data.Reminder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Result of a CSV import: the items that parsed successfully, plus a count of rows that
 * were skipped because they were malformed (too few columns, bad data, etc). Reporting
 * both lets the caller tell the user honestly what happened instead of just an item count.
 */
data class ImportResult(val items: List<FoodItem>, val skipped: Int)

object CsvHelper {
    private val gson = Gson()

    private val CSV_HEADER_COLUMNS =
        listOf("id", "name", "category", "quantity", "expiryDate", "reminders", "image", "icon")
    private val CSV_HEADER_LINE = CSV_HEADER_COLUMNS.joinToString(",")

    fun exportToCsv(context: Context, uri: Uri, items: List<FoodItem>): Boolean {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return false
            OutputStreamWriter(outputStream).use { writer ->
                writer.write("$CSV_HEADER_LINE\n")
                items.forEach { item ->
                    // Escape every field uniformly (including id/expiryDate, which used to
                    // be written raw) so nothing can desync the row on import.
                    val row = listOf(
                        escapeCsv(item.id),
                        escapeCsv(item.name),
                        escapeCsv(item.category),
                        escapeCsv(item.quantity.toString()),
                        escapeCsv(item.expiryDate),
                        escapeCsv(gson.toJson(item.reminders)),
                        escapeCsv(item.imagePath ?: ""),
                        escapeCsv(item.iconId)
                    ).joinToString(",")
                    writer.write("$row\n")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Imports items from a CSV file. Reads the whole stream up front and splits it into
     * records on newlines that fall outside quotes (see [splitCsvRecords]), so a quoted
     * field containing a real newline can't desync the parser. Each row is parsed and
     * inserted independently: a malformed row is skipped and counted rather than aborting
     * the rest of the file.
     */
    fun importFromCsv(context: Context, uri: Uri): ImportResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return ImportResult(emptyList(), 0)

        val text = InputStreamReader(inputStream).use { it.readText() }
        val records = splitCsvRecords(text)
        if (records.isEmpty()) return ImportResult(emptyList(), 0)

        // Only skip the first record if it actually matches the expected header; a
        // header-less CSV is treated as data so its first real row isn't silently lost.
        val startIndex = if (parseCsvRow(records[0]) == CSV_HEADER_COLUMNS) 1 else 0

        val importedItems = mutableListOf<FoodItem>()
        var skipped = 0
        for (i in startIndex until records.size) {
            try {
                val cols = parseCsvRow(records[i])
                importedItems.add(parseFoodItemRow(cols))
            } catch (e: Exception) {
                // Only this row is discarded; one bad line no longer aborts the whole import.
                skipped++
            }
        }
        return ImportResult(importedItems, skipped)
    }

    /**
     * Builds a [FoodItem] from one already-split CSV row. Pure and Context-free so it is
     * directly unit testable. Throws [IllegalArgumentException] for rows with too few
     * columns, which the caller in [importFromCsv] catches and counts as skipped.
     */
    internal fun parseFoodItemRow(cols: List<String>): FoodItem {
        require(cols.size >= 6) { "Expected at least 6 columns, got ${cols.size}" }

        val remindersType = object : TypeToken<List<Reminder>>() {}.type
        val reminders: List<Reminder> = try {
            // Gson returns null for an empty/blank cell WITHOUT throwing, so without the
            // `?: emptyList()` a null would slip past this catch into a non-null val and
            // crash on Kotlin's generated null-check.
            gson.fromJson(cols[5], remindersType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // The in-app stepper enforces a zero floor; a hand-edited CSV does not, so clamp here too.
        val quantity = (cols[3].toIntOrNull() ?: 1).coerceAtLeast(0)

        return FoodItem(
            id = cols[0],
            name = cols[1],
            category = cols[2],
            quantity = quantity,
            expiryDate = cols[4],
            reminders = reminders,
            imagePath = if (cols.size > 6 && cols[6].isNotBlank()) cols[6] else null,
            iconId = if (cols.size > 7 && cols[7].isNotBlank()) cols[7] else "general"
        )
    }

    internal fun escapeCsv(str: String): String {
        return "\"${str.replace("\"", "\"\"")}\""
    }

    /**
     * Splits a full CSV file's text into raw record strings, breaking only on newlines
     * that fall outside quoted fields (CRLF, LF, and bare CR are all treated as one line
     * break). This is the RFC 4180 fix: reading line-by-line before parsing quotes lets a
     * quoted field with an embedded newline desync the parser and shift every later column.
     * Each returned record may itself still contain embedded literal newlines from inside
     * a quoted field - [parseCsvRow] is what turns one record into its column values.
     */
    internal fun splitCsvRecords(text: String): List<String> {
        val records = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            when {
                c == '\"' -> {
                    current.append(c)
                    if (inQuotes && i + 1 < len && text[i + 1] == '\"') {
                        current.append(text[i + 1])
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == '\r' && !inQuotes -> {
                    records.add(current.toString())
                    current.setLength(0)
                    if (i + 1 < len && text[i + 1] == '\n') i++
                }
                c == '\n' && !inQuotes -> {
                    records.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) {
            records.add(current.toString())
        }
        return records
    }

    /**
     * Parses one already-isolated CSV record (which may contain embedded literal newlines
     * from within a quoted field) into its column values. Simple hand-rolled parser for
     * quoted fields and doubled-quote escaping.
     */
    internal fun parseCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < row.length && row[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
