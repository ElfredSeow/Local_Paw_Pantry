package com.example.foodtracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.data.Category
import com.example.foodtracker.data.FoodDatabase
import com.example.foodtracker.data.FoodRepository
import com.example.foodtracker.worker.ExpiryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FoodApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { FoodDatabase.getDatabase(this) }
    val repository by lazy { FoodRepository(database.foodDao()) }

    override fun onCreate() {
        super.onCreate()
        scheduleExpiryCheck()
        seedDefaultCategoriesIfEmpty()
    }

    /**
     * Seeds the default categories only when none exist yet (D1). Doing this on every
     * launch used to resurrect categories the user had deliberately deleted.
     */
    private fun seedDefaultCategoriesIfEmpty() {
        applicationScope.launch {
            if (repository.allCategories.first().isEmpty()) {
                listOf("Uncategorized", "Produce", "Meat", "Dairy", "Pantry", "Frozen")
                    .forEach { repository.insertCategory(Category(it)) }
            }
        }
    }

    /**
     * Schedules a single daily expiry check.
     *
     * Older builds called [WorkManager.enqueue] on every launch, which registered a brand-new
     * periodic worker each time and let them pile up indefinitely — the root cause of the
     * "non-stop notifications" reports (N1). We now:
     *  1. cancel the accumulated duplicates exactly once (guarded by a pref), then
     *  2. enqueue a single UNIQUE periodic worker with KEEP, so re-launches never add more.
     */
    private fun scheduleExpiryCheck() {
        val workManager = WorkManager.getInstance(this)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_WORKER_DEDUP, false)) {
            workManager.cancelAllWork()
            prefs.edit().putBoolean(KEY_WORKER_DEDUP, true).apply()
        }
        val workRequest = PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            EXPIRY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        private const val PREFS = "food_tracker_prefs"
        private const val KEY_WORKER_DEDUP = "worker_dedup_v2_done"
        private const val EXPIRY_WORK_NAME = "expiry_check"
    }
}
