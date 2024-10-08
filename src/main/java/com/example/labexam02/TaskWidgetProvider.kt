package com.example.labexam02

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        @SuppressLint("RemoteViewLayout")
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_task)

            // Set task data (For simplicity, using dummy data. Replace with actual task data)
            views.setTextViewText(R.id.widgetTaskTitle, "Upcoming Task")
            views.setTextViewText(R.id.widgetTaskDescription, "Complete your Kotlin project")

            // Open the app when the widget is clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widgetTaskTitle, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
