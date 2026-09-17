package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = RDialerBlue,
    onPrimary = Color.White,
    primaryContainer = RDialerLightBlue,
    onPrimaryContainer = RDialerDarkBlue,
    secondary = RDialerSlateMedium,
    onSecondary = Color.White,
    background = RDialerWhite,
    onBackground = RDialerSlateDark,
    surface = RDialerWhite,
    onSurface = RDialerSlateDark,
    surfaceVariant = RDialerSlateLight,
    onSurfaceVariant = RDialerSlateMedium,
    error = RDialerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = RDialerBlue,
    onPrimary = Color.White,
    secondary = RDialerSlateMedium,
    onSecondary = Color.White,
    background = RDialerSlateDark,
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = RDialerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep clean brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
