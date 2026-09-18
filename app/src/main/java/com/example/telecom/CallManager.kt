package com.example.telecom

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.data.ContactRepository
import com.example.data.models.ActiveCallState
import com.example.data.models.ConferenceParticipant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object CallManager {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var activeCall: Call? = null
    private var secondaryCall: Call? = null
    private var inCallService: InCallService? = null
    private var appContext: Context? = null

    private val _callState = MutableStateFlow<ActiveCallState?>(null)
    val callState: StateFlow<ActiveCallState?> = _callState.asStateFlow()

    init {
        // Synchronize recorder state with active call state
        scope.launch {
            CallRecorderManager.isRecording.collect { isRec ->
                _callState.value = _callState.value?.copy(isRecording = isRec)
            }
        }
        scope.launch {
            CallRecorderManager.recordingDurationSeconds.collect { recSecs ->
                _callState.value = _callState.value?.copy(recordingDurationSeconds = recSecs)
            }
        }
        scope.launch {
            CallRecorderManager.errorMessage.collect { err ->
                _callState.value = _callState.value?.copy(recordingErrorMessage = err)
            }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateCallState(call)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateCallState(call)
        }

        override fun onChildrenChanged(call: Call, children: MutableList<Call>?) {
            updateConferenceParticipants(call)
        }

        override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>?) {
            updateMergeCapability()
        }
    }

    fun onCallAdded(call: Call, service: InCallService, context: Context) {
        appContext = context.applicationContext
        inCallService = service
        call.registerCallback(callCallback)

        if (activeCall == null) {
            activeCall = call
            val rawNumber = extractPhoneNumber(call)
            val isIncoming = call.state == Call.STATE_RINGING
            val simLabel = SubscriptionHelper.resolveIncomingSimLabel(context, call.details)

            _callState.value = ActiveCallState(
                number = rawNumber,
                contactName = null,
                photoUri = null,
                telecomState = call.state,
                isIncoming = isIncoming,
                durationSeconds = 0L,
                isMuted = service.callAudioState?.isMuted == true,
                isSpeakerOn = service.callAudioState?.route == CallAudioState.ROUTE_SPEAKER,
                simLabel = simLabel,
                isRecording = CallRecorderManager.isRecording.value,
                recordingDurationSeconds = CallRecorderManager.recordingDurationSeconds.value
            )

            scope.launch {
                val contact = ContactRepository(context).findContactByNumber(rawNumber)
                val current = _callState.value
                if (current != null && activeCall == call) {
                    val updated = current.copy(
                        contactName = contact?.displayName,
                        photoUri = contact?.photoUri,
                        simLabel = simLabel
                    )
                    _callState.value = updated
                    if (call.state == Call.STATE_RINGING) {
                        CallNotificationManager.showIncomingCallNotification(context, updated)
                    }
                }
            }
        } else if (secondaryCall == null && activeCall != call) {
            // Second call added
            secondaryCall = call
            val secNumber = extractPhoneNumber(call)
            scope.launch {
                val secContact = ContactRepository(context).findContactByNumber(secNumber)
                val secState = ActiveCallState(
                    number = secNumber,
                    contactName = secContact?.displayName,
                    photoUri = secContact?.photoUri,
                    telecomState = call.state,
                    isIncoming = call.state == Call.STATE_RINGING,
                    durationSeconds = 0L
                )
                val current = _callState.value
                if (current != null) {
                    _callState.value = current.copy(
                        secondCall = secState,
                        canMergeCalls = canMergeWithCurrent(call),
                        canSwapCalls = true
                    )
                }
            }
        }
    }

    fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        if (activeCall == call) {
            if (secondaryCall != null) {
                // Promote secondary call to primary
                activeCall = secondaryCall
                secondaryCall = null
                val current = _callState.value
                val sec = current?.secondCall
                if (sec != null) {
                    _callState.value = sec.copy(
                        secondCall = null,
                        canMergeCalls = false,
                        canSwapCalls = false
                    )
                }
            } else {
                appContext?.let { CallRecorderManager.stopRecording(it) }
                activeCall = null
                stopTimer()
                _callState.value = null
            }
        } else if (secondaryCall == call) {
            secondaryCall = null
            val current = _callState.value
            if (current != null) {
                _callState.value = current.copy(
                    secondCall = null,
                    canMergeCalls = false,
                    canSwapCalls = false
                )
            }
        }
    }

    fun onAudioStateChanged(audioState: CallAudioState?) {
        val current = _callState.value ?: return
        if (audioState != null) {
            _callState.value = current.copy(
                isMuted = audioState.isMuted,
                isSpeakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER
            )
        }
    }

    private fun updateCallState(call: Call) {
        val currentState = _callState.value ?: return
        val isConference = call.details.hasProperty(Call.Details.PROPERTY_CONFERENCE)

        if (call.state == Call.STATE_ACTIVE && timerJob == null) {
            startTimer()
        } else if (call.state == Call.STATE_DISCONNECTED && secondaryCall == null) {
            stopTimer()
        }

        if (isConference) {
            updateConferenceParticipants(call)
            return
        }

        if (call == activeCall) {
            _callState.value = currentState.copy(
                telecomState = call.state,
                isMuted = inCallService?.callAudioState?.isMuted == true,
                isSpeakerOn = inCallService?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
            )
        } else if (call == secondaryCall) {
            val sec = currentState.secondCall
            if (sec != null) {
                _callState.value = currentState.copy(
                    secondCall = sec.copy(telecomState = call.state),
                    canMergeCalls = canMergeWithCurrent(call)
                )
            }
        }
    }

    private fun updateConferenceParticipants(call: Call) {
        val children = call.children ?: emptyList()
        val current = _callState.value ?: return
        val context = appContext

        if (children.isNotEmpty()) {
            scope.launch {
                val participants = children.mapIndexed { index, childCall ->
                    val num = extractPhoneNumber(childCall)
                    val contact = if (context != null) ContactRepository(context).findContactByNumber(num) else null
                    ConferenceParticipant(
                        id = "child_$index",
                        displayName = contact?.displayName ?: if (num.isNotBlank()) num else "Participant ${index + 1}",
                        phoneNumber = num,
                        photoUri = contact?.photoUri,
                        isHeld = childCall.state == Call.STATE_HOLDING,
                        durationSeconds = current.durationSeconds
                    )
                }
                _callState.value = current.copy(
                    isConference = true,
                    conferenceParticipants = participants,
                    secondCall = null,
                    canMergeCalls = false,
                    canSwapCalls = false
                )
            }
        }
    }

    private fun updateMergeCapability() {
        val c1 = activeCall
        val c2 = secondaryCall
        val current = _callState.value ?: return
        if (c1 != null && c2 != null) {
            _callState.value = current.copy(
                canMergeCalls = canMergeWithCurrent(c2)
            )
        }
    }

    private fun canMergeWithCurrent(call: Call): Boolean {
        val active = activeCall ?: return false
        return active.conferenceableCalls.contains(call) ||
                call.conferenceableCalls.contains(active) ||
                active.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) ||
                call.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0L
            while (isActive) {
                delay(1000)
                seconds++
                val current = _callState.value
                if (current != null && current.telecomState == Call.STATE_ACTIVE) {
                    _callState.value = current.copy(durationSeconds = seconds)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun initiateAddCall(context: Context, number: String, contactName: String? = null) {
        val current = _callState.value ?: return
        if (activeCall != null) {
            // Real Telecom mode
            try {
                activeCall?.hold()
            } catch (e: Exception) {
                // Ignore hold failure
            }
            TelecomHelper.placeCall(context, number)
        } else {
            // Demo / Emulator mode:
            val second = ActiveCallState(
                number = number,
                contactName = contactName,
                telecomState = Call.STATE_ACTIVE,
                isIncoming = false,
                durationSeconds = 0L
            )
            _callState.value = current.copy(
                telecomState = Call.STATE_HOLDING,
                secondCall = second,
                canMergeCalls = true,
                canSwapCalls = true
            )
        }
    }

    fun mergeCalls() {
        val current = _callState.value
        if (current != null) {
            _callState.value = current.copy(mergeStatusMessage = "Merging calls into conference...")
        }
        val c1 = activeCall
        val c2 = secondaryCall
        if (c1 != null && c2 != null) {
            try {
                if (c1.conferenceableCalls.contains(c2)) {
                    c1.conference(c2)
                    _callState.value = _callState.value?.copy(conferenceErrorMessage = null, mergeStatusMessage = "Calls successfully merged into conference!")
                } else if (c2.conferenceableCalls.contains(c1)) {
                    c2.conference(c1)
                    _callState.value = _callState.value?.copy(conferenceErrorMessage = null, mergeStatusMessage = "Calls successfully merged into conference!")
                } else if (c1.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
                    c1.mergeConference()
                    _callState.value = _callState.value?.copy(conferenceErrorMessage = null, mergeStatusMessage = "Calls successfully merged into conference!")
                } else {
                    // Try direct conference
                    try {
                        c1.conference(c2)
                        _callState.value = _callState.value?.copy(conferenceErrorMessage = null, mergeStatusMessage = "Calls successfully merged into conference!")
                    } catch (e: Exception) {
                        _callState.value = _callState.value?.copy(
                            conferenceErrorMessage = "Conference calling isn't supported by this device or carrier.",
                            mergeStatusMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _callState.value = _callState.value?.copy(
                    conferenceErrorMessage = "Conference calling isn't supported by this device or carrier.",
                    mergeStatusMessage = null
                )
            }
        } else if (_callState.value?.secondCall != null) {
            // Demo / Emulator mode simulation:
            val curr = _callState.value ?: return
            val sec = curr.secondCall ?: return
            val p1 = ConferenceParticipant(
                id = "part_1",
                displayName = curr.contactName ?: curr.number,
                phoneNumber = curr.number,
                photoUri = curr.photoUri,
                durationSeconds = curr.durationSeconds
            )
            val p2 = ConferenceParticipant(
                id = "part_2",
                displayName = sec.contactName ?: sec.number,
                phoneNumber = sec.number,
                photoUri = sec.photoUri,
                durationSeconds = sec.durationSeconds
            )
            _callState.value = curr.copy(
                isConference = true,
                conferenceParticipants = listOf(p1, p2),
                secondCall = null,
                canMergeCalls = false,
                canSwapCalls = false,
                telecomState = Call.STATE_ACTIVE,
                conferenceErrorMessage = null,
                mergeStatusMessage = "Calls successfully merged into conference!"
            )
        } else {
            _callState.value = _callState.value?.copy(
                conferenceErrorMessage = "Conference calling isn't supported by this device or carrier.",
                mergeStatusMessage = null
            )
        }
    }

    fun dismissMergeStatus() {
        _callState.value = _callState.value?.copy(mergeStatusMessage = null)
    }

    fun swapCalls() {
        val c1 = activeCall
        val c2 = secondaryCall
        if (c1 != null && c2 != null) {
            try {
                if (c1.state == Call.STATE_ACTIVE) {
                    c1.hold()
                    c2.unhold()
                } else if (c2.state == Call.STATE_ACTIVE) {
                    c2.hold()
                    c1.unhold()
                } else {
                    c1.unhold()
                }
            } catch (e: Exception) {
                // Swap exception
            }
        } else if (_callState.value?.secondCall != null) {
            val current = _callState.value ?: return
            val sec = current.secondCall ?: return
            _callState.value = sec.copy(
                telecomState = Call.STATE_ACTIVE,
                secondCall = current.copy(
                    telecomState = Call.STATE_HOLDING,
                    secondCall = null
                ),
                canMergeCalls = true,
                canSwapCalls = true
            )
        }
    }

    fun hold() {
        val c = activeCall
        if (c != null) {
            try {
                c.hold()
            } catch (e: Exception) {
                _callState.value = _callState.value?.copy(
                    conferenceErrorMessage = "Unable to place call on hold: ${e.message}"
                )
            }
        } else {
            val current = _callState.value ?: return
            _callState.value = current.copy(telecomState = Call.STATE_HOLDING)
        }
    }

    fun unhold() {
        val c = activeCall
        if (c != null) {
            try {
                c.unhold()
            } catch (e: Exception) {
                _callState.value = _callState.value?.copy(
                    conferenceErrorMessage = "Unable to resume call: ${e.message}"
                )
            }
        } else {
            val current = _callState.value ?: return
            _callState.value = current.copy(telecomState = Call.STATE_ACTIVE)
            if (timerJob == null) {
                startTimer()
            }
        }
    }

    fun toggleHold() {
        val current = _callState.value ?: return
        if (current.telecomState == Call.STATE_HOLDING) {
            unhold()
        } else {
            hold()
        }
    }

    fun dismissConferenceError() {
        _callState.value = _callState.value?.copy(conferenceErrorMessage = null)
    }

    fun disconnectParticipant(participantId: String) {
        val current = _callState.value ?: return
        if (current.isConference) {
            val remaining = current.conferenceParticipants.filter { it.id != participantId }
            if (remaining.size <= 1) {
                // If only 1 remains, exit conference back to single call
                val last = remaining.firstOrNull()
                _callState.value = current.copy(
                    isConference = false,
                    conferenceParticipants = emptyList(),
                    number = last?.phoneNumber ?: current.number,
                    contactName = last?.displayName ?: current.contactName,
                    photoUri = last?.photoUri ?: current.photoUri
                )
            } else {
                _callState.value = current.copy(conferenceParticipants = remaining)
            }
        }
    }

    // Video Calling Operations
    fun setVideoCallActive(active: Boolean, enableCamera: Boolean = true) {
        val current = _callState.value ?: return
        _callState.value = current.copy(
            isVideoCall = active,
            isLocalCameraEnabled = enableCamera,
            isRemoteVideoActive = false,
            videoStatusMessage = null
        )
    }

    fun toggleLocalCamera() {
        val current = _callState.value ?: return
        _callState.value = current.copy(isLocalCameraEnabled = !current.isLocalCameraEnabled)
    }

    fun setVideoStatusMessage(message: String?) {
        val current = _callState.value ?: return
        _callState.value = current.copy(videoStatusMessage = message)
    }

    fun simulateDemoIncomingCall(
        context: Context,
        number: String = "+91 98765 43210",
        name: String = "Runa Koroki",
        simLabel: String? = "SIM 1 • Jio (Demo)"
    ) {
        if (activeCall != null) return
        appContext = context.applicationContext
        val state = ActiveCallState(
            number = number,
            contactName = name,
            photoUri = null,
            telecomState = Call.STATE_RINGING,
            isIncoming = true,
            durationSeconds = 0L,
            isMuted = false,
            isSpeakerOn = false,
            simLabel = simLabel
        )
        _callState.value = state
        CallNotificationManager.showIncomingCallNotification(context, state)
    }

    fun answer() {
        if (activeCall != null) {
            activeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } else {
            val current = _callState.value
            if (current != null) {
                _callState.value = current.copy(telecomState = Call.STATE_ACTIVE)
                startTimer()
            }
        }
    }

    fun disconnect() {
        appContext?.let { CallRecorderManager.stopRecording(it) }
        if (activeCall != null) {
            try {
                if (activeCall?.state == Call.STATE_RINGING) {
                    activeCall?.reject(false, null)
                } else {
                    activeCall?.disconnect()
                }
            } catch (e: Exception) {
                activeCall?.disconnect()
            }
            if (secondaryCall != null) {
                try {
                    secondaryCall?.disconnect()
                } catch (e: Exception) {}
            }
        } else {
            val current = _callState.value
            if (current != null) {
                _callState.value = current.copy(telecomState = Call.STATE_DISCONNECTED)
                stopTimer()
                scope.launch {
                    delay(800)
                    _callState.value = null
                }
            }
        }
    }

    fun startCallRecording(context: Context): Boolean {
        val current = _callState.value ?: return false
        return CallRecorderManager.startRecording(
            context = context,
            contactName = current.contactName,
            phoneNumber = current.number,
            isIncoming = current.isIncoming
        )
    }

    fun stopCallRecording(context: Context) {
        CallRecorderManager.stopRecording(context)
    }

    fun toggleMute() {
        val service = inCallService
        if (service != null) {
            val currentMuted = service.callAudioState?.isMuted == true
            service.setMuted(!currentMuted)
        } else {
            val current = _callState.value ?: return
            _callState.value = current.copy(isMuted = !current.isMuted)
        }
    }

    fun toggleSpeaker() {
        val service = inCallService
        if (service != null) {
            val currentRoute = service.callAudioState?.route
            val newRoute = if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
                CallAudioState.ROUTE_EARPIECE
            } else {
                CallAudioState.ROUTE_SPEAKER
            }
            service.setAudioRoute(newRoute)
        } else {
            val current = _callState.value ?: return
            _callState.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
        }
    }

    fun sendDtmfTone(digit: Char) {
        activeCall?.playDtmfTone(digit)
        scope.launch {
            delay(200)
            activeCall?.stopDtmfTone()
        }
    }

    private fun extractPhoneNumber(call: Call): String {
        val handle = call.details.handle
        return if (handle != null) {
            handle.schemeSpecificPart ?: ""
        } else {
            call.details.gatewayInfo?.originalAddress?.schemeSpecificPart ?: ""
        }
    }
}

