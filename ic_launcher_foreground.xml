package com.example.foodtracker.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromReminderList(value: List<Reminder>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toReminderList(value: String): List<Reminder> {
        val listType = object : TypeToken<List<Reminder>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
