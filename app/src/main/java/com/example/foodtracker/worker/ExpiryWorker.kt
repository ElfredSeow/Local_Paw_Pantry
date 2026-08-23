package com.example.foodtracker.worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodtracker.FoodApplication
import com.example.foodtracker.MainActivity
import com.example.foodtracker.data.FoodItem
import com.example.foodtracker.data.Reminder
import com.example.foodtracker.util.DateUtils
import kotlinx.coroutines.flow.first

class ExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Defensive cast: applicationContext is only a FoodApplication in the real running
            // app. Under instrumented tests, TestListenableWorkerBuilder, or any custom
            // WorkerFactory it can be a different Application subclass, which would otherwise
            // throw ClassCastException here -- an exception WorkManager simply records as a
            // silently failed run. Fail cleanly instead of crashing.
            // TODO(real fix): inject FoodRepository via a proper WorkerFactory/DI (e.g. Hilt)
            // instead of reaching into the Application singleton from the worker. Out of scope
            // for this pass.
            val app = applicationContext as? FoodApplication
                ?: return Result.failure()
            val repository = app.repository

            // If notifications are disabled system-wide -- POST_NOTIFICATIONS denied on API 33+,
            // or the user turned the app's notifications off in system settings -- notify() would
            // silently no-op. Check once up front so we never mark a reminder as notified for a
            // notification that was never actually shown; otherwise that reminder tier would be
            // permanently consumed even after permission/notifications are later restored.
            val notificationsEnabled =
                NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()

            val items = repository.allItems.first()
            var notifiedCount = 0
            var anyExpiredNotified = false

            items.forEach { item ->
                val daysUntil = DateUtils.getDaysUntil(item.expiryDate)
                val updatedReminders = item.reminders.map { reminder ->
                    if (!reminder.isNotified && daysUntil <= reminder.daysBefore) {
                        if (notificationsEnabled) {
                            sendNotification(item, reminder, daysUntil)
                            notifiedCount++
                            if (daysUntil <= 0) anyExpiredNotified = true
                            reminder.copy(isNotified = true)
                        } else {
                            // Nothing was delivered, so leave isNotified = false: this reminder
                            // tier must still fire once notifications are re-enabled instead of
                            // being permanently consumed with no notification ever shown.
                            reminder
                        }
                    } else {
                        reminder
                    }
                }
                if (updatedReminders != item.reminders) {
                    repository.updateItem(item.copy(reminders = updatedReminders))
                }
            }

            // Collapse multiple alerts from a single run into one group with a summary instead
            // of N disconnected notifications. A single alert already reads fine on its own, so
            // only add the summary once there is actually more than one to bundle.
            if (notifiedCount > 1) {
                sendGroupSummaryNotification(notifiedCount, anyExpiredNotified)
            }

            Result.success()
        } catch (e: Exception) {
            // Never let an unexpected exception propagate out of doWork(): an uncaught throw
            // here would silently kill expiry notifications from this point forward. Ask
            // WorkManager to retry instead of crashing the run.
            Log.e(TAG, "ExpiryWorker run failed, will retry", e)
            Result.retry()
        }
    }

    @SuppressLint("MissingPermission") // gated by areNotificationsEnabled() check in doWork()
    private fun sendNotification(item: FoodItem, reminder: Reminder, daysUntil: Long) {
        ensureNotificationChannels()

        val isExpired = daysUntil <= 0
        val channelId = if (isExpired) CHANNEL_ID_EXPIRED else CHANNEL_ID_UPCOMING
        val title = if (isExpired) "Food Expired!" else "Upcoming Food Expiry!"
        val body = if (isExpired) {
            "${item.name} has expired!"
        } else {
            "${item.name} expires in $daysUntil day(s)."
        }
        val id = notificationId(item, reminder)

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(if (isExpired) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_EXPIRY)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    @SuppressLint("MissingPermission") // gated by areNotificationsEnabled() check in doWork()
    private fun sendGroupSummaryNotification(notifiedCount: Int, anyExpired: Boolean) {
        ensureNotificationChannels()

        // Match the summary's channel/priority to the most urgent item it bundles, so the
        // group as a whole doesn't present as lower priority than the expired-item alerts
        // inside it.
        val channelId = if (anyExpired) CHANNEL_ID_EXPIRED else CHANNEL_ID_UPCOMING
        val summary = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Food Expiry Updates")
            .setContentText("$notifiedCount items need your attention")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(if (anyExpired) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(GROUP_KEY_EXPIRY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val expiredChannel = NotificationChannel(
            CHANNEL_ID_EXPIRED,
            "Food Expired",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for food items that have already expired."
        }
        val upcomingChannel = NotificationChannel(
            CHANNEL_ID_UPCOMING,
            "Upcoming Food Expiry",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for food items that are approaching their expiry date."
        }

        notificationManager.createNotificationChannel(expiredChannel)
        notificationManager.createNotificationChannel(upcomingChannel)
    }

    // Stable, unique ID per (item, reminder) tier. System.currentTimeMillis().toInt() truncated
    // to Int and aliases roughly every 24.8 days, and same-millisecond collisions are realistic
    // when several items cross a threshold in the same pass -- either way one item's alert could
    // silently overwrite another's. Deriving the ID from the (stable) item and reminder ids
    // means each reminder tier keeps a consistent notification identity across runs, so a later
    // update replaces its own earlier notification rather than a different item's.
    private fun notificationId(item: FoodItem, reminder: Reminder): Int =
        (item.id + reminder.id).hashCode()

    companion object {
        private const val TAG = "ExpiryWorker"
        private const val CHANNEL_ID_EXPIRED = "expiry_notifications_expired"
        private const val CHANNEL_ID_UPCOMING = "expiry_notifications_upcoming"
        private const val GROUP_KEY_EXPIRY = "com.example.foodtracker.EXPIRY_GROUP"
        private const val SUMMARY_NOTIFICATION_ID = 987_654_321
    }
}
