package com.levelup.gym.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GameColors {
    val Background = Color(0xFF0F1117)
    val Surface = Color(0xFF181D2A)
    val SurfaceHigh = Color(0xFF222839)
    val Gold = Color(0xFFF5C451)
    val Xp = Color(0xFF7CF29C)
    val Arcane = Color(0xFFB388FF)
    val Ember = Color(0xFFFF8A65)
    val Muted = Color(0xFF8A93A8)
    val Track = Color(0xFF2C3346)
}

private val scheme = darkColorScheme(
    primary = GameColors.Gold,
    onPrimary = Color(0xFF231A00),
    secondary = GameColors.Arcane,
    onSecondary = Color(0xFF1E0F3A),
    tertiary = GameColors.Xp,
    onTertiary = Color(0xFF00210E),
    background = GameColors.Background,
    onBackground = Color(0xFFE7EAF2),
    surface = GameColors.Surface,
    onSurface = Color(0xFFE7EAF2),
    surfaceVariant = GameColors.SurfaceHigh,
    onSurfaceVariant = GameColors.Muted,
    surfaceContainer = GameColors.Surface,
    surfaceContainerHigh = GameColors.SurfaceHigh,
    surfaceContainerHighest = GameColors.SurfaceHigh,
    surfaceContainerLow = GameColors.Surface,
    outline = Color(0xFF3A4258),
    error = Color(0xFFFF6B6B),
)

private val base = Typography()
private val typography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp),
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
