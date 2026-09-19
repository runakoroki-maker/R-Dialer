package com.example.ui.messages

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.example.data.models.SmsConversation
import com.example.data.models.SmsMessageItem
import com.example.ai.SmartSmsInfo
import com.example.util.EmulatorDetector
import com.example.ui.contacts.PermissionDeniedCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(viewModel: MessagesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndLoad()
    }

    if (!uiState.hasPermission) {
        PermissionDeniedCard(
            title = "SMS permission is required to display your messages.",
            onGrantClick = {
                permissionLauncher.launch(Manifest.permission.READ_SMS)
            },
            onSettingsClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            when {
                uiState.selectedThreadId != null || uiState.selectedAddress != null -> {
                    ConversationDetailView(
                        recipientName = uiState.selectedName ?: uiState.selectedAddress ?: "Unknown",
                        recipientAddress = uiState.selectedAddress ?: "",
                        messages = uiState.messages,
                        isLoading = uiState.isLoading,
                        newMessageText = uiState.newMessageText,
                        currentSmartInfo = uiState.currentSmartInfo,
                        onTextChanged = { viewModel.updateNewMessageText(it) },
                        onSend = { body ->
                            uiState.selectedAddress?.let { address ->
                                viewModel.sendSms(address, body) { _ -> }
                            }
                        },
                        onRescan = {
                            viewModel.rescanCurrentConversation()
                        },
                        onBack = {
                            viewModel.closeConversation()
                        }
                    )
                }
                else -> {
                    val isEmulator = EmulatorDetector.isEmulator()
                    var showMenu by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Messages",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            if (isEmulator) {
                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.testTag("messages_overflow_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = Color(0xFF0F172A)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Check Demo Messages") },
                                            onClick = {
                                                showMenu = false
                                                viewModel.loadDemoZomatoMessage()
                                            },
                                            modifier = Modifier.testTag("menu_check_demo_messages")
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF2563EB))
                            }
                        } else if (uiState.conversations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No SMS conversations found",
                                        fontSize = 15.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(uiState.conversations) { conv ->
                                    val smartInfo = uiState.smartInfoMap[conv.address]
                                    ConversationItemRow(
                                        conversation = conv,
                                        smartInfo = smartInfo,
                                        onClick = {
                                            viewModel.selectConversation(
                                                conv.threadId,
                                                conv.address,
                                                conv.contactName
                                            )
                                        }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItemRow(
    conversation: SmsConversation,
    smartInfo: SmartSmsInfo?,
    onClick: () -> Unit
) {
    val displayName = smartInfo?.brand ?: conversation.contactName ?: conversation.address
    val timeFormatted = formatSmsTime(conversation.date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag("conversation_item"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (smartInfo?.isAutomated == true) Color(0xFFEFF6FF) else Color(0xFFDBEAFE)),
            contentAlignment = Alignment.Center
        ) {
            if (smartInfo?.isVerified == true && !smartInfo.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = smartInfo.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = if (smartInfo?.isAutomated == true) Icons.Default.Verified else Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = Color(0xFF1D4ED8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        fontWeight = if (!conversation.read) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (smartInfo?.isVerified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = conversation.snippet,
                fontSize = 13.sp,
                color = if (!conversation.read) Color(0xFF1E293B) else Color(0xFF64748B),
                fontWeight = if (!conversation.read) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (smartInfo?.isAutomated == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${smartInfo.brand ?: "System"} • ${smartInfo.messageType ?: "Automated"}",
                        fontSize = 11.sp,
                        color = Color(0xFF1D4ED8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationDetailView(
    recipientName: String,
    recipientAddress: String,
    messages: List<SmsMessageItem>,
    isLoading: Boolean,
    newMessageText: String,
    currentSmartInfo: SmartSmsInfo?,
    onTextChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog && currentSmartInfo != null) {
        SenderProfileDialog(
            smartInfo = currentSmartInfo,
            recipientAddress = recipientAddress,
            onRescan = onRescan,
            onDismiss = { showProfileDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("conversation_detail_view")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("back_to_conversations_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            if (currentSmartInfo?.isVerified == true && !currentSmartInfo.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = currentSmartInfo.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = currentSmartInfo != null) {
                        showProfileDialog = true
                    }
                    .padding(vertical = 4.dp)
                    .testTag("sender_header_click")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentSmartInfo?.brand ?: recipientName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (currentSmartInfo?.isVerified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (recipientName != recipientAddress && recipientAddress.isNotBlank()) {
                    Text(
                        text = recipientAddress,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        if (currentSmartInfo?.isAutomated == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF6FF))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated SMS: ${currentSmartInfo.brand ?: recipientName} (${currentSmartInfo.messageType})",
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFFAFAFA))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages in this conversation",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        MessageBubble(msg = msg)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessageText,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Text message", color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sms_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (newMessageText.isNotBlank()) {
                            onSend(newMessageText)
                        }
                    },
                    enabled = newMessageText.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_sms_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (newMessageText.isNotBlank()) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SenderProfileDialog(
    smartInfo: SmartSmsInfo,
    recipientAddress: String,
    onRescan: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_profile_button")
            ) {
                Text("Close", color = Color(0xFF2563EB))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!smartInfo.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = smartInfo.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = smartInfo.brand ?: recipientAddress,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (smartInfo.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp).testTag("verified_check_icon")
                            )
                        }
                    }
                    Text(
                        text = if (smartInfo.isVerified) "✓ Verified Sender" else recipientAddress,
                        fontSize = 12.sp,
                        color = if (smartInfo.isVerified) Color(0xFF16A34A) else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = smartInfo.description ?: "Official automated notification sender.",
                    fontSize = 14.sp,
                    color = Color(0xFF334155)
                )
                if (smartInfo.messageType != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Classification: ${smartInfo.messageType}",
                            fontSize = 12.sp,
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Scanned by Runa AI",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (smartInfo.isVerified) "✓ Verified" else "Not Verified",
                            fontSize = 13.sp,
                            color = if (smartInfo.isVerified) Color(0xFF16A34A) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onRescan,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("rescan_button")
                    ) {
                        Text("Re-scan", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun MessageBubble(msg: SmsMessageItem) {
    val isSent = msg.type == 2
    val bubbleColor = if (isSent) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val textColor = if (isSent) Color.White else Color(0xFF0F172A)
    val alignment = if (isSent) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 4.dp,
                        bottomEnd = if (isSent) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = msg.body,
                fontSize = 14.sp,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatSmsTime(msg.date),
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

private fun formatSmsTime(timeMillis: Long): String {
    if (timeMillis <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timeMillis
    val sdf = if (diff < 24 * 60 * 60 * 1000L) {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
    } else {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    }
    return sdf.format(Date(timeMillis))
}
