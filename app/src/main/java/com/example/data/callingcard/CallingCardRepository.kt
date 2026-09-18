package com.example.data.callingcard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository responsible for local persistence and separation of Calling Card data.
 *
 * Requirements:
 * 1. Google account data and Guest data must remain strictly separated.
 * 2. Stored locally via SharedPreferences using separate keys per ownerId.
 * 3. Guest data remains strictly local and is never merged with Google account data.
 * 4. Reactive state observation via StateFlow for immediate UI updates and live previews.
 */
class CallingCardRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CallingCardRepo"
        private const val PREFS_NAME = "r_dialer_calling_card_prefs"
        const val GUEST_OWNER_ID = "guest_session"

        @Volatile
        private var INSTANCE: CallingCardRepository? = null

        fun getInstance(context: Context): CallingCardRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallingCardRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Current active calling card loaded for current session
    private val _currentCard = MutableStateFlow<CallingCardData?>(null)
    val currentCard: StateFlow<CallingCardData?> = _currentCard.asStateFlow()

    /**
     * Loads the Calling Card for the given ownerId (Google UID or GUEST_OWNER_ID).
     */
    fun loadCardForOwner(
        ownerId: String,
        defaultName: String = "R Dialer User",
        googlePhotoUrl: String? = null
    ): CallingCardData {
        val prefix = "card_${ownerId}_"
        val exists = prefs.contains("${prefix}ownerId")

        val card = if (exists) {
            CallingCardData(
                ownerId = prefs.getString("${prefix}ownerId", ownerId) ?: ownerId,
                displayName = prefs.getString("${prefix}displayName", defaultName) ?: defaultName,
                styleType = CallingCardStyleType.fromString(prefs.getString("${prefix}styleType", null)),
                typography = CallingCardTypography.fromString(prefs.getString("${prefix}typography", null)),
                nameColorLong = prefs.getLong("${prefix}nameColorLong", 0xFF0F172A),
                fontSizeSp = prefs.getFloat("${prefix}fontSizeSp", 24f),
                background = CallingCardBackground.fromString(prefs.getString("${prefix}background", null)),
                customBackgroundImageUri = prefs.getString("${prefix}customBgUri", null),
                profilePictureUri = prefs.getString("${prefix}profilePictureUri", null),
                originalGooglePhotoUrl = prefs.getString("${prefix}originalGooglePhotoUrl", googlePhotoUrl),
                isProfilePictureRemoved = prefs.getBoolean("${prefix}isProfilePictureRemoved", false),
                visualEffectsEnabled = prefs.getBoolean("${prefix}visualEffectsEnabled", true),
                isSetupCompleted = prefs.getBoolean("${prefix}isSetupCompleted", false),
                updatedAt = prefs.getLong("${prefix}updatedAt", System.currentTimeMillis())
            )
        } else {
            // New Calling Card defaults based on session type
            CallingCardData(
                ownerId = ownerId,
                displayName = defaultName.ifBlank { "R Dialer User" },
                styleType = CallingCardStyleType.PROFILE_CARD,
                typography = CallingCardTypography.MODERN,
                nameColorLong = 0xFF0F172A,
                fontSizeSp = 24f,
                background = CallingCardBackground.SOFT_GLASS,
                customBackgroundImageUri = null,
                profilePictureUri = null,
                originalGooglePhotoUrl = googlePhotoUrl,
                isProfilePictureRemoved = false,
                visualEffectsEnabled = true,
                isSetupCompleted = false,
                updatedAt = System.currentTimeMillis()
            )
        }

        _currentCard.value = card
        return card
    }

    /**
     * Persists the Calling Card locally for its ownerId.
     */
    suspend fun saveCard(card: CallingCardData): Unit = withContext(Dispatchers.IO) {
        val prefix = "card_${card.ownerId}_"
        prefs.edit()
            .putString("${prefix}ownerId", card.ownerId)
            .putString("${prefix}displayName", card.displayName)
            .putString("${prefix}styleType", card.styleType.name)
            .putString("${prefix}typography", card.typography.name)
            .putLong("${prefix}nameColorLong", card.nameColorLong)
            .putFloat("${prefix}fontSizeSp", card.fontSizeSp)
            .putString("${prefix}background", card.background.name)
            .putString("${prefix}customBgUri", card.customBackgroundImageUri)
            .putString("${prefix}profilePictureUri", card.profilePictureUri)
            .putString("${prefix}originalGooglePhotoUrl", card.originalGooglePhotoUrl)
            .putBoolean("${prefix}isProfilePictureRemoved", card.isProfilePictureRemoved)
            .putBoolean("${prefix}visualEffectsEnabled", card.visualEffectsEnabled)
            .putBoolean("${prefix}isSetupCompleted", card.isSetupCompleted)
            .putLong("${prefix}updatedAt", System.currentTimeMillis())
            .apply()

        _currentCard.value = card.copy(updatedAt = System.currentTimeMillis())
        Log.i(TAG, "Saved CallingCard for owner: ${card.ownerId}")
    }

    /**
     * Checks if Account Setup has been completed for the owner.
     */
    fun isSetupCompletedForOwner(ownerId: String): Boolean {
        val prefix = "card_${ownerId}_"
        return prefs.getBoolean("${prefix}isSetupCompleted", false)
    }

    /**
     * Marks Account Setup as completed for the owner.
     */
    suspend fun markSetupCompleted(ownerId: String): Unit = withContext(Dispatchers.IO) {
        val prefix = "card_${ownerId}_"
        prefs.edit().putBoolean("${prefix}isSetupCompleted", true).apply()
        _currentCard.value?.let {
            if (it.ownerId == ownerId) {
                _currentCard.value = it.copy(isSetupCompleted = true)
            }
        }
    }

    /**
     * Clears in-memory card state on sign out or guest switch.
     */
    fun clearCurrentSession() {
        _currentCard.value = null
    }
}
