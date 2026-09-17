package com.example.ui.call

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.telecom.CallManager
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()
        handleIntent(intent)

        // Automatically finish when call ends or is removed
        lifecycleScope.launch {
            var hasSeenCall = CallManager.callState.value != null
            CallManager.callState.collectLatest { state ->
                if (state != null) {
                    hasSeenCall = true
                } else if (hasSeenCall) {
                    finishAndRemoveTask()
                }
            }
        }

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                InCallScreen(
                    onCallFinished = {
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_ANSWER_CALL) {
            CallManager.answer()
        }
    }

    private fun configureLockscreenFlags() {
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
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    companion object {
        const val ACTION_IN_CALL = "com.example.ui.call.ACTION_IN_CALL"
        const val ACTION_ANSWER_CALL = "com.example.ui.call.ACTION_ANSWER_CALL"
    }
}
