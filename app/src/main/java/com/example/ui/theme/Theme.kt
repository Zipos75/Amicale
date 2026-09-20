package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HockeyDarkColorScheme = darkColorScheme(
    primary = HockeyAccentDefault,
    onPrimary = HockeyDarkBg,
    primaryContainer = HockeySurfaceVariant,
    onPrimaryContainer = HockeyTextPrimary,
    secondary = HockeyAccentDefault,
    onSecondary = HockeyDarkBg,
    secondaryContainer = HockeySurface,
    onSecondaryContainer = HockeyTextPrimary,
    tertiary = HockeyWin,
    onTertiary = HockeyDarkBg,
    background = HockeyDarkBg,
    onBackground = HockeyTextPrimary,
    surface = HockeySurface,
    onSurface = HockeyTextPrimary,
    surfaceVariant = HockeySurfaceVariant,
    onSurfaceVariant = HockeyTextSecondary,
    outline = HockeyBorder,
    outlineVariant = HockeyBorder
)

@Composable
fun HockeyTheme(
    customAccentColor: Color = HockeyAccentDefault,
    content: @Composable () -> Unit
) {
    val onCustomAccent = getContrastingTextColor(customAccentColor)
    val colorScheme = HockeyDarkColorScheme.copy(
        primary = customAccentColor,
        onPrimary = onCustomAccent
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
