package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.PocketlyApp
import com.example.ui.viewmodel.PocketlyViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: PocketlyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val biometricAvailable = isBiometricReady()

        setContent {
            PocketlyApp(
                viewModel = viewModel,
                biometricAvailable = biometricAvailable,
                onRequestBiometricUnlock = { onSuccess ->
                    promptBiometricUnlock(onSuccess)
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    private fun isBiometricReady(): Boolean {
        val manager = BiometricManager.from(this)
        val status = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun promptBiometricUnlock(onSuccess: () -> Unit) {
        if (!isBiometricReady()) return
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Pocketly")
            .setSubtitle("Use your fingerprint to access your local finances")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(promptInfo)
    }
}
