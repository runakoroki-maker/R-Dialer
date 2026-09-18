package com.example.ui.callingcard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.callingcard.CallingCardBackground
import com.example.data.callingcard.CallingCardData
import com.example.data.callingcard.CallingCardStyleType
import com.example.data.callingcard.CallingCardTypography

/**
 * Modern Calling Card Preview component.
 * Supports:
 * - Style A: Profile Calling Card (Photo, Name, Background, Effects)
 * - Style B: Name + Background Calling Card (Large display name typography, custom colors, background)
 * - Live real-time updates as user edits any parameter
 */
@Composable
fun CallingCardPreviewCard(
    card: CallingCardData,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false
) {
    val isDarkBg = card.background.isDark
    val effectiveNameColor = card.nameColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .shadow(
                elevation = if (card.visualEffectsEnabled) 12.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = if (isDarkBg) Color(0x33000000) else Color(0x1A2563EB)
            )
            .testTag("calling_card_preview_container"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(card.background.asBrush())
                .then(
                    if (card.customBackgroundImageUri != null) {
                        Modifier
                    } else {
                        Modifier
                    }
                )
        ) {
            // Custom Background Image if selected
            if (!card.customBackgroundImageUri.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(card.customBackgroundImageUri),
                    contentDescription = "Custom Calling Card Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Soft gradient overlay to ensure text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )
            }

            // Glassmorphic Frost & Border overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = if (isDarkBg) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            // Top Header: R Dialer Brand Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isDarkBg) Color.White.copy(alpha = 0.2f) else Color(0xFF2563EB).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = if (isDarkBg) Color.White else Color(0xFF2563EB),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "R DIALER IDENTITY",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (isDarkBg) Color.White.copy(alpha = 0.75f) else Color(0xFF475569),
                        letterSpacing = 1.2.sp
                    )
                }

                if (card.visualEffectsEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkBg) Color.White.copy(alpha = 0.15f) else Color(0xFF2563EB).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isDarkBg) Color(0xFFFDE047) else Color(0xFF2563EB),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VERIFIED",
                                fontSize = 9.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                color = if (isDarkBg) Color.White else Color(0xFF1D4ED8),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // Card Body: Either Style A (Profile) or Style B (Name + Background)
            when (card.styleType) {
                CallingCardStyleType.PROFILE_CARD -> {
                    ProfileStyleBody(
                        card = card,
                        isDarkBg = isDarkBg,
                        effectiveNameColor = effectiveNameColor
                    )
                }
                CallingCardStyleType.NAME_BACKGROUND_CARD -> {
                    NameBackgroundStyleBody(
                        card = card,
                        isDarkBg = isDarkBg,
                        effectiveNameColor = effectiveNameColor
                    )
                }
            }
        }
    }
}

/**
 * Style A Body: Profile Calling Card (photo avatar + name + effects)
 */
@Composable
private fun ProfileStyleBody(
    card: CallingCardData,
    isDarkBg: Boolean,
    effectiveNameColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 44.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Profile Picture Avatar
        val photoUrl = card.effectivePhotoUrl
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkBg) Color.White.copy(alpha = 0.12f) else Color(0xFFEFF6FF)
                )
                .border(
                    width = 3.dp,
                    color = if (isDarkBg) Color.White.copy(alpha = 0.4f) else Color(0xFF3B82F6),
                    shape = CircleShape
                )
                .shadow(elevation = 6.dp, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(photoUrl),
                    contentDescription = "Calling Card Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .testTag("calling_card_preview_avatar"),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.displayName.firstOrNull()?.uppercase() ?: "R",
                        fontSize = 32.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Customized Name
        Text(
            text = card.displayName.ifBlank { "Your Name" },
            fontSize = card.fontSizeSp.sp,
            fontFamily = card.typography.fontFamily,
            fontWeight = card.typography.fontWeight,
            fontStyle = card.typography.fontStyle,
            letterSpacing = card.typography.letterSpacingSp.sp,
            color = effectiveNameColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calling_card_preview_name")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Calling Card • R Dialer",
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = if (isDarkBg) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
        )
    }
}

/**
 * Style B Body: Name + Background Calling Card (Large display name emphasis, no photo required)
 */
@Composable
private fun NameBackgroundStyleBody(
    card: CallingCardData,
    isDarkBg: Boolean,
    effectiveNameColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Subtle visual monogram badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDarkBg) Color.White.copy(alpha = 0.15f) else Color(0xFF2563EB).copy(alpha = 0.12f)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "PERSONAL CALLING CARD",
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = if (isDarkBg) Color.White.copy(alpha = 0.9f) else Color(0xFF1D4ED8),
                letterSpacing = 1.4.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Display Typography
        Text(
            text = card.displayName.ifBlank { "Your Name" },
            fontSize = (card.fontSizeSp + 6f).sp,
            fontFamily = card.typography.fontFamily,
            fontWeight = card.typography.fontWeight,
            fontStyle = card.typography.fontStyle,
            letterSpacing = card.typography.letterSpacingSp.sp,
            color = effectiveNameColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = (card.fontSizeSp + 10f).sp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calling_card_preview_name")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Modern Identity Style",
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = if (isDarkBg) Color.White.copy(alpha = 0.75f) else Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )
    }
}
