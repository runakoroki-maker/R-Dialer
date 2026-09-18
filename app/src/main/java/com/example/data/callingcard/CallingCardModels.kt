package com.example.data.callingcard

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Two primary Calling Card styles specified by product design.
 * Style A: Profile Calling Card (with photo, name, background, effects)
 * Style B: Name + Background Calling Card (focuses on large typography, color, background)
 */
enum class CallingCardStyleType(val label: String, val description: String) {
    PROFILE_CARD(
        label = "Profile Calling Card",
        description = "Shows your name with your profile photo, customizable background and effects."
    ),
    NAME_BACKGROUND_CARD(
        label = "Name + Background",
        description = "Emphasizes large bold typography, rich backgrounds, and custom color accents."
    );

    companion object {
        fun fromString(value: String?): CallingCardStyleType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PROFILE_CARD
        }
    }
}

/**
 * Built-in font styles for name typography:
 * Modern, Elegant, Bold, Minimal, Rounded, Futuristic, Handwritten, Luxury, Gaming, Neon, Classic
 */
enum class CallingCardTypography(
    val label: String,
    val fontFamily: FontFamily,
    val fontWeight: FontWeight,
    val fontStyle: FontStyle = FontStyle.Normal,
    val letterSpacingSp: Float = 0.5f
) {
    MODERN(
        label = "Modern",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacingSp = 0.5f
    ),
    ELEGANT(
        label = "Elegant",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        letterSpacingSp = 1.0f
    ),
    BOLD(
        label = "Bold",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacingSp = 0.2f
    ),
    MINIMAL(
        label = "Minimal",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        letterSpacingSp = 1.8f
    ),
    ROUNDED(
        label = "Rounded",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        letterSpacingSp = 0.8f
    ),
    FUTURISTIC(
        label = "Futuristic",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacingSp = 2.0f
    ),
    HANDWRITTEN(
        label = "Handwritten",
        fontFamily = FontFamily.Cursive,
        fontWeight = FontWeight.Normal,
        letterSpacingSp = 0.5f
    ),
    LUXURY(
        label = "Luxury",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        letterSpacingSp = 1.5f
    ),
    GAMING(
        label = "Gaming",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        letterSpacingSp = 1.2f
    ),
    NEON(
        label = "Neon",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacingSp = 2.2f
    ),
    CLASSIC(
        label = "Classic",
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        letterSpacingSp = 0.4f
    );

    companion object {
        fun fromString(value: String?): CallingCardTypography {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MODERN
        }
    }
}

/**
 * Built-in premium backgrounds:
 * Soft glass, Gradient, Abstract, Minimal, Dark luxury, Neon, Aurora, Nature-inspired, Geometric, Elegant
 */
enum class CallingCardBackground(
    val label: String,
    val isDark: Boolean,
    val gradientColors: List<Long>,
    val description: String
) {
    SOFT_GLASS(
        label = "Soft Glass",
        isDark = false,
        gradientColors = listOf(0xFFFFFFFF, 0xFFF1F5F9, 0xFFE2E8F0),
        description = "Light frosted surface with subtle reflections"
    ),
    GRADIENT(
        label = "Gradient",
        isDark = true,
        gradientColors = listOf(0xFF2563EB, 0xFF7C3AED),
        description = "Vibrant royal blue into electric violet"
    ),
    ABSTRACT(
        label = "Abstract",
        isDark = true,
        gradientColors = listOf(0xFF0F172A, 0xFF1E293B, 0xFF334155),
        description = "Modern slate depth with fluid contours"
    ),
    MINIMAL(
        label = "Minimal",
        isDark = false,
        gradientColors = listOf(0xFFF8FAFC, 0xFFF1F5F9),
        description = "Pristine neutral clean layout"
    ),
    DARK_LUXURY(
        label = "Dark Luxury",
        isDark = true,
        gradientColors = listOf(0xFF09090B, 0xFF18181B, 0xFF27272A),
        description = "Onyx titanium with gold or platinum shimmer"
    ),
    NEON(
        label = "Neon",
        isDark = true,
        gradientColors = listOf(0xFF050515, 0xFF1E1035, 0xFF0B192C),
        description = "Cyberpunk dark glow with luminescent accents"
    ),
    AURORA(
        label = "Aurora",
        isDark = true,
        gradientColors = listOf(0xFF064E3B, 0xFF0284C7, 0xFF4F46E5),
        description = "Boreal green and arctic sapphire waves"
    ),
    NATURE_INSPIRED(
        label = "Nature",
        isDark = false,
        gradientColors = listOf(0xFFECFDF5, 0xFFD1FAE5, 0xFFA7F3D0),
        description = "Botanical jade and refreshing herbal tones"
    ),
    GEOMETRIC(
        label = "Geometric",
        isDark = true,
        gradientColors = listOf(0xFF1E1B4B, 0xFF312E81, 0xFF4338CA),
        description = "Prismatic indigo architectural facets"
    ),
    ELEGANT(
        label = "Elegant",
        isDark = false,
        gradientColors = listOf(0xFFFFFBEB, 0xFFFEF3C7, 0xFFFDE68A),
        description = "Warm champagne gold and ivory satin"
    );

    fun asBrush(): Brush {
        return Brush.linearGradient(gradientColors.map { Color(it) })
    }

    companion object {
        fun fromString(value: String?): CallingCardBackground {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SOFT_GLASS
        }
    }
}

/**
 * Color presets for the user's name:
 * Preset colors, Light colors, Dark colors, Gradient options
 */
data class CallingCardColorPreset(
    val label: String,
    val hexColor: Long,
    val isLight: Boolean
) {
    val color: Color get() = Color(hexColor)

    companion object {
        val PRESETS = listOf(
            CallingCardColorPreset("Slate Dark", 0xFF0F172A, false),
            CallingCardColorPreset("Pure White", 0xFFFFFFFF, true),
            CallingCardColorPreset("Royal Blue", 0xFF2563EB, false),
            CallingCardColorPreset("Emerald", 0xFF059669, false),
            CallingCardColorPreset("Violet", 0xFF7C3AED, false),
            CallingCardColorPreset("Rose Coral", 0xFFE11D48, false),
            CallingCardColorPreset("Amber Gold", 0xFFD97706, false),
            CallingCardColorPreset("Cyan Ice", 0xFF06B6D4, true),
            CallingCardColorPreset("Neon Lime", 0xFF84CC16, true),
            CallingCardColorPreset("Charcoal", 0xFF334155, false),
            CallingCardColorPreset("Warm Sand", 0xFFF5E6C8, true),
            CallingCardColorPreset("Electric Pink", 0xFFEC4899, false)
        )

        fun findByColor(colorLong: Long): CallingCardColorPreset? {
            return PRESETS.firstOrNull { it.hexColor == colorLong }
        }
    }
}

/**
 * Complete Calling Card Model.
 * Fully separated by account session (Google User UID vs. Guest Mode session).
 */
data class CallingCardData(
    val ownerId: String,                      // Google UID or "guest"
    val displayName: String,                  // Customized name or user's account name
    val styleType: CallingCardStyleType = CallingCardStyleType.PROFILE_CARD,
    val typography: CallingCardTypography = CallingCardTypography.MODERN,
    val nameColorLong: Long = 0xFF0F172A,      // Stored as Long ARGB
    val fontSizeSp: Float = 24f,
    val background: CallingCardBackground = CallingCardBackground.SOFT_GLASS,
    val customBackgroundImageUri: String? = null,
    val profilePictureUri: String? = null,    // User-chosen picture or null
    val originalGooglePhotoUrl: String? = null, // Stored to allow "Restore Google Profile Picture"
    val isProfilePictureRemoved: Boolean = false, // When explicitly removed
    val visualEffectsEnabled: Boolean = true,
    val isSetupCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val nameColor: Color get() = Color(nameColorLong)

    /**
     * Resolves the effective profile picture URI/URL to display.
     * If user explicitly removed photo, returns null.
     * Else returns custom photo if set, or original Google photo.
     */
    val effectivePhotoUrl: String?
        get() {
            if (isProfilePictureRemoved) return null
            return profilePictureUri ?: originalGooglePhotoUrl
        }
}
