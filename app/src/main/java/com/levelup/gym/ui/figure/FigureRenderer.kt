package com.levelup.gym.ui.figure

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min

/** Colours of the hero figure: dark armour, gold trim, muscles glow like embers. */
object FigurePalette {
    val outline = Color(0xFF07090F)
    val armour = Color(0xFF5A6684)
    val armourLight = Color(0xFF7C88A8)
    val tunic = Color(0xFF2C3352)
    val gold = Color(0xFFF5C451)
    val helmet = Color(0xFF6B7796)
    val boot = Color(0xFF3B2F2A)
    val musclePrimary = Color(0xFFFF6A3D)
    val muscleSecondary = Color(0xFFFFB347)
    val floor = Color(0xFF3A4258)
    val trail = Color(0xFF7CF29C)
    val iron = Color(0xFF262B38)
    val steel = Color(0xFF8D97AE)
    val leather = Color(0xFF5B3A2E)
    val wood = Color(0xFF6B4E35)
}

/** Maps world units to the canvas so both poses and the props fit, keeping proportions. */
class Fit(minX: Float, minY: Float, maxX: Float, maxY: Float, w: Float, h: Float, pad: Float) {
    private val scale = min((w - 2 * pad) / (maxX - minX), (h - 2 * pad) / (maxY - minY))
    private val ox = (w - (maxX - minX) * scale) / 2 - minX * scale
    private val oy = (h - (maxY - minY) * scale) / 2 - minY * scale
    fun o(p: V) = Offset(ox + p.x * scale, oy + p.y * scale)
    fun s(len: Float) = len * scale
}

fun fitFor(art: ExerciseArt, w: Float, h: Float, pad: Float): Fit {
    val pts = mutableListOf<V>()
    for (pose in listOf(art.start, art.finish)) {
        val sk = Skeleton(pose, art.frontView)
        pts += sk.points()
        art.props.forEach { prop ->
            when (prop) {
                is Prop.Barbell -> anchor(sk, prop.anchor).let { pts += it + V(prop.r, prop.r); pts += it - V(prop.r, prop.r) }
                is Prop.Poly -> pts += prop.points
                is Prop.Line -> { pts += prop.a; pts += prop.b }
                is Prop.Disc -> { pts += prop.c + V(prop.r, prop.r); pts += prop.c - V(prop.r, prop.r) }
                is Prop.Cable -> pts += prop.pulley
                else -> {}
            }
        }
    }
    if (art.floor) { pts += V(pts.minOf { it.x }, 93f) }
    // Square-ish frame so every figure has the same scale feel.
    var minX = pts.minOf { it.x } - 4; var maxX = pts.maxOf { it.x } + 4
    var minY = pts.minOf { it.y } - 4; var maxY = pts.maxOf { it.y } + 2
    val wW = maxX - minX; val hW = maxY - minY
    val target = w / h
    if (wW / hW < target) { val extra = hW * target - wW; minX -= extra / 2; maxX += extra / 2 }
    else { val extra = wW / target - hW; minY -= extra / 2; maxY += extra / 2 }
    return Fit(minX, minY, maxX, maxY, w, h, pad)
}

fun anchor(sk: Skeleton, a: Anchor): V = when (a) {
    Anchor.HAND -> sk.armNear.end
    Anchor.HAND_FAR -> sk.armFar.end
    Anchor.ANKLE -> sk.legNear.end
    Anchor.HIP -> sk.hip
    Anchor.SHOULDER -> sk.shoulder
    Anchor.UPPER_BACK -> sk.shoulder - sk.fwd * 3.5f - sk.up * 1.5f
}

/**
 * Draws the hero at [t] between start (0) and finish (1).
 * [glow] 0..1 pulses the worked muscles. [trail] draws the dashed movement path with A and B markers.
 */
fun DrawScope.drawHero(art: ExerciseArt, t: Float, fit: Fit, glow: Float = 0.6f, trail: Boolean = false, dimmed: Boolean = false) {
    val pose = art.start.lerp(art.finish, t)
    val sk = Skeleton(pose, art.frontView)
    val P = FigurePalette

    // Arena glow behind the hero.
    val center = fit.o(sk.hip - sk.up * -8f)
    drawCircle(
        Brush.radialGradient(listOf(P.gold.copy(alpha = 0.10f), Color.Transparent), center, fit.s(55f)),
        fit.s(55f), center,
    )
    if (art.floor) {
        val y = fit.o(V(0f, 92f)).y
        drawLine(P.floor, Offset(0f, y), Offset(size.width, y), fit.s(1.2f))
        var x = 0f
        while (x < size.width) { drawLine(P.floor.copy(alpha = 0.5f), Offset(x, y), Offset(x - fit.s(3f), y + fit.s(3f)), fit.s(0.6f)); x += fit.s(8f) }
    }

    // Movement trail
    if (trail && !art.hold) {
        val path = Path()
        for (i in 0..24) {
            val p = anchor(Skeleton(art.start.lerp(art.finish, i / 24f), art.frontView), art.trail)
            val o = fit.o(p)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, P.trail.copy(alpha = 0.8f), style = Stroke(fit.s(0.9f), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(fit.s(2.2f), fit.s(1.8f)))))
        val a = fit.o(anchor(Skeleton(art.start, art.frontView), art.trail))
        val bSk = Skeleton(art.finish, art.frontView)
        val b = fit.o(anchor(bSk, art.trail))
        val before = fit.o(anchor(Skeleton(art.start.lerp(art.finish, 0.9f), art.frontView), art.trail))
        drawArrowHead(before, b, P.trail, fit.s(3f))
        drawCircle(P.trail, fit.s(1.4f), a)
    }

    val ink = if (dimmed) 0.55f else 1f
    fun c(col: Color, k: Float = 1f) = Color(col.red * k * ink, col.green * k * ink, col.blue * k * ink, col.alpha)

    // Props behind the body
    art.props.forEach { prop ->
        when (prop) {
            is Prop.Poly -> drawPoly(prop.points, fit, material(prop.material))
            is Prop.Line -> drawLine(material(prop.material), fit.o(prop.a), fit.o(prop.b), fit.s(prop.width), StrokeCap.Round)
            is Prop.Disc -> if (prop.r > 0) { drawCircle(material(prop.material), fit.s(prop.r), fit.o(prop.c)); drawCircle(P.outline, fit.s(prop.r), fit.o(prop.c), style = Stroke(fit.s(0.6f))) }
            is Prop.Cable -> drawLine(P.steel.copy(alpha = 0.8f), fit.o(anchor(sk, prop.anchor)), fit.o(prop.pulley), fit.s(0.6f))
            is Prop.Sled -> {
                val center2 = sk.legNear.end + dir(pose.foot) * (Body.FOOT * 0.5f)
                val along = dir(prop.angle + 90f) * 14f
                drawLine(P.iron, fit.o(center2 + along + dir(pose.foot - 90f) * -1.5f), fit.o(center2 - along), fit.s(3.5f), StrokeCap.Round)
            }
            else -> {}
        }
    }
    art.props.filterIsInstance<Prop.Dumbbell>().filter { it.anchor == Anchor.HAND && !art.frontView }.forEach {
        drawDumbbell(anchor(sk, Anchor.HAND), fit, dim = 0.6f) // far hand's dumbbell
    }

    val muscles = art.primary
    val second = art.secondary
    fun muscleColor(m: Muscle): Color? = when (m) {
        in muscles -> P.musclePrimary.copy(alpha = 0.75f + 0.25f * glow)
        in second -> P.muscleSecondary.copy(alpha = 0.45f + 0.15f * glow)
        else -> null
    }

    // Far limbs (dimmer)
    val farK = if (art.frontView) 1f else 0.62f
    drawLeg(sk.legFar, sk.footFar, fit, farK * ink, ::muscleColor, glow)
    drawArm(sk.armFar, fit, farK * ink, ::muscleColor, glow, front = art.frontView)

    // Torso, head
    drawTorso(sk, fit, ink, ::muscleColor, glow, art.frontView)
    drawHead(sk, fit, ink, art.frontView)

    // Near limbs
    drawLeg(sk.legNear, sk.footNear, fit, ink, ::muscleColor, glow)
    drawArm(sk.armNear, fit, ink, ::muscleColor, glow, front = art.frontView)

    // Props in front
    art.props.forEach { prop ->
        when (prop) {
            is Prop.Barbell -> drawPlate(anchor(sk, prop.anchor), prop.r, fit)
            is Prop.Dumbbell -> drawDumbbell(anchor(sk, prop.anchor), fit)
            is Prop.Roller -> {
                val at = anchor(sk, prop.anchor) + prop.offset
                drawCircle(P.leather, fit.s(prop.r), fit.o(at)); drawCircle(P.outline, fit.s(prop.r), fit.o(at), style = Stroke(fit.s(0.6f)))
            }
            else -> {}
        }
    }
}

private fun material(m: Material): Color = when (m) {
    Material.WOOD -> FigurePalette.wood
    Material.IRON -> FigurePalette.iron
    Material.LEATHER -> FigurePalette.leather
    Material.STEEL -> FigurePalette.steel
    Material.GOLD -> FigurePalette.gold
}

private fun DrawScope.drawPoly(points: List<V>, fit: Fit, color: Color, outline: Boolean = true) {
    val path = Path().apply { points.forEachIndexed { i, p -> val o = fit.o(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
    drawPath(path, color)
    if (outline) drawPath(path, FigurePalette.outline, style = Stroke(fit.s(0.6f), join = StrokeJoin.Round))
}

/** Tapered capsule from a (width wa) to b (width wb). Returns its outline points. */
private fun capsule(a: V, b: V, wa: Float, wb: Float): List<V> {
    val n = (b - a).norm().perp()
    return listOf(a + n * (wa / 2), b + n * (wb / 2), b - n * (wb / 2), a - n * (wa / 2))
}

/** Front half of a limb (side the limb's "front" faces). */
private fun halfCapsule(a: V, b: V, wa: Float, wb: Float, front: Boolean, from: Float = 0.08f, to: Float = 0.92f): List<V> {
    val d = b - a
    val n = d.norm().perp() * (if (front) 1f else -1f)
    val p1 = a + d * from; val p2 = a + d * to
    val w1 = wa + (wb - wa) * from; val w2 = wa + (wb - wa) * to
    val mid1 = a + d * (from + (to - from) * 0.35f); val wm = wa + (wb - wa) * (from + (to - from) * 0.35f)
    return listOf(p1, p2, p2 + n * (w2 * 0.42f), mid1 + n * (wm * 0.5f), p1 + n * (w1 * 0.38f))
}

private fun DrawScope.segment(a: V, b: V, wa: Float, wb: Float, fit: Fit, fill: Color) {
    drawPoly(capsule(a, b, wa, wb), fit, fill, outline = false)
    drawCircle(fill, fit.s(wa / 2), fit.o(a)); drawCircle(fill, fit.s(wb / 2), fit.o(b))
    val outline = FigurePalette.outline
    val pts = capsule(a, b, wa, wb)
    drawLine(outline, fit.o(pts[0]), fit.o(pts[1]), fit.s(0.55f)); drawLine(outline, fit.o(pts[2]), fit.o(pts[3]), fit.s(0.55f))
}

private fun DrawScope.glowPoly(points: List<V>, fit: Fit, color: Color, glow: Float) {
    val path = Path().apply { points.forEachIndexed { i, p -> val o = fit.o(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
    drawPath(path, color.copy(alpha = 0.22f * (0.6f + glow)), style = Stroke(fit.s(2.6f), join = StrokeJoin.Round))
    drawPath(path, color, style = Fill)
}

private fun shade(c: Color, k: Float) = Color(c.red * k, c.green * k, c.blue * k, c.alpha)

private fun DrawScope.drawArm(limb: Limb, fit: Fit, k: Float, muscle: (Muscle) -> Color?, glow: Float, front: Boolean) {
    val P = FigurePalette
    segment(limb.start, limb.joint, 5.6f, 4.6f, fit, shade(P.armour, k))
    muscle(Muscle.BICEPS)?.let { glowPoly(halfCapsule(limb.start, limb.joint, 5.6f, 4.6f, front = true), fit, shade(it, k.coerceAtLeast(0.8f)), glow) }
    muscle(Muscle.TRICEPS)?.let { glowPoly(halfCapsule(limb.start, limb.joint, 5.6f, 4.6f, front = false), fit, shade(it, k.coerceAtLeast(0.8f)), glow) }
    segment(limb.joint, limb.end, 4.6f, 3.6f, fit, shade(P.armourLight, k))
    muscle(Muscle.FOREARMS)?.let { glowPoly(capsule(limb.joint + (limb.end - limb.joint) * 0.1f, limb.end - (limb.end - limb.joint) * 0.25f, 3.8f, 2.8f), fit, shade(it, k.coerceAtLeast(0.8f)), glow) }
    // gold bracer
    val br = limb.joint + (limb.end - limb.joint) * 0.72f
    val n = (limb.end - limb.joint).norm().perp()
    drawLine(shade(P.gold, k), fit.o(br + n * 2f), fit.o(br - n * 2f), fit.s(1.3f), StrokeCap.Round)
    drawCircle(shade(P.armourLight, k), fit.s(2.3f), fit.o(limb.end))
    drawCircle(P.outline, fit.s(2.3f), fit.o(limb.end), style = Stroke(fit.s(0.5f)))
}

private fun DrawScope.drawLeg(limb: Limb, foot: V, fit: Fit, k: Float, muscle: (Muscle) -> Color?, glow: Float) {
    val P = FigurePalette
    segment(limb.start, limb.joint, 8.2f, 6.2f, fit, shade(P.armour, k))
    muscle(Muscle.QUADS)?.let { glowPoly(halfCapsule(limb.start, limb.joint, 8.2f, 6.2f, front = true), fit, shade(it, k.coerceAtLeast(0.8f)), glow) }
    muscle(Muscle.HAMSTRINGS)?.let { glowPoly(halfCapsule(limb.start, limb.joint, 8.2f, 6.2f, front = false), fit, shade(it, k.coerceAtLeast(0.8f)), glow) }
    segment(limb.joint, limb.end, 6.2f, 4.4f, fit, shade(P.armourLight, k))
    muscle(Muscle.CALVES)?.let { glowPoly(halfCapsule(limb.joint, limb.end, 6.2f, 4.4f, front = false, from = 0.12f, to = 0.7f), fit, shade(it, k.coerceAtLeast(0.8f)), glow) }
    // knee guard
    drawCircle(shade(P.gold, k), fit.s(1.5f), fit.o(limb.joint))
    // boot
    drawLine(shade(P.boot, k), fit.o(limb.end), fit.o(foot), fit.s(3.8f), StrokeCap.Round)
}

private fun DrawScope.drawTorso(sk: Skeleton, fit: Fit, k: Float, muscle: (Muscle) -> Color?, glow: Float, front: Boolean) {
    val P = FigurePalette
    val u = sk.up; val f = sk.fwd; val h = sk.hip
    if (front) {
        val side = V(1f, 0f)
        val pts = listOf(h + side * 6f, h + u * 20f + side * 10f, sk.shoulder + side * 9.5f, sk.shoulder - side * 9.5f, h + u * 20f - side * 10f, h - side * 6f)
        drawPoly(pts, fit, shade(P.tunic, k))
        muscle(Muscle.CHEST)?.let { glowPoly(listOf(h + u * 18f - side * 8f, h + u * 18f + side * 8f, h + u * 26f + side * 8f, h + u * 26f - side * 8f), fit, it, glow) }
        muscle(Muscle.CORE)?.let { glowPoly(listOf(h + u * 3f - side * 4f, h + u * 3f + side * 4f, h + u * 16f + side * 5f, h + u * 16f - side * 5f), fit, it, glow) }
        drawLine(shade(P.gold, k), fit.o(h + u * 3f - side * 6f), fit.o(h + u * 3f + side * 6f), fit.s(1.6f))
        listOf(sk.shoulderNear, sk.shoulderFar).forEach { s ->
            val col = muscle(Muscle.SHOULDERS)
            drawCircle(col ?: shade(P.armourLight, k), fit.s(4.4f), fit.o(s))
            if (col != null) drawCircle(col.copy(alpha = 0.25f), fit.s(6f), fit.o(s))
            drawCircle(P.outline, fit.s(4.4f), fit.o(s), style = Stroke(fit.s(0.5f)))
        }
        return
    }
    val pts = listOf(
        h - f * 5.6f, h + f * 5.8f, h + u * 20f + f * 7.6f, sk.shoulder + f * 5.4f + u * 1.2f,
        sk.shoulder - f * 5.2f + u * 1.2f, h + u * 18f - f * 6.6f,
    )
    drawPoly(pts, fit, shade(P.tunic, k))
    muscle(Muscle.CHEST)?.let { glowPoly(listOf(h + u * 15f + f * 0.5f, h + u * 26.5f + f * 0.5f, h + u * 26.5f + f * 5.2f, h + u * 21f + f * 7.2f, h + u * 15f + f * 6.6f), fit, it, glow) }
    muscle(Muscle.CORE)?.let { glowPoly(listOf(h + u * 4f + f * 0.5f, h + u * 13.5f + f * 0.5f, h + u * 13.5f + f * 6.7f, h + u * 4f + f * 5.8f), fit, it, glow) }
    muscle(Muscle.BACK)?.let { glowPoly(listOf(h + u * 6f - f * 0.5f, h + u * 26.5f - f * 0.5f, h + u * 26.5f - f * 5f, h + u * 18f - f * 6.4f, h + u * 6f - f * 5.6f), fit, it, glow) }
    muscle(Muscle.GLUTES)?.let { glowPoly(listOf(h - f * 1.5f + u * 2f, h - f * 6.2f + u * 2f, h - f * 6.5f - u * 3.5f, h - f * 2f - u * 4.5f), fit, it, glow) }
    // belt
    drawLine(shade(P.gold, k), fit.o(h + u * 3f - f * 5.7f), fit.o(h + u * 3f + f * 5.9f), fit.s(1.6f))
    // pauldron (shoulder guard)
    val col = muscle(Muscle.SHOULDERS)
    if (col != null) drawCircle(col.copy(alpha = 0.25f * (0.6f + glow)), fit.s(6.2f), fit.o(sk.shoulder))
    drawCircle(col ?: shade(P.armourLight, k), fit.s(4.6f), fit.o(sk.shoulder))
    drawCircle(P.outline, fit.s(4.6f), fit.o(sk.shoulder), style = Stroke(fit.s(0.5f)))
    drawCircle(shade(P.gold, k), fit.s(1f), fit.o(sk.shoulder))
}

private fun DrawScope.drawHead(sk: Skeleton, fit: Fit, k: Float, front: Boolean) {
    val P = FigurePalette
    val c = sk.headC; val u = sk.headUp; val f = V(-u.y, u.x)
    // neck
    drawLine(shade(P.armour, k), fit.o(sk.shoulder), fit.o(c), fit.s(3.6f))
    // plume
    val plume = if (front) listOf(c + u * 4.5f - f * 1.4f, c + u * 9.5f, c + u * 4.5f + f * 1.4f)
    else listOf(c + u * 4.8f + f * 1f, c + u * 8.5f - f * 2f, c - f * 7.5f + u * 1f, c - f * 4.5f + u * 3f)
    drawPoly(plume, fit, shade(Color(0xFFB8322A), k), outline = true)
    drawCircle(shade(P.helmet, k), fit.s(Body.HEAD_R), fit.o(c))
    drawCircle(P.outline, fit.s(Body.HEAD_R), fit.o(c), style = Stroke(fit.s(0.6f)))
    // visor slit / face guard
    if (front) {
        drawLine(P.gold, fit.o(c - f * 3.4f + u * 0.4f), fit.o(c + f * 3.4f + u * 0.4f), fit.s(1.2f), StrokeCap.Round)
        drawLine(P.gold, fit.o(c + u * 0.4f), fit.o(c - u * 4f), fit.s(1f), StrokeCap.Round)
    } else {
        drawLine(P.gold, fit.o(c + f * 1.2f + u * 0.6f), fit.o(c + f * 5.2f + u * 0.4f), fit.s(1.2f), StrokeCap.Round)
        drawLine(P.gold, fit.o(c + f * 3.4f + u * 0.5f), fit.o(c + f * 4.3f - u * 4.2f), fit.s(1f), StrokeCap.Round)
    }
}

private fun DrawScope.drawPlate(at: V, r: Float, fit: Fit) {
    val P = FigurePalette
    drawCircle(P.iron, fit.s(r), fit.o(at))
    drawCircle(P.gold, fit.s(r), fit.o(at), style = Stroke(fit.s(1f)))
    drawCircle(P.gold.copy(alpha = 0.35f), fit.s(r * 0.62f), fit.o(at), style = Stroke(fit.s(0.5f)))
    drawCircle(P.steel, fit.s(1.5f), fit.o(at))
}

private fun DrawScope.drawDumbbell(at: V, fit: Fit, dim: Float = 1f) {
    val P = FigurePalette
    drawCircle(shade(P.iron, dim), fit.s(3.6f), fit.o(at))
    drawCircle(shade(P.gold, dim), fit.s(3.6f), fit.o(at), style = Stroke(fit.s(0.8f)))
    drawCircle(shade(P.steel, dim), fit.s(1f), fit.o(at))
}

private fun DrawScope.drawArrowHead(from: Offset, to: Offset, color: Color, size: Float) {
    val d = Offset(to.x - from.x, to.y - from.y)
    val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y).coerceAtLeast(0.001f)
    val u = Offset(d.x / len, d.y / len)
    val n = Offset(-u.y, u.x)
    val p = Path().apply {
        moveTo(to.x + u.x * size * 0.4f, to.y + u.y * size * 0.4f)
        lineTo(to.x - u.x * size + n.x * size * 0.6f, to.y - u.y * size + n.y * size * 0.6f)
        lineTo(to.x - u.x * size - n.x * size * 0.6f, to.y - u.y * size - n.y * size * 0.6f)
        close()
    }
    drawPath(p, color)
}

@Suppress("unused") private fun clamp01(x: Float) = max(0f, min(1f, x))
