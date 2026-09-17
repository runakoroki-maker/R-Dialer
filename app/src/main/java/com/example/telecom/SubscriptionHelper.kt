package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.models.SimAccountInfo

object SubscriptionHelper {
    private const val PREFS_NAME = "rdialer_sim_prefs"
    private const val KEY_PREFERRED_SIM = "preferred_sim_mode"

    const val PREF_ALWAYS_ASK = "ALWAYS_ASK"
    const val PREF_SIM_1 = "SIM_1"
    const val PREF_SIM_2 = "SIM_2"
    const val PREF_SYSTEM_DEFAULT = "SYSTEM_DEFAULT"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPreferredSimMode(context: Context): String {
        return getPrefs(context).getString(KEY_PREFERRED_SIM, PREF_ALWAYS_ASK) ?: PREF_ALWAYS_ASK
    }

    fun setPreferredSimMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_PREFERRED_SIM, mode).apply()
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns list of currently active SIM subscriptions on the device.
     */
    fun getActiveSubscriptions(context: Context): List<SimAccountInfo> {
        if (!hasPhoneStatePermission(context)) return emptyList()

        return try {
            val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.getSystemService(SubscriptionManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SubscriptionManager.from(context)
            }

            val list: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
            if (list.isNullOrEmpty()) {
                emptyList()
            } else {
                list.map { info ->
                    SimAccountInfo(
                        subscriptionId = info.subscriptionId,
                        slotIndex = info.simSlotIndex,
                        displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                        carrierName = info.carrierName?.toString() ?: ""
                    )
                }.sortedBy { it.slotIndex }
            }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isDualSimActive(context: Context): Boolean {
        return getActiveSubscriptions(context).size >= 2
    }

    /**
     * Resolves the PhoneAccountHandle corresponding to a chosen subscription ID.
     */
    fun getPhoneAccountHandleForSubscription(context: Context, subscriptionId: Int): PhoneAccountHandle? {
        if (!hasPhoneStatePermission(context)) return null
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return null

        return try {
            val accounts = telecomManager.getCallCapablePhoneAccounts() ?: emptyList()
            accounts.firstOrNull { handle ->
                handle.id.contains(subscriptionId.toString())
            } ?: accounts.firstOrNull()
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the SIM label for an incoming call from its details.
     */
    fun resolveIncomingSimLabel(context: Context, details: Call.Details?): String? {
        if (details == null) return null
        val accountHandle = details.accountHandle ?: return null

        val activeSims = getActiveSubscriptions(context)
        if (activeSims.isEmpty()) return null

        // Try to match accountHandle ID with subscription ID or slot
        val matchedSim = activeSims.firstOrNull { sim ->
            accountHandle.id.contains(sim.subscriptionId.toString()) ||
                    accountHandle.id.contains(sim.slotIndex.toString())
        }

        return if (matchedSim != null) {
            matchedSim.label
        } else if (activeSims.size > 1) {
            // If dual SIM, display the account label or generic fallback
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            try {
                val account = telecomManager?.getPhoneAccount(accountHandle)
                account?.label?.toString()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
}
