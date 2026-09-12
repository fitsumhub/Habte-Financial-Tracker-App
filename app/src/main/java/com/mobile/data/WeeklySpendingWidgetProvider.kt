package com.mobile.data

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class WeeklySpendingWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.mobile.ACTION_REFRESH_SPENDING_WIDGET"
        const val ACTION_UPDATE_WIDGET = "com.mobile.ACTION_UPDATE_SPENDING_WIDGET"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WeeklySpendingWidgetUpdater.updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_REFRESH_WIDGET || action == ACTION_UPDATE_WIDGET || action == Intent.ACTION_BOOT_COMPLETED) {
            WeeklySpendingWidgetUpdater.updateAllWidgets(context)
        }
    }
}

