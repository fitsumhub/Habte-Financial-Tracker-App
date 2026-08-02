package com.mobile.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** One installed app that looks like it could be a bank/wallet app, matched against InstitutionCatalog. */
data class MonitorableApp(
    val packageName: String,
    val label: String,
    val institution: InstitutionProfile
)

/**
 * Scans installed, launchable apps for ones that plausibly belong to a known institution
 * in InstitutionCatalog, so NotificationCaptureScreen can offer a short, relevant picklist
 * instead of every app on the device. Reuses each institution's smsContains keywords
 * (originally written to match SMS sender text) against the app's own label/package name —
 * a bank's official app almost always contains the same brand keyword its SMS sender does
 * (e.g. "cbe" matches both "CBE" the SMS sender and "Commercial Bank of Ethiopia" / "CBE
 * Mobile Banking" the app label). Queries via ACTION_MAIN/CATEGORY_LAUNCHER rather than
 * PackageManager.getInstalledApplications() so this actually returns results on Android 11+
 * without needing the Play-Store-restricted QUERY_ALL_PACKAGES permission — see the
 * <queries> block in AndroidManifest.xml this depends on.
 */
object InstalledBankAppMatcher {
    fun findMonitorableApps(context: Context): List<MonitorableApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val seenPackages = mutableSetOf<String>()
        val results = mutableListOf<MonitorableApp>()
        for (info in resolved) {
            val packageName = info.activityInfo?.packageName ?: continue
            if (packageName == context.packageName) continue
            if (!seenPackages.add(packageName)) continue // some apps expose more than one launcher activity

            val label = try { info.loadLabel(pm).toString() } catch (e: Exception) { continue }
            val haystack = "${label.lowercase()} ${packageName.lowercase()}"
            val institution = InstitutionCatalog.ALL.find { profile ->
                profile.smsContains.any { keyword -> haystack.contains(keyword) }
            } ?: continue

            results.add(MonitorableApp(packageName, label, institution))
        }
        return results.sortedBy { it.label }
    }
}
