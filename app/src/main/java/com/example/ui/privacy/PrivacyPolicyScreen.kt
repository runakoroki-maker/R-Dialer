package com.example.ui.privacy

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * In-app Privacy Policy screen providing comprehensive transparency on permissions,
 * data storage, calling functionality, and planned account authentication.
 */
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_policy_screen"),
        containerColor = Color.White,
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
                        .testTag("privacy_back_button")
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
                    text = "Privacy Policy",
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Document Title & Last Updated
            Text(
                text = "R Dialer Privacy Policy",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last Updated: September 18, 2026",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Trust Statement Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7))
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
                        tint = Color(0xFF16A34A),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your privacy matters to us. R Dialer is designed to request only the permissions required for its features and to handle your information responsibly. We aim to keep data collection transparent and give you control over the information you choose to provide.",
                        fontSize = 13.sp,
                        color = Color(0xFF166534),
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Introduction
            PolicySection(
                title = "1. Introduction",
                content = "R Dialer is an Android phone and calling application designed to provide a modern, reliable, and fluid communication experience. This Privacy Policy details the types of device permissions R Dialer requests, how information is handled, and how user privacy is maintained throughout your use of the application."
            )

            // 2. Permissions
            PolicySection(
                title = "2. Permissions & Why We Need Them",
                content = "R Dialer requests only standard Android permissions necessary to perform user-initiated telephone tasks:\n\n" +
                        "• Phone & Calling (CALL_PHONE, MANAGE_OWN_CALLS): Required to initiate and route telephone calls through your mobile carrier or standard Android Telecom subsystem.\n\n" +
                        "• Contacts (READ_CONTACTS): Used to display your address book, resolve names during incoming or outgoing calls, and enable quick search in the dialer after permission is granted.\n\n" +
                        "• Call History (READ_CALL_LOG, WRITE_CALL_LOG): Enables the Recents tab to display recent incoming, outgoing, and missed calls with accurate timestamps and durations.\n\n" +
                        "• Microphone (RECORD_AUDIO): Required for voice transmission during active telephone calls and for optional local call recording when initiated by the user.\n\n" +
                        "• Camera (CAMERA): Requested only if you activate optional video call preview functionality during supported communications.\n\n" +
                        "• Notifications (POST_NOTIFICATIONS): Required on Android 13+ to display active call status banners and incoming call alerts."
            )

            // 3. Phone & Calling Functionality
            PolicySection(
                title = "3. Phone & Calling Functionality",
                content = "All call setup, audio streaming, dual-SIM selection, and in-call controls (such as mute, speakerphone, hold, and conference merging) are processed through your device's native hardware and your mobile network carrier. R Dialer does not intercept, monitor, or divert your telephone calls to private third-party servers."
            )

            // 4. Call Recording
            PolicySection(
                title = "4. Call Recording",
                content = "When local call recording is activated by the user where supported by the device and local regulations, audio recordings are stored strictly on your device's app-specific external storage directory (/Music/Recordings). Recordings are kept strictly local, are never automatically uploaded to any cloud server, and can be listened to or deleted at any time directly within the Recordings tab."
            )

            // 5. Data Storage
            PolicySection(
                title = "5. Data Storage & Local Processing",
                content = "R Dialer does not require an online account to perform its core phone/dialer functions. Information used for core device functionality—including contacts, call history logs, dialed numbers, and preferences—is processed strictly on your device according to Android's sandboxed storage mechanisms. R Dialer does not transmit your personal contacts or call logs to external servers."
            )

            // 6. Account & Google Sign-In
            PolicySection(
                title = "6. Account & Google Sign-In",
                content = "R Dialer supports optional Google Sign-In powered by Firebase Authentication using Google's official identity flow. R Dialer never requests, accesses, or stores your Google password or OTP credentials. When you choose to sign in, authentication is securely processed directly by Google and Firebase. Basic profile details (such as your display name, email address, and profile photo) are displayed in your account settings. Core calling, dialer, and contact features continue to operate completely offline without an account, and you may sign out at any time."
            )

            // 7. Data Security
            PolicySection(
                title = "7. Data Security",
                content = "We take reasonable and industry-standard technical measures to protect your information through Android's application sandboxing and permission security models. R Dialer does not sell, trade, or rent your personal information to third parties, advertising brokers, or data analytics firms. While no system can guarantee absolute invulnerability, we design our software with privacy and least-privilege principles by default."
            )

            // 8. User Controls & Choices
            PolicySection(
                title = "8. User Controls & Rights",
                content = "You retain full control over the permissions granted to R Dialer at all times:\n\n" +
                        "• System Permissions: You may grant or revoke Contacts, Phone, Microphone, or Storage permissions at any time via Android Settings > Apps > R Dialer > Permissions.\n\n" +
                        "• Local Data Management: You can clear call history logs, delete individual or all call recordings, and remove application cache via device settings."
            )

            // 9. Contact & Support
            PolicySection(
                title = "9. Contact Information",
                content = "If you have questions, feedback, or concerns regarding this Privacy Policy or R Dialer's data practices, you may reach out through the official project repository or contact support via:\n\n" +
                        "Email: support@rdialer.app\n" +
                        "Application ID: com.aistudio.rdialer.mbfx"
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = Color(0xFFE2E8F0))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "© R Dialer. All rights reserved.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            color = Color(0xFF475569),
            lineHeight = 19.sp
        )
    }
}
