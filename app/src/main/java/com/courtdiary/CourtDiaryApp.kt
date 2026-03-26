package com.courtdiary

import android.app.Application
import com.courtdiary.notification.NotificationHelper
import com.courtdiary.notification.NotificationScheduler

/**
 * Application class – runs once at process start.
 * Responsible for one-time initialisation: notification channel, WorkManager scheduling.
 */
class CourtDiaryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channel (required on API 26+)
        NotificationHelper.createNotificationChannel(this)

        // Schedule daily reminder worker
        NotificationScheduler.schedule(this)
    }
}
