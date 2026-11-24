package com.levelup.gym.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.levelup.gym.R
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.draw.drawBehind
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
        // forged frame and a dark glass tube
        drawRoundRect(Brush.verticalGradient(listOf(Materials.SteelLight, Materials.SteelMid, Materials.SteelDark)), cornerRadius = CornerRadius(r, r))
        val inner = Size(size.width - inset * 2, size.height - inset * 2)
        val ir = inner.height / 2
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF020305), Color(0xFF141820))), Offset(inset, inset), inner, CornerRadius(ir, ir))
        val w = inner.width * animated
        if (w > 0.5f) {
            // liquid fill, lit from above
            drawRoundRect(
                Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color, lerp(color, Color.Black, 0.45f)), inset, inset + inner.height),
                Offset(inset, inset), Size(w, inner.height), CornerRadius(ir, ir),
            )
            // glass reflection
            drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(inset + ir * 0.5f, inset + inner.height * 0.12f), Size((w - ir).coerceAtLeast(0f), inner.height * 0.22f), CornerRadius(ir, ir))
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
            drawPath(shield, Color.Black.copy(alpha = 0.5f), style = Stroke(w * 0.14f))
            drawPath(shield, Brush.linearGradient(listOf(Color(0xFF5A6070), Color(0xFF2A2E37), Color(0xFF14161B)), Offset(0f, 0f), Offset(w, h)))
            drawPath(shield, Materials.grainBrush, alpha = 0.25f)
            drawPath(shield, Brush.verticalGradient(listOf(Materials.GoldLight, Materials.GoldMid, Materials.GoldDark)), style = Stroke(w * 0.07f))
            drawPath(shield, Color.Black.copy(alpha = 0.5f), style = Stroke(w * 0.012f))
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
                fontFamily = Fonts.Display,
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
    val rim = accent ?: Materials.SteelMid
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                // drop shadow onto the stone
                drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(0f, 3.dp.toPx()), size, CornerRadius(12.dp.toPx()))
            }
            .clip(shape)
            .drawWithContent {
                val r = 12.dp.toPx()
                // worn dark steel plate with grain
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2E37), Color(0xFF1C1F26), Color(0xFF15171C))))
                drawRect(Materials.grainBrush, alpha = 0.16f)
                // soft top light
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), 0f, size.height * 0.4f))
                drawContent()
                metalFrame(if (accent != null) rim else Materials.SteelMid.copy(alpha = 0.9f), r, if (ornate) 3.dp.toPx() else 1.5.dp.toPx())
                if (ornate) {
                    val i = 9.dp.toPx(); val rr = 2.6.dp.toPx()
                    val metal = androidx.compose.ui.graphics.lerp(rim, Materials.SteelMid, 0.35f)
                    listOf(Offset(i, i), Offset(size.width - i, i), Offset(i, size.height - i), Offset(size.width - i, size.height - i))
                        .forEach { rivet(it, rr, metal) }
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
            fontFamily = Fonts.Display,
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

/** Forged gold plate with an engraved label: the main action button. */
@Composable
fun RpgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = GameColors.Gold,
) {
    val shape = RoundedCornerShape(10.dp)
    val light = lerp(color, Color.White, 0.55f)
    val dark = lerp(color, Color.Black, 0.55f)
    Box(
        modifier
            .height(54.dp)
            .drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.55f), Offset(0f, 3.dp.toPx()), size, CornerRadius(10.dp.toPx())) }
            .clip(shape)
            .drawWithContent {
                if (enabled) {
                    // polished metal: bright band near the top, darker towards the base
                    drawRect(Brush.verticalGradient(0f to light, 0.18f to color, 0.55f to lerp(color, Color.Black, 0.18f), 0.56f to lerp(color, Color.Black, 0.28f), 1f to dark))
                    drawRect(Materials.grainBrush, alpha = 0.12f)
                    drawLine(Color.White.copy(alpha = 0.7f), Offset(8.dp.toPx(), 1.5.dp.toPx()), Offset(size.width - 8.dp.toPx(), 1.5.dp.toPx()), 1.dp.toPx())
                } else {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF3A3E48), Color(0xFF23262E))))
                    drawRect(Materials.grainBrush, alpha = 0.12f)
                }
                drawContent()
                metalFrame(if (enabled) dark else Color(0xFF15171C), 10.dp.toPx(), 1.5.dp.toPx())
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) Color(0xFF2A1C02) else GameColors.Muted,
            fontFamily = Fonts.Display,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            // engraved: a light edge under the dark letters
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(Color.White.copy(alpha = if (enabled) 0.45f else 0f), Offset(0f, 1.5f), 0f),
            ),
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
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(GameColors.Xp.copy(alpha = 0.15f))
            .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedievalIcon(R.drawable.ic_xp_gem, 16.dp)
        Spacer(Modifier.width(3.dp))
        Text("+$xp XP", color = GameColors.Xp, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** One of the full-colour medieval icons from res/drawable (ic_*), drawn as-is. */
@Composable
fun MedievalIcon(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier, description: String? = null) {
    androidx.compose.foundation.Image(painterResource(res), description, modifier.size(size))
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
            MedievalIcon(R.drawable.ic_breakthrough_chalice, 20.dp, description = "New best")
            Text("NEW BEST", style = MaterialTheme.typography.labelSmall, color = GameColors.Gold)
        }
        XpPill(set.set.xp)
        trailing()
    }
}
