package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole {
    ADMIN,
    SHARED_USER,
    SIGNED_OUT
}

data class AccessSession(
    val activeEmail: String? = null,
    val googleEmail: String? = null,
    val role: UserRole = UserRole.SIGNED_OUT
) {
    val isAdminTwtVisible: Boolean
        get() = role == UserRole.ADMIN &&
            activeEmail?.lowercase() in AccessControlManager.ADMIN_EMAILS &&
            (googleEmail == null || googleEmail.lowercase() in AccessControlManager.ADMIN_EMAILS)

    val isSignedIn: Boolean
        get() = role != UserRole.SIGNED_OUT && !activeEmail.isNullOrBlank()
}

sealed class PasscodeLoginResult {
    data class Success(val session: AccessSession) : PasscodeLoginResult()
    data class Error(val messageFr: String, val messageEn: String) : PasscodeLoginResult()
}

class AccessControlManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("balatec_access_prefs", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(loadInitialSession())
    val session: StateFlow<AccessSession> = _session.asStateFlow()

    private fun loadInitialSession(): AccessSession {
        val savedEmail = prefs.getString("active_email", null)?.trim()?.lowercase()
        val savedGoogle = prefs.getString("google_email", null)?.trim()?.lowercase()
        val role = resolveRole(savedEmail, savedGoogle)
        return AccessSession(
            activeEmail = savedEmail,
            googleEmail = savedGoogle,
            role = role
        )
    }

    private fun resolveRole(activeEmail: String?, googleEmail: String?): UserRole {
        val effective = (googleEmail ?: activeEmail)?.trim()?.lowercase() ?: return UserRole.SIGNED_OUT
        return when {
            effective in ADMIN_EMAILS -> if (activeEmail == SECONDARY_EMAIL) UserRole.SHARED_USER else UserRole.ADMIN
            effective == SECONDARY_EMAIL -> UserRole.SHARED_USER
            else -> UserRole.SIGNED_OUT
        }
    }

    fun isEmailAuthorized(email: String?): Boolean {
        val normalized = email?.trim()?.lowercase() ?: return false
        return normalized in AUTHORIZED_EMAILS
    }

    fun onGoogleAuthenticated(email: String?): Boolean {
        val normalized = email?.trim()?.lowercase()
        if (!isEmailAuthorized(normalized)) {
            clearSession()
            return false
        }
        prefs.edit()
            .putString("google_email", normalized)
            .putString("active_email", normalized)
            .apply()
        _session.value = AccessSession(
            activeEmail = normalized,
            googleEmail = normalized,
            role = if (normalized in ADMIN_EMAILS) UserRole.ADMIN else UserRole.SHARED_USER
        )
        return true
    }

    fun verifyAndSwitchWithPasscode(emailInput: String, passcodeInput: String): PasscodeLoginResult {
        val email = emailInput.trim().lowercase()
        if (!isEmailAuthorized(email)) {
            return PasscodeLoginResult.Error(
                messageFr = "Accès refusé : seuls les administrateurs et $SECONDARY_EMAIL sont autorisés.",
                messageEn = "Access denied: only administrators and $SECONDARY_EMAIL are authorized."
            )
        }

        val expectedPasscode = when {
            email in ADMIN_EMAILS -> BuildConfig.ADMIN_PASSCODE
            email == SECONDARY_EMAIL -> BuildConfig.SECONDARY_PASSCODE
            else -> ""
        }

        if (expectedPasscode.isBlank() || expectedPasscode == "CHANGE_ME") {
            return PasscodeLoginResult.Error(
                messageFr = "Code d'accès non configuré dans AI Studio Secrets. Utilisez Google Sign-In ou configurez ADMIN_PASSCODE / SECONDARY_PASSCODE.",
                messageEn = "Passcode not configured in AI Studio Secrets. Use Google Sign-In or set ADMIN_PASSCODE / SECONDARY_PASSCODE."
            )
        }

        // Strict case-sensitive verification
        if (passcodeInput != expectedPasscode) {
            return PasscodeLoginResult.Error(
                messageFr = "Code d'accès incorrect (sensible à la casse).",
                messageEn = "Incorrect passcode (case-sensitive)."
            )
        }

        val currentGoogle = _session.value.googleEmail
        val effectiveRole = if (email in ADMIN_EMAILS && (currentGoogle == null || currentGoogle.lowercase() in ADMIN_EMAILS)) {
            UserRole.ADMIN
        } else {
            UserRole.SHARED_USER
        }

        prefs.edit().putString("active_email", email).apply()
        val updated = AccessSession(
            activeEmail = email,
            googleEmail = currentGoogle,
            role = effectiveRole
        )
        _session.value = updated
        return PasscodeLoginResult.Success(updated)
    }

    fun clearSession() {
        prefs.edit()
            .remove("active_email")
            .remove("google_email")
            .apply()
        _session.value = AccessSession(
            activeEmail = null,
            googleEmail = null,
            role = UserRole.SIGNED_OUT
        )
    }

    companion object {
        const val PRIMARY_ADMIN_EMAIL = "410bala@gmail.com"
        const val SECONDARY_ADMIN_EMAIL = "bala@balatec.com"
        const val ADMIN_EMAIL = PRIMARY_ADMIN_EMAIL
        val ADMIN_EMAILS = setOf(PRIMARY_ADMIN_EMAIL, SECONDARY_ADMIN_EMAIL)
        const val SECONDARY_EMAIL = "traore200@gmail.com"
        val AUTHORIZED_EMAILS = setOf(PRIMARY_ADMIN_EMAIL, SECONDARY_ADMIN_EMAIL, SECONDARY_EMAIL)
    }
}
