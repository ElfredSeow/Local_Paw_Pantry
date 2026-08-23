package com.example.foodtracker.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    // Hoisted so Room doesn't allocate a new Gson + TypeToken on every row conversion.
    // Room instantiates this Converters class once per database and reuses that instance.
    private val gson = Gson()
    private val reminderListType = object : TypeToken<List<Reminder>>() {}.type

    @TypeConverter
    fun fromReminderList(value: List<Reminder>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toReminderList(value: String): List<Reminder> {
        // Gson.fromJson throws JsonSyntaxException on malformed input, and returns null
        // (rather than throwing) when given an empty/blank string. Left unhandled, either
        // case crashes every read of the food_items table - every screen and the worker,
        // on every launch, permanently, for a single bad row. Fail safe to an empty list
        // instead of taking down the whole app.
        return try {
            gson.fromJson<List<Reminder>>(value, reminderListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
