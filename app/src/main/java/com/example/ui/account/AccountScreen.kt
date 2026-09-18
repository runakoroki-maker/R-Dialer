package com.example.ui.account

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.data.callingcard.CallingCardData
import com.example.data.callingcard.CallingCardRepository
import com.example.data.callingcard.GuestSessionManager
import com.example.ui.callingcard.CallingCardCustomizationScreen
import com.example.ui.callingcard.CallingCardPreviewCard
import kotlinx.coroutines.launch

/**
 * Screen modes within the Account destination
 */
private enum class AccountScreenSubView {
    MAIN,
    SETUP,
    CALLING_CARD_EDITOR
}

/**
 * Main Account Screen adhering strictly to R Dialer Calling Card + Guest Mode specifications:
 *
 * 1. Guest Mode for Emulator Testing:
 *    - "Continue with Email" and "Continue as Guest"
 *    - Choose display name dialog
 *    - Opens Account Setup flow
 *    - Clearly displays "Guest Mode" in account area
 *    - Stored strictly locally, separated from cloud data
 *
 * 2. Account Setup Flow:
 *    - Triggered after sign in or guest name setup when not completed
 *    - Allows customizing or removing profile picture
 *
 * 3. Calling Settings -> Calling Card:
 *    - Section in account area displaying current Calling Card preview
 *    - "Edit Calling Card" button
 *
 * 4. Calling Card Editor:
 *    - Full live preview & styling options
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val cardRepo = remember { CallingCardRepository.getInstance(context) }
    val guestManager = remember { GuestSessionManager.getInstance(context) }

    val isGuestActive by guestManager.isGuestActive.collectAsState()
    val guestDisplayName by guestManager.guestDisplayName.collectAsState()
    val isGuestSetupCompleted by guestManager.isGuestSetupCompleted.collectAsState()

    var currentSubView by remember { mutableStateOf(AccountScreenSubView.MAIN) }
    var isLoading by remember { mutableStateOf(false) }
    var showGuestNameDialog by remember { mutableStateOf(false) }
    var guestNameInput by remember { mutableStateOf("") }

    // Active session owner ID (Guest)
    val sessionOwnerId: String? = when {
        isGuestActive -> CallingCardRepository.GUEST_OWNER_ID
        else -> null
    }

    // Active Calling Card for current session
    var activeCallingCard by remember(sessionOwnerId) {
        mutableStateOf(
            if (sessionOwnerId != null) {
                val defName = guestDisplayName
                val photoUrl = null
                cardRepo.loadCardForOwner(sessionOwnerId, defName, photoUrl)
            } else null
        )
    }

    // Handle back button when within Setup or Editor subviews
    BackHandler(enabled = currentSubView != AccountScreenSubView.MAIN) {
        currentSubView = AccountScreenSubView.MAIN
    }

    // Route to subviews
    if (currentSubView == AccountScreenSubView.SETUP && sessionOwnerId != null) {
        val initialName = guestDisplayName
        val initialPhoto = null
        AccountSetupScreen(
            initialName = initialName,
            initialPhotoUrl = initialPhoto,
            isGuestMode = isGuestActive,
            onCompleteSetup = { finalName, customPhotoUri, isPhotoRemoved ->
                coroutineScope.launch {
                    val existing = activeCallingCard ?: cardRepo.loadCardForOwner(sessionOwnerId, finalName, initialPhoto)
                    val updated = existing.copy(
                        displayName = finalName,
                        profilePictureUri = customPhotoUri,
                        isProfilePictureRemoved = isPhotoRemoved,
                        isSetupCompleted = true
                    )
                    cardRepo.saveCard(updated)
                    cardRepo.markSetupCompleted(sessionOwnerId)
                    if (isGuestActive) {
                        guestManager.updateGuestDisplayName(finalName)
                        guestManager.setGuestSetupCompleted(true)
                    }
                    activeCallingCard = updated
                    currentSubView = AccountScreenSubView.MAIN
                    snackbarHostState.showSnackbar("Account setup completed!")
                }
            },
            onBack = { currentSubView = AccountScreenSubView.MAIN }
        )
        return
    }

    if (currentSubView == AccountScreenSubView.CALLING_CARD_EDITOR && activeCallingCard != null) {
        CallingCardCustomizationScreen(
            initialCard = activeCallingCard!!,
            onSave = { updatedCard ->
                coroutineScope.launch {
                    cardRepo.saveCard(updatedCard)
                    activeCallingCard = updatedCard
                    currentSubView = AccountScreenSubView.MAIN
                    snackbarHostState.showSnackbar("Calling Card saved successfully!")
                }
            },
            onCancel = { currentSubView = AccountScreenSubView.MAIN }
        )
        return
    }

    // MAIN ACCOUNT VIEW
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("account_screen"),
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("account_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Dialer",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isGuestActive) {
                // ==========================================
                // GUEST MODE ACTIVE STATE (Section 1)
                // ==========================================
                GuestUserView(
                    displayName = guestDisplayName,
                    callingCard = activeCallingCard,
                    isLoading = isLoading,
                    onOpenSetup = { currentSubView = AccountScreenSubView.SETUP },
                    onEditCallingCard = { currentSubView = AccountScreenSubView.CALLING_CARD_EDITOR },
                    onExitGuestMode = {
                        guestManager.exitGuestSession()
                        cardRepo.clearCurrentSession()
                        activeCallingCard = null
                    }
                )
            } else {
                // ==========================================
                // SIGNED OUT STATE
                // Choice: Continue with Email OR Continue as Guest
                // ==========================================
                SignedOutView(
                    onContinueWithEmail = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Email Sign-In — Coming Soon")
                        }
                    },
                    onContinueAsGuest = {
                        guestNameInput = "Guest User"
                        showGuestNameDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Information note about offline capability
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Offline-First Calling",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "R Dialer does not require an online account to perform core phone functions. Calling Card identity data is maintained safely on your device.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Guest Mode Name Choice Dialog (Section 1)
    if (showGuestNameDialog) {
        AlertDialog(
            onDismissRequest = { showGuestNameDialog = false },
            title = {
                Text(
                    text = "Welcome to R Dialer",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose your display name for Guest Mode on this device/emulator:",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = guestNameInput,
                        onValueChange = { guestNameInput = it },
                        singleLine = true,
                        label = { Text("Display Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("guest_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestNameDialog = false
                        val cleanName = guestNameInput.trim().ifBlank { "Guest User" }
                        guestManager.startGuestSession(cleanName)
                        activeCallingCard = cardRepo.loadCardForOwner(
                            CallingCardRepository.GUEST_OWNER_ID,
                            cleanName,
                            null
                        )
                        // Directly open Account Setup flow as specified
                        currentSubView = AccountScreenSubView.SETUP
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("guest_continue_button")
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestNameDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}



/**
 * Guest Mode Profile View (Section 1)
 */
@Composable
private fun GuestUserView(
    displayName: String,
    callingCard: CallingCardData?,
    isLoading: Boolean,
    onOpenSetup: () -> Unit,
    onEditCallingCard: () -> Unit,
    onExitGuestMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guest_mode_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Guest Avatar
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7))
                .border(3.dp, Color(0xFFF59E0B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val customPhoto = callingCard?.effectivePhotoUrl
            if (!customPhoto.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(customPhoto),
                    contentDescription = "Guest Profile Photo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "G",
                    color = Color(0xFFB45309),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = callingCard?.displayName ?: displayName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("guest_display_name")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Clearly display "Guest Mode" in account area (Section 1 requirement)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFEF3C7))
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
                .testTag("guest_mode_status_badge")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Guest Mode (Local Emulator Session)",
                    color = Color(0xFF92400E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Calling Settings Section
        CallingSettingsSection(
            callingCard = callingCard,
            onEditCallingCard = onEditCallingCard
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Exit Guest Mode
        OutlinedButton(
            onClick = onExitGuestMode,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("exit_guest_mode_button")
        ) {
            Text("Exit Guest Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Section 3: Calling Settings -> Calling Card
 */
@Composable
private fun CallingSettingsSection(
    callingCard: CallingCardData?,
    onEditCallingCard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calling_settings_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calling Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Button(
                    onClick = onEditCallingCard,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_calling_card_button")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Calling Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Calling Card",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your personal modern identity card during supported calling experiences.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current Calling Card Preview (Section 3)
            if (callingCard != null) {
                CallingCardPreviewCard(card = callingCard)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tap Edit Calling Card to create your card", fontSize = 13.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}

/**
 * Signed-out view offering:
 * - "Continue with Email" (Coming Soon)
 * - "Continue as Guest"
 */
@Composable
private fun SignedOutView(
    onContinueWithEmail: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signed_out_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // User Avatar Placeholder
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                    )
                )
                .border(2.dp, Color(0xFFBFDBFE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Account Avatar Placeholder",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose how to proceed to access your profile, Calling Settings, and Calling Card.",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 1. "Continue with Email" Button (Coming Soon)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .clickable(onClick = onContinueWithEmail)
                .testTag("continue_with_email_button"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Continue with Email",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.testTag("continue_with_email_title")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Coming Soon",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.testTag("continue_with_email_coming_soon")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. "Continue as Guest" Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .clickable(onClick = onContinueAsGuest)
                .testTag("continue_as_guest_button"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Continue as Guest",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Local emulator session",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Concise Trust Message
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Privacy & Local Separation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "R Dialer operates locally on your device. Calling Card identity and settings are stored securely on device.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
