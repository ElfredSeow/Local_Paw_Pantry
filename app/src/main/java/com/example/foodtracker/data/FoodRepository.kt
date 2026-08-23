package com.example.foodtracker.data

import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodDao: FoodDao) {
    val allItems: Flow<List<FoodItem>> = foodDao.getAllItems()
    val allCategories: Flow<List<Category>> = foodDao.getAllCategories()

    suspend fun insertItem(item: FoodItem) {
        foodDao.insertItem(item)
    }

    suspend fun insertItems(items: List<FoodItem>) {
        foodDao.insertItems(items)
    }

    suspend fun deleteItem(item: FoodItem) {
        foodDao.deleteItem(item)
    }

    suspend fun updateItem(item: FoodItem) {
        foodDao.updateItem(item)
    }

    suspend fun insertCategory(category: Category) {
        foodDao.insertCategory(category)
    }

    suspend fun getCategoryCount(): Int {
        return foodDao.getCategoryCount()
    }

    // Deleting a category always reassigns its items first, atomically - there is no plain
    // "delete category" path here on purpose, so callers can't accidentally orphan items.
    suspend fun deleteCategoryAndReassign(category: Category, fallback: String = "Uncategorized") {
        foodDao.deleteCategoryAndReassign(category, fallback)
    }
}
