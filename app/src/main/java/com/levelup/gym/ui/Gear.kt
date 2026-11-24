package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
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

/** A drawn item in a slot frame. Shapes are drawn on a 48x48 grid with metal shading. */
@Composable
fun GearIcon(gear: Gear?, slot: Slot, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 48f
        // recessed slot
        val frame = gear?.let { Metals.rarityColor(it.rarity) } ?: Color(0xFF3A3F4C)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0B0D12), Color(0xFF1B1F29))), cornerRadius = CornerRadius(8 * k))
        drawRoundRect(frame.copy(alpha = if (gear == null) 0.5f else 0.9f), cornerRadius = CornerRadius(8 * k), style = Stroke(1.6f * k))
        if (gear != null) {
            drawCircle(Brush.radialGradient(listOf(frame.copy(alpha = 0.28f), Color.Transparent), center = center, radius = 22 * k), 22 * k)
        }
        scale(k, pivot = Offset.Zero) {
            val metal = Metals.of(gear?.rarity ?: Rarity.COMMON)
            val alpha = if (gear == null) 0.18f else 1f
            drawSlotShape(slot, metal, gear?.rarity ?: Rarity.COMMON, alpha)
        }
    }
}

private fun DrawScope.metalFill(path: Path, m: Metals.Metal, alpha: Float) {
    val b = path.getBounds()
    drawPath(path, Brush.linearGradient(listOf(m.light, m.mid, m.dark), Offset(b.left, b.top), Offset(b.right, b.bottom)), alpha = alpha)
    drawPath(path, Color(0xFF0E0C0A), alpha = alpha, style = Stroke(1.4f, join = StrokeJoin.Round))
}

private fun DrawScope.highlight(from: Offset, to: Offset, alpha: Float) =
    drawLine(Color.White.copy(alpha = 0.55f * alpha), from, to, 1.2f, cap = StrokeCap.Round)

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
            drawPath(p, Color(0xFF0E0C0A), alpha = a, style = Stroke(1.4f, join = StrokeJoin.Round))
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

/** A wooden chest with iron bands, drawn with wood grain and metal shading. [open] lifts the lid (0..1). */
fun DrawScope.drawChest(tier: ChestTier, open: Float = 0f, glow: Float = 0f) {
    val w = size.width; val h = size.height
    val bandMetal = when (tier) {
        ChestTier.WOODEN -> Metals.of(Rarity.COMMON)
        ChestTier.IRON -> Metals.of(Rarity.UNCOMMON)
        ChestTier.GOLDEN -> Metals.of(Rarity.LEGENDARY)
    }
    val wood = if (tier == ChestTier.GOLDEN) Color(0xFF5A2E1A) else Color(0xFF7A4B2A)
    val ink = Color(0xFF120D0A)
    val left = w * 0.12f; val right = w * 0.88f
    val bodyTop = h * 0.48f; val bottom = h * 0.9f
    // shadow
    drawOval(Color.Black.copy(alpha = 0.45f), Offset(left - w * 0.02f, bottom - h * 0.03f), Size(right - left + w * 0.04f, h * 0.08f))
    if (glow > 0f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD873).copy(alpha = 0.7f * glow), Color.Transparent), center = Offset(w / 2, bodyTop), radius = w * 0.55f), w * 0.55f, Offset(w / 2, bodyTop))
    }
    // body
    drawRect(Brush.verticalGradient(listOf(lerp(wood, Color.White, 0.12f), wood, lerp(wood, Color.Black, 0.45f)), bodyTop, bottom), Offset(left, bodyTop), Size(right - left, bottom - bodyTop))
    // planks and grain
    val planks = 3
    for (i in 1 until planks) {
        val y = bodyTop + (bottom - bodyTop) * i / planks
        drawLine(ink.copy(alpha = 0.6f), Offset(left, y), Offset(right, y), w * 0.008f)
    }
    for (i in 0 until 9) {
        val y = bodyTop + (bottom - bodyTop) * (i + 0.5f) / 9
        drawLine(Color.Black.copy(alpha = 0.12f), Offset(left + w * 0.03f * (i % 3), y), Offset(right - w * 0.05f * (i % 2), y + h * 0.006f), w * 0.004f)
    }
    drawRect(ink, Offset(left, bodyTop), Size(right - left, bottom - bodyTop), style = Stroke(w * 0.012f))
    // lid (opens upward)
    val lidH = h * 0.2f
    val lift = open * h * 0.16f
    val lidTop = bodyTop - lidH - lift
    val lid = Path().apply {
        moveTo(left, bodyTop - lift)
        lineTo(left, lidTop + lidH * 0.45f)
        cubicTo(left, lidTop - lidH * 0.1f, right, lidTop - lidH * 0.1f, right, lidTop + lidH * 0.45f)
        lineTo(right, bodyTop - lift); close()
    }
    if (open > 0f) {
        // inside glow
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE08A).copy(alpha = 0.9f * open), Color(0xFF3A1E0E)), bodyTop - lift, bodyTop + h * 0.04f), Offset(left + w * 0.02f, bodyTop - lift), Size(right - left - w * 0.04f, lift + h * 0.02f))
    }
    drawPath(lid, Brush.verticalGradient(listOf(lerp(wood, Color.White, 0.2f), wood, lerp(wood, Color.Black, 0.3f)), lidTop, bodyTop))
    drawPath(lid, ink, style = Stroke(w * 0.012f, join = StrokeJoin.Round))
    // iron bands
    listOf(0.24f, 0.76f).forEach { fx ->
        val x = left + (right - left) * fx
        val bw = w * 0.06f
        drawRect(Brush.horizontalGradient(listOf(bandMetal.light, bandMetal.mid, bandMetal.dark), x - bw / 2, x + bw / 2), Offset(x - bw / 2, bodyTop), Size(bw, bottom - bodyTop))
        drawRect(Brush.horizontalGradient(listOf(bandMetal.light, bandMetal.mid, bandMetal.dark), x - bw / 2, x + bw / 2), Offset(x - bw / 2, lidTop + lidH * 0.12f), Size(bw, bodyTop - lift - lidTop - lidH * 0.12f))
        listOf(bodyTop + h * 0.04f, bottom - h * 0.04f).forEach { y ->
            drawCircle(bandMetal.light, w * 0.011f, Offset(x, y)); drawCircle(ink, w * 0.011f, Offset(x, y), style = Stroke(w * 0.004f))
        }
    }
    drawRect(Brush.verticalGradient(listOf(bandMetal.light, bandMetal.dark), bottom - h * 0.035f, bottom), Offset(left, bottom - h * 0.035f), Size(right - left, h * 0.035f))
    // lock plate
    val lw = w * 0.14f; val lh = h * 0.15f
    val lx = w / 2 - lw / 2; val ly = bodyTop - lift * 0.0f - lh * 0.35f
    drawRoundRect(Brush.linearGradient(listOf(bandMetal.light, bandMetal.mid, bandMetal.dark), Offset(lx, ly), Offset(lx + lw, ly + lh)), Offset(lx, ly), Size(lw, lh), CornerRadius(w * 0.015f))
    drawRoundRect(ink, Offset(lx, ly), Size(lw, lh), CornerRadius(w * 0.015f), style = Stroke(w * 0.008f))
    drawCircle(ink, w * 0.016f, Offset(w / 2, ly + lh * 0.42f))
    drawRect(ink, Offset(w / 2 - w * 0.006f, ly + lh * 0.42f), Size(w * 0.012f, lh * 0.3f))
}
