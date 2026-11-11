package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("balatec_security_prefs", Context.MODE_PRIVATE)

    var isLocked: Boolean = isPinLockEnabled()
        private set

    fun isPinLockEnabled(): Boolean {
        return prefs.getBoolean("pin_lock_enabled", false) && hasPin()
    }

    fun setPinLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pin_lock_enabled", enabled).apply()
        if (!enabled) {
            isLocked = false
        }
    }

    fun hasPin(): Boolean {
        return prefs.getString("pin_hash", null) != null
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val hash = hashPin(pin)
        prefs.edit()
            .putString("pin_hash", hash)
            .putBoolean("pin_lock_enabled", true)
            .apply()
        isLocked = false
        return true
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val inputHash = hashPin(inputPin)
        val valid = storedHash == inputHash
        if (valid) {
            isLocked = false
        }
        return valid
    }

    fun unlockByBiometric() {
        isLocked = false
    }

    fun lock() {
        if (isPinLockEnabled()) {
            isLocked = true
        }
    }

    fun removePin() {
        prefs.edit()
            .remove("pin_hash")
            .putBoolean("pin_lock_enabled", false)
            .putBoolean("biometric_enabled", false)
            .apply()
        isLocked = false
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("balatec_salt_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
