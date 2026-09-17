package com.example.telecom

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.data.ContactRepository
import com.example.data.models.ActiveCallState
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
    private var inCallService: InCallService? = null

    private val _callState = MutableStateFlow<ActiveCallState?>(null)
    val callState: StateFlow<ActiveCallState?> = _callState.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateCallState(call)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateCallState(call)
        }
    }

    fun onCallAdded(call: Call, service: InCallService, context: Context) {
        activeCall?.unregisterCallback(callCallback)
        activeCall = call
        inCallService = service
        call.registerCallback(callCallback)

        val rawNumber = extractPhoneNumber(call)
        val isIncoming = call.state == Call.STATE_RINGING

        // Immediately set state synchronously so caller UI and notification have data instantly
        _callState.value = ActiveCallState(
            number = rawNumber,
            contactName = null,
            photoUri = null,
            telecomState = call.state,
            isIncoming = isIncoming,
            durationSeconds = 0L,
            isMuted = service.callAudioState?.isMuted == true,
            isSpeakerOn = service.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
        )

        // Resolve contact info asynchronously
        scope.launch {
            val contact = ContactRepository(context).findContactByNumber(rawNumber)
            val current = _callState.value
            if (current != null && activeCall == call) {
                val updated = current.copy(
                    contactName = contact?.displayName,
                    photoUri = contact?.photoUri
                )
                _callState.value = updated
                if (call.state == Call.STATE_RINGING) {
                    CallNotificationManager.showIncomingCallNotification(context, updated)
                }
            }
        }
    }

    fun onCallRemoved(call: Call) {
        if (activeCall == call) {
            call.unregisterCallback(callCallback)
            activeCall = null
            stopTimer()
            _callState.value = null
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
        val currentState = _callState.value
        val isIncoming = call.state == Call.STATE_RINGING

        if (call.state == Call.STATE_ACTIVE && timerJob == null) {
            startTimer()
        } else if (call.state == Call.STATE_DISCONNECTED) {
            stopTimer()
        }

        if (currentState != null) {
            _callState.value = currentState.copy(
                telecomState = call.state,
                isIncoming = isIncoming,
                isMuted = inCallService?.callAudioState?.isMuted == true,
                isSpeakerOn = inCallService?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
            )
        } else {
            val num = extractPhoneNumber(call)
            _callState.value = ActiveCallState(
                number = num,
                contactName = null,
                photoUri = null,
                telecomState = call.state,
                isIncoming = isIncoming,
                durationSeconds = 0L,
                isMuted = inCallService?.callAudioState?.isMuted == true,
                isSpeakerOn = inCallService?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
            )
        }
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

    fun answer() {
        activeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun disconnect() {
        if (activeCall?.state == Call.STATE_RINGING) {
            try {
                activeCall?.reject(false, null)
            } catch (e: Exception) {
                activeCall?.disconnect()
            }
        } else {
            activeCall?.disconnect()
        }
    }

    fun toggleMute() {
        val service = inCallService ?: return
        val currentMuted = service.callAudioState?.isMuted == true
        service.setMuted(!currentMuted)
    }

    fun toggleSpeaker() {
        val service = inCallService ?: return
        val currentRoute = service.callAudioState?.route
        val newRoute = if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
            CallAudioState.ROUTE_EARPIECE
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        service.setAudioRoute(newRoute)
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
