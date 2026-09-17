package com.example.ui.call

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.telecom.CallManager
import com.example.ui.components.IncomingCallCard
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomingCallPopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()

        // Auto-dismiss popup if call is no longer ringing (e.g. caller hung up or answered elsewhere)
        lifecycleScope.launch {
            CallManager.callState.collectLatest { state ->
                if (state == null || state.telecomState != Call.STATE_RINGING) {
                    finish()
                }
            }
        }

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val callState by CallManager.callState.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    callState?.let { state ->
                        IncomingCallCard(
                            callState = state,
                            onAnswer = {
                                CallManager.answer()
                                val intent = Intent(this@IncomingCallPopupActivity, InCallActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                finish()
                            },
                            onDecline = {
                                CallManager.disconnect()
                                finish()
                            },
                            onOpenFullScreen = {
                                val intent = Intent(this@IncomingCallPopupActivity, InCallActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun configureWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
    }
}
