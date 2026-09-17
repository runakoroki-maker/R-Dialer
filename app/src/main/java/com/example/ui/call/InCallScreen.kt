package com.example.ui.call

import android.telecom.Call
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.CallManager
import com.example.telecom.TelecomHelper
import com.example.ui.components.ContactAvatar
import com.example.ui.components.DialerKeypad
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    onCallFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by CallManager.callState.collectAsState()
    var showInCallKeypad by remember { mutableStateOf(false) }

    if (callState == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF38BDF8))
        }
        return
    }

    val state = callState!!
    val isIncoming = state.telecomState == Call.STATE_RINGING
    val isDisconnected = state.telecomState == Call.STATE_DISCONNECTED

    LaunchedEffect(isDisconnected) {
        if (isDisconnected) {
            delay(1200)
            onCallFinished()
        }
    }

    val statusText = when (state.telecomState) {
        Call.STATE_RINGING -> "Incoming Call..."
        Call.STATE_DIALING -> "Calling..."
        Call.STATE_CONNECTING -> "Connecting..."
        Call.STATE_ACTIVE -> TelecomHelper.formatDuration(state.durationSeconds)
        Call.STATE_HOLDING -> "On Hold"
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> "Call Ended"
        else -> "In Call"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A), // Deep Slate Navy
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Call identity & Avatar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp)
            ) {
                Text(
                    text = "R DIALER",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                ContactAvatar(
                    photoUri = state.photoUri,
                    displayName = state.contactName,
                    initial = state.initial,
                    size = 110.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = state.displayTitle,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.testTag("in_call_contact_name")
                )

                if (!state.contactName.isNullOrBlank() && state.number.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.number,
                        color = Color(0xFF94A3B8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Call status badge
                Text(
                    text = statusText,
                    color = if (isIncoming) Color(0xFF34D399) else Color(0xFFE2E8F0),
                    fontSize = if (state.telecomState == Call.STATE_ACTIVE) 22.sp else 16.sp,
                    fontWeight = if (state.telecomState == Call.STATE_ACTIVE) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.testTag("in_call_status_text")
                )
            }

            // Middle section: In-call DTMF Keypad if toggled
            AnimatedVisibility(
                visible = showInCallKeypad,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DialerKeypad(
                    onDigitClick = { digit ->
                        CallManager.sendDtmfTone(digit.first())
                    },
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Bottom Section: Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                if (isIncoming) {
                    // Incoming Call Actions: 🔴 Decline & 🟢 Answer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "Decline",
                            backgroundColor = Color(0xFFEF4444),
                            iconColor = Color.White,
                            size = 72.dp,
                            testTag = "incoming_decline_button",
                            onClick = {
                                CallManager.disconnect()
                                onCallFinished()
                            }
                        )

                        InCallActionButton(
                            icon = Icons.Default.Call,
                            label = "Answer",
                            backgroundColor = Color(0xFF10B981),
                            iconColor = Color.White,
                            size = 72.dp,
                            testTag = "incoming_answer_button",
                            onClick = {
                                CallManager.answer()
                            }
                        )
                    }
                } else {
                    // Active / Outgoing Call Controls: Mute, Speaker, Keypad, and End Call
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallActionButton(
                            icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (state.isMuted) "Unmute" else "Mute",
                            backgroundColor = if (state.isMuted) Color(0xFF2563EB) else Color(0xFF334155),
                            iconColor = Color.White,
                            testTag = "in_call_mute_button",
                            onClick = { CallManager.toggleMute() }
                        )

                        InCallActionButton(
                            icon = if (state.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            label = if (state.isSpeakerOn) "Earpiece" else "Speaker",
                            backgroundColor = if (state.isSpeakerOn) Color(0xFF2563EB) else Color(0xFF334155),
                            iconColor = Color.White,
                            testTag = "in_call_speaker_button",
                            onClick = { CallManager.toggleSpeaker() }
                        )

                        InCallActionButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            backgroundColor = if (showInCallKeypad) Color(0xFF2563EB) else Color(0xFF334155),
                            iconColor = Color.White,
                            testTag = "in_call_keypad_button",
                            onClick = { showInCallKeypad = !showInCallKeypad }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Big red End Call button
                    InCallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = "End",
                        backgroundColor = Color(0xFFEF4444),
                        iconColor = Color.White,
                        size = 72.dp,
                        testTag = "in_call_end_button",
                        onClick = {
                            CallManager.disconnect()
                            onCallFinished()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InCallActionButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .testTag(testTag),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = backgroundColor,
                contentColor = iconColor
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(size * 0.48f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
