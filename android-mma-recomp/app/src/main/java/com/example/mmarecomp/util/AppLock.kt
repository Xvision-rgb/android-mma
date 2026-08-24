package com.example.mmarecomp.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

/** Vrai si l'appareil a au moins une biométrie ou un verrouillage d'écran
 *  configuré — sinon activer le verrouillage applicatif serait un mur sans
 *  porte : l'appelant doit ignorer la préférence dans ce cas plutôt que de
 *  bloquer l'accès à l'app. */
fun canUseAppLock(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/** Ouvre l'invite biométrique/PIN système. `onResult(true)` si authentifié,
 *  `onResult(false)` en cas d'échec ou d'annulation — jamais de blocage
 *  silencieux, c'est à l'appelant de proposer un nouvel essai. */
fun requestAppUnlock(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Déverrouiller l'app")
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}
