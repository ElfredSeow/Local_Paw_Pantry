package com.example.foodtracker.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens this app's system notification settings so the user can (re-)enable notifications
 * after denying the permission. Shared by the in-app permission banner (N4) and the
 * Settings screen's "Enable notifications" action (N5).
 */
fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
