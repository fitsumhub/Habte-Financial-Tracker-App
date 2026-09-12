package com.mobile.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationCaptureListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        try {
            SettingsRepository.init(applicationContext)
            FinanceRepository.init(applicationContext)
        } catch (t: Throwable) {
            Log.e("NotifCaptureService", "Error during onCreate", t)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            SettingsRepository.autoEnableDetectedApps(InstalledBankAppMatcher.findMonitorableApps(applicationContext))
        } catch (t: Throwable) {
            Log.e("NotifCaptureService", "Error in onListenerConnected", t)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try {
            if (!SettingsRepository.notificationCaptureEnabled.value) return
            val packageName = sbn.packageName ?: return
            val institutionId = SettingsRepository.monitoredApps.value[packageName] ?: return
            val institution = InstitutionCatalog.ALL.find { it.id == institutionId } ?: return

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // Safely extract text fields catching any BadParcelableException from Samsung/OEMs
            val title = try {
                extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            } catch (e: Throwable) {
                ""
            }
            val text = try {
                extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            } catch (e: Throwable) {
                ""
            }
            val bigText = try {
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
            } catch (e: Throwable) {
                ""
            }

            val body = listOf(title, text, bigText).filter { it.isNotBlank() }.distinct().joinToString("\n")
            if (body.isBlank()) return

            val parsed = SmsParser.parseBody(
                institution = institution,
                body = body,
                timestamp = sbn.postTime,
                idPrefix = "notif",
                sourceKey = packageName
            ) ?: return

            val alreadyCaptured = FinanceRepository.transactions.value.any { existing ->
                existing.id == parsed.id || (
                    existing.bankShortName == parsed.bankShortName &&
                    existing.type == parsed.type &&
                    existing.amount == parsed.amount &&
                    existing.accountSuffix == parsed.accountSuffix &&
                    run {
                        val existingMillis = transactionTimestampMillis(existing) ?: return@run false
                        kotlin.math.abs(existingMillis - sbn.postTime) < 10 * 60 * 1000L
                    }
                )
            }
            if (alreadyCaptured) return

            FinanceRepository.addTransaction(parsed)
            TransactionNotifier.notify(applicationContext, parsed)
        } catch (t: Throwable) {
            Log.e("NotifCaptureService", "Error processing posted notification", t)
        }
    }
}
