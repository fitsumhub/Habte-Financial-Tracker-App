package com.mobile.data

import android.content.Context

/**
 * The one real crash-safety net available here: Kotlin's Compose compiler plugin rejects
 * try/catch around composable calls outright, so there's no way to catch a composition-time
 * exception from a wrapper composable. This instead hooks the JVM's default uncaught
 * exception handler (works for exceptions from a recomposition, a LaunchedEffect coroutine,
 * a click callback — anywhere). There's no safe way to keep running after an uncaught
 * exception on the main thread, so instead of trying to recover mid-crash, this persists
 * the crash and lets the process terminate normally — MainActivity checks for it on the
 * next cold start and shows ErrorFallback instead of resuming as if nothing happened.
 */
object CrashReporter {
    private const val PREFS_NAME = "habte_crash"
    private const val KEY_LAST_CRASH = "last_crash_trace"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, throwable.stackTraceToString())
                    .apply()
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    /** Returns the previous launch's crash trace, if any, and clears it so it only shows once. */
    fun consumeLastCrash(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trace = prefs.getString(KEY_LAST_CRASH, null) ?: return null
        prefs.edit().remove(KEY_LAST_CRASH).apply()
        return trace
    }
}

/** Wraps a persisted crash trace as a displayable Throwable for ErrorFallback, without
 *  attaching the current (unrelated) call stack — only the original trace text matters. */
class PersistedCrash(trace: String) : Throwable(trace) {
    init { stackTrace = emptyArray() }
}
