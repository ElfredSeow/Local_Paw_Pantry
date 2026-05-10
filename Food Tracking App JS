package com.example.foodtracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodtracker.FoodApplication
import com.example.foodtracker.util.DateUtils
import kotlinx.coroutines.flow.first

class ExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as FoodApplication).repository
        val items = repository.allItems.first()

        items.forEach { item ->
            val daysUntil = DateUtils.getDaysUntil(item.expiryDate)
            val updatedReminders = item.reminders.map { reminder ->
                if (!reminder.isNotified && daysUntil <= reminder.daysBefore) {
                    sendNotification(item.name, daysUntil)
                    reminder.copy(isNotified = true)
                } else {
                    reminder
                }
            }
            if (updatedReminders != item.reminders) {
                repository.updateItem(item.copy(reminders = updatedReminders))
            }
        }

        return Result.success()
    }

    private fun sendNotification(itemName: String, daysUntil: Long) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "expiry_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Food Expiry", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (daysUntil <= 0) "Food Expired!" else "Upcoming Food Expiry!"
        val body = if (daysUntil <= 0) "$itemName has expired!" else "$itemName expires in $daysUntil day(s)."

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
