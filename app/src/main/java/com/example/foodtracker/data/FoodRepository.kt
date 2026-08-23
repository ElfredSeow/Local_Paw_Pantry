package com.example.foodtracker.data

import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodDao: FoodDao) {
    val allItems: Flow<List<FoodItem>> = foodDao.getAllItems()
    val allCategories: Flow<List<Category>> = foodDao.getAllCategories()

    suspend fun insertItem(item: FoodItem) {
        foodDao.insertItem(item)
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

    suspend fun deleteCategory(category: Category) {
        foodDao.deleteCategory(category)
    }
}
