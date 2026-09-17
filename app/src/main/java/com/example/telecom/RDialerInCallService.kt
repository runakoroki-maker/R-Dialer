package com.example.telecom

import android.app.KeyguardManager
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.ui.call.InCallActivity
import com.example.ui.call.IncomingCallPopupActivity

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

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.onAudioStateChanged(audioState)
    }

    private fun handleCallState(call: Call, state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                val currentState = CallManager.callState.value
                if (currentState != null) {
                    CallNotificationManager.showIncomingCallNotification(applicationContext, currentState)
                }

                val keyguardManager = getSystemService(KeyguardManager::class.java)
                val isLocked = keyguardManager?.isKeyguardLocked == true

                if (isLocked) {
                    // Locked device: Present full-screen incoming call UI
                    val intent = Intent(this, InCallActivity::class.java).apply {
                        action = InCallActivity.ACTION_IN_CALL
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                } else {
                    // Unlocked device (using another app like YouTube, Chrome, or on Home):
                    // Launch compact Samsung-style floating incoming call popup
                    val popupIntent = Intent(this, IncomingCallPopupActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(popupIntent)
                }
            }
            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                val intent = Intent(this, InCallActivity::class.java).apply {
                    action = InCallActivity.ACTION_IN_CALL
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
