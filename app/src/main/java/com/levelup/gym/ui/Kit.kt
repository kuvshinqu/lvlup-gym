package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelup.gym.R

// ------------------------------------------------------------------ Drawing helpers

/**
 * The house look: a rounded block with a thick ink outline, a hard shadow straight below,
 * a glossy top and a darker lip at the bottom, like a painted wooden toy.
 */
fun DrawScope.chunkyBlock(
    base: Color,
    radius: Float,
    outline: Float = 2.5.dp.toPx(),
    shadow: Float = 4.dp.toPx(),
    lip: Float = 4.dp.toPx(),
    topLeft: Offset = Offset.Zero,
    blockSize: Size = size,
    rim: Color? = null,
    gloss: Float = 0.22f,
) {
    val w = blockSize.width; val h = blockSize.height - shadow
    val cr = CornerRadius(radius)
    if (shadow > 0f) drawRoundRect(GameColors.Ink.copy(alpha = 0.55f), topLeft + Offset(0f, shadow), Size(w, h), cr)
    drawRoundRect(GameColors.Ink, topLeft, Size(w, h), cr)
    val o = outline
    val inner = Size(w - o * 2, h - o * 2)
    val ir = CornerRadius((radius - o).coerceAtLeast(0f))
    val light = lerp(base, Color.White, 0.28f)
    val dark = lerp(base, Color.Black, 0.35f)
    drawRoundRect(dark, topLeft + Offset(o, o), inner, ir)
    drawRoundRect(
        Brush.verticalGradient(listOf(light, base, base), topLeft.y + o, topLeft.y + o + inner.height - lip),
        topLeft + Offset(o, o), Size(inner.width, inner.height - lip), ir,
    )
    // gloss across the upper part
    if (gloss > 0f) {
        drawRoundRect(
            Color.White.copy(alpha = gloss), topLeft + Offset(o + o, o + o * 0.8f),
            Size(inner.width - o * 2, (inner.height - lip) * 0.38f), CornerRadius((radius - o * 2).coerceAtLeast(0f)),
        )
    }
    if (rim != null) {
        drawRoundRect(rim, topLeft + Offset(o * 1.6f, o * 1.6f), Size(w - o * 3.2f, h - o * 3.2f), CornerRadius((radius - o * 1.6f).coerceAtLeast(0f)), style = Stroke(o * 1.1f))
    }
}

/** A small gold stud with a shine. */
fun DrawScope.stud(c: Offset, r: Float, color: Color = GameColors.Gold) {
    drawCircle(GameColors.Ink, r * 1.35f, c)
    drawCircle(Brush.radialGradient(listOf(lerp(color, Color.White, 0.7f), color, lerp(color, Color.Black, 0.35f)), center = c - Offset(r * 0.3f, r * 0.4f), radius = r * 1.4f), r, c)
}

// ------------------------------------------------------------------ Text

/** Game title text: white letters with a thick ink outline and a drop, readable on anything. */
@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = Color.White,
    outline: Color = GameColors.Ink,
    outlineWidth: Dp = 4.dp,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { outlineWidth.toPx() }
    val s = style.copy(fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize, textAlign = textAlign ?: style.textAlign)
    val fill = if (textAlign != null) Modifier.fillMaxWidth() else Modifier
    Box(modifier) {
        Text(
            text, fill, maxLines = maxLines,
            style = s.copy(color = outline, drawStyle = Stroke(px, join = StrokeJoin.Round), shadow = Shadow(outline, Offset(0f, px * 0.9f), 0f)),
        )
        Text(text, fill, maxLines = maxLines, style = s.copy(color = color, shadow = null))
    }
}

// ------------------------------------------------------------------ Panels

/**
 * A panel. [accent] adds a coloured inner rim and gold studs, for the card that matters most on a screen.
 */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    ornate: Boolean = accent != null,
    content: @Composable () -> Unit,
) {
    val shadow = 5.dp
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                chunkyBlock(GameColors.Panel, 16.dp.toPx(), shadow = shadow.toPx(), lip = 5.dp.toPx(), rim = accent?.copy(alpha = 0.95f), gloss = 0.07f)
                if (ornate) {
                    val i = 10.dp.toPx(); val r = 3.2.dp.toPx(); val b = size.height - shadow.toPx()
                    listOf(Offset(i, i), Offset(size.width - i, i), Offset(i, b - i), Offset(size.width - i, b - i)).forEach { stud(it, r, accent ?: GameColors.Gold) }
                }
            }
            .padding(start = if (ornate) 20.dp else 16.dp, end = if (ornate) 20.dp else 16.dp, top = if (ornate) 18.dp else 14.dp, bottom = (if (ornate) 18.dp else 14.dp) + shadow + 5.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

/** A cream parchment strip with ink text: for notes, lore and tips. */
@Composable
fun ParchmentNote(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().drawBehind { chunkyBlock(GameColors.Parchment, 12.dp.toPx(), outline = 2.dp.toPx(), shadow = 3.dp.toPx(), lip = 3.dp.toPx(), gloss = 0f) }
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4A3216))
    }
}

/** Section heading on a red ribbon with folded tails. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = GameColors.Ruby) {
    Box(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .drawBehind { ribbon(color) }
                .padding(start = 34.dp, end = 34.dp, top = 5.dp, bottom = 9.dp),
        ) {
            OutlinedText(text.uppercase(), style = MaterialTheme.typography.titleSmall, outlineWidth = 3.dp, fontSize = 15.sp)
        }
    }
}

private fun DrawScope.ribbon(color: Color) {
    val w = size.width; val h = size.height - 4.dp.toPx(); val tail = 16.dp.toPx(); val drop = 4.dp.toPx()
    val ink = GameColors.Ink
    val o = 2.2.dp.toPx()
    fun tailPath(left: Boolean) = Path().apply {
        val x0 = if (left) 0f else w; val x1 = if (left) tail + 6f else w - tail - 6f; val dir = if (left) 1 else -1
        moveTo(x0, drop); lineTo(x1, drop); lineTo(x1, h + drop); lineTo(x0, h + drop); lineTo(x0 + dir * tail * 0.45f, (h + drop * 2) / 2); close()
    }
    listOf(true, false).forEach { left ->
        val p = tailPath(left)
        drawPath(p, lerp(color, Color.Black, 0.4f))
        drawPath(p, ink, style = Stroke(o, join = StrokeJoin.Round))
    }
    val body = Path().apply {
        moveTo(tail * 0.6f, 0f); lineTo(w - tail * 0.6f, 0f); lineTo(w - tail * 0.6f, h); lineTo(tail * 0.6f, h); close()
    }
    drawPath(body, Brush.verticalGradient(listOf(lerp(color, Color.White, 0.3f), color, lerp(color, Color.Black, 0.2f)), 0f, h))
    drawRect(Color.White.copy(alpha = 0.22f), Offset(tail * 0.6f + o, o * 1.2f), Size(w - tail * 1.2f - o * 2, h * 0.28f))
    drawPath(body, ink, style = Stroke(o, join = StrokeJoin.Round))
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .drawBehind { chunkyBlock(GameColors.PanelDark, 12.dp.toPx(), shadow = 3.dp.toPx(), lip = 3.dp.toPx(), gloss = 0.06f) }
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        OutlinedText(value, style = MaterialTheme.typography.titleLarge, outlineWidth = 3.dp)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
    }
}

// ------------------------------------------------------------------ Buttons

enum class ButtonStyle(val base: Color) {
    GOLD(Color(0xFFFFB923)), GREEN(Color(0xFF5BC236)), BLUE(Color(0xFF3D8EE8)), PURPLE(Color(0xFF9B5CF0)), RED(Color(0xFFE5483D)), GREY(Color(0xFF6A7690)),
}

/** The chunky button: it sinks into its lip when pressed. */
@Composable
fun GameButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.BLUE,
    height: Dp = 50.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateFloatAsState(if (pressed && enabled) 1f else 0f, tween(80), label = "press")
    val base = if (enabled) style.base else ButtonStyle.GREY.base.copy(alpha = 1f)
    Box(
        modifier
            .heightIn(min = height)
            .drawBehind {
                val s = 4.dp.toPx() * (1 - sink)
                chunkyBlock(base, 14.dp.toPx(), shadow = s, lip = 4.dp.toPx() * (1 - sink * 0.6f), topLeft = Offset(0f, 4.dp.toPx() - s), blockSize = Size(size.width, size.height - (4.dp.toPx() - s)), gloss = if (enabled) 0.28f else 0.1f)
            }
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .offset(y = 4.dp * sink - 2.dp)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
            LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(shadow = Shadow(GameColors.Ink.copy(alpha = 0.8f), Offset(0f, 3f), 0f), textAlign = TextAlign.Center),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, content = content)
        }
    }
}

/** The big call to action, with outlined lettering. */
@Composable
fun RpgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = GameColors.Gold,
) {
    val style = when (color) {
        GameColors.Arcane -> ButtonStyle.PURPLE
        GameColors.Xp -> ButtonStyle.GREEN
        else -> ButtonStyle.GOLD
    }
    GameButton(onClick, modifier, enabled, style, height = 60.dp) {
        OutlinedText(text, style = MaterialTheme.typography.titleMedium, outlineWidth = 3.5.dp, fontSize = 19.sp, textAlign = TextAlign.Center, color = if (enabled) Color.White else Color(0xFFDDE3EE))
    }
}

/** Same calls as Material's buttons, drawn in the house style. */
@Composable
fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    GameButton(onClick, modifier, enabled, ButtonStyle.GREEN, content = content)

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    GameButton(onClick, modifier, enabled, ButtonStyle.BLUE, height = 46.dp, content = content)

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    GameButton(onClick, modifier, enabled, ButtonStyle.PURPLE, content = content)

// ------------------------------------------------------------------ Progress

/** Chunky bar with a glossy fill and optional text inside. */
@Composable
fun XpBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = GameColors.Xp,
    height: Dp = 14.dp,
    label: String? = null,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "xp")
    val h = if (label != null) maxOf(height, 24.dp) else height
    Box(modifier.fillMaxWidth().height(h + 6.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val o = 2.5.dp.toPx()
            val bh = size.height - 3.dp.toPx()
            val r = bh / 2
            drawRoundRect(GameColors.Ink.copy(alpha = 0.5f), Offset(0f, 3.dp.toPx()), Size(size.width, bh), CornerRadius(r))
            drawRoundRect(GameColors.Ink, Offset.Zero, Size(size.width, bh), CornerRadius(r))
            val inner = Size(size.width - o * 2, bh - o * 2)
            val ir = CornerRadius(inner.height / 2)
            drawRoundRect(GameColors.Track, Offset(o, o), inner, ir)
            drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(o, o), Size(inner.width, inner.height * 0.35f), ir)
            val w = (inner.width * animated).coerceAtLeast(if (animated > 0f) inner.height else 0f)
            if (w > 0f) {
                drawRoundRect(lerp(color, Color.Black, 0.3f), Offset(o, o), Size(w, inner.height), ir)
                drawRoundRect(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color), o, o + inner.height * 0.75f), Offset(o, o), Size(w, inner.height * 0.78f), ir)
                drawRoundRect(Color.White.copy(alpha = 0.4f), Offset(o + inner.height * 0.3f, o + inner.height * 0.14f), Size((w - inner.height * 0.6f).coerceAtLeast(0f), inner.height * 0.2f), ir)
            }
        }
        if (label != null) {
            OutlinedText(label, Modifier.offset(y = (-2).dp), style = MaterialTheme.typography.titleSmall, outlineWidth = 3.dp, fontSize = 14.sp)
        }
    }
}

/** Level crest: a blue shield with a gold rim and an outlined number. */
@Composable
fun LevelBadge(level: Int, size: Dp = 64.dp, color: Color = GameColors.Gold) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            fun shield(inset: Float) = Path().apply {
                moveTo(w * 0.5f, h * 0.03f + inset)
                lineTo(w * 0.93f - inset, h * 0.17f + inset * 0.6f)
                lineTo(w * 0.88f - inset, h * 0.58f)
                quadraticTo(w * 0.80f - inset * 0.6f, h * 0.84f - inset * 0.5f, w * 0.5f, h * 0.97f - inset)
                quadraticTo(w * 0.20f + inset * 0.6f, h * 0.84f - inset * 0.5f, w * 0.12f + inset, h * 0.58f)
                lineTo(w * 0.07f + inset, h * 0.17f + inset * 0.6f)
                close()
            }
            val o = w * 0.045f
            drawPath(shield(0f).apply { translate(Offset(0f, w * 0.04f)) }, GameColors.Ink.copy(alpha = 0.5f))
            drawPath(shield(0f), GameColors.Ink)
            drawPath(shield(o), Brush.verticalGradient(listOf(lerp(color, Color.White, 0.5f), color, lerp(color, Color.Black, 0.35f))))
            drawPath(shield(o * 2.6f), GameColors.Ink)
            drawPath(shield(o * 3.4f), Brush.verticalGradient(listOf(Color(0xFF5B8CE6), Color(0xFF2B4FA0), Color(0xFF1C3576))))
            drawPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.26f, h * 0.18f, w * 0.74f, h * 0.36f)) }, Color.White.copy(alpha = 0.18f))
        }
        OutlinedText("$level", style = MaterialTheme.typography.displaySmall, fontSize = (size.value * 0.42f).sp, outlineWidth = (size.value * 0.06f).dp)
    }
}

/** Green reward pill: gem + "+40 XP". */
@Composable
fun XpPill(xp: Int, modifier: Modifier = Modifier, compact: Boolean = false, @androidx.annotation.DrawableRes icon: Int = R.drawable.ic_xp_gem) {
    Row(
        modifier
            .drawBehind { chunkyBlock(Color(0xFF2E9E3A), size.height / 2, outline = 2.dp.toPx(), shadow = 2.dp.toPx(), lip = 2.dp.toPx(), gloss = 0.2f) }
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedievalIcon(icon, 18.dp)
        Spacer(Modifier.width(3.dp))
        OutlinedText(if (compact) "+$xp" else "+$xp XP", style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp, fontSize = 14.sp, maxLines = 1)
    }
}

/** A resource counter: icon on the left edge of a dark pill. */
@Composable
fun ResourcePill(value: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Box(modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier.padding(start = 16.dp)
                .drawBehind { chunkyBlock(GameColors.PanelDark, 12.dp.toPx(), outline = 2.dp.toPx(), shadow = 2.dp.toPx(), lip = 2.dp.toPx(), gloss = 0.08f) }
                .padding(start = 22.dp, end = 12.dp, top = 4.dp, bottom = 7.dp),
        ) {
            OutlinedText(value, style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp, fontSize = 16.sp)
        }
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) { icon() }
    }
}

/** A drawn gold coin. */
@Composable
fun Coin(size: Dp = 30.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(GameColors.Ink, r)
        drawCircle(Color(0xFFB8740E), r * 0.86f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF0A8), Color(0xFFFFC93C), Color(0xFFD99317)), center = center - Offset(r * 0.25f, r * 0.3f), radius = r), r * 0.72f)
        drawCircle(Color(0xFFB8740E), r * 0.48f, style = Stroke(r * 0.1f))
        drawCircle(Color.White.copy(alpha = 0.6f), r * 0.14f, center - Offset(r * 0.32f, r * 0.34f))
    }
}
