package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.ui.call.InCallActivity

class RDialerInCallService : InCallService() {

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallState(call, state)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callListener)
        CallManager.onCallAdded(call, this, applicationContext)
        handleCallState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callListener)
        CallManager.onCallRemoved(call)
        CallNotificationManager.cancelCallNotification(applicationContext)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.onAudioStateChanged(audioState)
    }

    private fun handleCallState(call: Call, state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                // Incoming call: Post high-priority Heads-Up Notification with full-screen intent.
                // This respects multitasking so if user is on YouTube/Chrome, a banner appears
                // without force-stopping their current task.
                val currentState = CallManager.callState.value
                if (currentState != null) {
                    CallNotificationManager.showIncomingCallNotification(applicationContext, currentState)
                }
            }
            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                // Outgoing call: Open in-call activity directly
                val intent = Intent(this, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            Call.STATE_ACTIVE -> {
                val currentState = CallManager.callState.value
                if (currentState != null) {
                    CallNotificationManager.showOngoingCallNotification(applicationContext, currentState)
                }
            }
            Call.STATE_DISCONNECTED -> {
                CallNotificationManager.cancelCallNotification(applicationContext)
            }
        }
    }
}
