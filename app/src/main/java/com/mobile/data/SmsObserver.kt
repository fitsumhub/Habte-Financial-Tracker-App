package com.mobile.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time ContentObserver that monitors changes to the SMS provider.
 * Whenever a new SMS is received or updated on the device, it automatically
 * performs an ultra-fast incremental transaction sync without requiring manual intervention.
 */
object SmsObserver {
    private var observer: ContentObserver? = null
    private var debounceJob: Job? = null
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    fun register(context: Context) {
        if (observer != null) return
        val appContext = context.applicationContext

        val hasReadPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReadPermission) return

        val newObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Debounce rapid multi-part SMS writes
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(300L)
                    try {
                        FinanceRepository.init(appContext)
                        FinanceRepository.syncHistoricalSms(appContext, fullResync = false)
                    } catch (e: Exception) {
                        // Ignore and gracefully handle
                    }
                }
            }
        }
        observer = newObserver

        try {
            appContext.contentResolver.registerContentObserver(
                Uri.parse("content://sms"),
                true,
                newObserver
            )
        } catch (e: Exception) {
            observer = null
        }
    }

    fun unregister(context: Context) {
        observer?.let {
            try {
                context.applicationContext.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                // Ignore
            }
            observer = null
        }
    }
}

