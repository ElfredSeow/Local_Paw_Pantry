package com.example.foodtracker

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.data.Category
import com.example.foodtracker.data.FoodDatabase
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.worker.ExpiryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FoodApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { FoodDatabase.getDatabase(this) }
    val repository by lazy { FoodRepository(database.foodDao()) }

    override fun onCreate() {
        super.onCreate()
        scheduleExpiryCheck()
        // Initialize default categories if database is empty
        applicationScope.launch {
            val defaultCategories = listOf(
                "Uncategorized", "Produce", "Meat", "Dairy", "Pantry", "Frozen"
            )
            defaultCategories.forEach { 
                repository.insertCategory(Category(it))
            }
        }
    }

    private fun scheduleExpiryCheck() {
        val workRequest = PeriodicWorkRequestBuilder<ExpiryWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
