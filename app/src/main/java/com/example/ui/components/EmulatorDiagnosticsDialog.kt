package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.DeviceEnvironmentInfo

@Composable
fun EmulatorLoginDetectedDialog(
    onDismiss: () -> Unit,
    onViewDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("emulator_detected_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)), // Warm Amber
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emulator Detected",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "R Dialer Emulator Login Detected",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.2).sp
            )
        },
        text = {
            Column {
                Text(
                    text = "This application is currently running inside an Android emulator / virtual environment.",
                    fontSize = 14.sp,
                    color = Color(0xFF334155),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Phone calls in this environment are DEMO calls and may not represent real cellular calling.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("emulator_dialog_ok_button")
            ) {
                Text("OK", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onViewDiagnostics,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("emulator_dialog_diagnostics_button")
            ) {
                Text("Diagnostics", color = Color(0xFF475569))
            }
        }
    )
}

@Composable
fun DeviceEnvironmentDialog(
    environmentInfo: DeviceEnvironmentInfo,
    onDismiss: () -> Unit,
    onSimulateIncomingCall: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("device_environment_dialog"),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (environmentInfo.isEmulator) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (environmentInfo.isEmulator) Icons.Default.Warning else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (environmentInfo.isEmulator) Color(0xFFD97706) else Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Device Environment",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (environmentInfo.isEmulator) "Demo Call Mode Active" else "Real Device Mode",
                                fontSize = 12.sp,
                                color = if (environmentInfo.isEmulator) Color(0xFFD97706) else Color(0xFF16A34A),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("device_env_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Core Diagnostics Cards
                DiagnosticItemCard(
                    title = "Environment",
                    value = environmentInfo.environmentName,
                    statusBadge = if (environmentInfo.isEmulator) "EMULATOR" else "PHYSICAL",
                    isWarning = environmentInfo.isEmulator
                )

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticItemCard(
                    title = "Call Mode",
                    value = environmentInfo.callMode,
                    statusBadge = if (environmentInfo.isEmulator) "DEMO" else "REAL",
                    isWarning = environmentInfo.isEmulator
                )

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticItemCard(
                    title = "Telecom",
                    value = environmentInfo.telecomStatus,
                    statusBadge = if (environmentInfo.isEmulator) "SIMULATED" else "ACTIVE",
                    isWarning = false
                )

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticItemCard(
                    title = "Real Cellular Calling",
                    value = environmentInfo.cellularStatus,
                    statusBadge = if (environmentInfo.isEmulator) "NOT AVAILABLE" else "CARRIER",
                    isWarning = environmentInfo.isEmulator
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Technical Hardware Specs
                Text(
                    text = "HARDWARE & BUILD SIGNATURE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(14.dp)
                ) {
                    Column {
                        SpecCodeRow(label = "Model", value = environmentInfo.deviceModel)
                        SpecCodeRow(label = "OEM", value = environmentInfo.manufacturer)
                        SpecCodeRow(label = "Hardware", value = environmentInfo.hardware)
                        SpecCodeRow(label = "Product", value = environmentInfo.product)
                        if (environmentInfo.matchedIndicators.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Matched Indicators: ${environmentInfo.matchedIndicators.size}",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            environmentInfo.matchedIndicators.take(3).forEach { ind ->
                                Text(
                                    text = "• $ind",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Demo Simulator Action (especially for emulator testing)
                if (onSimulateIncomingCall != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            onSimulateIncomingCall()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (environmentInfo.isEmulator) Color(0xFFD97706) else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_demo_incoming_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (environmentInfo.isEmulator) "Simulate Demo Incoming Call" else "Test Incoming Call Flow",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(
    title: String,
    value: String,
    statusBadge: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isWarning) Color(0xFFFEF3C7) else Color(0xFFDCFCE7))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) Color(0xFFB45309) else Color(0xFF15803D),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun SpecCodeRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
