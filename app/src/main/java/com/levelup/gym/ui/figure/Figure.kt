package com.levelup.gym.ui.figure

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A small 2D "hero" rig. The figure is drawn side-on facing right (or front-on for a few moves).
 * World units: a standing hero is about 85 tall, the floor is at y = 92, y grows downward.
 * Angles are in degrees.
 */
enum class Muscle(val label: String) {
    CHEST("Chest"), BACK("Back"), SHOULDERS("Shoulders"), BICEPS("Biceps"), TRICEPS("Triceps"),
    FOREARMS("Forearms"), CORE("Core"), GLUTES("Glutes"), QUADS("Quads"), HAMSTRINGS("Hamstrings"), CALVES("Calves"),
}

data class V(val x: Float, val y: Float) {
    operator fun plus(o: V) = V(x + o.x, y + o.y)
    operator fun minus(o: V) = V(x - o.x, y - o.y)
    operator fun times(k: Float) = V(x * k, y * k)
    val length get() = sqrt(x * x + y * y)
    fun norm(): V { val l = length; return if (l < 1e-4f) V(0f, 1f) else V(x / l, y / l) }
    /** Perpendicular: for a limb pointing down (0,1) this is "forward" (1,0). */
    fun perp() = V(y, -x)
    fun lerp(o: V, t: Float) = V(x + (o.x - x) * t, y + (o.y - y) * t)
}

fun v(x: Number, y: Number) = V(x.toFloat(), y.toFloat())

/** 0° = straight down, 90° = forward (right), 180° = up. */
fun dir(deg: Float): V { val r = Math.toRadians(deg.toDouble()); return V(sin(r).toFloat(), cos(r).toFloat()) }

object Body {
    const val TORSO = 28f
    const val NECK = 3.5f
    const val HEAD_R = 5.6f
    const val UPPER_ARM = 15f
    const val FOREARM = 14f
    const val THIGH = 21f
    const val SHIN = 20f
    const val FOOT = 6.5f
    const val SHOULDER_HALF = 9.5f // front view
    const val HIP_HALF = 4.5f      // front view
}

/**
 * A pose. Hands and ankles are targets; elbows and knees are solved (two-bone IK).
 * [elbowBend] / [kneeBend] pick which side the joint folds to (+1 or -1).
 * Far-side limbs default to the near side.
 */
data class Pose(
    val hip: V,
    val torso: Float,
    val hand: V,
    val ankle: V,
    val handFar: V? = null,
    val ankleFar: V? = null,
    val foot: Float = 90f,
    val footFar: Float? = null,
    val elbowBend: Int = 1,
    val kneeBend: Int = -1,
    val elbowBendFar: Int? = null,
    val kneeBendFar: Int? = null,
    val head: Float = 0f,
) {
    fun lerp(o: Pose, t: Float) = Pose(
        hip = hip.lerp(o.hip, t),
        torso = torso + (o.torso - torso) * t,
        hand = hand.lerp(o.hand, t),
        ankle = ankle.lerp(o.ankle, t),
        handFar = (handFar ?: hand).lerp(o.handFar ?: o.hand, t),
        ankleFar = (ankleFar ?: ankle).lerp(o.ankleFar ?: o.ankle, t),
        foot = foot + (o.foot - foot) * t,
        footFar = (footFar ?: foot) + ((o.footFar ?: o.foot) - (footFar ?: foot)) * t,
        elbowBend = elbowBend, kneeBend = kneeBend,
        elbowBendFar = elbowBendFar, kneeBendFar = kneeBendFar,
        head = head + (o.head - head) * t,
    )
}

/** Solved joint positions for a pose. */
class Skeleton(val pose: Pose, val front: Boolean) {
    val up: V = if (front) V(0f, -1f) else V(sin(rad(pose.torso)), -cos(rad(pose.torso)))
    /** Direction the chest faces. */
    val fwd: V = V(-up.y, up.x)
    val hip = pose.hip
    val shoulder = hip + up * Body.TORSO
    val headUp: V = rotate(up, pose.head)
    val headC = shoulder + up * Body.NECK + headUp * Body.HEAD_R

    // Side view: one arm/leg near, one far. Front view: right side = "near", left side = "far".
    val shoulderNear = if (front) shoulder + V(Body.SHOULDER_HALF, 0f) else shoulder
    val shoulderFar = if (front) shoulder - V(Body.SHOULDER_HALF, 0f) else shoulder
    val hipNear = if (front) hip + V(Body.HIP_HALF, 0f) else hip
    val hipFar = if (front) hip - V(Body.HIP_HALF, 0f) else hip

    val armNear = ik(shoulderNear, pose.hand, Body.UPPER_ARM, Body.FOREARM, pose.elbowBend)
    val armFar = ik(shoulderFar, pose.handFar ?: pose.hand, Body.UPPER_ARM, Body.FOREARM, pose.elbowBendFar ?: pose.elbowBend)
    val legNear = ik(hipNear, pose.ankle, Body.THIGH, Body.SHIN, pose.kneeBend)
    val legFar = ik(hipFar, pose.ankleFar ?: pose.ankle, Body.THIGH, Body.SHIN, pose.kneeBendFar ?: pose.kneeBend)

    val footNear = legNear.end + dir(pose.foot) * Body.FOOT
    val footFar = legFar.end + dir(pose.footFar ?: pose.foot) * Body.FOOT

    fun points(): List<V> = listOf(
        hip, shoulder, headC + headUp * Body.HEAD_R, headC + fwd * Body.HEAD_R, headC - fwd * Body.HEAD_R,
        armNear.joint, armNear.end, armFar.joint, armFar.end,
        legNear.joint, legNear.end, legFar.joint, legFar.end, footNear, footFar,
    )

    companion object {
        fun rad(d: Float) = Math.toRadians(d.toDouble()).toFloat()
        fun rotate(p: V, deg: Float): V {
            val r = rad(deg); val c = cos(r); val s = sin(r)
            return V(p.x * c - p.y * s, p.x * s + p.y * c)
        }
    }
}

data class Limb(val start: V, val joint: V, val end: V)

/**
 * Two-bone IK. The end is clamped to what the limb can reach.
 * Near full reach the joint locks out: a standing leg or a pressed arm is drawn straight rather than
 * slightly bent. The bones shorten by at most 3.5% to get there, smoothly, so animations don't pop.
 */
fun ik(start: V, target0: V, l1o: Float, l2o: Float, bend: Int): Limb {
    val full = l1o + l2o
    val dist = (target0 - start).length
    val lockFrom = 0.85f * full
    val lockAt = 0.965f * full
    val k = when {
        dist <= lockFrom -> 1f
        dist < lockAt -> 1f - 0.035f * (dist - lockFrom) / (lockAt - lockFrom)
        else -> (dist / full).coerceAtMost(1f)
    }
    val l1 = l1o * k; val l2 = l2o * k
    val target = target0
    val d0 = target - start
    val u = d0.norm()
    val d = d0.length.coerceIn(abs(l1 - l2) + 0.01f, l1 + l2 - 0.0001f)
    val a = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
    val h = sqrt((l1 * l1 - a * a).coerceAtLeast(0f))
    val n = V(-u.y, u.x)
    val joint = start + u * a + n * (h * bend)
    val end = start + u * d
    return Limb(start, joint, end)
}

fun angleOf(p: V): Float = Math.toDegrees(atan2(p.x.toDouble(), p.y.toDouble())).toFloat()
