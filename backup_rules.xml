package com.example.foodtracker.util

import android.content.Context
import android.net.Uri
import com.example.foodtracker.data.FoodItem
import com.example.foodtracker.data.Reminder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object CsvHelper {
    private val gson = Gson()

    fun exportToCsv(context: Context, uri: Uri, items: List<FoodItem>): Boolean {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            val writer = OutputStreamWriter(outputStream)
            writer.write("id,name,category,quantity,expiryDate,reminders,image,icon\n")
            items.forEach { item ->
                val row = listOf(
                    item.id,
                    escapeCsv(item.name),
                    escapeCsv(item.category),
                    item.quantity.toString(),
                    item.expiryDate,
                    escapeCsv(gson.toJson(item.reminders)),
                    escapeCsv(item.imagePath ?: ""),
                    escapeCsv(item.iconId)
                ).joinToString(",")
                writer.write(row + "\n")
            }
            writer.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importFromCsv(context: Context, uri: Uri): List<FoodItem> {
        val importedItems = mutableListOf<FoodItem>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine() // Skip header
            var line: String? = reader.readLine()
            while (line != null) {
                val cols = parseCsvRow(line)
                if (cols.size >= 6) {
                    val remindersType = object : TypeToken<List<Reminder>>() {}.type
                    val reminders: List<Reminder> = try {
                        gson.fromJson(cols[5], remindersType)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    importedItems.add(
                        FoodItem(
                            id = cols[0],
                            name = cols[1],
                            category = cols[2],
                            quantity = cols[3].toIntOrNull() ?: 1,
                            expiryDate = cols[4],
                            reminders = reminders,
                            imagePath = if (cols.size > 6 && cols[6].isNotBlank()) cols[6] else null,
                            iconId = if (cols.size > 7) cols[7] else "general"
                        )
                    )
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return importedItems
    }

    private fun escapeCsv(str: String): String {
        return "\"${str.replace("\"", "\"\"")}\""
    }

    private fun parseCsvRow(row: String): List<String> {
        // Simple CSV parser for quoted fields
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
