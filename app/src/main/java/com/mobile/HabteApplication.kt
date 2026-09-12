package com.mobile

import android.app.Application
import android.util.Log
import com.mobile.data.CertificateRepository
import com.mobile.data.CrashReporter
import com.mobile.data.FinanceRepository
import com.mobile.data.PaymentReminderRepository
import com.mobile.data.SettingsRepository

/**
 * Custom Application class for Habte - Personal Finance.
 * Guarantees that whenever the process is created (by an Activity, BroadcastReceiver,
 * AppWidgetProvider, or NotificationListenerService), global crash reporting and
 * all data repositories are safely and reliably initialized before any component runs.
 */
class HabteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // 1. Install crash reporter before anything else can execute
            CrashReporter.install(this)

            // 2. Safely initialize repositories with applicationContext
            SettingsRepository.init(this)
            FinanceRepository.init(this)
            PaymentReminderRepository.init(this)
            CertificateRepository.init(this)
        } catch (t: Throwable) {
            Log.e("HabteApplication", "Error during application initialization", t)
        }
    }
}
