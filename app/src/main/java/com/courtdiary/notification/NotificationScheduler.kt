package com.courtdiary.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.courtdiary.MainActivity
import java.util.Calendar

object NotificationScheduler {

    private const val ALARM_REQUEST_CODE = 9001

    /**
     * Schedules a daily alarm at 8:00 AM using setAlarmClock() — the same API used by
     * alarm clock apps. Unlike setExactAndAllowWhileIdle(), setAlarmClock() is shown in
     * the status bar and CANNOT be suppressed by Doze mode, MIUI battery killer,
     * Samsung battery optimiser, or any other OEM power management system.
     */
    fun schedule(context: Context) {
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

        // showIntent opens the app when the user taps the alarm in the status bar
        val showIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildAlarmPendingIntent(context))
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
