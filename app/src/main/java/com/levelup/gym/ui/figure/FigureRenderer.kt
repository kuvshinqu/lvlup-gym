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

/** Painted palette: muted steel armour, warm leather, ink outlines; worked muscles in a red-orange wash. */
object FigurePalette {
    val outline = Color(0xFF120E0C)
    val armour = Color(0xFF7D8BA6)
    val armourLight = Color(0xFF9AA7BF)
    val tunic = Color(0xFF34406A)
    val gold = Color(0xFFE9B949)
    val helmet = Color(0xFF8B97AF)
    val boot = Color(0xFF5A3B28)
    val glove = Color(0xFF6E4A31)
    val plume = Color(0xFFC0392B)
    val musclePrimary = Color(0xFFE8553A)
    val muscleSecondary = Color(0xFFF2A541)
    val floor = Color(0xFF4A4F63)
    val trail = Color(0xFF7CF29C)
    val iron = Color(0xFF2E3242)
    val steel = Color(0xFFA3ABBD)
    val leather = Color(0xFF7A4A32)
    val wood = Color(0xFF8A6440)
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
                is Prop.Stack -> { pts += V(prop.x - 7f, prop.top - 8f); pts += V(prop.x + 7f, 92f) }
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
 * Draws the hero at [t] between start (0) and finish (1) in a flat, painted style:
 * one ink silhouette per body part, flat colour, a light and a dark brush stroke for form,
 * and worked muscles as a warm paint wash. [glow] 0..1 pulses the wash. [trail] draws the movement path.
 */
fun DrawScope.drawHero(art: ExerciseArt, t: Float, fit: Fit, glow: Float = 0.6f, trail: Boolean = false, dimmed: Boolean = false) {
    val pose = art.start.lerp(art.finish, t)
    val sk = Skeleton(pose, art.frontView)
    val P = FigurePalette
    val pen = Pen(this, fit)

    if (art.floor) {
        val y = fit.o(V(0f, 92f)).y
        // a broad, uneven ground stroke
        pen.brushRaw(listOf(Offset(0f, y + fit.s(0.6f)), Offset(size.width, y + fit.s(0.9f))), fit.s(2.6f), P.floor.copy(alpha = 0.55f))
        pen.brushRaw(listOf(Offset(size.width * 0.04f, y), Offset(size.width * 0.96f, y - fit.s(0.2f))), fit.s(0.9f), P.floor)
        // cast shadow under the hero
        val feet = listOf(sk.footNear, sk.footFar, sk.legNear.end, sk.legFar.end).filter { it.y > 84f }
        if (feet.isNotEmpty()) {
            val cx = feet.map { it.x }.average().toFloat()
            val w = fit.s(18f); val c = fit.o(V(cx, 92f))
            drawOval(Color.Black.copy(alpha = 0.28f), Offset(c.x - w / 2, c.y - fit.s(1.3f)), androidx.compose.ui.geometry.Size(w, fit.s(2.6f)))
        }
    }

    if (trail && !art.hold) {
        val pts = (0..24).map { fit.o(anchor(Skeleton(art.start.lerp(art.finish, it / 24f), art.frontView), art.trail)) }
        val path = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(path, P.trail.copy(alpha = 0.85f), style = Stroke(fit.s(0.9f), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(fit.s(0.1f), fit.s(2.2f)))))
        drawArrowHead(pts[21], pts[24], P.trail, fit.s(3f))
        drawCircle(P.trail, fit.s(1.4f), pts[0])
    }

    val ink = if (dimmed) 0.55f else 1f
    fun c(col: Color, k: Float = 1f) = shade(col, k * ink)

    // Machines: weight stacks and lever arms
    val startKnee = Skeleton(art.start, art.frontView).legNear.joint
    art.props.filterIsInstance<Prop.Stack>().forEach { st ->
        val lever = art.props.filterIsInstance<Prop.Lever>().firstOrNull()
        val to = if (st.toLever && lever != null) (lever.pivot ?: startKnee) else st.via
        pen.drawStack(st, t, to, ink)
    }
    art.props.filterIsInstance<Prop.Lever>().forEach { lv ->
        val pivot = lv.pivot ?: startKnee
        val pad = anchor(sk, lv.anchor) + lv.offset
        pen.brush(listOf(pivot, pad), 2.2f, c(P.iron), outline = true)
        pen.disc(pivot, 2.2f, c(P.steel))
        pen.disc(pivot, 0.8f, c(P.iron))
    }

    // Props behind the body
    art.props.forEach { prop ->
        when (prop) {
            is Prop.Poly -> pen.shape(prop.points.map { fit.o(it) }, c(material(prop.material)), smooth = false)
            is Prop.Line -> pen.brush(listOf(prop.a, prop.b), prop.width, c(material(prop.material)), outline = true)
            is Prop.Disc -> if (prop.r > 0) pen.disc(prop.c, prop.r, c(material(prop.material)))
            is Prop.Cable -> drawLine(P.steel.copy(alpha = 0.85f), fit.o(anchor(sk, prop.anchor)), fit.o(prop.pulley), fit.s(0.55f))
            is Prop.Sled -> {
                val center2 = sk.legNear.end + dir(pose.foot) * (Body.FOOT * 0.5f)
                val along = dir(prop.angle + 90f) * 14f
                pen.brush(listOf(center2 + along + dir(pose.foot - 90f) * -1.5f, center2 - along), 3.5f, c(P.iron), outline = true)
                // weight plates loaded on the sled's horns
                val horn = center2 + dir(pose.foot - 90f) * -5f + along * 0.55f
                pen.disc(horn, 6f, c(P.iron)); pen.disc(horn, 4.4f, c(shade(P.iron, 1.25f))); pen.disc(horn, 1.1f, c(P.steel))
            }
            else -> {}
        }
    }
    art.props.filterIsInstance<Prop.Dumbbell>().filter { it.anchor == Anchor.HAND && !art.frontView }.forEach {
        pen.drawDumbbell(sk.armFar.end, ink, dim = 0.6f)
    }

    fun wash(m: Muscle): Color? = when (m) {
        in art.primary -> P.musclePrimary.copy(alpha = 0.92f + 0.08f * glow)
        in art.secondary -> P.muscleSecondary.copy(alpha = 0.85f)
        else -> null
    }

    val farK = if (art.frontView) 1f else 0.7f
    pen.drawLeg(sk.legFar, sk.footFar, farK * ink, ::wash, seed = 11)
    pen.drawArm(sk.armFar, farK * ink, ::wash, seed = 23)
    pen.drawTorso(sk, ink, ::wash, art.frontView)
    pen.drawHead(sk, ink, art.frontView)
    pen.drawLeg(sk.legNear, sk.footNear, ink, ::wash, seed = 37)
    pen.drawArm(sk.armNear, ink, ::wash, seed = 41)
    if (art.frontView) pen.drawShoulder(sk.shoulderNear, ink, wash(Muscle.SHOULDERS))

    art.props.forEach { prop ->
        when (prop) {
            is Prop.Barbell -> pen.drawPlate(anchor(sk, prop.anchor), prop.r, ink)
            is Prop.Dumbbell -> pen.drawDumbbell(anchor(sk, prop.anchor), ink)
            is Prop.Roller -> pen.disc(anchor(sk, prop.anchor) + prop.offset, prop.r, c(P.leather))
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

private fun shade(c: Color, k: Float) = Color((c.red * k).coerceIn(0f, 1f), (c.green * k).coerceIn(0f, 1f), (c.blue * k).coerceIn(0f, 1f), c.alpha)
private fun tint(c: Color, toward: Color, k: Float) = Color(c.red + (toward.red - c.red) * k, c.green + (toward.green - c.green) * k, c.blue + (toward.blue - c.blue) * k, c.alpha)

/** Light comes from the upper left; strokes on that side are lighter. */
private val LIGHT = V(-0.55f, -0.83f)

/** Small drawing toolkit for the painted look. All sizes in world units unless the name says raw. */
private class Pen(val scope: DrawScope, val fit: Fit) {
    val inkW = 1.05f

    /** Smooth closed path through screen points (midpoint quadratics), like a loaded brush. */
    fun smoothPath(pts: List<Offset>, closed: Boolean = true): Path = Path().apply {
        if (pts.size < 3) { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; if (closed) close(); return@apply }
        val n = pts.size
        fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
        val start = mid(pts[n - 1], pts[0])
        moveTo(start.x, start.y)
        for (i in 0 until n) {
            val p = pts[i]; val m = mid(p, pts[(i + 1) % n])
            quadraticTo(p.x, p.y, m.x, m.y)
        }
        close()
    }

    /** Filled shape with an ink outline. */
    fun shape(pts: List<Offset>, fill: Color, smooth: Boolean = true) {
        val path = if (smooth) smoothPath(pts) else Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
        scope.drawPath(path, FigurePalette.outline, style = Stroke(fit.s(inkW * 2), join = StrokeJoin.Round))
        scope.drawPath(path, fill)
    }

    fun disc(c: V, r: Float, fill: Color) {
        scope.drawCircle(FigurePalette.outline, fit.s(r + inkW), fit.o(c))
        scope.drawCircle(fill, fit.s(r), fit.o(c))
    }

    /** A brush stroke with a few bristle tracks, in world units. */
    fun brush(pts: List<V>, width: Float, color: Color, outline: Boolean = false, bristles: Boolean = false) {
        val o = pts.map { fit.o(it) }
        if (outline) brushRaw(o, fit.s(width + inkW * 2), FigurePalette.outline, bristles = false)
        brushRaw(o, fit.s(width), color, bristles)
    }

    fun brushRaw(o: List<Offset>, w: Float, color: Color, bristles: Boolean = true) {
        val path = Path().apply { o.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        scope.drawPath(path, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (!bristles || o.size < 2) return
        // bristle tracks: thin lighter and darker lines inside the stroke
        val d = Offset(o.last().x - o.first().x, o.last().y - o.first().y)
        val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y).coerceAtLeast(0.001f)
        val n = Offset(-d.y / len, d.x / len)
        listOf(-0.28f to 1.12f, 0.3f to 0.86f).forEach { (k, light) ->
            val p2 = Path().apply { o.forEachIndexed { i, p -> val q = Offset(p.x + n.x * w * k, p.y + n.y * w * k); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
            scope.drawPath(p2, shade(color, light).copy(alpha = color.alpha * 0.55f), style = Stroke(w * 0.18f, cap = StrokeCap.Round))
        }
    }
}

/** A limb as one tapered, slightly bulging shape from [a] through [j] to [b]. */
private class LimbShape(a: V, j: V, b: V, widths: FloatArray, seed: Int) {
    val spine = ArrayList<V>(); val w = ArrayList<Float>(); val normals = ArrayList<V>()

    init {
        // widths: start, bulge1, joint, bulge2, end
        val steps = 7
        for (i in 0..steps) { val s = i / steps.toFloat(); spine += a.lerp(j, s); w += profile(widths[0], widths[1], widths[2], s) }
        for (i in 1..steps) { val s = i / steps.toFloat(); spine += j.lerp(b, s); w += profile(widths[2], widths[3], widths[4], s) }
        for (i in spine.indices) {
            val prev = spine[(i - 1).coerceAtLeast(0)]; val next = spine[(i + 1).coerceAtMost(spine.size - 1)]
            normals += (next - prev).norm().perp()
        }
        // hand-drawn wobble, fixed per limb so it does not shimmer while animating
        for (i in w.indices) w[i] = w[i] * (1f + 0.05f * kotlin.math.sin(seed * 1.7f + i * 2.3f))
    }

    private fun profile(w0: Float, wb: Float, w1: Float, s: Float): Float =
        if (s < 0.32f) w0 + (wb - w0) * (s / 0.32f) else wb + (w1 - wb) * ((s - 0.32f) / 0.68f)

    fun outline(): List<V> {
        val left = spine.indices.map { spine[it] + normals[it] * (w[it] / 2) }
        val right = spine.indices.map { spine[it] - normals[it] * (w[it] / 2) }.reversed()
        val endCap = spine.last() + (spine.last() - spine[spine.size - 2]).norm() * (w.last() * 0.45f)
        val startCap = spine.first() - (spine[1] - spine.first()).norm() * (w.first() * 0.45f)
        return left + endCap + right + startCap
    }

    /** One side of a segment (0 = upper, 1 = lower), as a wash shape. side +1 = normal side. */
    fun half(segment: Int, side: Int, from: Float = 0.1f, to: Float = 0.85f): List<V> {
        val base = if (segment == 0) 0 else 7
        val idx = (0..7).map { base + it }.filter { val s = (it - base) / 7f; s in from..to }
        val inner = idx.map { spine[it] + normals[it] * (w[it] * -0.02f * side) }
        val outer = idx.map { spine[it] + normals[it] * (w[it] * 0.42f * side) }
        return inner + outer.reversed()
    }

    /** A stroke along one side, used for light and shadow. */
    fun sideLine(side: Int, k: Float, from: Int = 1, to: Int = spine.size - 2): List<V> =
        (from..to).map { spine[it] + normals[it] * (w[it] * k * side) }

    /** Which side (+1/-1) faces the light, judged at the middle. */
    fun litSide(): Int { val m = normals[normals.size / 2]; return if (m.x * LIGHT.x + m.y * LIGHT.y > 0) 1 else -1 }
}

private fun Pen.paintLimb(limb: LimbShape, base: Color, k: Float) {
    shape(limb.outline().map { fit.o(it) }, shade(base, k))
}

private fun Pen.formStrokes(shape: LimbShape, base: Color, k: Float, widthK: Float) {
    val lit = shape.litSide()
    brush(shape.sideLine(lit, 0.2f, 2, shape.spine.size - 4), widthK * 0.7f, tint(shade(base, k), Color.White, 0.3f).copy(alpha = 0.5f))
}

private fun Pen.paintWash(points: List<V>, color: Color) {
    val path = smoothPath(points.map { fit.o(it) })
    scope.drawPath(path, color)
    scope.drawPath(path, shade(color.copy(alpha = 1f), 0.55f), style = Stroke(fit.s(0.55f), join = StrokeJoin.Round))
}

private fun Pen.drawArm(limb: Limb, k: Float, wash: (Muscle) -> Color?, seed: Int) {
    val P = FigurePalette
    val s = LimbShape(limb.start, limb.joint, limb.end, floatArrayOf(5.4f, 5.9f, 4.2f, 4.5f, 3.3f), seed)
    paintLimb(s, P.armour, k)
    wash(Muscle.BICEPS)?.let { paintWash(s.half(0, 1), shade(it, k.coerceAtLeast(0.85f))) }
    wash(Muscle.TRICEPS)?.let { paintWash(s.half(0, -1), shade(it, k.coerceAtLeast(0.85f))) }
    wash(Muscle.FOREARMS)?.let { paintWash(s.half(1, 1, 0.05f, 0.65f) + s.half(1, -1, 0.05f, 0.65f).reversed(), shade(it, k.coerceAtLeast(0.85f))) }
    formStrokes(s, P.armour, k, 1.1f)
    // gold bracer and leather gauntlet
    val br = limb.joint + (limb.end - limb.joint) * 0.7f
    val n = (limb.end - limb.joint).norm().perp()
    brush(listOf(br + n * 2.1f, br - n * 2.1f), 1.2f, shade(P.gold, k))
    disc(limb.end, 2.2f, shade(P.glove, k))
}

private fun Pen.drawLeg(limb: Limb, foot: V, k: Float, wash: (Muscle) -> Color?, seed: Int) {
    val P = FigurePalette
    // boot first so the shin overlaps it
    brush(listOf(limb.end, foot), 3.6f, shade(P.boot, k), outline = true)
    val s = LimbShape(limb.start, limb.joint, limb.end, floatArrayOf(8.2f, 8.6f, 5.8f, 6.2f, 4.2f), seed)
    paintLimb(s, P.armour, k)
    wash(Muscle.QUADS)?.let { paintWash(s.half(0, 1), shade(it, k.coerceAtLeast(0.85f))) }
    wash(Muscle.HAMSTRINGS)?.let { paintWash(s.half(0, -1), shade(it, k.coerceAtLeast(0.85f))) }
    wash(Muscle.CALVES)?.let { paintWash(s.half(1, -1, 0.1f, 0.65f), shade(it, k.coerceAtLeast(0.85f))) }
    formStrokes(s, P.armour, k, 1.5f)
    // knee plate: a small painted crescent
    val kn = limb.joint
    val nn = (limb.end - limb.start).norm().perp()
    brush(listOf(kn + nn * 2.4f + (limb.end - kn).norm() * -1.2f, kn + nn * 2.8f, kn + nn * 2.4f + (limb.end - kn).norm() * 1.4f), 1.1f, shade(P.gold, k * 0.95f))
}

private fun Pen.drawShoulder(at: V, k: Float, wash: Color?) {
    val P = FigurePalette
    disc(at, 4.2f, wash ?: shade(P.armourLight, k))
    scope.drawArc(tint(wash ?: shade(P.armourLight, k), Color.White, 0.35f), 200f, 90f, false,
        topLeft = fit.o(at - V(2.8f, 2.8f)), size = androidx.compose.ui.geometry.Size(fit.s(5.6f), fit.s(5.6f)),
        style = Stroke(fit.s(0.9f), cap = StrokeCap.Round))
}

private fun Pen.drawTorso(sk: Skeleton, k: Float, wash: (Muscle) -> Color?, front: Boolean) {
    val P = FigurePalette
    val u = sk.up; val f = sk.fwd; val h = sk.hip
    if (front) {
        val side = V(1f, 0f)
        val pts = listOf(h - side * 6f - u * 1f, h + side * 6f - u * 1f, h + u * 20f + side * 9.6f, sk.shoulder + side * 9f + u * 1f, sk.shoulder - side * 9f + u * 1f, h + u * 20f - side * 9.6f)
        shape(pts.map { fit.o(it) }, shade(P.tunic, k))
        wash(Muscle.CHEST)?.let { paintWash(listOf(h + u * 18f - side * 7.5f, h + u * 18f + side * 7.5f, h + u * 26f + side * 7.5f, h + u * 26f - side * 7.5f), it) }
        wash(Muscle.CORE)?.let { paintWash(listOf(h + u * 4f - side * 4f, h + u * 4f + side * 4f, h + u * 16f + side * 5f, h + u * 16f - side * 5f), it) }
        brush(listOf(h + u * 24f - side * 6f, h + u * 24f + side * 4f), 1.2f, tint(shade(P.tunic, k), Color.White, 0.25f).copy(alpha = 0.6f))
        brush(listOf(h + u * 3f - side * 6.2f, h + u * 3f + side * 6.2f), 1.7f, shade(P.gold, k), outline = true)
        drawShoulder(sk.shoulderFar, k, wash(Muscle.SHOULDERS))
        return
    }
    val pts = listOf(
        h - f * 5.4f - u * 1f, h + f * 5.6f - u * 1f, h + u * 20f + f * 7.4f, sk.shoulder + f * 5.2f + u * 1.2f,
        sk.shoulder - f * 5f + u * 1.2f, h + u * 18f - f * 6.4f,
    )
    shape(pts.map { fit.o(it) }, shade(P.tunic, k))
    wash(Muscle.CHEST)?.let { paintWash(listOf(h + u * 15f + f * 0.8f, h + u * 26f + f * 0.8f, h + u * 26f + f * 4.8f, h + u * 21f + f * 6.8f, h + u * 15f + f * 6.2f), it) }
    wash(Muscle.CORE)?.let { paintWash(listOf(h + u * 4f + f * 0.8f, h + u * 13.5f + f * 0.8f, h + u * 13.5f + f * 6.4f, h + u * 4f + f * 5.4f), it) }
    wash(Muscle.BACK)?.let { paintWash(listOf(h + u * 6f - f * 0.8f, h + u * 26f - f * 0.8f, h + u * 26f - f * 4.6f, h + u * 18f - f * 6f, h + u * 6f - f * 5.2f), it) }
    wash(Muscle.GLUTES)?.let { paintWash(listOf(h - f * 1.5f + u * 2.5f, h - f * 5.8f + u * 2.5f, h - f * 6f - u * 3f, h - f * 2f - u * 4f), it) }
    // form: light along the chest, shadow along the back
    val litFront = (f.x * LIGHT.x + f.y * LIGHT.y) > 0
    val lightSide = if (litFront) f else f * -1f
    brush(listOf(h + u * 6f + lightSide * 3.6f, h + u * 22f + lightSide * 4.6f), 1.2f, tint(shade(P.tunic, k), Color.White, 0.28f).copy(alpha = 0.65f))
    brush(listOf(h + u * 5f - lightSide * 3.8f, h + u * 20f - lightSide * 4.2f), 1.4f, shade(P.tunic, k * 0.65f).copy(alpha = 0.6f))
    brush(listOf(h + u * 3f - f * 5.4f, h + u * 3f + f * 5.6f), 1.7f, shade(P.gold, k), outline = true)
    drawShoulder(sk.shoulder, k, wash(Muscle.SHOULDERS))
}

private fun Pen.drawHead(sk: Skeleton, k: Float, front: Boolean) {
    val P = FigurePalette
    val c = sk.headC; val u = sk.headUp; val f = V(-u.y, u.x)
    brush(listOf(sk.shoulder, c), 3.4f, shade(P.armour, k), outline = true)
    // plume: three brush strokes sweeping back
    val plumeCol = shade(P.plume, k)
    if (front) {
        brush(listOf(c + u * 4.6f, c + u * 7.5f, c + u * 9.6f + f * 0.6f), 2.2f, plumeCol, outline = true)
    } else {
        brush(listOf(c + u * 4.8f + f * 0.6f, c + u * 7.2f - f * 2f, c + u * 4.6f - f * 7f), 2.4f, plumeCol, outline = true)
        brush(listOf(c + u * 5.4f - f * 1f, c + u * 5.8f - f * 5f), 1.2f, tint(plumeCol, Color.White, 0.3f).copy(alpha = 0.7f))
    }
    disc(c, Body.HEAD_R, shade(P.helmet, k))
    // helmet highlight crescent
    scope.drawArc(tint(shade(P.helmet, k), Color.White, 0.38f), 195f, 80f, false,
        topLeft = fit.o(c - V(Body.HEAD_R * 0.68f, Body.HEAD_R * 0.68f)), size = androidx.compose.ui.geometry.Size(fit.s(Body.HEAD_R * 1.36f), fit.s(Body.HEAD_R * 1.36f)),
        style = Stroke(fit.s(1f), cap = StrokeCap.Round))
    if (front) {
        brush(listOf(c - f * 3.4f + u * 0.4f, c + f * 3.4f + u * 0.4f), 1.1f, P.outline)
        brush(listOf(c + u * 0.4f, c - u * 3.8f), 0.9f, P.outline)
        brush(listOf(c + u * 2.6f - f * 3f, c + u * 2.6f + f * 3f), 0.8f, shade(P.gold, k))
    } else {
        brush(listOf(c + f * 1.4f + u * 0.6f, c + f * 5.2f + u * 0.4f), 1.1f, P.outline)
        brush(listOf(c + f * 3.6f + u * 0.5f, c + f * 4.3f - u * 3.8f), 0.9f, P.outline)
        brush(listOf(c + u * 2.8f + f * 1f, c + u * 2.4f + f * 4.6f), 0.8f, shade(P.gold, k))
    }
}

private fun Pen.drawPlate(at: V, r: Float, k: Float) {
    val P = FigurePalette
    disc(at, r, shade(P.iron, k))
    scope.drawCircle(shade(P.gold, k), fit.s(r * 0.82f), fit.o(at), style = Stroke(fit.s(1.1f)))
    scope.drawArc(Color.White.copy(alpha = 0.22f), 200f, 70f, false,
        topLeft = fit.o(at - V(r * 0.55f, r * 0.55f)), size = androidx.compose.ui.geometry.Size(fit.s(r * 1.1f), fit.s(r * 1.1f)),
        style = Stroke(fit.s(1.2f), cap = StrokeCap.Round))
    disc(at, 1.5f, shade(P.steel, k))
}

private fun Pen.drawDumbbell(at: V, k: Float, dim: Float = 1f) {
    val P = FigurePalette
    disc(at, 3.4f, shade(P.iron, k * dim))
    scope.drawCircle(shade(P.gold, k * dim), fit.s(2.6f), fit.o(at), style = Stroke(fit.s(0.8f)))
    disc(at, 0.9f, shade(P.steel, k * dim))
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

/** A weight stack: frame, plates, pin; the top plates rise with [t]. Cable from its pulley to [to]. */
private fun Pen.drawStack(st: Prop.Stack, t: Float, to: V?, k: Float) {
    val P = FigurePalette
    val half = 5f
    val pulley = V(st.x, st.top - 3f)
    // cable first, so the frame sits over it
    val rise = st.lift * t
    val plateH = 2.4f
    val n = ((92f - (st.top + 12f)) / plateH).toInt().coerceIn(4, 12)
    val stackTop = 92f - n * plateH
    val lifted = 3
    val liftedTop = stackTop - rise
    scope.drawLine(P.steel.copy(alpha = 0.9f), fit.o(V(st.x, liftedTop)), fit.o(pulley), fit.s(0.6f))
    if (to != null) scope.drawLine(P.steel.copy(alpha = 0.9f), fit.o(pulley), fit.o(to), fit.s(0.6f))
    // frame
    brush(listOf(V(st.x - half - 1.4f, 92f), V(st.x - half - 1.4f, st.top - 4f), V(st.x + half + 1.4f, st.top - 4f), V(st.x + half + 1.4f, 92f)), 1.6f, shade(P.iron, k), outline = true)
    disc(pulley, 1.8f, shade(P.steel, k))
    // guide rod
    scope.drawLine(shade(P.steel, k * 0.8f), fit.o(V(st.x, st.top - 4f)), fit.o(V(st.x, 92f)), fit.s(0.5f))
    for (i in 0 until n) {
        val up = if (i >= n - lifted) rise else 0f
        val y = 92f - (i + 1) * plateH - up
        val col = if (i >= n - lifted) shade(P.gold, k * 0.9f) else shade(P.iron, k * 1.3f)
        shape(listOf(fit.o(V(st.x - half, y)), fit.o(V(st.x + half, y)), fit.o(V(st.x + half, y + plateH - 0.3f)), fit.o(V(st.x - half, y + plateH - 0.3f))), col, smooth = false)
    }
}
