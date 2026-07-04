package com.example.foodtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllItems(): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoodItem)

    @Delete
    suspend fun deleteItem(item: FoodItem)

    @Update
    suspend fun updateItem(item: FoodItem)

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getItemById(id: String): FoodItem?

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE food_items SET category = 'Uncategorized' WHERE category = :name")
    suspend fun reassignItemsToUncategorized(name: String)

    /**
     * Deletes a category and moves any items that referenced it to "Uncategorized",
     * atomically, so an item can never point at a category that no longer exists (D2).
     */
    @Transaction
    suspend fun deleteCategoryAndReassign(category: Category) {
        reassignItemsToUncategorized(category.name)
        deleteCategory(category)
    }
}
