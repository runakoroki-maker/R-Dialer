package com.example.telecom

import android.content.Context
import android.content.SharedPreferences

object CallRecordingPreferences {
    private const val PREFS_NAME = "rdialer_call_recording_prefs"
    private const val KEY_ENABLED = "record_specified_enabled"
    private const val KEY_CONTACT_NAME = "record_specified_name"
    private const val KEY_PHONE_NUMBER = "record_specified_number"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getContactName(context: Context): String? {
        return getPrefs(context).getString(KEY_CONTACT_NAME, null)
    }

    fun getPhoneNumber(context: Context): String? {
        return getPrefs(context).getString(KEY_PHONE_NUMBER, null)
    }

    fun setContact(context: Context, name: String?, number: String?) {
        getPrefs(context).edit()
            .putString(KEY_CONTACT_NAME, name)
            .putString(KEY_PHONE_NUMBER, number)
            .apply()
    }

    fun clearContact(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_CONTACT_NAME)
            .remove(KEY_PHONE_NUMBER)
            .apply()
    }
}
