package com.example.data.callingcard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the local guest session for emulator and local testing.
 *
 * Requirements:
 * - Do not use Firebase Authentication for Guest.
 * - Do not create a fake Google account.
 * - Create a clearly separated local guest session.
 * - Ask user to choose a display name.
 * - Clearly display "Guest Mode" in the account area.
 * - Guest data remains local to device/emulator.
 * - Guest mode must never be presented as a real authenticated Google account.
 */
class GuestSessionManager private constructor(context: Context) {

    companion object {
        private const val TAG = "GuestSessionManager"
        private const val PREFS_NAME = "r_dialer_guest_session_prefs"
        private const val KEY_IS_GUEST_ACTIVE = "is_guest_active"
        private const val KEY_GUEST_DISPLAY_NAME = "guest_display_name"
        private const val KEY_GUEST_SETUP_COMPLETED = "guest_setup_completed"

        @Volatile
        private var INSTANCE: GuestSessionManager? = null

        fun getInstance(context: Context): GuestSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GuestSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isGuestActive = MutableStateFlow(prefs.getBoolean(KEY_IS_GUEST_ACTIVE, false))
    val isGuestActive: StateFlow<Boolean> = _isGuestActive.asStateFlow()

    private val _guestDisplayName = MutableStateFlow(prefs.getString(KEY_GUEST_DISPLAY_NAME, "Guest User") ?: "Guest User")
    val guestDisplayName: StateFlow<String> = _guestDisplayName.asStateFlow()

    private val _isGuestSetupCompleted = MutableStateFlow(prefs.getBoolean(KEY_GUEST_SETUP_COMPLETED, false))
    val isGuestSetupCompleted: StateFlow<Boolean> = _isGuestSetupCompleted.asStateFlow()

    /**
     * Starts a local guest session with the specified display name.
     */
    fun startGuestSession(displayName: String) {
        val cleanName = displayName.trim().ifBlank { "Guest User" }
        prefs.edit()
            .putBoolean(KEY_IS_GUEST_ACTIVE, true)
            .putString(KEY_GUEST_DISPLAY_NAME, cleanName)
            .putBoolean(KEY_GUEST_SETUP_COMPLETED, false)
            .apply()

        _isGuestActive.value = true
        _guestDisplayName.value = cleanName
        _isGuestSetupCompleted.value = false
        Log.i(TAG, "Guest session started with name: $cleanName")
    }

    /**
     * Updates the guest display name.
     */
    fun updateGuestDisplayName(newName: String) {
        val cleanName = newName.trim().ifBlank { "Guest User" }
        prefs.edit().putString(KEY_GUEST_DISPLAY_NAME, cleanName).apply()
        _guestDisplayName.value = cleanName
    }

    /**
     * Marks Account Setup as completed for Guest Mode.
     */
    fun setGuestSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_GUEST_SETUP_COMPLETED, completed).apply()
        _isGuestSetupCompleted.value = completed
    }

    /**
     * Exits/clears the guest session.
     */
    fun exitGuestSession() {
        prefs.edit()
            .putBoolean(KEY_IS_GUEST_ACTIVE, false)
            .apply()
        _isGuestActive.value = false
        Log.i(TAG, "Guest session exited")
    }
}
