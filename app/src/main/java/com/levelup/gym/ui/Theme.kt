package com.levelup.gym.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelup.gym.R

/**
 * "Ironhold" style: a bold, toy-like medieval kingdom.
 * Night-blue sky and stone-blue panels with thick ink outlines and hard shadows,
 * gold for rewards, green for go, chunky rounded type.
 */
object GameColors {
    val Background = Color(0xFF0E1A3A)
    val Surface = Color(0xFF26437F)
    val SurfaceHigh = Color(0xFF31539A)
    val Panel = Color(0xFF2B4A8C)
    val PanelLight = Color(0xFF4069B6)
    val PanelDark = Color(0xFF1B305E)
    val Ink = Color(0xFF0B1226)
    val Gold = Color(0xFFFFC93C)
    val GoldDeep = Color(0xFFE0921B)
    val Xp = Color(0xFF7CE84A)
    val Arcane = Color(0xFFC490FF)
    val Ember = Color(0xFFFF7A45)
    val Ruby = Color(0xFFE8473F)
    val Sky = Color(0xFF59B8FF)
    val Parchment = Color(0xFFF6E7C1)
    val ParchmentDark = Color(0xFFD9BE86)
    val Muted = Color(0xFFB4C4E8)
    val Track = Color(0xFF15234A)
}

private val scheme = darkColorScheme(
    primary = GameColors.Gold,
    onPrimary = Color(0xFF3A2400),
    secondary = GameColors.Arcane,
    onSecondary = Color(0xFF1E0F3A),
    tertiary = GameColors.Xp,
    onTertiary = Color(0xFF0B2A00),
    background = GameColors.Background,
    onBackground = Color.White,
    surface = GameColors.Surface,
    onSurface = Color.White,
    surfaceVariant = GameColors.SurfaceHigh,
    onSurfaceVariant = GameColors.Muted,
    surfaceContainer = GameColors.Surface,
    surfaceContainerHigh = GameColors.SurfaceHigh,
    surfaceContainerHighest = GameColors.SurfaceHigh,
    surfaceContainerLow = GameColors.Surface,
    secondaryContainer = GameColors.PanelLight,
    onSecondaryContainer = Color.White,
    outline = Color(0xFF6F8CC8),
    outlineVariant = Color(0xFF3B5694),
    error = Color(0xFFFF6B6B),
)

/**
 * Lilita One (chunky, rounded, built for game titles and numbers) with Nunito for reading text.
 * Both OFL, bundled in res/font; licences in /licenses.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object Fonts {
    val Display = FontFamily(Font(R.font.lilita_one))
    val Body = FontFamily(
        listOf(500, 600, 700, 800, 900).map { w ->
            Font(R.font.nunito, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        }
    )
}

/** Dark drop under display text, so titles sit on any background. */
val TitleShadow = Shadow(Color(0xCC060B1A), Offset(0f, 4f), 0f)

private val base = Typography()
private fun display(size: Int, line: Int = size + 4) =
    TextStyle(fontFamily = Fonts.Display, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.3.sp, shadow = TitleShadow)

private val typography = Typography(
    displayLarge = display(44),
    displayMedium = display(36),
    displaySmall = display(30),
    headlineLarge = display(30),
    headlineMedium = display(27, 32),
    headlineSmall = display(23),
    titleLarge = display(21),
    titleMedium = display(18, 22).copy(shadow = Shadow(Color(0x99060B1A), Offset(0f, 2.5f), 0f)),
    titleSmall = display(15, 19).copy(shadow = Shadow(Color(0x99060B1A), Offset(0f, 2f), 0f)),
    bodyLarge = base.bodyLarge.copy(fontFamily = Fonts.Body, fontWeight = FontWeight(700)),
    bodyMedium = base.bodyMedium.copy(fontFamily = Fonts.Body, fontWeight = FontWeight(600)),
    bodySmall = base.bodySmall.copy(fontFamily = Fonts.Body, fontWeight = FontWeight(600)),
    labelLarge = TextStyle(fontFamily = Fonts.Display, fontSize = 16.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Fonts.Body, fontWeight = FontWeight(800), fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Fonts.Display, fontSize = 12.sp, letterSpacing = 1.4.sp),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
