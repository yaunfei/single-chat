package com.yaunfei.singlechat.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF4F46E5)
private val IndigoDark = Color(0xFF3730A3)
private val IndigoLight = Color(0xFFE0E7FF)
private val Teal = Color(0xFF0F766E)
private val TealLight = Color(0xFFCCFBF1)
private val Amber = Color(0xFFB45309)

val LightColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealLight,
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF16161D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16161D),
    surfaceVariant = Color(0xFFECECF4),
    onSurfaceVariant = Color(0xFF5A5A6B),
    outline = Color(0xFFD4D4E0),
    outlineVariant = Color(0xFFE7E7F0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2F2F3A),
    inverseOnSurface = Color(0xFFF4F4FA),
    inversePrimary = Color(0xFFC7D2FE),
    scrim = Color.Black,
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoLight,
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = TealLight,
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFFF7D6),
    background = Color(0xFF0E0E12),
    onBackground = Color(0xFFE8E8F0),
    surface = Color(0xFF17171E),
    onSurface = Color(0xFFE8E8F0),
    surfaceVariant = Color(0xFF262633),
    onSurfaceVariant = Color(0xFFB9B9C8),
    outline = Color(0xFF4A4A5A),
    outlineVariant = Color(0xFF2E2E3C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE8E8F0),
    inverseOnSurface = Color(0xFF2F2F3A),
    inversePrimary = Indigo,
    scrim = Color.Black,
)
