package com.courtdiary.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.courtdiary.database.CaseDatabase
import com.courtdiary.repository.CaseRepository
import java.time.LocalDate
import java.time.ZoneId

/**
 * WorkManager worker that runs once per day.
 * Checks for cases today and tomorrow, then posts notifications.
 */
class CaseReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dao = CaseDatabase.getDatabase(context).caseDao()
            val repo = CaseRepository(dao)

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val todayEnd = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val tomorrowStart = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val tomorrowEnd = tomorrow.atTime(23, 59, 59).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            val todayCases = repo.getCasesByDateOnce(todayStart, todayEnd)
            val tomorrowCases = repo.getCasesByDateOnce(tomorrowStart, tomorrowEnd)

            // Notification for tomorrow's cases
            if (tomorrowCases.isNotEmpty()) {
                val count = tomorrowCases.size
                val title = "Court Cases Tomorrow"
                val message = if (count == 1) {
                    "You have 1 court hearing tomorrow: Case ${tomorrowCases.first().caseNumber}"
                } else {
                    "You have $count court hearings scheduled for tomorrow."
                }
                NotificationHelper.showNotification(
                    context, title, message, NotificationHelper.NOTIF_ID_TOMORROW
                )
            }

            // Notification for today's cases
            if (todayCases.isNotEmpty()) {
                val count = todayCases.size
                val title = "Court Cases Today"
                val message = if (count == 1) {
                    "You have 1 court hearing today: Case ${todayCases.first().caseNumber}"
                } else {
                    "You have $count court hearings scheduled for today."
                }
                NotificationHelper.showNotification(
                    context, title, message, NotificationHelper.NOTIF_ID_TODAY
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
