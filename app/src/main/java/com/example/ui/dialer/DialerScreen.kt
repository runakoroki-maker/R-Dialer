package com.example.ui.dialer

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.TelecomHelper
import com.example.ui.components.ContactAvatar
import com.example.ui.components.DialerKeypad

import androidx.compose.material.icons.filled.ArrowDropDown
import com.example.telecom.SubscriptionHelper
import com.example.ui.components.SimSelectionDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    onRequestCallPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkDefaultDialerStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.checkDefaultDialerStatus()
        viewModel.refreshSimInfo()
    }

    if (uiState.isSimSelectionVisible) {
        val currentPrefSim = when (uiState.preferredSimMode) {
            SubscriptionHelper.PREF_SIM_1 -> 1
            SubscriptionHelper.PREF_SIM_2 -> 2
            else -> null
        }
        SimSelectionDialog(
            simList = uiState.activeSims,
            currentPreferredSim = currentPrefSim,
            targetNumber = uiState.inputNumber,
            onSimSelected = { sim, rememberChoice ->
                viewModel.selectSimAndCall(sim, rememberChoice) { errorMsg ->
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                viewModel.setSimSelectionVisible(false)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Default dialer banner + App identity + Contact match preview
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Default Phone App prompt banner if not yet granted
            AnimatedVisibility(
                visible = !uiState.isDefaultDialer,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("default_dialer_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEFF6FF)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Set as default phone app for full in-call UI",
                                fontSize = 12.sp,
                                color = Color(0xFF1E3A8A),
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                val intent = TelecomHelper.createRequestDefaultDialerIntent(context)
                                if (intent != null) {
                                    roleRequestLauncher.launch(intent)
                                } else {
                                    Toast.makeText(context, "Default dialer settings not supported on this device", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("set_default_dialer_button")
                        ) {
                            Text(text = "Enable", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Contact recognition card (Priority: photo -> first letter -> unknown)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("contact_preview_card"),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.inputNumber.isNotEmpty()) {
                    val contact = uiState.matchedContact
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(
                                photoUri = contact?.photoUri,
                                displayName = contact?.displayName,
                                initial = contact?.initial ?: "?",
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = contact?.displayName ?: "Unknown Caller",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (contact != null) "In Contacts" else "Not Saved",
                                    fontSize = 11.sp,
                                    color = if (contact != null) Color(0xFF059669) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large readable phone number display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.inputNumber.ifEmpty { " " },
                    fontSize = if (uiState.inputNumber.length > 13) 26.sp else 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 1.sp,
                    modifier = Modifier.testTag("dialer_phone_number_text")
                )
            }
        }

        // Keypad section
        DialerKeypad(
            onDigitClick = { digit ->
                viewModel.appendDigit(digit)
            },
            modifier = Modifier.padding(vertical = 6.dp)
        )

        // Bottom Controls: Call button and Backspace button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left spacer for symmetry
            Box(modifier = Modifier.size(68.dp))

            // Center: Call button + Optional compact Dual-SIM selector
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF10B981), Color(0xFF059669))
                            )
                        )
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = {
                                if (!TelecomHelper.hasCallPermission(context)) {
                                    onRequestCallPermission()
                                } else {
                                    viewModel.placeCall { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        .testTag("call_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Place Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (uiState.activeSims.size >= 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF))
                            .clickable { viewModel.setSimSelectionVisible(true) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("dialer_sim_selector"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.selectedSim?.label ?: "SIM 1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select SIM",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Backspace button (tap delete, long press clear all)
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.inputNumber.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.deleteDigit() },
                        modifier = Modifier
                            .size(52.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { viewModel.deleteDigit() },
                                onLongClick = { viewModel.clearNumber() }
                            )
                            .testTag("backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete Digit",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
