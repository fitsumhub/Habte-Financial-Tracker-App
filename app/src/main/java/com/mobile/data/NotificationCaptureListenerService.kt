package com.mobile.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads notifications from bank/wallet apps found on the device (see
 * SettingsRepository.monitoredApps / InstalledBankAppMatcher) and, when one looks like a
 * transaction alert, parses it into a Transaction the same way SmsParser handles an SMS —
 * a second, complementary capture path for banks that push app notifications instead of
 * (or in addition to) SMS. Every other app's notifications are never inspected: both the
 * master switch and the per-package allowlist are checked before any content is read.
 *
 * Fully automatic once the user grants the one unavoidable system permission (Android's
 * notification-listener access, granted from system Settings — see NotificationCaptureScreen):
 * no per-app setup is required, since onListenerConnected() below auto-enables every matching
 * bank/wallet app it finds installed.
 *
 * Android binds this service independently of MainActivity ever having run (e.g. right
 * after boot, if the user granted notification access previously), so — like
 * PaymentReminderReceiver — it can't assume FinanceRepository/SettingsRepository are
 * already initialized and does so itself.
 */
class NotificationCaptureListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(applicationContext)
        FinanceRepository.init(applicationContext)
    }

    // Fires once Android actually establishes the listener connection — i.e. exactly when
    // notification access has just been granted (or the service reconnects after boot/update).
    // This is what makes capture automatic: the user never has to open NotificationCaptureScreen
    // and tick boxes for it to start working.
    override fun onListenerConnected() {
        super.onListenerConnected()
        SettingsRepository.autoEnableDetectedApps(InstalledBankAppMatcher.findMonitorableApps(applicationContext))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!SettingsRepository.notificationCaptureEnabled.value) return
        val institutionId = SettingsRepository.monitoredApps.value[sbn.packageName] ?: return
        val institution = InstitutionCatalog.ALL.find { it.id == institutionId } ?: return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        // bigText very often just repeats text verbatim — distinct() keeps the combined
        // body from getting the same sentence pasted into it twice.
        val body = listOf(title, text, bigText).filter { it.isNotBlank() }.distinct().joinToString("\n")
        if (body.isBlank()) return

        val parsed = SmsParser.parseBody(
            institution = institution,
            body = body,
            timestamp = sbn.postTime,
            idPrefix = "notif",
            sourceKey = sbn.packageName
        ) ?: return

        // A bank that pushes both an SMS and an app notification for the same event would
        // otherwise create two separate transactions here — the SMS and notification paths
        // use deliberately different id namespaces (idPrefix/sourceKey), so Room's
        // insert-ignore dedupe can't catch a cross-source duplicate the way it catches a
        // repeated SMS. Skip if a transaction from the same bank, amount, and type was
        // already captured within the last few minutes, regardless of which source it came
        // from — matches the same signature the Duplicate Check tool looks for after the
        // fact, just applied before the duplicate is ever written.
        val alreadyCaptured = FinanceRepository.transactions.value.any { existing ->
            existing.bankShortName == parsed.bankShortName &&
                existing.type == parsed.type &&
                existing.amount == parsed.amount &&
                run {
                    val existingMillis = transactionTimestampMillis(existing) ?: return@run false
                    kotlin.math.abs(existingMillis - sbn.postTime) < 10 * 60 * 1000L
                }
        }
        if (alreadyCaptured) return

        // Both calls are already internally async/synchronous-safe on their own (same
        // direct-call pattern SmsReceiver uses) — no coroutine wrapper needed here.
        FinanceRepository.addTransaction(parsed)
        TransactionNotifier.notify(applicationContext, parsed)
    }
}
