package com.example.foodtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val quantity: Int,
    val expiryDate: String, // YYYY-MM-DD
    val imagePath: String? = null,
    val iconId: String = "general",
    val reminders: List<Reminder> = emptyList()
)

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val daysBefore: Int,
    val isNotified: Boolean = false
)
