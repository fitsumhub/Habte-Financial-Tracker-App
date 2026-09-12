package com.mobile.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SettingsRepository {
    private const val PREFS_NAME = "habte_settings"
    private lateinit var prefs: SharedPreferences

    private val _biometricEnabled = MutableStateFlow(true)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _autoHideBalances = MutableStateFlow(false)
    val autoHideBalances: StateFlow<Boolean> = _autoHideBalances.asStateFlow()
    
    private val _privacyMode = MutableStateFlow(false)
    val privacyMode: StateFlow<Boolean> = _privacyMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _smsAlerts = MutableStateFlow(true)
    val smsAlerts: StateFlow<Boolean> = _smsAlerts.asStateFlow()

    private val _dateFormat = MutableStateFlow("MM/DD/YYYY")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    // "Gregorian" or "Ethiopian" — which calendar system dates are displayed in across the app.
    private val _calendarSystem = MutableStateFlow("Gregorian")
    val calendarSystem: StateFlow<String> = _calendarSystem.asStateFlow()

    // Default financial overview period used in profile/dashboard reporting.
    private val _financialOverviewPeriod = MutableStateFlow("Monthly")
    val financialOverviewPeriod: StateFlow<String> = _financialOverviewPeriod.asStateFlow()

    private val _theme = MutableStateFlow("Light")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _hasPinSet = MutableStateFlow(false)
    val hasPinSet: StateFlow<Boolean> = _hasPinSet.asStateFlow()

    private val _hasSeenOnboarding = MutableStateFlow(false)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    // Master switch for NotificationCaptureListenerService — on by default so capture starts
    // working automatically as soon as the user grants the OS-level notification access
    // permission (that permission grant is itself the real consent gate; Android requires it
    // to be a deliberate action in system Settings, so this switch doesn't need to duplicate
    // it). Still exposed as a toggle so the user can fully disable capture without having to
    // revoke system-level notification access to do it.
    private val _notificationCaptureEnabled = MutableStateFlow(true)
    val notificationCaptureEnabled: StateFlow<Boolean> = _notificationCaptureEnabled.asStateFlow()

    // Installed bank/wallet apps being monitored, packageName -> InstitutionCatalog id (see
    // NotificationCaptureScreen / InstalledBankAppMatcher). Keyed on package name so
    // NotificationCaptureListenerService can resolve which institution a notification belongs
    // to without touching PackageManager at notification time. A notification from any other
    // app is never inspected, regardless of notificationCaptureEnabled. Populated
    // automatically by autoEnableDetectedApps() — every matching bank/wallet app found on the
    // device is monitored by default, not opt-in per app; the user can still uncheck one in
    // NotificationCaptureScreen to stop monitoring it.
    private val _monitoredApps = MutableStateFlow<Map<String, String>>(emptyMap())
    val monitoredApps: StateFlow<Map<String, String>> = _monitoredApps.asStateFlow()

    private val _userName = MutableStateFlow("Account Holder")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    private val _lastSmsSyncTimestamp = MutableStateFlow(0L)
    val lastSmsSyncTimestamp: StateFlow<Long> = _lastSmsSyncTimestamp.asStateFlow()

    // Zero or more of: "Every 12 Hours", "Daily", "Weekly", "Every 15 Days", "Monthly" —
    // each selected frequency gets its own independent recurring summary notification.
    private val _summaryFrequencies = MutableStateFlow(setOf("Daily"))
    val summaryFrequencies: StateFlow<Set<String>> = _summaryFrequencies.asStateFlow()

    // Epoch millis until which banner/interstitial ads are suppressed, earned by watching
    // a rewarded ad (see AdMobService.showRewardedIfLoaded). 0 means no active grant.
    private val _adFreeUntilMillis = MutableStateFlow(0L)
    val adFreeUntilMillis: StateFlow<Long> = _adFreeUntilMillis.asStateFlow()

    private val _widgetsEnabled = MutableStateFlow(true)
    val widgetsEnabled: StateFlow<Boolean> = _widgetsEnabled.asStateFlow()

    private val _preferredWidgetStyle = MutableStateFlow("Daily Digest")
    val preferredWidgetStyle: StateFlow<String> = _preferredWidgetStyle.asStateFlow()

    private val _hiddenAccountIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenAccountIds: StateFlow<Set<String>> = _hiddenAccountIds.asStateFlow()

    private val _profileAvatarRing = MutableStateFlow("Emerald")
    val profileAvatarRing: StateFlow<String> = _profileAvatarRing.asStateFlow()

    /** True while an ad-free grant from a watched rewarded ad is still active. */
    private var applicationContext: Context? = null

    fun isAdFreeActive(): Boolean = System.currentTimeMillis() < _adFreeUntilMillis.value

    @Synchronized
    fun init(context: Context) {
        try {
            applicationContext = context.applicationContext
            if (::prefs.isInitialized) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _biometricEnabled.value = prefs.getBoolean("biometric", true)
            _autoHideBalances.value = prefs.getBoolean("auto_hide", false)
            _privacyMode.value = prefs.getBoolean("privacy_mode", false)
            _notificationsEnabled.value = prefs.getBoolean("notifications", true)
            _smsAlerts.value = prefs.getBoolean("sms_alerts", true)
            _dateFormat.value = prefs.getString("date_format", "MM/DD/YYYY") ?: "MM/DD/YYYY"
            _calendarSystem.value = prefs.getString("calendar_system", "Gregorian") ?: "Gregorian"
            _financialOverviewPeriod.value = prefs.getString("financial_overview_period", "Monthly") ?: "Monthly"
            _theme.value = prefs.getString("theme", "Light") ?: "Light"
            _hasPinSet.value = prefs.contains("pin_hash")
            _hasSeenOnboarding.value = prefs.getBoolean("has_seen_onboarding", false)
            _notificationCaptureEnabled.value = prefs.getBoolean("notification_capture_enabled", true)
            _monitoredApps.value = prefs.getStringSet("monitored_apps", emptySet())
                ?.mapNotNull { encoded ->
                    val parts = encoded.split("::", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                ?.toMap() ?: emptyMap()
            _userName.value = prefs.getString("user_name", "Account Holder") ?: "Account Holder"
            _userEmail.value = prefs.getString("user_email", "") ?: ""
            _profilePhotoUri.value = prefs.getString("profile_photo_uri", null)
            _lastSmsSyncTimestamp.value = prefs.getLong("last_sms_sync_timestamp", 0L)
            _summaryFrequencies.value = prefs.getStringSet("summary_frequencies", setOf("Daily"))?.toSet() ?: setOf("Daily")
            _adFreeUntilMillis.value = prefs.getLong("ad_free_until", 0L)
            _widgetsEnabled.value = prefs.getBoolean("widgets_enabled", true)
            _preferredWidgetStyle.value = prefs.getString("preferred_widget_style", "Daily Digest") ?: "Daily Digest"
            _hiddenAccountIds.value = prefs.getStringSet("hidden_account_ids", emptySet())?.toSet() ?: emptySet()
            _profileAvatarRing.value = prefs.getString("profile_avatar_ring", "Emerald") ?: "Emerald"
        } catch (t: Throwable) {
            android.util.Log.e("SettingsRepository", "Error initializing settings", t)
        }
    }

    private fun safeEdit(action: SharedPreferences.Editor.() -> Unit) {
        try {
            if (::prefs.isInitialized) {
                val editor = prefs.edit()
                editor.action()
                editor.apply()
            }
        } catch (t: Throwable) {
            android.util.Log.e("SettingsRepository", "Error saving preference", t)
        }
    }

    fun setProfileAvatarRing(ring: String) {
        _profileAvatarRing.value = ring
        prefs.edit().putString("profile_avatar_ring", ring).apply()
    }

    fun setWidgetsEnabled(enabled: Boolean) {
        _widgetsEnabled.value = enabled
        prefs.edit().putBoolean("widgets_enabled", enabled).apply()
        applicationContext?.let { WeeklySpendingWidgetUpdater.updateAllWidgets(it) }
    }

    fun setPreferredWidgetStyle(style: String) {
        _preferredWidgetStyle.value = style
        prefs.edit().putString("preferred_widget_style", style).apply()
        applicationContext?.let { WeeklySpendingWidgetUpdater.updateAllWidgets(it) }
    }

    fun toggleAccountVisibility(accountId: String) {
        val current = _hiddenAccountIds.value.toMutableSet()
        if (current.contains(accountId)) {
            current.remove(accountId)
        } else {
            current.add(accountId)
        }
        _hiddenAccountIds.value = current
        prefs.edit().putStringSet("hidden_account_ids", current).apply()
        applicationContext?.let { WeeklySpendingWidgetUpdater.updateAllWidgets(it) }
    }

    fun isAccountHidden(accountId: String): Boolean = _hiddenAccountIds.value.contains(accountId)

    fun setProfilePhotoUri(uri: String?) {
        _profilePhotoUri.value = uri
        prefs.edit().putString("profile_photo_uri", uri).apply()
    }

    fun setLastSmsSyncTimestamp(timestamp: Long) {
        _lastSmsSyncTimestamp.value = timestamp
        prefs.edit().putLong("last_sms_sync_timestamp", timestamp).apply()
    }

    fun setBiometric(enabled: Boolean) {
        _biometricEnabled.value = enabled
        prefs.edit().putBoolean("biometric", enabled).apply()
    }

    fun setAutoHideBalances(enabled: Boolean) {
        _autoHideBalances.value = enabled
        prefs.edit().putBoolean("auto_hide", enabled).apply()
    }
    
    fun setPrivacyMode(enabled: Boolean) {
        _privacyMode.value = enabled
        prefs.edit().putBoolean("privacy_mode", enabled).apply()
    }

    fun setNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications", enabled).apply()
        applicationContext?.let {
            if (enabled) {
                SummaryScheduler.rescheduleAll(it, _summaryFrequencies.value)
            } else {
                SummaryScheduler.rescheduleAll(it, emptySet())
            }
        }
    }

    fun setSmsAlerts(enabled: Boolean) {
        _smsAlerts.value = enabled
        prefs.edit().putBoolean("sms_alerts", enabled).apply()
    }

    fun setDateFormat(value: String) {
        _dateFormat.value = value
        prefs.edit().putString("date_format", value).apply()
    }

    fun setCalendarSystem(value: String) {
        _calendarSystem.value = value
        prefs.edit().putString("calendar_system", value).apply()
    }

    fun setFinancialOverviewPeriod(value: String) {
        _financialOverviewPeriod.value = value
        prefs.edit().putString("financial_overview_period", value).apply()
    }

    fun setTheme(value: String) {
        _theme.value = value
        prefs.edit().putString("theme", value).apply()
    }

    fun setUserName(value: String) {
        _userName.value = value
        prefs.edit().putString("user_name", value).apply()
    }

    fun setUserEmail(value: String) {
        _userEmail.value = value
        prefs.edit().putString("user_email", value).apply()
    }

    fun setSummaryFrequencies(values: Set<String>) {
        _summaryFrequencies.value = values
        // SharedPreferences requires a fresh Set instance for putStringSet — never
        // pass through a Set that might still be mutated/reused elsewhere.
        prefs.edit().putStringSet("summary_frequencies", HashSet(values)).apply()
        applicationContext?.let {
            if (_notificationsEnabled.value) {
                SummaryScheduler.rescheduleAll(it, values)
            }
        }
    }

    /** Grants an ad-free window of [durationMillis] from now, extending any grant already in progress. */
    fun grantAdFree(durationMillis: Long) {
        val until = (System.currentTimeMillis() + durationMillis).coerceAtLeast(_adFreeUntilMillis.value)
        _adFreeUntilMillis.value = until
        prefs.edit().putLong("ad_free_until", until).apply()
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        _hasSeenOnboarding.value = seen
        prefs.edit().putBoolean("has_seen_onboarding", seen).apply()
    }

    fun setNotificationCaptureEnabled(enabled: Boolean) {
        _notificationCaptureEnabled.value = enabled
        prefs.edit().putBoolean("notification_capture_enabled", enabled).apply()
    }

    fun setMonitoredApps(apps: Map<String, String>) {
        _monitoredApps.value = apps
        // Plain SharedPreferences has no native Map type — encode as "packageName::institutionId"
        // strings. SharedPreferences also requires a fresh Set instance for putStringSet.
        val encoded = apps.map { (packageName, institutionId) -> "$packageName::$institutionId" }.toHashSet()
        prefs.edit().putStringSet("monitored_apps", encoded).apply()
    }

    /**
     * Merges any newly detected bank/wallet app into [monitoredApps] automatically — the
     * mechanism that makes notification capture "just work" without the user having to check
     * each app individually. Only ever adds; never removes an app the user has since
     * unchecked, since an absent entry can mean either "never detected yet" or "user turned it
     * off" and this can't tell those apart. Called from NotificationCaptureListenerService on
     * every listener (re)connect (so capture starts automatically the moment permission is
     * granted, with no screen visit required) and from NotificationCaptureScreen on open (so
     * a newly installed bank app shows up pre-enabled the next time the user looks).
     */
    fun autoEnableDetectedApps(matched: List<MonitorableApp>) {
        if (matched.isEmpty()) return
        val current = _monitoredApps.value
        val missing = matched.filter { it.packageName !in current }
        if (missing.isEmpty()) return
        val updated = current.toMutableMap()
        missing.forEach { updated[it.packageName] = it.institution.id }
        setMonitoredApps(updated)
    }

    fun setAppPin(pin: String) {
        if (!::prefs.isInitialized) return
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        safeEdit {
            putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            putString("pin_hash", hashPin(pin, salt))
        }
        _hasPinSet.value = true
    }

    /** Returns true if [pin] matches the stored PIN, or if no PIN has been set yet. */
    fun verifyPin(pin: String): Boolean {
        if (!_hasPinSet.value) return true
        if (!::prefs.isInitialized) return false
        val saltStr = prefs.getString("pin_salt", null) ?: return false
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        return hashPin(pin, salt) == storedHash
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 12000, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun clearAll(context: Context) {
        _userName.value = "Account Holder"
        _userEmail.value = ""
        _profilePhotoUri.value = null
        _lastSmsSyncTimestamp.value = 0L
        _theme.value = "Light"                     // BUG FIX: was "Dark", default is "Light"
        _calendarSystem.value = "Gregorian"        // BUG FIX: was missing — prefs cleared but StateFlow not reset
        _dateFormat.value = "MM/DD/YYYY"           // BUG FIX: was missing — prefs cleared but StateFlow not reset
        _financialOverviewPeriod.value = "Monthly"
        _notificationsEnabled.value = true
        _smsAlerts.value = true
        _notificationCaptureEnabled.value = true
        _summaryFrequencies.value = setOf("Daily")
        _biometricEnabled.value = true
        _autoHideBalances.value = false
        _privacyMode.value = false
        _hasPinSet.value = false
        _hasSeenOnboarding.value = false
        _monitoredApps.value = emptyMap()
        _adFreeUntilMillis.value = 0L              // BUG FIX: was missing — ad-free grant must clear on reset

        prefs.edit().clear().apply()
    }
}
