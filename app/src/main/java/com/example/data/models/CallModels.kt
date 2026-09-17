package com.example.data.models

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED,
    UNKNOWN
}

data class ContactItem(
    val id: Long,
    val lookupKey: String = "",
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null
) {
    val initial: String
        get() {
            val trimmed = displayName.trim()
            return if (trimmed.isNotEmpty()) {
                trimmed.first().uppercase()
            } else {
                "?"
            }
        }
}

data class CallLogEntry(
    val id: Long,
    val number: String,
    val cachedName: String? = null,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Long,
    val photoUri: String? = null
) {
    val displayName: String
        get() = if (!cachedName.isNullOrBlank()) cachedName else if (number.isNotBlank()) number else "Unknown Caller"

    val initial: String
        get() {
            val name = displayName.trim()
            return if (name.isNotEmpty() && name.first().isLetter()) {
                name.first().uppercase()
            } else {
                "?"
            }
        }
}

data class ActiveCallState(
    val number: String,
    val contactName: String? = null,
    val photoUri: String? = null,
    val telecomState: Int, // Call.STATE_RINGING, Call.STATE_ACTIVE, etc.
    val isIncoming: Boolean = false,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
) {
    val displayTitle: String
        get() = if (!contactName.isNullOrBlank()) contactName else if (number.isNotBlank()) number else "Unknown Number"

    val initial: String
        get() {
            val name = displayTitle.trim()
            return if (name.isNotEmpty() && name.first().isLetter()) {
                name.first().uppercase()
            } else {
                "?"
            }
        }
}
