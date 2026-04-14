package com.courtdiary.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.courtdiary.database.CaseDatabase
import com.courtdiary.repository.CaseRepository
import com.courtdiary.utils.SmsHelper
import com.courtdiary.widget.CourtDiaryWidget
import com.courtdiary.viewmodel.PreferencesKeys
import com.courtdiary.viewmodel.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // On device reboot, just re-schedule the alarm — no notifications needed
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.schedule(context)
            return
        }

        // Re-schedule for the next day before doing anything else
        NotificationScheduler.schedule(context)

        // Acquire WakeLock to keep CPU alive during async DB work
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CourtDiary:NotificationWakeLock"
        ).apply { acquire(30_000L) }   // 30s to cover DB + SMS sending

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStore.data.first()
                val notificationsOn = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
                val smsOn = prefs[PreferencesKeys.SMS_REMINDER_ENABLED] ?: false

                val dao = CaseDatabase.getDatabase(context).caseDao()
                val repo = CaseRepository(dao)

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

                val todayStart    = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val todayEnd      = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val tomorrowStart = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val tomorrowEnd   = tomorrow.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val todayCases    = repo.getCasesByDateOnce(todayStart, todayEnd)
                val tomorrowCases = repo.getCasesByDateOnce(tomorrowStart, tomorrowEnd)

                // ── App notifications ────────────────────────────────────────
                if (notificationsOn) {
                    if (tomorrowCases.isNotEmpty()) {
                        val count = tomorrowCases.size
                        val message = if (count == 1)
                            "1 court hearing tomorrow: Case ${tomorrowCases.first().caseNumber}"
                        else "$count court hearings scheduled for tomorrow"
                        NotificationHelper.showNotification(
                            context, "Court Cases Tomorrow", message, NotificationHelper.NOTIF_ID_TOMORROW
                        )
                    }
                    if (todayCases.isNotEmpty()) {
                        val count = todayCases.size
                        val message = if (count == 1)
                            "1 court hearing today: Case ${todayCases.first().caseNumber}"
                        else "$count court hearings scheduled for today"
                        NotificationHelper.showNotification(
                            context, "Court Cases Today", message, NotificationHelper.NOTIF_ID_TODAY
                        )
                    }
                }

                // ── SMS reminders to clients (tomorrow's cases only) ─────────
                if (smsOn && tomorrowCases.isNotEmpty()) {
                    val hasSmsPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasSmsPermission) {
                        tomorrowCases.forEach { case ->
                            SmsHelper.sendHearingReminder(
                                context  = context,
                                phone    = case.clientPhone,
                                clientName  = case.clientName,
                                caseNumber  = case.caseNumber,
                                courtName   = case.courtName
                            )
                        }
                    }
                }
                // ── Refresh home screen widget ───────────────────────────────
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, CourtDiaryWidget::class.java)
                )
                widgetIds.forEach { id ->
                    CourtDiaryWidget.updateWidget(context, appWidgetManager, id)
                }

            } finally {
                wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
