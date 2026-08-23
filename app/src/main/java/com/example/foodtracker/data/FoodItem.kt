package com.example.foodtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["category"]),
        Index(value = ["expiryDate"])
    ]
)
data class FoodItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    // Plain String on purpose, not a @ForeignKey to Category.name: SQLite can't ALTER TABLE
    // to add a foreign key constraint to an existing table, so wiring that up would require
    // a full food_items table rebuild. Out of scope for this pass.
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
