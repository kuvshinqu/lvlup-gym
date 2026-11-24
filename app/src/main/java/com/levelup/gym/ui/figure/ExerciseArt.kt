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
            start = Pose(hip = v(40.7, 73.9), torso = 66.9f, hand = v(68, 91), ankle = v(3, 90), foot = 150f, elbowBend = 1, kneeBend = -1),
            finish = Pose(hip = v(42.5, 81), torso = 79f, hand = v(68, 91), ankle = v(3, 90), foot = 150f, elbowBend = 1, kneeBend = -1),
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
            props = flatBench(30, 56, 70) + listOf(Prop.Roller(Anchor.ANKLE, v(-3f, -22f), 3.4f), Prop.Cable(Anchor.HAND, v(42, -6))),
        ),
        "Seated Cable Row" to ExerciseArt(
            start = Pose(hip = v(35, 80), torso = 18f, hand = v(62, 62), ankle = v(70, 84), foot = 160f, elbowBend = 1, kneeBend = 1),
            finish = Pose(hip = v(35, 80), torso = -6f, hand = v(46, 67), ankle = v(70, 84), foot = 160f, elbowBend = 1, kneeBend = 1),
            primary = setOf(BACK), secondary = setOf(BICEPS),
            props = listOf(rect(22, 84, 26, 4, Material.LEATHER), line(76, 72, 76, 92, 3), Prop.Cable(Anchor.HAND, v(78, 66)), Prop.Disc(v(78, 66), 2.5f, Material.STEEL)),
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
            start = Pose(hip = v(30, 70), torso = -62f, hand = v(36, 74), ankle = v(58, 56), foot = 150f, kneeBend = 1),
            finish = Pose(hip = v(30, 70), torso = -62f, hand = v(36, 74), ankle = v(67, 49), foot = 150f, kneeBend = 1),
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
            props = flatBench(18, 72, 64) + Prop.Roller(Anchor.ANKLE, v(0f, -3.5f), 3.4f),
            trail = Anchor.ANKLE,
        ),
        "Leg Extension" to ExerciseArt(
            start = Pose(hip = v(40, 64), torso = -6f, hand = v(46, 68), ankle = v(62, 85), foot = 100f, kneeBend = -1),
            finish = Pose(hip = v(40, 64), torso = -6f, hand = v(46, 68), ankle = v(80, 62), foot = 170f, kneeBend = -1),
            primary = setOf(QUADS),
            props = listOf(
                rect(30, 66, 32, 5, Material.LEATHER), line(46, 71, 46, 92, 3),
                Prop.Poly(listOf(v(29, 66), v(25, 36), v(30, 35), v(34, 66)), Material.LEATHER),
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
            start = Pose(hip = v(45, 66), torso = -3f, hand = v(47, 27), ankle = v(68, 91), elbowBend = -1, kneeBend = -1),
            finish = Pose(hip = v(45, 66), torso = -3f, hand = v(45, 10), ankle = v(68, 91), elbowBend = -1, kneeBend = -1),
            primary = setOf(SHOULDERS), secondary = setOf(TRICEPS),
            props = listOf(
                rect(34, 68, 22, 4, Material.LEATHER), line(45, 72, 45, 92, 3),
                Prop.Poly(listOf(v(33, 68), v(32, 36), v(37, 36), v(38, 68)), Material.LEATHER),
                Prop.Dumbbell(),
            ),
        ),
        "Lateral Raise" to ExerciseArt(
            start = Pose(hip = v(50, 51), torso = 0f, hand = v(62, 51), handFar = v(38, 51), ankle = v(56, 91), ankleFar = v(44, 91), elbowBend = 1, elbowBendFar = -1, foot = 90f, footFar = 270f),
            finish = Pose(hip = v(50, 51), torso = 0f, hand = v(87, 26), handFar = v(13, 26), ankle = v(56, 91), ankleFar = v(44, 91), elbowBend = 1, elbowBendFar = -1, foot = 90f, footFar = 270f),
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
            props = listOf(line(78, -4, 78, 92, 3), Prop.Disc(v(76, -2), 2.5f, Material.STEEL), Prop.Cable(Anchor.HAND, v(76, -2))),
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
            start = Pose(hip = v(40, 80), torso = 32f, hand = v(69, 65), ankle = v(66, 84), foot = 150f, kneeBend = 1),
            finish = Pose(hip = v(22, 80), torso = -22f, hand = v(32, 66), ankle = v(61, 84), foot = 150f, kneeBend = 1),
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

    fun forName(name: String): ExerciseArt? = arts[name]
    val names: Set<String> get() = arts.keys
}
