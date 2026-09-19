package com.example.ui.recents

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CallLogEntry
import com.example.data.models.CallType
import com.example.telecom.TelecomHelper
import com.example.ui.components.ContactAvatar
import com.example.ui.contacts.PermissionDeniedCard

@Composable
fun RecentsScreen(
    viewModel: RecentsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndLoad()
    }

    // Caller Profile Dialog
    val selectedEntry = uiState.selectedProfileEntry
    if (selectedEntry != null) {
        CallerProfileDialog(
            entry = selectedEntry,
            callLogs = uiState.profileCallLogs,
            isBlocked = uiState.isNumberBlocked,
            reportMessage = uiState.reportStatusMessage,
            onCall = {
                TelecomHelper.placeCall(context, selectedEntry.number)
            },
            onSms = {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${selectedEntry.number}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(smsIntent)
                } catch (e: Exception) {
                    // ignore
                }
            },
            onToggleBlock = {
                viewModel.toggleBlockCurrentNumber()
            },
            onReport = {
                viewModel.reportCurrentNumber()
            },
            onDeleteLog = { logId ->
                viewModel.deleteCallLogEntry(logId)
            },
            onDismiss = {
                viewModel.closeCallerProfile()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        when {
            !uiState.hasPermission -> {
                PermissionDeniedCard(
                    title = "Call history permission is required to display your recent calls.",
                    onGrantClick = {
                        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    },
                    onSettingsClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            }
            uiState.callLogs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recent calls found",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            else -> {
                Text(
                    text = "Recent Calls",
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recents_list")
                ) {
                    items(
                        items = uiState.callLogs,
                        key = { it.id }
                    ) { logEntry ->
                        CallLogRow(
                            entry = logEntry,
                            onClick = {
                                viewModel.openCallerProfile(logEntry)
                            },
                            onCallClick = {
                                TelecomHelper.placeCall(context, logEntry.number)
                            }
                        )
                        HorizontalDivider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 68.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogRow(
    entry: CallLogEntry,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = TelecomHelper.formatCallLogDate(entry.timestamp)
    val (typeIcon, typeColor, typeLabel) = when (entry.type) {
        CallType.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, Color(0xFF10B981), "Incoming")
        CallType.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, Color(0xFF3B82F6), "Outgoing")
        CallType.MISSED -> Triple(Icons.Default.CallMissed, Color(0xFFEF4444), "Missed")
        CallType.REJECTED -> Triple(Icons.Default.CallMissed, Color(0xFFF97316), "Declined")
        else -> Triple(Icons.AutoMirrored.Filled.CallReceived, Color(0xFF64748B), "Call")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            photoUri = entry.photoUri,
            displayName = entry.cachedName,
            initial = entry.initial,
            size = 48.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (entry.type == CallType.MISSED) Color(0xFFDC2626) else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = typeLabel,
                    tint = typeColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$typeLabel • $dateText",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.durationSeconds > 0) {
                Text(
                    text = "Duration: ${TelecomHelper.formatDuration(entry.durationSeconds)}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
        IconButton(
            onClick = onCallClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9))
                .testTag("call_recents_${entry.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call ${entry.displayName}",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CallerProfileDialog(
    entry: CallLogEntry,
    callLogs: List<CallLogEntry>,
    isBlocked: Boolean,
    reportMessage: String?,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit,
    onDeleteLog: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_caller_profile")
            ) {
                Text("Close", color = Color(0xFF2563EB))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(
                    photoUri = entry.photoUri,
                    displayName = entry.cachedName,
                    initial = entry.initial,
                    size = 52.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.number,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileActionButton(
                        icon = Icons.Default.Call,
                        label = "Call",
                        color = Color(0xFF2563EB),
                        onClick = onCall,
                        testTag = "profile_call_button"
                    )
                    ProfileActionButton(
                        icon = Icons.Default.Chat,
                        label = "SMS",
                        color = Color(0xFF10B981),
                        onClick = onSms,
                        testTag = "profile_sms_button"
                    )
                    ProfileActionButton(
                        icon = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        label = if (isBlocked) "Unblock" else "Block",
                        color = if (isBlocked) Color(0xFF10B981) else Color(0xFFEF4444),
                        onClick = onToggleBlock,
                        testTag = "profile_block_button"
                    )
                    ProfileActionButton(
                        icon = Icons.Default.Warning,
                        label = "Report",
                        color = Color(0xFFF59E0B),
                        onClick = onReport,
                        testTag = "profile_report_button"
                    )
                }

                if (!reportMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = reportMessage,
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Text(
                    text = "Call History (${callLogs.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                if (callLogs.isEmpty()) {
                    Text(
                        text = "No history available",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(callLogs, key = { it.id }) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val dateStr = TelecomHelper.formatCallLogDate(log.timestamp)
                                    val typeStr = log.type.name.lowercase().replaceFirstChar { it.uppercase() }
                                    Text(
                                        text = "$typeStr • $dateStr",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                    if (log.durationSeconds > 0) {
                                        Text(
                                            text = "Duration: ${TelecomHelper.formatDuration(log.durationSeconds)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteLog(log.id) },
                                    modifier = Modifier.size(32.dp).testTag("delete_log_${log.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete entry",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155)
        )
    }
}
