package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.levelup.gym.game.ChestTier
import com.levelup.gym.game.Gear
import com.levelup.gym.game.Rarity
import com.levelup.gym.game.Slot
import com.levelup.gym.ui.figure.HeroLook

/** Real-world materials for each rarity: worn iron, bright steel, blued steel, blackened steel, gilded steel. */
object Metals {
    data class Metal(val light: Color, val mid: Color, val dark: Color)

    fun of(r: Rarity): Metal = when (r) {
        Rarity.COMMON -> Metal(Color(0xFFB4B2AC), Color(0xFF7C7A74), Color(0xFF3E3C38))
        Rarity.UNCOMMON -> Metal(Color(0xFFE9EEF2), Color(0xFFA9B3BC), Color(0xFF55606B))
        Rarity.RARE -> Metal(Color(0xFFA9C4EA), Color(0xFF4F72A8), Color(0xFF1E3157))
        Rarity.EPIC -> Metal(Color(0xFF8E7FA8), Color(0xFF463A5E), Color(0xFF1A1426))
        Rarity.LEGENDARY -> Metal(Color(0xFFFFE9A6), Color(0xFFD9A63A), Color(0xFF7A5414))
    }

    fun cloth(r: Rarity): Color = when (r) {
        Rarity.COMMON -> Color(0xFF6B5B45)
        Rarity.UNCOMMON -> Color(0xFF2F5A3A)
        Rarity.RARE -> Color(0xFF1F477E)
        Rarity.EPIC -> Color(0xFF4E2475)
        Rarity.LEGENDARY -> Color(0xFF8E1F1A)
    }

    fun rarityColor(r: Rarity) = Color(r.color)
}

/** The hero's colours from the gear they wear. */
object GearLooks {
    fun lookFor(equipped: Map<Slot, Gear>): HeroLook {
        val d = HeroLook.Default
        fun metal(slot: Slot) = equipped[slot]?.let { Metals.of(it.rarity) }
        return HeroLook(
            armour = metal(Slot.ARMOR)?.mid ?: d.armour,
            armourLight = metal(Slot.PAULDRONS)?.light ?: d.armourLight,
            tunic = equipped[Slot.CLOAK]?.let { Metals.cloth(it.rarity) } ?: d.tunic,
            helmet = metal(Slot.HELM)?.mid ?: d.helmet,
            boot = equipped[Slot.BOOTS]?.let { if (it.rarity == Rarity.COMMON) d.boot else Metals.of(it.rarity).dark } ?: d.boot,
            glove = equipped[Slot.GAUNTLETS]?.let { if (it.rarity == Rarity.COMMON) d.glove else Metals.of(it.rarity).mid } ?: d.glove,
            plume = equipped[Slot.HELM]?.let { Metals.rarityColor(it.rarity) } ?: d.plume,
        )
    }
}

/** An item in a chunky slot tinted by its rarity. Shapes are drawn on a 48x48 grid. */
@Composable
fun GearIcon(gear: Gear?, slot: Slot, size: Dp, modifier: Modifier = Modifier, framed: Boolean = true) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 48f
        if (framed) {
            val base = gear?.let { lerp(Metals.rarityColor(it.rarity), Color.Black, 0.35f) } ?: Color(0xFF1C2C55)
            chunkyBlock(base, 10 * k, outline = 2.2f * k, shadow = 2.5f * k, lip = 3f * k, gloss = if (gear == null) 0.04f else 0.16f)
            if (gear != null) drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), center = Offset(24 * k, 22 * k), radius = 20 * k), 20 * k, Offset(24 * k, 22 * k))
        }
        val inset = if (framed) 0.82f else 1f
        val shift = if (framed) Offset(24 * k * (1 - inset), 22 * k * (1 - inset)) else Offset.Zero
        translate(shift.x, shift.y) {
            scale(k * inset, pivot = Offset.Zero) {
                val metal = Metals.of(gear?.rarity ?: Rarity.COMMON)
                drawSlotShape(slot, metal, gear?.rarity ?: Rarity.COMMON, if (gear == null) 0.22f else 1f)
            }
        }
    }
}

/** A collectible item card: rarity frame, big item art, name on a parchment strip. */
@Composable
fun ItemCard(gear: Gear, modifier: Modifier = Modifier, badge: String? = "+${gear.bonusPercent}%") {
    val rc = Metals.rarityColor(gear.rarity)
    Box(
        modifier
            .aspectRatio(0.74f)
            .drawBehind {
                chunkyBlock(rc, 14.dp.toPx(), shadow = 4.dp.toPx(), lip = 4.dp.toPx(), gloss = 0.18f)
                // dark window for the art
                val o = 7.dp.toPx()
                drawRoundRect(GameColors.Ink, Offset(o, o + 14.dp.toPx()), Size(size.width - o * 2, size.height * 0.56f), CornerRadius(9.dp.toPx()))
                drawRoundRect(
                    Brush.radialGradient(listOf(lerp(rc, Color.White, 0.15f), lerp(rc, Color.Black, 0.55f), Color(0xFF0D1530)), center = Offset(size.width / 2, size.height * 0.42f), radius = size.width * 0.6f),
                    Offset(o + 2.dp.toPx(), o + 16.dp.toPx()), Size(size.width - o * 2 - 4.dp.toPx(), size.height * 0.56f - 4.dp.toPx()), CornerRadius(7.dp.toPx()),
                )
            },
    ) {
        Column(Modifier.fillMaxSize().padding(start = 7.dp, end = 7.dp, top = 2.dp, bottom = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedText(gear.rarity.label.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, outlineWidth = 2.5.dp)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GearIcon(gear, gear.slot, 64.dp, framed = false)
            }
            Box(
                Modifier.fillMaxWidth().drawBehind { chunkyBlock(GameColors.Parchment, 6.dp.toPx(), outline = 1.8.dp.toPx(), shadow = 0f, lip = 2.dp.toPx(), gloss = 0f) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    gear.name, color = Color(0xFF3B2610), maxLines = 2, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall.copy(shadow = null, fontSize = 12.sp, lineHeight = 13.sp),
                )
            }
        }
        if (badge != null) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp)
                .drawBehind { chunkyBlock(GameColors.Xp.let { lerp(it, Color.Black, 0.15f) }, size.height / 2, outline = 1.8.dp.toPx(), shadow = 1.5.dp.toPx(), lip = 2.dp.toPx(), gloss = 0.2f) }
                .padding(start = 6.dp, end = 6.dp, top = 1.dp, bottom = 4.dp)) {
                OutlinedText(badge, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, outlineWidth = 2.dp)
            }
        }
    }
}

private fun DrawScope.metalFill(path: Path, m: Metals.Metal, alpha: Float) {
    val b = path.getBounds()
    drawPath(path, Brush.linearGradient(listOf(m.light, m.mid, m.dark), Offset(b.left, b.top), Offset(b.right, b.bottom)), alpha = alpha)
    drawPath(path, GameColors.Ink, alpha = alpha, style = Stroke(2.4f, join = StrokeJoin.Round))
}

private fun DrawScope.highlight(from: Offset, to: Offset, alpha: Float) =
    drawLine(Color.White.copy(alpha = 0.75f * alpha), from, to, 2.2f, cap = StrokeCap.Round)

private fun path(vararg pts: Pair<Float, Float>, close: Boolean = true) = Path().apply {
    pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
    if (close) close()
}

private fun DrawScope.drawSlotShape(slot: Slot, m: Metals.Metal, rarity: Rarity, a: Float) {
    when (slot) {
        Slot.HELM -> {
            val dome = Path().apply {
                moveTo(12f, 36f); lineTo(12f, 22f)
                cubicTo(12f, 10f, 36f, 10f, 36f, 22f)
                lineTo(36f, 36f); lineTo(28f, 39f); lineTo(20f, 39f); close()
            }
            metalFill(dome, m, a)
            drawRect(Color(0xFF0E0C0A), Offset(15f, 24f), Size(18f, 3f), alpha = a)
            drawRect(Color(0xFF0E0C0A), Offset(22.8f, 27f), Size(2.4f, 9f), alpha = a)
            highlight(Offset(16f, 20f), Offset(22f, 14f), a)
            drawLine(Metals.rarityColor(rarity), Offset(24f, 12f), Offset(30f, 5f), 3f, cap = StrokeCap.Round, alpha = a)
        }
        Slot.ARMOR -> {
            val plate = path(14f to 10f, 20f to 12f, 28f to 12f, 34f to 10f, 38f to 16f, 35f to 24f, 34f to 38f, 24f to 41f, 14f to 38f, 13f to 24f, 10f to 16f)
            metalFill(plate, m, a)
            drawLine(Color(0xFF0E0C0A), Offset(24f, 14f), Offset(24f, 39f), 1f, alpha = a * 0.7f)
            drawLine(Color(0xFF0E0C0A), Offset(15f, 30f), Offset(33f, 30f), 1f, alpha = a * 0.7f)
            highlight(Offset(17f, 16f), Offset(19f, 27f), a)
            listOf(Offset(16f, 33f), Offset(32f, 33f), Offset(24f, 36f)).forEach { drawCircle(m.light, 1f, it, alpha = a) }
        }
        Slot.PAULDRONS -> {
            listOf(0f, 7f, 14f).forEachIndexed { i, dy ->
                val p = Path().apply {
                    moveTo(9f + i, 16f + dy); cubicTo(14f, 8f + dy, 34f, 8f + dy, 39f - i, 16f + dy)
                    lineTo(37f - i, 21f + dy); cubicTo(32f, 15f + dy, 16f, 15f + dy, 11f + i, 21f + dy); close()
                }
                metalFill(p, m, a)
            }
            highlight(Offset(15f, 13f), Offset(26f, 11f), a)
        }
        Slot.GAUNTLETS -> {
            val cuff = path(14f to 30f, 32f to 30f, 34f to 42f, 12f to 42f)
            val fist = Path().apply {
                moveTo(12f, 30f); lineTo(12f, 16f)
                cubicTo(12f, 10f, 20f, 9f, 22f, 12f); cubicTo(26f, 8f, 34f, 9f, 35f, 15f)
                lineTo(36f, 30f); close()
            }
            metalFill(cuff, m, a); metalFill(fist, m, a)
            listOf(17f, 23f, 29f).forEach { x -> drawLine(Color(0xFF0E0C0A), Offset(x, 13f), Offset(x, 22f), 1f, alpha = a * 0.7f) }
            highlight(Offset(15f, 15f), Offset(15f, 26f), a)
        }
        Slot.CLOAK -> {
            val cloth = Metals.cloth(rarity)
            val p = Path().apply {
                moveTo(17f, 9f); lineTo(31f, 9f); lineTo(39f, 40f)
                cubicTo(34f, 37f, 31f, 42f, 27f, 39f); cubicTo(23f, 42f, 19f, 37f, 15f, 41f)
                cubicTo(12f, 38f, 10f, 41f, 9f, 40f); close()
            }
            drawPath(p, Brush.linearGradient(listOf(lerp(cloth, Color.White, 0.25f), cloth, lerp(cloth, Color.Black, 0.55f)), Offset(10f, 9f), Offset(38f, 40f)), alpha = a)
            drawPath(p, GameColors.Ink, alpha = a, style = Stroke(2.4f, join = StrokeJoin.Round))
            listOf(20f to 30f, 25f to 34f, 30f to 31f).forEach { (x1, x2) -> drawLine(Color.Black.copy(alpha = 0.35f), Offset(x1, 13f), Offset(x2 - 6f, 38f), 1.2f, alpha = a) }
            drawCircle(m.mid, 2.6f, Offset(24f, 10f), alpha = a)
            drawCircle(Color(0xFF0E0C0A), 2.6f, Offset(24f, 10f), alpha = a, style = Stroke(1f))
        }
        Slot.BOOTS -> {
            val boot = path(16f to 8f, 28f to 8f, 28f to 30f, 38f to 34f, 39f to 40f, 13f to 40f, 14f to 26f)
            metalFill(boot, m, a)
            drawRect(Color(0xFF0E0C0A), Offset(13f, 38f), Size(26f, 2.5f), alpha = a)
            drawLine(Color(0xFF0E0C0A), Offset(15f, 20f), Offset(28f, 20f), 1f, alpha = a * 0.7f)
            highlight(Offset(18f, 11f), Offset(18f, 26f), a)
        }
        Slot.AMULET -> {
            val chain = Path().apply { moveTo(13f, 8f); cubicTo(14f, 24f, 34f, 24f, 35f, 8f) }
            drawPath(chain, m.mid, alpha = a, style = Stroke(1.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 1.2f))))
            val setting = Path().apply { addOval(androidx.compose.ui.geometry.Rect(16f, 22f, 32f, 40f)) }
            metalFill(setting, m, a)
            val gem = Metals.rarityColor(rarity)
            drawOval(Brush.radialGradient(listOf(lerp(gem, Color.White, 0.6f), gem, lerp(gem, Color.Black, 0.5f)), center = Offset(22f, 28f), radius = 8f), Offset(19f, 25f), Size(10f, 12f), alpha = a)
            drawCircle(Color.White.copy(alpha = 0.8f), 1.2f, Offset(21.5f, 28f), alpha = a)
        }
    }
}

/** Colours of a chest tier: body, its shade, trims and the lock. */
private data class ChestPaint(val body: Color, val bodyDark: Color, val trim: Color, val trimDark: Color, val lock: Color, val gem: Color?, val planks: Boolean)

private fun paintFor(tier: ChestTier) = when (tier) {
    ChestTier.WOODEN -> ChestPaint(Color(0xFFD08442), Color(0xFF8A4520), Color(0xFF8F9AB2), Color(0xFF4D566C), Color(0xFFFFC93C), null, true)
    ChestTier.IRON -> ChestPaint(Color(0xFF7FA6D8), Color(0xFF3F5F94), Color(0xFFEFF4FA), Color(0xFF8FA0BC), Color(0xFFFFC93C), Color(0xFF59B8FF), false)
    ChestTier.GOLDEN -> ChestPaint(Color(0xFFFFD555), Color(0xFFD2861A), Color(0xFFD0342C), Color(0xFF7E1A16), Color(0xFFFFF1B0), Color(0xFFE8473F), false)
}

/**
 * A toy-like chest: thick ink outline, arched lid, chunky bands and corner caps, a big lock.
 * [open] lifts and tips the lid back (0..1); [glow] lights the inside and throws rays.
 */
fun DrawScope.drawChest(tier: ChestTier, open: Float = 0f, glow: Float = 0f, rays: Float = 0f) {
    val p = paintFor(tier)
    val w = size.width; val h = size.height
    val ink = GameColors.Ink
    val t = w * 0.032f
    val left = w * 0.12f; val right = w * 0.88f
    val seam = h * 0.52f; val bottom = h * 0.9f
    val cx = w / 2

    // light rays behind an open chest
    if (glow > 0f) {
        val rc = Offset(cx, seam - h * 0.06f)
        rotate(rays * 360f, rc) {
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0).toFloat()
                val a2 = Math.toRadians(i * 30.0 + 12).toFloat()
                val r = w * 0.9f
                val ray = Path().apply {
                    moveTo(rc.x, rc.y)
                    lineTo(rc.x + r * kotlin.math.cos(a), rc.y + r * kotlin.math.sin(a))
                    lineTo(rc.x + r * kotlin.math.cos(a2), rc.y + r * kotlin.math.sin(a2)); close()
                }
                drawPath(ray, Brush.radialGradient(listOf(Color(0xFFFFF1A8).copy(alpha = 0.55f * glow), Color.Transparent), center = rc, radius = r))
            }
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6C8).copy(alpha = 0.9f * glow), Color(0xFFFFC93C).copy(alpha = 0.35f * glow), Color.Transparent), center = rc, radius = w * 0.45f), w * 0.45f, rc)
    }

    // ground shadow
    drawOval(Color.Black.copy(alpha = 0.35f), Offset(left - w * 0.03f, bottom - h * 0.035f), Size(right - left + w * 0.06f, h * 0.09f))

    // ---- body
    val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(left, seam, right, bottom, CornerRadius(w * 0.05f))) }
    drawPath(body, ink, style = Stroke(t * 2, join = StrokeJoin.Round))
    drawPath(body, Brush.verticalGradient(listOf(lerp(p.body, Color.White, 0.15f), p.body, p.bodyDark), seam, bottom))
    if (p.planks) {
        for (i in 1..2) {
            val y = seam + (bottom - seam) * i / 3f
            drawLine(lerp(p.bodyDark, Color.Black, 0.3f), Offset(left + t, y), Offset(right - t, y), t * 0.55f)
        }
    } else {
        // riveted plates
        for (i in 0..5) {
            val x = left + (right - left) * (0.08f + i * 0.168f)
            drawCircle(lerp(p.body, Color.White, 0.45f), t * 0.42f, Offset(x, seam + h * 0.045f))
        }
    }
    // inside of an open chest
    if (open > 0f) {
        val inside = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(left + t, seam - h * 0.05f * open, right - t, seam + h * 0.05f, CornerRadius(w * 0.03f))) }
        drawPath(inside, Brush.verticalGradient(listOf(Color(0xFFFFF3B5), Color(0xFFFFB627), Color(0xFF7A3A10)), seam - h * 0.05f, seam + h * 0.05f))
        drawPath(inside, ink, style = Stroke(t, join = StrokeJoin.Round))
    }

    fun band(x: Float, top: Float, bot: Float) {
        val bw = w * 0.1f
        val r = androidx.compose.ui.geometry.RoundRect(x - bw / 2, top, x + bw / 2, bot, CornerRadius(w * 0.02f))
        val path = Path().apply { addRoundRect(r) }
        drawPath(path, Brush.horizontalGradient(listOf(lerp(p.trim, Color.White, 0.35f), p.trim, p.trimDark), x - bw / 2, x + bw / 2))
        drawPath(path, ink, style = Stroke(t * 0.9f, join = StrokeJoin.Round))
        drawCircle(lerp(p.trim, Color.White, 0.6f), t * 0.45f, Offset(x, bot - t * 1.6f))
        drawCircle(lerp(p.trim, Color.White, 0.6f), t * 0.45f, Offset(x, top + t * 1.6f))
    }
    band(left + (right - left) * 0.22f, seam, bottom)
    band(left + (right - left) * 0.78f, seam, bottom)
    // corner caps
    listOf(left, right).forEach { x ->
        val dir = if (x == left) 1f else -1f
        val cap = Path().apply {
            moveTo(x - dir * t * 0.3f, bottom - h * 0.13f); lineTo(x + dir * w * 0.07f, bottom - h * 0.02f)
            lineTo(x + dir * w * 0.07f, bottom + t * 0.3f); lineTo(x - dir * t * 0.3f, bottom + t * 0.3f); close()
        }
        drawPath(cap, Brush.verticalGradient(listOf(p.trim, p.trimDark), bottom - h * 0.13f, bottom))
        drawPath(cap, ink, style = Stroke(t * 0.9f, join = StrokeJoin.Round))
    }

    // ---- lid (arched); opening lifts it and tips it back, showing less of its front
    val lift = open * h * 0.2f
    val squash = 1f - 0.55f * open
    val lidBottom = seam - lift
    val lidTop = lidBottom - h * 0.27f * squash
    val over = w * 0.025f
    val lid = Path().apply {
        moveTo(left - over, lidBottom)
        lineTo(left - over, lidTop + (lidBottom - lidTop) * 0.45f)
        cubicTo(left - over, lidTop - (lidBottom - lidTop) * 0.12f, right + over, lidTop - (lidBottom - lidTop) * 0.12f, right + over, lidTop + (lidBottom - lidTop) * 0.45f)
        lineTo(right + over, lidBottom); close()
    }
    drawPath(lid, ink, style = Stroke(t * 2, join = StrokeJoin.Round))
    // as the lid tips back we see more of its dark underside
    val under = lerp(p.bodyDark, Color.Black, 0.35f)
    drawPath(lid, Brush.verticalGradient(listOf(lerp(lerp(p.body, Color.White, 0.3f), under, open * 0.7f), lerp(p.body, under, open * 0.7f), lerp(p.bodyDark, under, open)), lidTop, lidBottom))
    if (p.planks) drawLine(lerp(p.bodyDark, Color.Black, 0.3f), Offset(left, lidTop + (lidBottom - lidTop) * 0.5f), Offset(right, lidTop + (lidBottom - lidTop) * 0.5f), t * 0.55f)
    // gloss
    drawOval(Color.White.copy(alpha = 0.35f * (1 - open)), Offset(left + w * 0.08f, lidTop + (lidBottom - lidTop) * 0.12f), Size(w * 0.3f, (lidBottom - lidTop) * 0.22f))
    // lid bands and rim
    listOf(0.22f, 0.78f).forEach { fx -> band(left + (right - left) * fx, lidTop + (lidBottom - lidTop) * 0.05f, lidBottom) }
    val rim = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(left - over, lidBottom - h * 0.06f, right + over, lidBottom + h * 0.01f, CornerRadius(w * 0.02f))) }
    drawPath(rim, Brush.verticalGradient(listOf(lerp(p.trim, Color.White, 0.3f), p.trimDark), lidBottom - h * 0.06f, lidBottom))
    drawPath(rim, ink, style = Stroke(t * 0.9f, join = StrokeJoin.Round))

    // ---- lock: a fat shield with a keyhole (or a gem), sitting on the seam
    if (open < 0.5f) {
        val lw = w * 0.2f; val lh = h * 0.22f
        val ly = seam - lh * 0.45f
        val lock = Path().apply {
            moveTo(cx - lw / 2, ly); lineTo(cx + lw / 2, ly); lineTo(cx + lw / 2, ly + lh * 0.55f)
            quadraticTo(cx + lw / 2, ly + lh * 0.9f, cx, ly + lh); quadraticTo(cx - lw / 2, ly + lh * 0.9f, cx - lw / 2, ly + lh * 0.55f); close()
        }
        drawPath(lock, ink, style = Stroke(t * 1.6f, join = StrokeJoin.Round))
        drawPath(lock, Brush.verticalGradient(listOf(lerp(p.lock, Color.White, 0.5f), p.lock, lerp(p.lock, Color(0xFF8A4D00), 0.55f)), ly, ly + lh))
        drawOval(Color.White.copy(alpha = 0.45f), Offset(cx - lw * 0.32f, ly + lh * 0.08f), Size(lw * 0.3f, lh * 0.18f))
        val g = p.gem
        if (g != null) {
            val gc = Offset(cx, ly + lh * 0.48f); val gr = lw * 0.22f
            val gem = Path().apply { moveTo(gc.x, gc.y - gr); lineTo(gc.x + gr, gc.y); lineTo(gc.x, gc.y + gr); lineTo(gc.x - gr, gc.y); close() }
            drawPath(gem, Brush.linearGradient(listOf(lerp(g, Color.White, 0.6f), g, lerp(g, Color.Black, 0.4f)), gc - Offset(gr, gr), gc + Offset(gr, gr)))
            drawPath(gem, ink, style = Stroke(t * 0.8f, join = StrokeJoin.Round))
        } else {
            drawCircle(ink, lw * 0.11f, Offset(cx, ly + lh * 0.42f))
            drawRect(ink, Offset(cx - lw * 0.05f, ly + lh * 0.42f), Size(lw * 0.1f, lh * 0.28f))
        }
    }
}
