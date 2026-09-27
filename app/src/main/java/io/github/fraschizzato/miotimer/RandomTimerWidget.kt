package io.github.fraschizzato.miotimer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class RandomTimerWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val intent = Intent(context, TimerService::class.java)
                .setAction(TimerService.ACTION_START_RANDOM)
            val pendingIntent = PendingIntent.getForegroundService(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_random_timer)
            views.setOnClickPendingIntent(R.id.random_timer_start, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
