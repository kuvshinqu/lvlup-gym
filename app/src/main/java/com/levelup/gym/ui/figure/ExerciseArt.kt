package com.levelup.gym.ui.figure

import com.levelup.gym.ui.figure.Muscle.BACK
import com.levelup.gym.ui.figure.Muscle.BICEPS
import com.levelup.gym.ui.figure.Muscle.CALVES
import com.levelup.gym.ui.figure.Muscle.CHEST
import com.levelup.gym.ui.figure.Muscle.CORE
import com.levelup.gym.ui.figure.Muscle.FOREARMS
import com.levelup.gym.ui.figure.Muscle.GLUTES
import com.levelup.gym.ui.figure.Muscle.HAMSTRINGS
import com.levelup.gym.ui.figure.Muscle.QUADS
import com.levelup.gym.ui.figure.Muscle.SHOULDERS
import com.levelup.gym.ui.figure.Muscle.TRICEPS

enum class Anchor { HAND, HAND_FAR, ANKLE, HIP, SHOULDER, UPPER_BACK }

enum class Material { WOOD, IRON, LEATHER, STEEL, GOLD }

sealed interface Prop {
    /** Barbell seen from the side: a plate on the bar end. */
    data class Barbell(val anchor: Anchor = Anchor.HAND, val r: Float = 9f) : Prop
    data class Dumbbell(val anchor: Anchor = Anchor.HAND) : Prop
    /** Cable from the hand (or ankle) to a pulley. */
    data class Cable(val anchor: Anchor, val pulley: V) : Prop
    /** Machine roller pad next to a joint, offset in the body's frame. */
    data class Roller(val anchor: Anchor, val offset: V = V(0f, 0f), val r: Float = 3.2f) : Prop
    /** Leg-press sled plate under the feet. */
    data class Sled(val angle: Float) : Prop
    data class Poly(val points: List<V>, val material: Material) : Prop
    data class Line(val a: V, val b: V, val width: Float, val material: Material) : Prop
    data class Disc(val c: V, val r: Float, val material: Material) : Prop
    /** Machine lever arm from a fixed pivot (default: the near knee in the start pose) to a pad at the anchor. */
    data class Lever(val anchor: Anchor, val offset: V = V(0f, 0f), val pivot: V? = null) : Prop
    /**
     * Weight stack standing on the floor at [x], frame top at [top]. The top plates rise with the movement.
     * A cable runs from the stack's pulley to [via] (a fixed pulley), or to the lever pivot when [toLever] is set.
     */
    data class Stack(val x: Float, val top: Float, val via: V? = null, val toLever: Boolean = false, val lift: Float = 7f) : Prop
}

data class ExerciseArt(
    val start: Pose,
    val finish: Pose,
    val primary: Set<Muscle>,
    val secondary: Set<Muscle> = emptySet(),
    val props: List<Prop> = emptyList(),
    /** The point whose path from start to finish is drawn as the movement arrow. */
    val trail: Anchor = Anchor.HAND,
    val frontView: Boolean = false,
    val floor: Boolean = true,
    /** Static hold (plank): no movement arrow. */
    val hold: Boolean = false,
)

private fun rect(x: Number, y: Number, w: Number, h: Number, m: Material) =
    Prop.Poly(listOf(v(x, y), v(x.toFloat() + w.toFloat(), y), v(x.toFloat() + w.toFloat(), y.toFloat() + h.toFloat()), v(x, y.toFloat() + h.toFloat())), m)

private fun line(x1: Number, y1: Number, x2: Number, y2: Number, w: Number = 2, m: Material = Material.IRON) =
    Prop.Line(v(x1, y1), v(x2, y2), w.toFloat(), m)

/** Flat bench: top surface at [top], from x1 to x2. */
private fun flatBench(x1: Number, x2: Number, top: Number = 66) = listOf(
    line(x1.toFloat() + 5, top, x1.toFloat() + 5, 92, 2.5),
    line(x2.toFloat() - 5, top, x2.toFloat() - 5, 92, 2.5),
    rect(x1, top, x2.toFloat() - x1.toFloat(), 4, Material.LEATHER),
)


/** Same move with dumbbells in place of a barbell. */
private fun ExerciseArt.dumbbells() = copy(props = props.map { if (it is Prop.Barbell) Prop.Dumbbell(it.anchor) else it })

/** Same move with a barbell in place of dumbbells. */
private fun ExerciseArt.barbell(r: Float = 9f) = copy(props = props.map { if (it is Prop.Dumbbell) Prop.Barbell(it.anchor, r) else it })

/** Smith machine: the bar rides a fixed vertical rail at [x]. */
private fun ExerciseArt.smith(x: Float) = copy(
    props = listOf(line(x, -4, x, 92, 1.8, Material.STEEL), line(x - 8, -4, x + 8, -4, 3), line(x - 8, 92, x + 8, 92, 3)) +
        props.map { if (it is Prop.Dumbbell) Prop.Barbell(it.anchor, 6.5f) else it },
)

private fun ExerciseArt.plus(vararg extra: Prop) = copy(props = props + extra)
private fun ExerciseArt.without(drop: (Prop) -> Boolean) = copy(props = props.filterNot(drop))

private val standing = Pose(hip = v(50, 51), torso = 0f, hand = v(52, 52), ankle = v(51, 91))

object ExerciseArts {
    private val arts: Map<String, ExerciseArt> = mapOf(
        "Bench Press" to ExerciseArt(
            // Arms locked over the shoulders, then the bar comes down to the lower chest with the elbows dropping below the bench.
            start = Pose(hip = v(60, 61), torso = -90f, hand = v(34, 33), ankle = v(77, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(60, 61), torso = -90f, hand = v(43, 55), ankle = v(77, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(CHEST), secondary = setOf(TRICEPS, SHOULDERS),
            props = flatBench(16, 66) + Prop.Barbell(r = 6.5f),
        ),
        "Incline Dumbbell Press" to ExerciseArt(
            start = Pose(hip = v(58, 64), torso = -55f, hand = v(44, 45), ankle = v(76, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(58, 64), torso = -55f, hand = v(37, 20), ankle = v(76, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(CHEST), secondary = setOf(SHOULDERS, TRICEPS),
            props = listOf(
                line(60, 69, 60, 92, 2.5), line(40, 69, 32, 92, 2.5),
                rect(52, 67, 14, 4, Material.LEATHER),
                Prop.Poly(listOf(v(56, 68), v(30, 50), v(27, 54), v(53, 72)), Material.LEATHER),
                Prop.Dumbbell(),
            ),
        ),
        "Push-up" to ExerciseArt(
            start = Pose(hip = v(40.7, 73.9), torso = 66.9f, hand = v(68, 91), ankle = v(1, 90), foot = 150f, elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(42.5, 81), torso = 79f, hand = v(68, 91), ankle = v(1, 90), foot = 150f, elbowBend = 1, kneeBend = -1),
            primary = setOf(CHEST), secondary = setOf(TRICEPS, SHOULDERS, CORE),
            trail = Anchor.SHOULDER,
        ),
        "Barbell Row" to ExerciseArt(
            start = Pose(hip = v(44, 56), torso = 55f, hand = v(66, 69), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(44, 56), torso = 55f, hand = v(57, 56), ankle = v(51, 91), elbowBend = 1),
            primary = setOf(BACK), secondary = setOf(BICEPS, FOREARMS),
            props = listOf(Prop.Barbell()),
        ),
        "Pull-up" to ExerciseArt(
            start = Pose(hip = v(56.9, 61.7), torso = -8f, hand = v(55, 6), ankle = v(47, 89), elbowBend = 1, foot = 20f),
            finish = Pose(hip = v(55.9, 41.7), torso = -8f, hand = v(55, 6), ankle = v(46, 69), elbowBend = 1, foot = 20f),
            primary = setOf(BACK), secondary = setOf(BICEPS, FOREARMS),
            props = listOf(Prop.Disc(v(55, 6), 2.2f, Material.STEEL), line(30, 6, 80, 6, 1.2, Material.STEEL)),
            trail = Anchor.SHOULDER, floor = false,
        ),
        "Lat Pulldown" to ExerciseArt(
            start = Pose(hip = v(45, 67), torso = -12f, hand = v(44, 12), ankle = v(67, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(45, 67), torso = -12f, hand = v(48, 37), ankle = v(67, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(BACK), secondary = setOf(BICEPS),
            props = flatBench(30, 56, 70) + listOf(Prop.Stack(16f, 6f, via = v(42, -6)), line(16, -6, 42, -6, 2.5), Prop.Disc(v(42, -6), 2.4f, Material.STEEL), Prop.Roller(Anchor.ANKLE, v(-3f, -22f), 3.4f), Prop.Cable(Anchor.HAND, v(42, -6))),
        ),
        "Seated Cable Row" to ExerciseArt(
            start = Pose(hip = v(35, 80), torso = 18f, hand = v(62, 62), ankle = v(70, 84), foot = 160f, elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(35, 80), torso = -6f, hand = v(46, 67), ankle = v(70, 84), foot = 160f, elbowBend = 1, kneeBend = -1),
            primary = setOf(BACK), secondary = setOf(BICEPS),
            props = listOf(rect(22, 84, 26, 4, Material.LEATHER), Prop.Stack(90f, 46f, via = v(78, 66)), line(76, 72, 76, 92, 3), Prop.Cable(Anchor.HAND, v(78, 66)), Prop.Disc(v(78, 66), 2.5f, Material.STEEL)),
        ),
        "Deadlift" to ExerciseArt(
            start = Pose(hip = v(37, 60), torso = 76f, hand = v(58, 83), ankle = v(52, 91)),
            finish = Pose(hip = v(50, 51), torso = -2f, hand = v(53, 52), ankle = v(52, 91)),
            primary = setOf(BACK, GLUTES, HAMSTRINGS), secondary = setOf(QUADS, FOREARMS),
            props = listOf(Prop.Barbell()),
        ),
        "Squat" to ExerciseArt(
            start = Pose(hip = v(48, 51), torso = 6f, hand = v(46, 30), ankle = v(52, 91), elbowBend = 1),
            finish = Pose(hip = v(32, 71), torso = 42f, hand = v(47, 56), ankle = v(52, 91), elbowBend = 1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(CORE, HAMSTRINGS),
            props = listOf(Prop.Barbell(Anchor.UPPER_BACK)),
            trail = Anchor.HIP,
        ),
        "Romanian Deadlift" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = 0f, hand = v(52, 52), ankle = v(52, 91)),
            finish = Pose(hip = v(40, 53), torso = 70f, hand = v(63, 72), ankle = v(52, 91)),
            primary = setOf(HAMSTRINGS, GLUTES), secondary = setOf(BACK),
            props = listOf(Prop.Barbell()),
        ),
        "Leg Press" to ExerciseArt(
            start = Pose(hip = v(30, 70), torso = -62f, hand = v(36, 74), ankle = v(58, 56), foot = 150f, kneeBend = -1),
            finish = Pose(hip = v(30, 70), torso = -62f, hand = v(36, 74), ankle = v(67, 49), foot = 150f, kneeBend = -1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(HAMSTRINGS),
            props = listOf(
                Prop.Poly(listOf(v(24, 76), v(4, 62), v(1, 66), v(22, 81)), Material.LEATHER),
                rect(18, 74, 22, 5, Material.LEATHER), line(28, 79, 28, 92, 3),
                line(50, 76, 92, 46, 2.5), Prop.Sled(-35f),
            ),
            trail = Anchor.ANKLE,
        ),
        "Bulgarian Split Squat" to ExerciseArt(
            start = Pose(hip = v(46, 52), torso = 6f, hand = v(48, 80), ankle = v(58, 91), ankleFar = v(22, 69), footFar = 175f, kneeBendFar = -1),
            finish = Pose(hip = v(44, 66), torso = 14f, hand = v(48, 93), ankle = v(58, 91), ankleFar = v(22, 69), footFar = 175f, kneeBendFar = -1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(HAMSTRINGS),
            props = flatBench(6, 26, 71) + Prop.Dumbbell(),
            trail = Anchor.HIP,
        ),
        "Leg Curl" to ExerciseArt(
            start = Pose(hip = v(40, 60), torso = 90f, hand = v(72, 67), ankle = v(-1, 62), foot = 0f, elbowBend = -1, kneeBend = -1),
            finish = Pose(hip = v(40, 60), torso = 90f, hand = v(72, 67), ankle = v(26, 41), foot = 270f, elbowBend = -1, kneeBend = -1),
            primary = setOf(HAMSTRINGS), secondary = setOf(CALVES),
            props = flatBench(18, 72, 64) + listOf(Prop.Stack(-14f, 34f, toLever = true), Prop.Lever(Anchor.ANKLE, v(0f, -3.5f)), Prop.Roller(Anchor.ANKLE, v(0f, -3.5f), 3.4f)),
            trail = Anchor.ANKLE,
        ),
        "Leg Extension" to ExerciseArt(
            start = Pose(hip = v(40, 64), torso = -6f, hand = v(46, 68), ankle = v(62, 85), foot = 100f, kneeBend = -1),
            finish = Pose(hip = v(40, 64), torso = -6f, hand = v(46, 68), ankle = v(80, 62), foot = 170f, kneeBend = -1),
            primary = setOf(QUADS),
            props = listOf(
                rect(30, 66, 32, 5, Material.LEATHER), line(46, 71, 46, 92, 3),
                Prop.Poly(listOf(v(29, 66), v(25, 36), v(30, 35), v(34, 66)), Material.LEATHER),
                Prop.Stack(12f, 30f, toLever = true), Prop.Lever(Anchor.ANKLE, v(0f, 3.5f)),
                Prop.Roller(Anchor.ANKLE, v(0f, 3.5f), 3.4f),
            ),
            trail = Anchor.ANKLE,
        ),
        "Calf Raise" to ExerciseArt(
            start = Pose(hip = v(51, 47), torso = 0f, hand = v(53, 75), ankle = v(52.4, 88), foot = 110f),
            finish = Pose(hip = v(54, 40), torso = 0f, hand = v(56, 68), ankle = v(55, 81), foot = 30f),
            primary = setOf(CALVES),
            props = listOf(rect(56, 86, 20, 6, Material.WOOD), Prop.Dumbbell()),
            trail = Anchor.HIP,
        ),
        "Overhead Press" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = -3f, hand = v(55, 27), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(50, 51), torso = -3f, hand = v(49, -6), ankle = v(51, 91), elbowBend = 1),
            primary = setOf(SHOULDERS), secondary = setOf(TRICEPS, CORE),
            props = listOf(Prop.Barbell()),
        ),
        "Dumbbell Shoulder Press" to ExerciseArt(
            start = Pose(hip = v(45, 66), torso = -3f, hand = v(51, 36), ankle = v(68, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(45, 66), torso = -3f, hand = v(45, 10), ankle = v(68, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(SHOULDERS), secondary = setOf(TRICEPS),
            props = listOf(
                rect(34, 68, 22, 4, Material.LEATHER), line(45, 72, 45, 92, 3),
                Prop.Poly(listOf(v(33, 68), v(32, 36), v(37, 36), v(38, 68)), Material.LEATHER),
                Prop.Dumbbell(),
            ),
        ),
        "Lateral Raise" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = 0f, hand = v(61, 51.5), handFar = v(39, 51.5), ankle = v(56, 91), ankleFar = v(44, 91), elbowBend = 1, elbowBendFar = -1, foot = 90f, footFar = 270f),
            finish = Pose(hip = v(50, 51), torso = 0f, hand = v(88, 24), handFar = v(12, 24), ankle = v(56, 91), ankleFar = v(44, 91), elbowBend = 1, elbowBendFar = -1, foot = 90f, footFar = 270f),
            primary = setOf(SHOULDERS),
            props = listOf(Prop.Dumbbell(), Prop.Dumbbell(Anchor.HAND_FAR)),
            frontView = true,
        ),
        "Bicep Curl" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = 0f, hand = v(52, 52), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(50, 51), torso = 0f, hand = v(57, 27), ankle = v(51, 91), elbowBend = -1),
            primary = setOf(BICEPS), secondary = setOf(FOREARMS),
            props = listOf(Prop.Dumbbell()),
        ),
        "Hammer Curl" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = 0f, hand = v(52, 52), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(50, 51), torso = 0f, hand = v(57, 28), ankle = v(51, 91), elbowBend = -1),
            primary = setOf(BICEPS, FOREARMS),
            props = listOf(Prop.Dumbbell()),
        ),
        "Tricep Pushdown" to ExerciseArt(
            start = Pose(hip = v(48, 51), torso = 10f, hand = v(62, 34), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(48, 51), torso = 10f, hand = v(56, 54), ankle = v(51, 91), elbowBend = 1),
            primary = setOf(TRICEPS),
            props = listOf(Prop.Stack(90f, 20f, via = v(76, -2)), line(78, -4, 78, 92, 3), Prop.Disc(v(76, -2), 2.5f, Material.STEEL), Prop.Cable(Anchor.HAND, v(76, -2))),
        ),
        "Overhead Tricep Extension" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = -2f, hand = v(43, 17), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(50, 51), torso = -2f, hand = v(50, -6), ankle = v(51, 91), elbowBend = 1),
            primary = setOf(TRICEPS), secondary = setOf(CORE),
            props = listOf(Prop.Dumbbell()),
        ),
        "Plank (reps = seconds)" to ExerciseArt(
            start = Pose(hip = v(38.6, 81.7), torso = 78.3f, hand = v(78, 90), ankle = v(-1.6, 90), foot = 150f, elbowBend = 1),
            finish = Pose(hip = v(38.6, 81.2), torso = 78.0f, hand = v(78, 90), ankle = v(-1.6, 90), foot = 150f, elbowBend = 1),
            primary = setOf(CORE), secondary = setOf(SHOULDERS, GLUTES),
            hold = true,
        ),
        "Hanging Leg Raise" to ExerciseArt(
            start = Pose(hip = v(55, 62), torso = -2f, hand = v(55, 6), ankle = v(56, 103), foot = 10f, elbowBend = 1),
            finish = Pose(hip = v(54, 60), torso = -12f, hand = v(55, 6), ankle = v(93, 50), foot = 160f, elbowBend = 1, kneeBend = -1),
            primary = setOf(CORE), secondary = setOf(FOREARMS),
            props = listOf(Prop.Disc(v(55, 6), 2.2f, Material.STEEL), line(30, 6, 80, 6, 1.2, Material.STEEL)),
            trail = Anchor.ANKLE, floor = false,
        ),
        "Running" to ExerciseArt(
            start = Pose(hip = v(50, 52), torso = 10f, hand = v(43, 48), handFar = v(65, 38), ankle = v(66, 88), ankleFar = v(30, 74), footFar = 30f, elbowBend = 1, elbowBendFar = -1, kneeBend = -1, kneeBendFar = -1),
            finish = Pose(hip = v(50, 52), torso = 10f, hand = v(65, 38), handFar = v(43, 48), ankle = v(30, 74), ankleFar = v(66, 88), foot = 30f, footFar = 90f, elbowBend = 1, elbowBendFar = -1, kneeBend = -1, kneeBendFar = -1),
            primary = setOf(QUADS, CALVES), secondary = setOf(HAMSTRINGS, GLUTES),
            trail = Anchor.ANKLE,
        ),
        "Cycling" to ExerciseArt(
            start = Pose(hip = v(40, 55), torso = 42f, hand = v(70, 46), ankle = v(55, 84), ankleFar = v(41, 77), kneeBend = -1),
            finish = Pose(hip = v(40, 55), torso = 42f, hand = v(70, 46), ankle = v(41, 77), ankleFar = v(55, 84), kneeBend = -1),
            primary = setOf(QUADS), secondary = setOf(HAMSTRINGS, CALVES, GLUTES),
            props = listOf(
                line(48, 80, 40, 58, 3), line(48, 80, 70, 48, 3), line(48, 80, 48, 92, 3), line(36, 92, 64, 92, 3),
                rect(32, 56, 14, 3, Material.LEATHER), line(70, 48, 72, 44, 3),
                Prop.Disc(v(48, 80), 8f, Material.STEEL),
            ),
            trail = Anchor.ANKLE,
        ),
        "Rowing Machine" to ExerciseArt(
            start = Pose(hip = v(40, 80), torso = 32f, hand = v(69, 65), ankle = v(66, 84), foot = 150f, kneeBend = -1),
            finish = Pose(hip = v(22, 80), torso = -22f, hand = v(32, 66), ankle = v(61, 84), foot = 150f, kneeBend = -1),
            primary = setOf(BACK, QUADS), secondary = setOf(BICEPS, HAMSTRINGS),
            props = listOf(
                line(4, 88, 84, 88, 3), rect(16, 83, 30, 3, Material.IRON),
                Prop.Disc(v(80, 76), 9f, Material.IRON), Prop.Cable(Anchor.HAND, v(78, 72)),
            ),
            trail = Anchor.HIP,
        ),
        "Incline Walk" to ExerciseArt(
            start = Pose(hip = v(48, 50), torso = 8f, hand = v(42, 50), handFar = v(60, 46), ankle = v(60, 89), ankleFar = v(38, 92), footFar = 70f, elbowBend = 1, elbowBendFar = -1),
            finish = Pose(hip = v(48, 50), torso = 8f, hand = v(60, 46), handFar = v(42, 50), ankle = v(38, 92), ankleFar = v(60, 89), foot = 70f, footFar = 90f, elbowBend = 1, elbowBendFar = -1),
            primary = setOf(GLUTES, CALVES), secondary = setOf(HAMSTRINGS, QUADS),
            props = listOf(Prop.Poly(listOf(v(10, 96), v(92, 86), v(92, 90), v(10, 100)), Material.IRON), line(84, 87, 86, 40, 3), line(86, 40, 72, 40, 3)),
            trail = Anchor.ANKLE, floor = false,
        ),
        "Elliptical" to ExerciseArt(
            start = Pose(hip = v(46, 46), torso = 6f, hand = v(66, 38), handFar = v(56, 44), ankle = v(60, 84), ankleFar = v(38, 82), elbowBend = 1),
            finish = Pose(hip = v(46, 46), torso = 6f, hand = v(56, 44), handFar = v(66, 38), ankle = v(38, 82), ankleFar = v(60, 84), elbowBend = 1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(HAMSTRINGS, BACK),
            props = listOf(line(30, 88, 76, 88, 3), line(74, 88, 70, 30, 3), Prop.Disc(v(76, 80), 8f, Material.IRON)),
            trail = Anchor.ANKLE,
        ),
    )

    /** The hero's idle stance for the hero card. */
    val idle = ExerciseArt(
        start = Pose(hip = v(50, 51), torso = 0f, hand = v(64, 60), handFar = v(37, 52), ankle = v(57, 91), ankleFar = v(43, 91), elbowBend = 1, elbowBendFar = -1, foot = 90f, footFar = 270f),
        finish = Pose(hip = v(50, 51), torso = 0f, hand = v(64, 60), handFar = v(37, 52), ankle = v(57, 91), ankleFar = v(43, 91), elbowBend = 1, elbowBendFar = -1, foot = 90f, footFar = 270f),
        primary = emptySet(),
        props = listOf(
            Prop.Line(v(66, 54), v(66, 90), 2.2f, Material.STEEL),
            Prop.Line(v(61, 57), v(71, 57), 2f, Material.GOLD),
            Prop.Disc(v(66, 53), 1.6f, Material.GOLD),
        ),
        frontView = true, hold = true,
    )

    private fun base(name: String) = arts.getValue(name)

    private val seat = listOf(rect(32, 68, 24, 4, Material.LEATHER), line(44, 72, 44, 92, 3))

    /** Art for the library exercises: new poses plus equipment variants of the starter poses. */
    private val more: Map<String, ExerciseArt> = mapOf(
        // Chest
        "Dumbbell Bench Press" to base("Bench Press").dumbbells(),
        "Smith Machine Bench Press" to base("Bench Press").copy(
            start = base("Bench Press").start.copy(hand = v(39, 33)),
            finish = base("Bench Press").finish.copy(hand = v(40, 55)),
        ).smith(39.5f),
        "Incline Barbell Bench Press" to base("Incline Dumbbell Press").barbell(6.5f),
        "Smith Machine Incline Press" to base("Incline Dumbbell Press").copy(
            finish = base("Incline Dumbbell Press").finish.copy(hand = v(44, 19)),
        ).smith(44f),
        "Machine Chest Press" to ExerciseArt(
            start = Pose(hip = v(42, 66), torso = -4f, hand = v(54, 44), ankle = v(66, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(42, 66), torso = -4f, hand = v(68, 42), ankle = v(66, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(CHEST), secondary = setOf(TRICEPS, SHOULDERS),
            props = listOf(rect(30, 68, 24, 4, Material.LEATHER), line(42, 72, 42, 92, 3),
                Prop.Poly(listOf(v(29, 68), v(28, 34), v(33, 34), v(34, 68)), Material.LEATHER),
                Prop.Stack(8f, 30f, via = v(30, 22)), Prop.Lever(Anchor.HAND, pivot = v(36, 20))),
        ),
        "Dumbbell Fly" to ExerciseArt(
            // Arms open wide with a soft bend; from the side the hands travel from over the chest down to bench height.
            start = Pose(hip = v(60, 61), torso = -90f, hand = v(36, 33), ankle = v(77, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(60, 61), torso = -90f, hand = v(40, 64), ankle = v(77, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(CHEST), secondary = setOf(SHOULDERS),
            props = flatBench(16, 66) + Prop.Dumbbell(),
        ),
        "Cable Fly" to ExerciseArt(
            start = Pose(hip = v(46, 52), torso = 12f, hand = v(54, 36), ankle = v(56, 91), ankleFar = v(38, 91), elbowBend = 1),
            finish = Pose(hip = v(46, 52), torso = 12f, hand = v(76, 44), ankle = v(56, 91), ankleFar = v(38, 91), elbowBend = 1),
            primary = setOf(CHEST), secondary = setOf(SHOULDERS),
            props = listOf(line(22, -4, 22, 92, 3), Prop.Disc(v(24, 26), 2.4f, Material.STEEL), Prop.Cable(Anchor.HAND, v(24, 26))),
        ),
        "Pec Deck" to ExerciseArt(
            start = Pose(hip = v(42, 66), torso = -4f, hand = v(46, 40), ankle = v(66, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(42, 66), torso = -4f, hand = v(64, 42), ankle = v(66, 91), elbowBend = 1, kneeBend = -1),
            primary = setOf(CHEST), secondary = setOf(SHOULDERS),
            props = listOf(rect(30, 68, 24, 4, Material.LEATHER), line(42, 72, 42, 92, 3),
                Prop.Poly(listOf(v(29, 68), v(28, 34), v(33, 34), v(34, 68)), Material.LEATHER),
                Prop.Stack(8f, 30f, via = v(30, 20)), Prop.Lever(Anchor.HAND, pivot = v(38, 22))),
        ),
        "Dips" to ExerciseArt(
            start = Pose(hip = v(50, 56), torso = 10f, hand = v(56, 56), ankle = v(38, 90), kneeBend = -1, foot = 30f, elbowBend = -1),
            finish = Pose(hip = v(48, 72), torso = 22f, hand = v(56, 56), ankle = v(34, 104), kneeBend = -1, foot = 30f, elbowBend = -1),
            primary = setOf(CHEST, TRICEPS), secondary = setOf(SHOULDERS),
            props = listOf(line(44, 57.5, 76, 57.5, 2.2, Material.STEEL), line(72, 57.5, 72, 108, 3)),
            trail = Anchor.SHOULDER, floor = false,
        ),
        // Back
        "Dumbbell Row" to base("Barbell Row").dumbbells().plus(rect(70, 62, 26, 4, Material.LEATHER), line(92, 66, 92, 92, 2.5)),
        "Chest-Supported Dumbbell Row" to base("Barbell Row").dumbbells().plus(
            Prop.Poly(listOf(v(50, 62), v(70, 42), v(73, 45), v(53, 66)), Material.LEATHER), line(58, 62, 58, 92, 2.5),
        ),
        "Smith Machine Row" to base("Barbell Row").copy(
            finish = base("Barbell Row").finish.copy(hand = v(64, 56)),
        ).smith(65f),
        "Chin-up" to base("Pull-up"),
        "Dumbbell Shrug" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = -2f, hand = v(53, 53), ankle = v(52, 91)),
            finish = Pose(hip = v(50, 50), torso = -2f, hand = v(53, 50), ankle = v(52, 91), head = -4f),
            primary = setOf(BACK), secondary = setOf(FOREARMS),
            props = listOf(Prop.Dumbbell()),
        ),
        "Straight-Arm Pulldown" to ExerciseArt(
            start = Pose(hip = v(46, 52), torso = 25f, hand = v(80, 13), ankle = v(52, 91), elbowBend = 1),
            finish = Pose(hip = v(46, 52), torso = 25f, hand = v(63, 54), ankle = v(52, 91), elbowBend = 1),
            primary = setOf(BACK), secondary = setOf(TRICEPS, CORE),
            props = listOf(Prop.Stack(96f, 12f, via = v(90, -2)), line(92, -4, 92, 92, 3), Prop.Disc(v(90, -2), 2.5f, Material.STEEL), Prop.Cable(Anchor.HAND, v(90, -2))),
        ),
        "Barbell Shrug" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = -2f, hand = v(53, 53), ankle = v(52, 91)),
            finish = Pose(hip = v(50, 50), torso = -2f, hand = v(53, 50), ankle = v(52, 91), head = -4f),
            primary = setOf(BACK), secondary = setOf(FOREARMS),
            props = listOf(Prop.Barbell()),
        ),
        // Legs
        "Goblet Squat" to ExerciseArt(
            start = Pose(hip = v(48, 51), torso = 6f, hand = v(58, 34), ankle = v(52, 91), elbowBend = 1),
            finish = Pose(hip = v(34, 71), torso = 30f, hand = v(57, 58), ankle = v(52, 91), elbowBend = 1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(CORE),
            props = listOf(Prop.Dumbbell()),
            trail = Anchor.HIP,
        ),
        "Front Squat" to ExerciseArt(
            start = Pose(hip = v(48, 51), torso = 4f, hand = v(58, 26), ankle = v(52, 91), elbowBend = 1),
            finish = Pose(hip = v(34, 72), torso = 24f, hand = v(62, 50), ankle = v(52, 91), elbowBend = 1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(CORE, BACK),
            props = listOf(Prop.Barbell(Anchor.SHOULDER)),
            trail = Anchor.HIP,
        ),
        "Smith Machine Squat" to base("Squat").smith(46f),
        "Hack Squat" to base("Squat").copy(
            start = base("Squat").start.copy(hip = v(46, 52), torso = -12f),
            finish = base("Squat").finish.copy(hip = v(32, 72), torso = 22f),
        ).without { it is Prop.Barbell }.plus(
            line(18, 92, 56, 10, 3), Prop.Roller(Anchor.UPPER_BACK, r = 2.8f),
        ),
        "Walking Lunge" to ExerciseArt(
            start = Pose(hip = v(46, 52), torso = 4f, hand = v(48, 80), ankle = v(60, 91), ankleFar = v(34, 91), footFar = 70f, kneeBendFar = -1),
            finish = Pose(hip = v(44, 66), torso = 6f, hand = v(47, 92), ankle = v(60, 91), ankleFar = v(24, 90), footFar = 60f, kneeBendFar = -1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(HAMSTRINGS),
            props = listOf(Prop.Dumbbell()),
            trail = Anchor.HIP,
        ),
        "Smith Machine Split Squat" to ExerciseArt(
            start = Pose(hip = v(46, 52), torso = 2f, hand = v(46, 26), ankle = v(60, 91), ankleFar = v(30, 91), footFar = 70f, kneeBendFar = -1, elbowBend = 1),
            finish = Pose(hip = v(44, 67), torso = 4f, hand = v(44, 41), ankle = v(60, 91), ankleFar = v(22, 90), footFar = 60f, kneeBendFar = -1, elbowBend = 1),
            primary = setOf(QUADS, GLUTES), secondary = setOf(HAMSTRINGS),
            props = listOf(Prop.Barbell(Anchor.UPPER_BACK, 6.5f)),
            trail = Anchor.HIP,
        ).smith(44f),
        "Dumbbell Romanian Deadlift" to base("Romanian Deadlift").dumbbells(),
        "Hip Thrust" to ExerciseArt(
            // Upper back on a bench, bar over the hips: drive the hips up until the body is flat from shoulders to knees.
            start = Pose(hip = v(38, 82), torso = -48f, hand = v(38, 78), ankle = v(66, 91), kneeBend = -1, elbowBend = 1),
            finish = Pose(hip = v(45, 63), torso = -90f, hand = v(44, 60), ankle = v(66, 91), kneeBend = -1, elbowBend = 1),
            primary = setOf(GLUTES), secondary = setOf(HAMSTRINGS, QUADS),
            props = flatBench(-4, 22, 66) + Prop.Barbell(Anchor.HIP, 6.5f),
            trail = Anchor.HIP,
        ),
        "Glute Bridge" to ExerciseArt(
            start = Pose(hip = v(42, 85), torso = -88f, hand = v(32, 90), ankle = v(64, 91), kneeBend = -1, elbowBend = 1),
            finish = Pose(hip = v(40, 76), torso = -105f, hand = v(32, 90), ankle = v(64, 91), kneeBend = -1, elbowBend = 1),
            primary = setOf(GLUTES), secondary = setOf(HAMSTRINGS, CORE),
            trail = Anchor.HIP,
        ),
        "Seated Leg Curl" to ExerciseArt(
            start = Pose(hip = v(40, 64), torso = -6f, hand = v(46, 68), ankle = v(80, 62), foot = 170f, kneeBend = -1),
            finish = Pose(hip = v(40, 64), torso = -6f, hand = v(46, 68), ankle = v(52, 80), foot = 120f, kneeBend = -1),
            primary = setOf(HAMSTRINGS), secondary = setOf(CALVES),
            props = listOf(
                rect(30, 66, 32, 5, Material.LEATHER), line(46, 71, 46, 92, 3),
                Prop.Poly(listOf(v(29, 66), v(25, 36), v(30, 35), v(34, 66)), Material.LEATHER),
                rect(48, 56, 16, 4, Material.LEATHER),
                Prop.Stack(12f, 30f, toLever = true), Prop.Lever(Anchor.ANKLE, v(0f, -3.5f)),
                Prop.Roller(Anchor.ANKLE, v(0f, -3.5f), 3.4f),
            ),
            trail = Anchor.ANKLE,
        ),
        "Seated Calf Raise" to ExerciseArt(
            start = Pose(hip = v(38, 66), torso = -2f, hand = v(56, 60), ankle = v(60, 86), foot = 110f, kneeBend = -1),
            finish = Pose(hip = v(38, 64), torso = -2f, hand = v(56, 58), ankle = v(60.5, 83), foot = 60f, kneeBend = -1),
            primary = setOf(CALVES),
            props = listOf(rect(26, 68, 22, 4, Material.LEATHER), line(36, 72, 36, 92, 3), rect(56, 86, 16, 6, Material.WOOD), rect(50, 56, 16, 4, Material.LEATHER), line(66, 58, 74, 92, 2.5)),
            trail = Anchor.ANKLE,
        ),
        "Smith Machine Calf Raise" to base("Calf Raise").copy(
            start = base("Calf Raise").start.copy(hand = v(50, 24), elbowBend = 1),
            finish = base("Calf Raise").finish.copy(hand = v(53, 17), elbowBend = 1),
        ).without { it is Prop.Dumbbell }.plus(Prop.Barbell(Anchor.UPPER_BACK, 6.5f)).smith(48f),
        // Shoulders
        "Face Pull" to ExerciseArt(
            start = Pose(hip = v(46, 52), torso = -6f, hand = v(70, 30), ankle = v(52, 91), ankleFar = v(38, 91), elbowBend = 1),
            finish = Pose(hip = v(46, 52), torso = -6f, hand = v(52, 22), ankle = v(52, 91), ankleFar = v(38, 91), elbowBend = -1),
            primary = setOf(SHOULDERS), secondary = setOf(BACK),
            props = listOf(Prop.Stack(96f, 12f, via = v(88, 24)), line(90, -4, 90, 92, 3), Prop.Disc(v(88, 24), 2.4f, Material.STEEL), Prop.Cable(Anchor.HAND, v(88, 24))),
        ),
        "Arnold Press" to base("Dumbbell Shoulder Press"),
        "Smith Machine Shoulder Press" to base("Dumbbell Shoulder Press").copy(
            start = base("Dumbbell Shoulder Press").start.copy(hand = v(50, 36)),
            finish = base("Dumbbell Shoulder Press").finish.copy(hand = v(50, 10)),
        ).smith(50f),
        "Machine Shoulder Press" to base("Dumbbell Shoulder Press").without { it is Prop.Dumbbell }.plus(
            Prop.Stack(14f, 20f, via = v(32, 4)), Prop.Lever(Anchor.HAND, pivot = v(30, 8)),
        ),
        "Cable Lateral Raise" to base("Lateral Raise").without { it is Prop.Dumbbell }.plus(
            Prop.Cable(Anchor.HAND, v(40, 90)), Prop.Cable(Anchor.HAND_FAR, v(60, 90)),
        ),
        "Rear Delt Fly" to ExerciseArt(
            start = Pose(hip = v(44, 56), torso = 55f, hand = v(68, 68), ankle = v(51, 91), elbowBend = 1),
            finish = Pose(hip = v(44, 56), torso = 55f, hand = v(64, 47), ankle = v(51, 91), elbowBend = -1),
            primary = setOf(SHOULDERS), secondary = setOf(BACK),
            props = listOf(Prop.Dumbbell()),
        ),
        // Arms
        "Barbell Curl" to base("Bicep Curl").barbell(),
        "Cable Curl" to base("Bicep Curl").without { it is Prop.Dumbbell }.plus(
            Prop.Cable(Anchor.HAND, v(66, 88)), Prop.Disc(v(66, 88), 2.4f, Material.STEEL), line(70, 60, 70, 92, 3),
        ),
        "Incline Dumbbell Curl" to ExerciseArt(
            // Arms hang behind the body, so the biceps start long.
            start = Pose(hip = v(58, 64), torso = -55f, hand = v(34, 76), ankle = v(76, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(58, 64), torso = -55f, hand = v(42, 52), ankle = v(76, 91), elbowBend = -1, kneeBend = -1),
            primary = setOf(BICEPS), secondary = setOf(FOREARMS),
            props = listOf(
                line(60, 69, 60, 92, 2.5), line(40, 69, 32, 92, 2.5),
                rect(52, 67, 14, 4, Material.LEATHER),
                Prop.Poly(listOf(v(56, 68), v(30, 50), v(27, 54), v(53, 72)), Material.LEATHER),
                Prop.Dumbbell(),
            ),
        ),
        "Preacher Curl" to ExerciseArt(
            start = Pose(hip = v(44, 66), torso = 8f, hand = v(71, 58), ankle = v(64, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(44, 66), torso = 8f, hand = v(58, 33), ankle = v(64, 91), elbowBend = -1, kneeBend = -1),
            primary = setOf(BICEPS), secondary = setOf(FOREARMS),
            props = seat + listOf(Prop.Poly(listOf(v(52, 43), v(64, 56), v(67, 53), v(55, 40)), Material.LEATHER), line(60, 54, 60, 92, 2.5), Prop.Barbell(r = 6.5f)),
        ),
        "Skull Crusher" to ExerciseArt(
            start = Pose(hip = v(60, 61), torso = -90f, hand = v(34, 33), ankle = v(77, 91), elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(60, 61), torso = -90f, hand = v(22, 46), ankle = v(77, 91), elbowBend = -1, kneeBend = -1),
            primary = setOf(TRICEPS),
            props = flatBench(16, 66) + Prop.Barbell(r = 6.5f),
        ),
        "Close-Grip Bench Press" to base("Bench Press"),
        "Cable Overhead Tricep Extension" to base("Overhead Tricep Extension").without { it is Prop.Dumbbell }.plus(
            Prop.Cable(Anchor.HAND, v(20, 70)), Prop.Disc(v(20, 70), 2.4f, Material.STEEL), line(16, 40, 16, 92, 3),
        ),
        // Core
        "Cable Crunch" to ExerciseArt(
            start = Pose(hip = v(52, 66), torso = 4f, hand = v(58, 32), ankle = v(32, 88), foot = 20f, elbowBend = -1),
            finish = Pose(hip = v(52, 66), torso = 62f, hand = v(80, 46), ankle = v(32, 88), foot = 20f, elbowBend = -1),
            primary = setOf(CORE),
            props = listOf(line(40, -4, 40, 92, 3), Prop.Disc(v(42, 2), 2.4f, Material.STEEL), Prop.Cable(Anchor.HAND, v(42, 2))),
        ),
        "Ab Wheel Rollout" to ExerciseArt(
            start = Pose(hip = v(46, 67.4), torso = 58f, hand = v(70, 84), ankle = v(21, 89), foot = 20f, elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(58, 75.5), torso = 84f, hand = v(100, 83), ankle = v(21, 89), foot = 20f, elbowBend = 1, kneeBend = -1),
            primary = setOf(CORE), secondary = setOf(SHOULDERS, BACK),
            props = listOf(Prop.Roller(Anchor.HAND, v(0f, 0f), 4f)),
            trail = Anchor.HAND,
        ),
        // Cardio
        "Stair Climber" to base("Incline Walk"),
        "Walking" to base("Incline Walk").copy(props = emptyList(), floor = true,
            start = base("Incline Walk").start.copy(ankleFar = v(38, 91)),
            finish = base("Incline Walk").finish.copy(ankle = v(38, 91))),
        "Jump Rope" to base("Calf Raise").without { it is Prop.Dumbbell || it is Prop.Poly }.copy(
            start = base("Calf Raise").start.copy(hand = v(60, 60), elbowBend = 1),
            finish = base("Calf Raise").finish.copy(hand = v(63, 53), elbowBend = 1),
            primary = setOf(CALVES, QUADS), secondary = setOf(SHOULDERS),
        ).plus(Prop.Cable(Anchor.HAND, v(44, 93))),
        "Swimming" to ExerciseArt(
            start = Pose(hip = v(40, 60), torso = 92f, hand = v(90, 58), handFar = v(42, 66), ankle = v(2, 58), ankleFar = v(4, 64), foot = 0f, elbowBend = 1),
            finish = Pose(hip = v(40, 60), torso = 92f, hand = v(42, 66), handFar = v(90, 58), ankle = v(4, 64), ankleFar = v(2, 58), foot = 0f, elbowBend = 1),
            primary = setOf(BACK, SHOULDERS), secondary = setOf(CORE, QUADS),
            props = listOf(line(-6, 52, 106, 52, 1, Material.STEEL)),
            floor = false,
        ),
    )

    fun forName(name: String): ExerciseArt? = arts[name] ?: more[name]
    val names: Set<String> get() = arts.keys + more.keys
}
