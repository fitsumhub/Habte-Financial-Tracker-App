package com.mobile

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import com.mobile.data.SettingsRepository
import com.mobile.ui.RootLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsRepository.init(this)

        setContent {
            var isAuthenticated by remember { mutableStateOf(!SettingsRepository.biometricEnabled.value) }
            val privacyMode by SettingsRepository.privacyMode.collectAsState()

            // Update Privacy Mode (Screenshot Protection)
            LaunchedEffect(privacyMode) {
                if (privacyMode) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Trigger Biometric Auth if enabled
            LaunchedEffect(Unit) {
                if (SettingsRepository.biometricEnabled.value) {
                    val biometricManager = BiometricManager.from(this@MainActivity)
                    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    
                    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                        showBiometricPrompt { authenticated ->
                            isAuthenticated = authenticated
                        }
                    } else {
                        // Biometrics not available/enrolled, bypass for now to avoid lockout
                        isAuthenticated = true
                    }
                }
            }

            if (isAuthenticated) {
                RootLayout()
            } else {
                // Show a simple lock screen or black surface while authenticating
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF070912)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text("App Locked", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    // Allow retry or handle specific errors here
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Habte")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
