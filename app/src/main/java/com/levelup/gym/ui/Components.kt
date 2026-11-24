package com.levelup.gym.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import com.levelup.gym.data.SetWithExercise
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.Units
import com.levelup.gym.game.ExerciseGuides
import com.levelup.gym.game.UnitsFormat

/** Framed, segmented XP bar with a gem at the tip. */
@Composable
fun XpBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = GameColors.Xp,
    height: Dp = 12.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "xp")
    Canvas(modifier.fillMaxWidth().height(height + 4.dp)) {
        val r = size.height / 2
        val inset = 2.dp.toPx()
        // frame
        drawRoundRect(GameColors.Gold.copy(alpha = 0.55f), cornerRadius = CornerRadius(r, r), style = Stroke(1.dp.toPx()))
        val inner = Size(size.width - inset * 2, size.height - inset * 2)
        val ir = inner.height / 2
        drawRoundRect(Color(0xFF0A0C12), Offset(inset, inset), inner, CornerRadius(ir, ir))
        val w = inner.width * animated
        if (w > 0.5f) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color), startX = inset, endX = inset + w),
                Offset(inset, inset), Size(w, inner.height), CornerRadius(ir, ir),
            )
            // sheen
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(inset, inset), Size(w, inner.height * 0.4f), CornerRadius(ir, ir))
        }
        // segment ticks
        for (i in 1 until 10) {
            val x = inset + inner.width * i / 10f
            drawLine(Color.Black.copy(alpha = 0.45f), Offset(x, inset + 1), Offset(x, size.height - inset - 1), 1.dp.toPx())
        }
        // gem at the tip
        if (w > ir) {
            val c = Offset(inset + w - ir, size.height / 2)
            val g = ir * 0.75f
            val gem = Path().apply { moveTo(c.x, c.y - g); lineTo(c.x + g, c.y); lineTo(c.x, c.y + g); lineTo(c.x - g, c.y); close() }
            drawPath(gem, Color.White.copy(alpha = 0.85f))
        }
    }
}

/** Level crest: a gold-rimmed shield with the level number. */
@Composable
fun LevelBadge(level: Int, size: Dp = 64.dp, color: Color = GameColors.Gold) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            val shield = Path().apply {
                moveTo(w * 0.5f, h * 0.02f)
                lineTo(w * 0.92f, h * 0.16f)
                lineTo(w * 0.88f, h * 0.56f)
                quadraticTo(w * 0.80f, h * 0.84f, w * 0.5f, h * 0.98f)
                quadraticTo(w * 0.20f, h * 0.84f, w * 0.12f, h * 0.56f)
                lineTo(w * 0.08f, h * 0.16f)
                close()
            }
            drawPath(shield, Brush.verticalGradient(listOf(Color(0xFF2A2340), Color(0xFF12141F))))
            drawPath(shield, color.copy(alpha = 0.25f), style = Stroke(w * 0.12f))
            drawPath(shield, color, style = Stroke(w * 0.035f))
            // laurel ticks
            for (i in 0..3) {
                val y = h * (0.38f + i * 0.1f)
                drawLine(color.copy(alpha = 0.6f), Offset(w * 0.2f, y), Offset(w * 0.27f, y - h * 0.04f), w * 0.02f)
                drawLine(color.copy(alpha = 0.6f), Offset(w * 0.8f, y), Offset(w * 0.73f, y - h * 0.04f), w * 0.02f)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LV", style = MaterialTheme.typography.labelSmall, color = color, fontSize = (size.value * 0.13f).sp)
            Text(
                "$level",
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = color,
                lineHeight = (size.value * 0.38f).sp,
            )
        }
    }
}

/**
 * Panel in the style of an old quest log. [accent] gives a coloured rim; [ornate] adds gold corner pieces.
 */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    ornate: Boolean = accent != null,
    content: @Composable () -> Unit,
) {
    val rim = accent ?: GameColors.Gold.copy(alpha = 0.35f)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1C2233), Color(0xFF141927))))
            .border(1.dp, rim.copy(alpha = if (accent != null) 0.6f else 0.18f), shape)
            .drawWithContent {
                drawContent()
                if (ornate) {
                    val l = 14.dp.toPx(); val i = 5.dp.toPx(); val sw = 1.5.dp.toPx(); val d = 3.dp.toPx()
                    val col = rim
                    listOf(
                        Triple(Offset(i, i), 1f, 1f), Triple(Offset(size.width - i, i), -1f, 1f),
                        Triple(Offset(i, size.height - i), 1f, -1f), Triple(Offset(size.width - i, size.height - i), -1f, -1f),
                    ).forEach { (p, sx, sy) ->
                        drawLine(col, p, Offset(p.x + l * sx, p.y), sw)
                        drawLine(col, p, Offset(p.x, p.y + l * sy), sw)
                        val gem = Path().apply {
                            moveTo(p.x + d * 1.6f * sx, p.y + d * 0.2f * sy); lineTo(p.x + d * 2.6f * sx, p.y + d * 1.3f * sy)
                            lineTo(p.x + d * 1.6f * sx, p.y + d * 2.4f * sy); lineTo(p.x + d * 0.6f * sx, p.y + d * 1.3f * sy); close()
                        }
                        drawPath(gem, col)
                    }
                }
            }
            .padding(if (ornate) 20.dp else 16.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
    }
}

/** Section heading with gold rules and diamonds: ◆── SKILLS ──◆ (drawn, not text glyphs). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Ornament(Modifier.weight(1f), flip = false)
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = GameColors.Gold.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Ornament(Modifier.weight(1f), flip = true)
    }
}

@Composable
private fun Ornament(modifier: Modifier, flip: Boolean) {
    Canvas(modifier.height(10.dp)) {
        val y = size.height / 2
        val col = GameColors.Gold.copy(alpha = 0.5f)
        drawLine(Brush.horizontalGradient(if (flip) listOf(col, Color.Transparent) else listOf(Color.Transparent, col)), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        val g = 3.5.dp.toPx(); val cx = if (flip) g else size.width - g
        val gem = Path().apply { moveTo(cx, y - g); lineTo(cx + g, y); lineTo(cx, y + g); lineTo(cx - g, y); close() }
        drawPath(gem, GameColors.Gold.copy(alpha = 0.8f))
    }
}

/** Gold, engraved-looking main action button. */
@Composable
fun RpgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = GameColors.Gold,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(lerp(color, Color.White, 0.25f), color, lerp(color, Color.Black, 0.3f)))
                else Brush.verticalGradient(listOf(GameColors.SurfaceHigh, GameColors.SurfaceHigh))
            )
            .border(1.5.dp, if (enabled) lerp(color, Color.Black, 0.45f) else GameColors.Track, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) Color(0xFF231A00) else GameColors.Muted,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GameColors.SurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun XpPill(xp: Int, modifier: Modifier = Modifier) {
    Text(
        "+$xp XP",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(GameColors.Xp.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = GameColors.Xp,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

/** "8 × 60 kg" for strength, "30 min · 5 km" for cardio. */
fun describeSet(s: SetWithExercise, units: Units): String =
    if (s.exerciseKind == ExerciseKind.CARDIO) {
        buildList {
            if (s.set.durationSec > 0) add(UnitsFormat.duration(s.set.durationSec))
            if (s.set.distanceM > 0) add(UnitsFormat.distance(s.set.distanceM, units))
        }.joinToString(" · ").ifEmpty { "Cardio" }
    } else {
        val guide = ExerciseGuides.forName(s.exerciseName)
        val amount = if (guide?.timed == true) "${s.set.reps} s" else "${s.set.reps}"
        when {
            s.set.weightKg > 0 && guide?.bodyweight == true -> "$amount × bodyweight + ${UnitsFormat.weight(s.set.weightKg, units)}"
            s.set.weightKg > 0 -> "$amount × ${UnitsFormat.weight(s.set.weightKg, units)}"
            guide?.bodyweight == true -> "$amount × bodyweight"
            else -> "$amount reps"
        }
    }

@Composable
fun SetRow(
    index: Int,
    set: SetWithExercise,
    units: Units,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "$index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(describeSet(set, units), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (set.set.isPersonalRecord) {
            Text("NEW BEST", style = MaterialTheme.typography.labelSmall, color = GameColors.Gold)
        }
        XpPill(set.set.xp)
        trailing()
    }
}
