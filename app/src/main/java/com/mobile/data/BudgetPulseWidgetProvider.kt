package com.mobile.data

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class BudgetPulseWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH_WIDGET = "com.mobile.ACTION_REFRESH_BUDGET_PULSE_WIDGET"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WeeklySpendingWidgetUpdater.updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WeeklySpendingWidgetUpdater.updateAllWidgets(context)
        }
    }
}

