package com.example.foodtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllItems(): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoodItem)

    // Bulk insert for CSV import, wrapped in a single transaction instead of one
    // transaction per row.
    @Transaction
    suspend fun insertItems(items: List<FoodItem>) {
        items.forEach { insertItem(it) }
    }

    @Delete
    suspend fun deleteItem(item: FoodItem)

    @Update
    suspend fun updateItem(item: FoodItem)

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getItemById(id: String): FoodItem?

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE food_items SET category = :fallback WHERE category = :name")
    suspend fun reassignItemsToCategory(name: String, fallback: String)

    // Reassigns this category's items to `fallback` and deletes the category itself, as one
    // atomic transaction, so items are never left pointing at a category that no longer
    // exists (and a crash/cancellation between the two steps can't leave things half-done).
    @Transaction
    suspend fun deleteCategoryAndReassign(category: Category, fallback: String) {
        reassignItemsToCategory(category.name, fallback)
        deleteCategory(category)
    }
}
