package com.courtdiary.notification

import android.content.Context
import androidx.work.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val WORK_NAME = "court_diary_daily_reminder"

    /**
     * Enqueues a periodic daily work request targeting 8:00 AM.
     * Uses KEEP policy so existing schedules are not replaced on every app start.
     */
    fun schedule(context: Context) {
        val initialDelay = calculateDelayUntil8AM()

        val request = PeriodicWorkRequestBuilder<CaseReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Cancels the scheduled reminder. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Returns milliseconds until next 8:00 AM (minimum 0). */
    private fun calculateDelayUntil8AM(): Long {
        val now = LocalDateTime.now()
        val next8AM = if (now.hour < 8) {
            now.toLocalDate().atTime(8, 0)
        } else {
            now.toLocalDate().plusDays(1).atTime(8, 0)
        }
        val delay = ChronoUnit.MILLIS.between(
            now.atZone(ZoneId.systemDefault()).toInstant(),
            next8AM.atZone(ZoneId.systemDefault()).toInstant()
        )
        return maxOf(delay, 0L)
    }
}
