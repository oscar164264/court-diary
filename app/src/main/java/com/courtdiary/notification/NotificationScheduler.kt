package com.courtdiary.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.courtdiary.MainActivity
import java.util.Calendar

object NotificationScheduler {

    private const val ALARM_REQUEST_CODE = 9001

    /**
     * Schedules a daily alarm at 8:00 AM.
     *
     * Strategy:
     * - API 31+: use setAlarmClock() if SCHEDULE_EXACT_ALARM is granted (shown in status bar,
     *   cannot be suppressed by Doze/MIUI/Samsung). Falls back to setInexactRepeating() if
     *   the user has not granted the permission yet, so the app never crashes.
     * - API < 31: setAlarmClock() works without any special permission.
     */
    fun schedule(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmPendingIntent = buildAlarmPendingIntent(context)

            val triggerTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }.timeInMillis

            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()

            if (canExact) {
                // setAlarmClock() — highest priority exact alarm, shown in status bar,
                // cannot be killed by any OEM battery saver.
                val showIntent = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
                alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)
            } else {
                // Permission not yet granted — use inexact daily repeat as a safe fallback.
                // Once the user grants the permission (via Settings row), the next call to
                // schedule() will upgrade to setAlarmClock().
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    AlarmManager.INTERVAL_DAY,
                    alarmPendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Should not happen after the canScheduleExactAlarms() check, but guard anyway
            // so a permission edge-case never crashes the app.
        }
    }

    fun cancel(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildAlarmPendingIntent(context))
        } catch (_: Exception) {}
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
