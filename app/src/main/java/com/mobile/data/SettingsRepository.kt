package com.mobile.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    
    private val _emailUpdates = MutableStateFlow(false)
    val emailUpdates: StateFlow<Boolean> = _emailUpdates.asStateFlow()
    
    private val _smsAlerts = MutableStateFlow(true)
    val smsAlerts: StateFlow<Boolean> = _smsAlerts.asStateFlow()

    private val _currency = MutableStateFlow("ETB")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _language = MutableStateFlow("English")
    val language: StateFlow<String> = _language.asStateFlow()
    
    private val _dateFormat = MutableStateFlow("MM/DD/YYYY")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _theme = MutableStateFlow("Dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _appPin = MutableStateFlow("1234") // Default PIN
    val appPin: StateFlow<String> = _appPin.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _biometricEnabled.value = prefs.getBoolean("biometric", true)
        _autoHideBalances.value = prefs.getBoolean("auto_hide", false)
        _privacyMode.value = prefs.getBoolean("privacy_mode", false)
        _notificationsEnabled.value = prefs.getBoolean("notifications", true)
        _emailUpdates.value = prefs.getBoolean("email_updates", false)
        _smsAlerts.value = prefs.getBoolean("sms_alerts", true)
        _currency.value = prefs.getString("currency", "ETB") ?: "ETB"
        _language.value = prefs.getString("language", "English") ?: "English"
        _dateFormat.value = prefs.getString("date_format", "MM/DD/YYYY") ?: "MM/DD/YYYY"
        _theme.value = prefs.getString("theme", "Dark") ?: "Dark"
        _appPin.value = prefs.getString("app_pin", "1234") ?: "1234"
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
    }
    
    fun setEmailUpdates(enabled: Boolean) {
        _emailUpdates.value = enabled
        prefs.edit().putBoolean("email_updates", enabled).apply()
    }
    
    fun setSmsAlerts(enabled: Boolean) {
        _smsAlerts.value = enabled
        prefs.edit().putBoolean("sms_alerts", enabled).apply()
    }

    fun setCurrency(value: String) {
        _currency.value = value
        prefs.edit().putString("currency", value).apply()
    }

    fun setLanguage(value: String) {
        _language.value = value
        prefs.edit().putString("language", value).apply()
    }
    
    fun setDateFormat(value: String) {
        _dateFormat.value = value
        prefs.edit().putString("date_format", value).apply()
    }

    fun setTheme(value: String) {
        _theme.value = value
        prefs.edit().putString("theme", value).apply()
    }

    fun setAppPin(value: String) {
        _appPin.value = value
        prefs.edit().putString("app_pin", value).apply()
    }
}
