package com.example.data.models

import android.telecom.Call

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
    val photoUri: String? = null,
    val businessName: String? = null,
    val address: String? = null
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

data class ConferenceParticipant(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val isHeld: Boolean = false,
    val durationSeconds: Long = 0L
) {
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
    val telecomState: Int = Call.STATE_ACTIVE, // Call.STATE_RINGING, Call.STATE_ACTIVE, etc.
    val isIncoming: Boolean = false,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val simLabel: String? = null,
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val recordingErrorMessage: String? = null,
    // Conference & Multi-Call features
    val isConference: Boolean = false,
    val conferenceParticipants: List<ConferenceParticipant> = emptyList(),
    val secondCall: ActiveCallState? = null,
    val canMergeCalls: Boolean = false,
    val canSwapCalls: Boolean = false,
    val conferenceErrorMessage: String? = null,
    val mergeStatusMessage: String? = null,
    // Video Calling features
    val isVideoCall: Boolean = false,
    val isLocalCameraEnabled: Boolean = false,
    val isRemoteVideoActive: Boolean = false,
    val videoStatusMessage: String? = null
) {
    val displayTitle: String
        get() = if (isConference) {
            "Conference Call"
        } else if (!contactName.isNullOrBlank()) {
            contactName
        } else if (number.isNotBlank()) {
            number
        } else {
            "Unknown Number"
        }

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

data class SimAccountInfo(
    val subscriptionId: Int,
    val slotIndex: Int, // 0 for SIM 1, 1 for SIM 2
    val displayName: String,
    val carrierName: String
) {
    val simNumber: Int get() = slotIndex + 1
    val label: String
        get() = if (carrierName.isNotBlank() && carrierName != displayName) {
            "SIM $simNumber • $carrierName"
        } else if (displayName.isNotBlank()) {
            "SIM $simNumber • $displayName"
        } else {
            "SIM $simNumber"
        }
}

data class CallRecording(
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val contactName: String?,
    val phoneNumber: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val isIncoming: Boolean,
    val fileSize: Long = 0L
) {
    val displayName: String
        get() = if (!contactName.isNullOrBlank()) contactName else if (phoneNumber.isNotBlank()) phoneNumber else "Unknown Caller"

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
