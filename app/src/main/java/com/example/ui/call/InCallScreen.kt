package com.example.ui.call

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.Call
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.models.ConferenceParticipant
import com.example.telecom.CallManager
import com.example.telecom.CallRecorderManager
import com.example.telecom.TelecomHelper
import com.example.ui.components.ContactAvatar
import com.example.ui.components.DialerKeypad
import com.example.util.EmulatorDetector
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InCallScreen(
    onCallFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val callState by CallManager.callState.collectAsState()
    val environmentInfo by EmulatorDetector.environmentInfo.collectAsState()
    val isEmulator = environmentInfo.isEmulator

    var showInCallKeypad by remember { mutableStateOf(false) }
    var showAddCallSheet by remember { mutableStateOf(false) }
    val addCallSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CallManager.startCallRecording(context)
        }
    }

    // Camera permission launcher for real user privacy
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CallManager.setVideoCallActive(true, enableCamera = true)
        } else {
            CallManager.setVideoStatusMessage("Video unavailable. Camera permission was not granted. Continue Audio Call")
        }
    }

    if (callState == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
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

    // If Video Call Mode is active, render VideoCallGlassScreen
    if (state.isVideoCall) {
        VideoCallGlassScreen(
            state = state,
            onSwitchToAudioOnly = { CallManager.setVideoCallActive(false) },
            onEndCall = {
                CallManager.disconnect()
                onCallFinished()
            }
        )
        return
    }

    val statusText = when {
        state.isConference -> if (isEmulator) "Demo Conference • ${TelecomHelper.formatDuration(state.durationSeconds)}" else "Conference • ${TelecomHelper.formatDuration(state.durationSeconds)}"
        state.telecomState == Call.STATE_RINGING -> if (isEmulator) "Demo Incoming Call..." else "Incoming Call..."
        state.telecomState == Call.STATE_DIALING -> if (isEmulator) "Demo Calling..." else "Calling..."
        state.telecomState == Call.STATE_CONNECTING -> if (isEmulator) "Demo Connecting..." else "Connecting..."
        state.telecomState == Call.STATE_ACTIVE -> if (isEmulator) "Demo • ${TelecomHelper.formatDuration(state.durationSeconds)}" else TelecomHelper.formatDuration(state.durationSeconds)
        state.telecomState == Call.STATE_HOLDING -> "On Hold"
        state.telecomState == Call.STATE_DISCONNECTED || state.telecomState == Call.STATE_DISCONNECTING -> "Call Ended"
        else -> "In Call"
    }

    // Pulse animation for recording badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0).copy(alpha = 0.6f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Brand & Badges
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "R DIALER",
                            color = Color(0xFF2563EB),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    if (isEmulator) {
                        var showEmulatorMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showEmulatorMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Emulator Menu",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showEmulatorMenu,
                                onDismissRequest = { showEmulatorMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Simulate Another Incoming Call") },
                                    onClick = {
                                        showEmulatorMenu = false
                                        CallManager.simulateDemoCallWaiting(context)
                                    }
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }

                if (isEmulator) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("in_call_demo_banner")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isIncoming) "DEMO INCOMING CALL — EMULATOR" else "DEMO CALL — EMULATOR",
                                color = Color(0xFF92400E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Premium Frosted Glass Card for Caller / Conference Info
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    color = Color.White.copy(alpha = 0.85f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.isConference) {
                            // Conference Call View
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF))
                                    .border(2.dp, Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Conference",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Conference Call Active",
                                color = Color(0xFF0F172A),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Successfully Merged • ${state.conferenceParticipants.size} Participants Connected",
                                    color = Color(0xFF065F46),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // List of conference participants
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                state.conferenceParticipants.forEachIndexed { index, participant ->
                                    ConferenceParticipantRow(
                                        participant = participant,
                                        onDisconnect = {
                                            CallManager.disconnectParticipant(participant.id)
                                        }
                                    )
                                    if (index < state.conferenceParticipants.size - 1) {
                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        } else {
                            // Single Caller View
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFDBEAFE).copy(alpha = 0.8f),
                                                Color(0xFFEFF6FF).copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                ContactAvatar(
                                    photoUri = state.photoUri,
                                    displayName = state.displayTitle,
                                    initial = state.initial,
                                    size = 94.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = state.displayTitle,
                                color = Color(0xFF0F172A),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.testTag("in_call_contact_name")
                            )

                            if (!state.contactName.isNullOrBlank() && state.number.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.number,
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }

                            if (!state.simLabel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = state.simLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Duration / Status
                        Text(
                            text = statusText,
                            color = if (isIncoming) Color(0xFF059669) else if (state.telecomState == Call.STATE_HOLDING) Color(0xFFD97706) else Color(0xFF0F172A),
                            fontSize = if (state.telecomState == Call.STATE_ACTIVE) 24.sp else 16.sp,
                            fontWeight = if (state.telecomState == Call.STATE_ACTIVE) FontWeight.Bold else FontWeight.SemiBold,
                            modifier = Modifier.testTag("in_call_status_text")
                        )

                        // Call On Hold Status Banner & Quick Resume
                        if (state.telecomState == Call.STATE_HOLDING) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("call_on_hold_banner")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Call On Hold",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }

                            if (isEmulator) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Demo Mode: Cellular Hold simulated. Physical SIM required for network hold.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }

                            // Prominent Resume Button on Caller Card
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { CallManager.unhold() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("in_call_resume_banner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resume Call", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Recording Badge
                        if (state.isRecording) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDC2626))
                                            .alpha(pulseAlpha)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "REC ${TelecomHelper.formatDuration(state.recordingDurationSeconds)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        // Conference Error Banner
                        if (!state.conferenceErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.conferenceErrorMessage ?: "",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF92400E),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { CallManager.dismissConferenceError() }
                                    )
                                }
                            }
                        }

                        // Merge Status / Feedback Banner
                        if (!state.mergeStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (state.mergeStatusMessage?.contains("Merging") == true) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = Color(0xFF059669),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = state.mergeStatusMessage ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF065F46),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { CallManager.dismissMergeStatus() }
                                    )
                                }
                            }
                        }

                        // Dedicated Multiple Calls Section (When second call exists)
                        if (state.secondCall != null) {
                            val sec = state.secondCall
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(18.dp)),
                                color = Color(0xFFF8FAFC),
                                shadowElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "Calls",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEFF6FF))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CallMerge,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "2 Active Calls Ready — Tap Merge Calls to create Conference",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Call 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            ContactAvatar(
                                                photoUri = state.photoUri,
                                                displayName = state.displayTitle,
                                                initial = state.initial,
                                                size = 38.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = state.displayTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val isPrimaryHeld = state.telecomState == Call.STATE_HOLDING
                                                Text(
                                                    text = if (isPrimaryHeld) "On Hold" else "Active",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isPrimaryHeld) Color(0xFFD97706) else Color(0xFF059669)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { CallManager.disconnectCall1() },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFEE2E2))
                                                .testTag("end_call_1_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CallEnd,
                                                contentDescription = "End Call 1",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Call 2 / Call Waiting
                                    val isSecRinging = sec.telecomState == Call.STATE_RINGING
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            ContactAvatar(
                                                photoUri = sec.photoUri,
                                                displayName = sec.displayTitle,
                                                initial = sec.initial,
                                                size = 38.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sec.displayTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (isSecRinging) "Incoming Call Waiting..." else (if (sec.telecomState == Call.STATE_HOLDING) "On Hold" else "Active"),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isSecRinging) Color(0xFFD97706) else (if (sec.telecomState == Call.STATE_HOLDING) Color(0xFFD97706) else Color(0xFF059669))
                                                )
                                            }
                                        }
                                        if (!isSecRinging) {
                                            IconButton(
                                                onClick = { CallManager.disconnectCall2() },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFEE2E2))
                                                    .testTag("end_call_2_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CallEnd,
                                                    contentDescription = "End Call 2",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Action Buttons: Swap/Merge or Answer/Decline Call Waiting
                                    if (isSecRinging) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { CallManager.declineSecondCall() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .testTag("decline_second_call_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CallEnd,
                                                    contentDescription = "Decline",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Decline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Button(
                                                onClick = { CallManager.answerSecondCall() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .testTag("answer_second_call_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Answer",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Answer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { CallManager.swapCalls() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .testTag("swap_calls_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapCalls,
                                                    contentDescription = "Swap Calls",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Swap Calls", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Button(
                                                onClick = { CallManager.mergeCalls() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .testTag("merge_calls_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CallMerge,
                                                    contentDescription = "Merge Calls",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Merge Calls", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // In-Call Keypad
            AnimatedVisibility(
                visible = showInCallKeypad,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp)),
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    DialerKeypad(
                        onDigitClick = { digit ->
                            CallManager.sendDtmfTone(digit.first())
                        },
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Bottom Section: Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (isIncoming) {
                    // Incoming Call: Decline & Answer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallGlassActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "Decline",
                            backgroundColor = Color(0xFFEF4444),
                            iconColor = Color.White,
                            size = 70.dp,
                            testTag = "incoming_decline_button",
                            onClick = {
                                CallManager.disconnect()
                                onCallFinished()
                            }
                        )

                        InCallGlassActionButton(
                            icon = Icons.Default.Call,
                            label = "Answer",
                            backgroundColor = Color(0xFF10B981),
                            iconColor = Color.White,
                            size = 70.dp,
                            testTag = "incoming_answer_button",
                            onClick = { CallManager.answer() }
                        )
                    }
                } else {
                    // Row 1: Mute, Speaker, Hold
                    val isHeld = state.telecomState == Call.STATE_HOLDING
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallGlassActionButton(
                            icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (state.isMuted) "Unmute" else "Mute",
                            backgroundColor = if (state.isMuted) Color(0xFF2563EB) else Color.White,
                            iconColor = if (state.isMuted) Color.White else Color(0xFF1E293B),
                            testTag = "in_call_mute_button",
                            onClick = { CallManager.toggleMute() }
                        )

                        InCallGlassActionButton(
                            icon = if (state.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            label = if (state.isSpeakerOn) "Earpiece" else "Speaker",
                            backgroundColor = if (state.isSpeakerOn) Color(0xFF2563EB) else Color.White,
                            iconColor = if (state.isSpeakerOn) Color.White else Color(0xFF1E293B),
                            testTag = "in_call_speaker_button",
                            onClick = { CallManager.toggleSpeaker() }
                        )

                        InCallGlassActionButton(
                            icon = if (isHeld) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (isHeld) "Resume" else "Hold",
                            backgroundColor = if (isHeld) Color(0xFFFEF3C7) else Color.White,
                            iconColor = if (isHeld) Color(0xFFD97706) else Color(0xFF1E293B),
                            testTag = "in_call_hold_button",
                            onClick = { CallManager.toggleHold() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 2: Add Call, Keypad, Record
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallGlassActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Add Call",
                            backgroundColor = Color.White,
                            iconColor = Color(0xFF2563EB),
                            testTag = "in_call_add_call_button",
                            onClick = { showAddCallSheet = true }
                        )

                        InCallGlassActionButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            backgroundColor = if (showInCallKeypad) Color(0xFF2563EB) else Color.White,
                            iconColor = if (showInCallKeypad) Color.White else Color(0xFF1E293B),
                            testTag = "in_call_keypad_button",
                            onClick = { showInCallKeypad = !showInCallKeypad }
                        )

                        InCallGlassActionButton(
                            icon = if (state.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            label = if (state.isRecording) "Stop" else "Record",
                            backgroundColor = if (state.isRecording) Color(0xFFDC2626) else Color.White,
                            iconColor = if (state.isRecording) Color.White else Color(0xFFDC2626),
                            testTag = "in_call_record_button",
                            onClick = {
                                if (state.isRecording) {
                                    CallManager.stopCallRecording(context)
                                } else {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        CallManager.startCallRecording(context)
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 3: Video Call & Merge (when available)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InCallGlassActionButton(
                            icon = Icons.Default.Videocam,
                            label = "Video Call",
                            backgroundColor = Color.White,
                            iconColor = Color(0xFF2563EB),
                            testTag = "in_call_video_button",
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    CallManager.setVideoCallActive(true, enableCamera = true)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )

                        if (state.canMergeCalls || state.secondCall != null) {
                            InCallGlassActionButton(
                                icon = Icons.Default.CallMerge,
                                label = "Merge",
                                backgroundColor = Color(0xFF10B981),
                                iconColor = Color.White,
                                testTag = "in_call_quick_merge_button",
                                onClick = { CallManager.mergeCalls() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // End Call Button
                    InCallGlassActionButton(
                        icon = Icons.Default.CallEnd,
                        label = if (state.isConference) "End Conference" else "End",
                        backgroundColor = Color(0xFFEF4444),
                        iconColor = Color.White,
                        size = 70.dp,
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

    // Add Call Sheet Modal
    if (showAddCallSheet) {
        AddCallSheet(
            sheetState = addCallSheetState,
            onDismiss = { showAddCallSheet = false },
            onNumberSelected = { number, name ->
                CallManager.initiateAddCall(context, number, name)
            }
        )
    }
}

@Composable
fun ConferenceParticipantRow(
    participant: ConferenceParticipant,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ContactAvatar(
                photoUri = participant.photoUri,
                displayName = participant.displayName,
                initial = participant.initial,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = participant.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (participant.isHeld) "On Hold" else "Connected",
                    fontSize = 11.sp,
                    color = if (participant.isHeld) Color(0xFFD97706) else Color(0xFF10B981)
                )
            }
        }

        IconButton(
            onClick = onDisconnect,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEE2E2))
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End call for ${participant.displayName}",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InCallGlassActionButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 58.dp,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .testTag(testTag),
            shape = CircleShape,
            color = backgroundColor,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF475569),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
