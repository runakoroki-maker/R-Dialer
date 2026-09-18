package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import android.telecom.Call
import com.example.ui.components.IncomingCallCard
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.CallManager
import com.example.telecom.CallNotificationManager
import com.example.telecom.TelecomHelper
import com.example.ui.account.AccountScreen
import com.example.ui.call.InCallActivity
import com.example.ui.contacts.ContactsScreen
import com.example.ui.contacts.ContactsViewModel
import com.example.ui.dialer.DialerScreen
import com.example.ui.dialer.DialerViewModel
import com.example.ui.privacy.PrivacyPolicyScreen
import com.example.ui.recents.RecentsScreen
import com.example.ui.recents.RecentsViewModel
import com.example.ui.recordings.RecordingsScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.DeviceEnvironmentDialog
import com.example.ui.components.EmulatorLoginDetectedDialog
import com.example.util.EmulatorDetector

enum class SubScreen {
    ACCOUNT,
    PRIVACY_POLICY
}

enum class DialerNavTab(val title: String) {
    DIALER("Dialer"),
    RECENTS("Recents"),
    CONTACTS("Contacts"),
    RECORDINGS("Recordings")
}

class MainActivity : ComponentActivity() {

    private val dialerViewModel: DialerViewModel by viewModels()
    private val recentsViewModel: RecentsViewModel by viewModels()
    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmulatorDetector.refresh(this)
        enableEdgeToEdge()

        CallNotificationManager.createNotificationChannels(this)
        handleIntent(intent)

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                MainAppScreen(
                    dialerViewModel = dialerViewModel,
                    recentsViewModel = recentsViewModel,
                    contactsViewModel = contactsViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EmulatorDetector.refresh(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data: Uri? = intent.data
        if (data != null && data.scheme == "tel") {
            val number = data.schemeSpecificPart
            if (!number.isNullOrBlank()) {
                dialerViewModel.setNumber(number)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    dialerViewModel: DialerViewModel,
    recentsViewModel: RecentsViewModel,
    contactsViewModel: ContactsViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DialerNavTab.DIALER) }

    var activeSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = activeSubScreen != null) {
        activeSubScreen = null
    }

    if (activeSubScreen == SubScreen.ACCOUNT) {
        AccountScreen(onBack = { activeSubScreen = null })
        return
    }

    if (activeSubScreen == SubScreen.PRIVACY_POLICY) {
        PrivacyPolicyScreen(onBack = { activeSubScreen = null })
        return
    }

    val environmentInfo by EmulatorDetector.environmentInfo.collectAsState()
    var hasDismissedEmulatorDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    if (environmentInfo.isEmulator && !hasDismissedEmulatorDialog) {
        EmulatorLoginDetectedDialog(
            onDismiss = { hasDismissedEmulatorDialog = true },
            onViewDiagnostics = {
                hasDismissedEmulatorDialog = true
                showDiagnosticsDialog = true
            }
        )
    }

    if (showDiagnosticsDialog) {
        DeviceEnvironmentDialog(
            environmentInfo = environmentInfo,
            onDismiss = { showDiagnosticsDialog = false },
            onSimulateIncomingCall = {
                CallManager.simulateDemoIncomingCall(context)
            }
        )
    }

    // Active ongoing call observation for floating status banner
    val activeCallState by CallManager.callState.collectAsState()

    // Multiple permissions launcher for Contacts, Call Log, and Phone
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.READ_CONTACTS] == true) {
            contactsViewModel.onPermissionResult(true)
        }
        if (results[Manifest.permission.READ_CALL_LOG] == true) {
            recentsViewModel.onPermissionResult(true)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF8FAFC),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == DialerNavTab.DIALER,
                    onClick = { currentTab = DialerNavTab.DIALER },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Dialer",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Dialer", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2563EB),
                        selectedTextColor = Color(0xFF2563EB),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_dialer")
                )

                NavigationBarItem(
                    selected = currentTab == DialerNavTab.RECENTS,
                    onClick = {
                        currentTab = DialerNavTab.RECENTS
                        recentsViewModel.refresh()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recents",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Recents", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2563EB),
                        selectedTextColor = Color(0xFF2563EB),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_recents")
                )

                NavigationBarItem(
                    selected = currentTab == DialerNavTab.CONTACTS,
                    onClick = {
                        currentTab = DialerNavTab.CONTACTS
                        contactsViewModel.refresh()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Contacts",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Contacts", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2563EB),
                        selectedTextColor = Color(0xFF2563EB),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_contacts")
                )

                NavigationBarItem(
                    selected = currentTab == DialerNavTab.RECORDINGS,
                    onClick = {
                        currentTab = DialerNavTab.RECORDINGS
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Recordings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Recordings", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2563EB),
                        selectedTextColor = Color(0xFF2563EB),
                        indicatorColor = Color(0xFFDBEAFE),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_tab_recordings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // App Identity Header
            AppHeader(
                activeTab = currentTab,
                isEmulator = environmentInfo.isEmulator,
                onOpenAccount = { activeSubScreen = SubScreen.ACCOUNT },
                onOpenPrivacy = { activeSubScreen = SubScreen.PRIVACY_POLICY },
                onOpenAbout = { showAboutDialog = true },
                onOpenDiagnostics = { showDiagnosticsDialog = true }
            )

            // Persistent Demo Mode indicator bar if in emulator
            AnimatedVisibility(
                visible = environmentInfo.isEmulator,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                        .clickable { showDiagnosticsDialog = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("demo_mode_emulator_indicator")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEMO MODE — Emulator",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                letterSpacing = 0.3.sp
                            )
                        }
                        Text(
                            text = "Diagnostics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            // Incoming Call Popup if incoming call arrives while user is inside R Dialer
            AnimatedVisibility(
                visible = activeCallState != null && activeCallState?.telecomState == Call.STATE_RINGING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activeCallState?.let { call ->
                    IncomingCallCard(
                        callState = call,
                        onAnswer = {
                            CallManager.answer()
                            val intent = Intent(context, InCallActivity::class.java).apply {
                                action = InCallActivity.ACTION_IN_CALL
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            context.startActivity(intent)
                        },
                        onDecline = {
                            CallManager.disconnect()
                        },
                        onOpenFullScreen = {
                            val intent = Intent(context, InCallActivity::class.java).apply {
                                action = InCallActivity.ACTION_IN_CALL
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Active in-call return banner if call is currently active/connected in background
            AnimatedVisibility(
                visible = activeCallState != null && activeCallState?.telecomState != Call.STATE_RINGING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                activeCallState?.let { call ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                val intent = Intent(context, InCallActivity::class.java).apply {
                                    action = InCallActivity.ACTION_IN_CALL
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                            }
                            .testTag("active_call_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Call in progress • ${call.displayTitle}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = TelecomHelper.formatDuration(call.durationSeconds),
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                            Text(
                                text = "Return",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            // Tab contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentTab) {
                    DialerNavTab.DIALER -> {
                        DialerScreen(
                            viewModel = dialerViewModel,
                            onRequestCallPermission = {
                                permissionsLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                            }
                        )
                    }
                    DialerNavTab.RECENTS -> {
                        RecentsScreen(viewModel = recentsViewModel)
                    }
                    DialerNavTab.CONTACTS -> {
                        ContactsScreen(
                            viewModel = contactsViewModel,
                            onContactSelected = { number ->
                                dialerViewModel.setNumber(number)
                                currentTab = DialerNavTab.DIALER
                            }
                        )
                    }
                    DialerNavTab.RECORDINGS -> {
                        RecordingsScreen(
                            onCallClick = { number ->
                                dialerViewModel.setNumber(number)
                                currentTab = DialerNavTab.DIALER
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "R Dialer",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Modern calling, made simple.",
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "© R Dialer",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.testTag("about_dialog_close")
                ) {
                    Text("Close", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun AppHeader(
    activeTab: DialerNavTab,
    modifier: Modifier = Modifier,
    isEmulator: Boolean = false,
    onOpenAccount: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Visual Identity: The stylized "R" badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "R Dialer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = activeTab.title,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right side: Emulator badge + 3-Dot Overflow Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEmulator) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenDiagnostics)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("emulator_header_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emulator Mode Active",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EMULATOR",
                            color = Color(0xFF92400E),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("main_menu_overflow_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .width(200.dp)
                ) {
                    // 1. Account
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Account",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenAccount()
                        },
                        modifier = Modifier.testTag("menu_account")
                    )

                    // 2. Privacy Policy
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Privacy Policy",
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenPrivacy()
                        },
                        modifier = Modifier.testTag("menu_privacy")
                    )

                    // 3. About
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "About",
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenAbout()
                        },
                        modifier = Modifier.testTag("menu_about")
                    )

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 4. Version
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenAbout()
                        },
                        modifier = Modifier.testTag("menu_version")
                    )
                }
            }
        }
    }
}
