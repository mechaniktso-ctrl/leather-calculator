package ru.leather.calc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LeatherWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        /** Перерисовывает все виджеты на домашнем экране. */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, LeatherWidget::class.java)
                )
                if (ids.isNotEmpty()) {
                    ids.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
                }
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("leather_calc", Context.MODE_PRIVATE)
            val area = prefs.getString("w_area", "") ?: ""
            val cost = prefs.getString("w_cost", "") ?: ""
            val cur = prefs.getString("cur", "₽") ?: "₽"
            val project = prefs.getString("projName", "") ?: ""

            val views = RemoteViews(context.packageName, R.layout.widget_leather)
            views.setTextViewText(
                R.id.widget_title,
                if (project.isBlank()) "Калькулятор кожи" else project
            )
            views.setTextViewText(
                R.id.widget_area,
                if (area.isBlank()) "— дм²" else "$area дм²"
            )
            views.setTextViewText(
                R.id.widget_cost,
                if (cost.isBlank()) "Откройте расчёт" else "Кожа: $cost $cur"
            )

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_area, pending)
            views.setOnClickPendingIntent(R.id.widget_title, pending)
            views.setOnClickPendingIntent(R.id.widget_hint, pending)
            return views
        }
    }
}
