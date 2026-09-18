package com.example.ui.callingcard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.callingcard.CallingCardBackground
import com.example.data.callingcard.CallingCardColorPreset
import com.example.data.callingcard.CallingCardData
import com.example.data.callingcard.CallingCardStyleType
import com.example.data.callingcard.CallingCardTypography
import kotlinx.coroutines.launch

/**
 * Full Calling Card Customization & Live Editor Screen.
 *
 * Implements:
 * - Section 4: Style A (Profile) vs Style B (Name + Background)
 * - Section 5: Built-in licensed font styles with immediate live preview
 * - Section 6: Name color customization (presets, light, dark, custom)
 * - Section 7: Built-in backgrounds (10 premium themes) & custom background gallery picker
 * - Section 8: Profile picture controls (Change, Remove, Restore Google photo)
 * - Section 9: Live calling card preview updating immediately with Save and Cancel actions
 * - Section 10: White glassmorphism aesthetic matching R Dialer design language
 */
@Composable
fun CallingCardCustomizationScreen(
    initialCard: CallingCardData,
    onSave: (CallingCardData) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workingCard by remember(initialCard) { mutableStateOf(initialCard) }
    val scrollState = rememberScrollState()

    // Photo Picker launcher for Profile Picture
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            workingCard = workingCard.copy(
                profilePictureUri = uri.toString(),
                isProfilePictureRemoved = false
            )
        }
    }

    // Photo Picker launcher for Custom Card Background
    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            workingCard = workingCard.copy(
                customBackgroundImageUri = uri.toString()
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("calling_card_customization_screen"),
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
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calling_card_editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel and Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Customize Calling Card",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Real-time preview & styling",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        bottomBar = {
            // Save and Cancel Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_calling_card_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onSave(workingCard) },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("save_calling_card_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Calling Card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // =========================================================
            // 1. LIVE PREVIEW (Section 9)
            // =========================================================
            Text(
                text = "LIVE PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CallingCardPreviewCard(card = workingCard)

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================
            // 2. STYLE SELECTOR (Style A vs Style B - Section 4)
            // =========================================================
            SectionTitle(title = "Calling Card Style", icon = Icons.Default.Style)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CallingCardStyleType.entries.forEach { styleType ->
                    val isSelected = workingCard.styleType == styleType
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                workingCard = workingCard.copy(styleType = styleType)
                            }
                            .testTag("style_selector_${styleType.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = styleType.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (styleType == CallingCardStyleType.PROFILE_CARD) "Name + Photo" else "Bold Name Only",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================
            // 3. NAME & DISPLAY NAME (Section 4 & 5)
            // =========================================================
            SectionTitle(title = "Display Name", icon = Icons.Default.TextFields)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = workingCard.displayName,
                onValueChange = { newName ->
                    workingCard = workingCard.copy(displayName = newName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calling_card_name_input"),
                label = { Text("Your Name on Calling Card") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // Font Size Slider
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                Text("${workingCard.fontSizeSp.toInt()} sp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
            }
            Slider(
                value = workingCard.fontSizeSp,
                onValueChange = { workingCard = workingCard.copy(fontSizeSp = it) },
                valueRange = 18f..36f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB)
                ),
                modifier = Modifier.testTag("font_size_slider")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================
            // 4. NAME TYPOGRAPHY (Section 5)
            // Modern, Elegant, Bold, Minimal, Rounded, Futuristic, Handwritten, Luxury, Gaming, Neon, Classic
            // =========================================================
            SectionTitle(title = "Name Typography", icon = Icons.Default.FormatSize)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CallingCardTypography.entries) { fontOption ->
                    val isSelected = workingCard.typography == fontOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                workingCard = workingCard.copy(typography = fontOption)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("typography_${fontOption.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fontOption.label,
                            fontFamily = fontOption.fontFamily,
                            fontWeight = fontOption.fontWeight,
                            fontStyle = fontOption.fontStyle,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Color(0xFF1E293B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================
            // 5. NAME COLOR (Section 6)
            // Preset colors, Light, Dark, Custom
            // =========================================================
            SectionTitle(title = "Name Color", icon = Icons.Default.ColorLens)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CallingCardColorPreset.PRESETS) { preset ->
                    val isSelected = workingCard.nameColorLong == preset.hexColor
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable {
                                workingCard = workingCard.copy(nameColorLong = preset.hexColor)
                            }
                            .testTag("color_preset_${preset.label.replace(" ", "_").lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (preset.isLight) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================
            // 6. BACKGROUNDS (Section 7)
            // 10 Built-in themes + Custom Background from gallery
            // =========================================================
            SectionTitle(title = "Background Design", icon = Icons.Default.FormatPaint)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CallingCardBackground.entries) { bgTheme ->
                    val isSelected = workingCard.background == bgTheme && workingCard.customBackgroundImageUri == null
                    Card(
                        modifier = Modifier
                            .size(width = 110.dp, height = 70.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                workingCard = workingCard.copy(
                                    background = bgTheme,
                                    customBackgroundImageUri = null
                                )
                            }
                            .testTag("background_theme_${bgTheme.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgTheme.asBrush())
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = bgTheme.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bgTheme.isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Custom Background gallery button
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    backgroundPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("choose_custom_background_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (workingCard.customBackgroundImageUri != null) "Change Custom Background" else "Select Image from Gallery",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (workingCard.customBackgroundImageUri != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Remove Custom Background",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                workingCard = workingCard.copy(customBackgroundImageUri = null)
                            }
                            .padding(4.dp)
                    )
                }
            }

            // =========================================================
            // 7. PROFILE PICTURE CONTROLS (Section 8 - For Style A)
            // =========================================================
            if (workingCard.styleType == CallingCardStyleType.PROFILE_CARD) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Profile Picture", icon = Icons.Default.AddPhotoAlternate)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Change Picture Button
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("change_profile_picture_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Change Picture", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Remove Picture Button
                            OutlinedButton(
                                onClick = {
                                    workingCard = workingCard.copy(
                                        profilePictureUri = null,
                                        isProfilePictureRemoved = true
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("remove_profile_picture_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                            ) {
                                Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Restore Google profile picture option if original was available
                        if (!workingCard.originalGooglePhotoUrl.isNullOrBlank() &&
                            (workingCard.isProfilePictureRemoved || workingCard.profilePictureUri != null)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    workingCard = workingCard.copy(
                                        profilePictureUri = null,
                                        isProfilePictureRemoved = false
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("restore_google_photo_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Google Profile Picture", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 8. VISUAL EFFECTS TOGGLE
            // =========================================================
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visual Glow & Elevation Effects",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Subtle modern shadow depth and identity badge",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = workingCard.visualEffectsEnabled,
                        onCheckedChange = { workingCard = workingCard.copy(visualEffectsEnabled = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2563EB)),
                        modifier = Modifier.testTag("visual_effects_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}
