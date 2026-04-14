package com.courtdiary.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.courtdiary.R
import com.courtdiary.database.CaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class CourtDiaryWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = CaseDatabase.getDatabase(context)
                val cal = Calendar.getInstance()

                // Start of today (midnight)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                // End of today (23:59:59)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfDay = cal.timeInMillis

                val count = db.caseDao().getCasesByDateOnce(startOfDay, endOfDay).size

                val views = RemoteViews(context.packageName, R.layout.widget_court_diary)
                views.setTextViewText(R.id.widget_count, count.toString())
                views.setTextViewText(
                    R.id.widget_label,
                    if (count == 1) "hearing today" else "hearings today"
                )

                // Tap to open app
                val intent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                if (intent != null) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_count, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_label, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
