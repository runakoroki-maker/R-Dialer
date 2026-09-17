package com.example.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KeypadButtonData(
    val digit: String,
    val letters: String,
    val dtmfTone: Int
)

val KeypadRows = listOf(
    listOf(
        KeypadButtonData("1", "", ToneGenerator.TONE_DTMF_1),
        KeypadButtonData("2", "ABC", ToneGenerator.TONE_DTMF_2),
        KeypadButtonData("3", "DEF", ToneGenerator.TONE_DTMF_3)
    ),
    listOf(
        KeypadButtonData("4", "GHI", ToneGenerator.TONE_DTMF_4),
        KeypadButtonData("5", "JKL", ToneGenerator.TONE_DTMF_5),
        KeypadButtonData("6", "MNO", ToneGenerator.TONE_DTMF_6)
    ),
    listOf(
        KeypadButtonData("7", "PQRS", ToneGenerator.TONE_DTMF_7),
        KeypadButtonData("8", "TUV", ToneGenerator.TONE_DTMF_8),
        KeypadButtonData("9", "WXYZ", ToneGenerator.TONE_DTMF_9)
    ),
    listOf(
        KeypadButtonData("*", "", ToneGenerator.TONE_DTMF_S),
        KeypadButtonData("0", "+", ToneGenerator.TONE_DTMF_0),
        KeypadButtonData("#", "", ToneGenerator.TONE_DTMF_P)
    )
)

@Composable
fun DialerKeypad(
    onDigitClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_VOICE_CALL, 50)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
        }
    }

    fun playToneAndVibrate(tone: Int) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        try {
            toneGenerator?.startTone(tone, 120)
        } catch (e: Exception) {
            // Ignore if tone generation fails
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { button ->
                    KeypadButton(
                        data = button,
                        onClick = {
                            playToneAndVibrate(button.dtmfTone)
                            onDigitClick(button.digit)
                        },
                        onLongClick = if (button.digit == "0") {
                            {
                                playToneAndVibrate(button.dtmfTone)
                                onDigitClick("+")
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    data: KeypadButtonData,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFFF1F5F9)) // Clean light subtle neutral button surface
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_button_${data.digit}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = data.digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A), // Dark slate navy
                lineHeight = 30.sp
            )
            if (data.letters.isNotEmpty()) {
                Text(
                    text = data.letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = Color(0xFF64748B) // Slate secondary
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}
