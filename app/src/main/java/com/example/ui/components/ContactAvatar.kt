package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

private val AvatarColors = listOf(
    Pair(Color(0xFF2563EB), Color(0xFF1D4ED8)), // Royal Blue
    Pair(Color(0xFF0D9488), Color(0xFF0F766E)), // Teal
    Pair(Color(0xFF7C3AED), Color(0xFF6D28D9)), // Violet
    Pair(Color(0xFFE11D48), Color(0xFFBE123C)), // Rose
    Pair(Color(0xFFD97706), Color(0xFFB45309)), // Amber
    Pair(Color(0xFF0891B2), Color(0xFF0E7490)), // Cyan
    Pair(Color(0xFF4F46E5), Color(0xFF4338CA)), // Indigo
    Pair(Color(0xFF059669), Color(0xFF047857))  // Emerald
)

@Composable
fun ContactAvatar(
    photoUri: String?,
    displayName: String?,
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: TextUnit = (size.value * 0.42f).sp
) {
    val context = LocalContext.current
    val parsedUri = if (!photoUri.isNullOrBlank()) Uri.parse(photoUri) else null

    // Determine deterministic avatar color based on name/initial
    val colorIndex = abs((displayName ?: initial).hashCode()) % AvatarColors.size
    val (color1, color2) = AvatarColors[colorIndex]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color1, color2))),
        contentAlignment = Alignment.Center
    ) {
        if (parsedUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(parsedUri)
                    .crossfade(true)
                    .build(),
                contentDescription = displayName ?: "Contact Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            val char = initial.trim()
            if (char.isNotEmpty() && char != "?") {
                Text(
                    text = char.take(1).uppercase(),
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = displayName ?: "Unknown Contact",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
