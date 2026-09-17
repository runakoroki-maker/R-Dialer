package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ActiveCallState

@Composable
fun IncomingCallCard(
    callState: ActiveCallState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("incoming_call_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header: Label & Open Full Screen Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Incoming Call",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenFullScreen)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("incoming_call_fullscreen_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open Full Screen",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Open Full Screen",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact Avatar
            ContactAvatar(
                photoUri = callState.photoUri,
                displayName = callState.contactName,
                initial = callState.initial,
                size = 64.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Caller Identity: Name or Phone number
            Text(
                text = callState.displayTitle,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!callState.contactName.isNullOrBlank() && callState.number.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = callState.number,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            } else if (callState.contactName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Unknown Caller",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Action Buttons: 🔴 Decline & 🟢 Answer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔴 Decline
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("incoming_call_decline_button")
                ) {
                    FilledIconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Decline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444)
                    )
                }

                // 🟢 Answer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("incoming_call_answer_button")
                ) {
                    FilledIconButton(
                        onClick = onAnswer,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer Call",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Answer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
