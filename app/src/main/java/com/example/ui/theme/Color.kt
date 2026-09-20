package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Official Default Palette for Belgian Hockey App
val HockeyDarkBg = Color(0xFF051F30)
val HockeySurface = Color(0xFF0A2E45)
val HockeySurfaceVariant = Color(0xFF0E3854)
val HockeyBorder = Color(0xFF1B506F)
val HockeyAccentDefault = Color(0xFF60B6E7)
val HockeyTextPrimary = Color(0xFFEFF7FC)
val HockeyTextSecondary = Color(0xFF8CAFC6)

// Match Results Colors
val HockeyWin = Color(0xFF5BD6A0)
val HockeyDraw = Color(0xFFE5C266)
val HockeyDefeat = Color(0xFFF08585)

// Field Pitch & Indicator
val HockeyTurfGreen = Color(0xFF1A6B48)
val HockeyPenaltyYellow = Color(0xFFFFD166)

// 12 Presets for Belgian Hockey Clubs
val ClubColorPresets = listOf(
    Color(0xFF60B6E7), // Sky Blue (e.g. Wellington, Waterloo Ducks)
    Color(0xFF003865), // Deep Navy (e.g. Racing, Dragons)
    Color(0xFFD90429), // Crimson / Red (e.g. Léopold, Antwerp)
    Color(0xFF008037), // Emerald Green (e.g. Louvain-la-Neuve, Baudouin)
    Color(0xFFFF9E00), // Amber Orange (e.g. Oranje-Rood, Braxgata)
    Color(0xFFFFD166), // Golden Yellow (e.g. Herakles, Gantoise)
    Color(0xFF7209B7), // Royal Purple (e.g. Indiana)
    Color(0xFF2A9D8F), // Teal (e.g. Namur)
    Color(0xFFE63946), // Bright Scarlet (e.g. White Star)
    Color(0xFF1D3557), // Prussian Blue (e.g. Uccle Sport)
    Color(0xFF6A994E), // Olive Green (e.g. Mechelse)
    Color(0xFFF4F1DE)  // Off White / Cream (e.g. Beerschot)
)

/**
 * Calculates luminance and returns dark or white text color for optimal readability outdoors.
 * Formula: 0.299*R + 0.587*G + 0.114*B
 */
fun getContrastingTextColor(backgroundColor: Color): Color {
    val argb = backgroundColor.toArgb()
    val r = (argb shr 16 and 0xFF) / 255.0
    val g = (argb shr 8 and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return if (luminance > 0.55) HockeyDarkBg else Color.White
}

fun Color.toHex(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

fun colorFromHex(hex: String?, defaultColor: Color = Color(0xFFD90429)): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        defaultColor
    }
}

