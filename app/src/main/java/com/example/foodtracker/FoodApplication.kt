package com.example.foodtracker

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.data.Category
import com.example.foodtracker.data.FoodDatabase
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.worker.ExpiryWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class FoodApplication : Application() {

    // Without this handler, an exception thrown while seeding (or any other work launched on
    // this scope) is silently swallowed by the SupervisorJob - it never surfaces anywhere.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled exception in FoodApplication.applicationScope", throwable)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    val database by lazy { FoodDatabase.getDatabase(this) }
    val repository by lazy { FoodRepository(database.foodDao()) }

    override fun onCreate() {
        super.onCreate()
        scheduleExpiryCheck()
        applicationScope.launch {
            // Only seed on first run. Without this guard, every process start re-inserts all
            // defaults; OnConflictStrategy.IGNORE hides the duplicate-insert, but it also
            // silently re-creates a default category the user deliberately deleted.
            if (repository.getCategoryCount() == 0) {
                val defaultCategories = listOf(
                    "Uncategorized", "Produce", "Meat", "Dairy", "Pantry", "Frozen"
                )
                defaultCategories.forEach {
                    repository.insertCategory(Category(it))
                }
            }
        }
    }

    private fun scheduleExpiryCheck() {
        // getDaysUntil() only changes once per calendar day, so polling more often than daily
        // buys nothing. enqueueUniquePeriodicWork + KEEP means this is a no-op if the periodic
        // work is already scheduled, instead of enqueuing a fresh chain (with a new random
        // UUID) on every cold start, reboot, and WorkManager-spawned process forever.
        val workRequest = PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNext9AM(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /** Milliseconds from now until the next 9:00 AM local time (today, or tomorrow if today's has passed). */
    private fun millisUntilNext9AM(): Long {
        val now = LocalDateTime.now()
        var nextRun = LocalDateTime.of(now.toLocalDate(), LocalTime.of(9, 0))
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        return Duration.between(now, nextRun).toMillis()
    }

    private companion object {
        const val TAG = "FoodApplication"
    }
}
