package com.levelup.gym.game

import kotlin.math.roundToInt

/**
 * All XP and level rules live here so they're easy to tune.
 *
 * Points reward effort, not just tonnage, so a light lifter and a strong one earn the same for a hard set:
 *
 * Strength set: 5 + 15 x effort, where effort = this set's estimated 1-rep max (Epley) / your best on that
 *               exercise, capped at 1. Bodyweight moves compare reps with your best reps instead.
 *               Warm-ups (under 60% of your best) earn 2 XP. Your first set on an exercise counts as full effort.
 * New best:     +20 when the set beats your best estimated 1-rep max (or best reps) on that exercise.
 * Junk volume:  after 24 sets in one workout each further set earns at most 3 XP; more isn't better.
 * Cardio:       5 + 0.6 per minute (max 60 min) + 1 per km (max 15).
 * Finish:       +30 for a finished workout, +150 more for the 4th workout of the week (Mon-Sun).
 * Hero level:   reaching level L takes 80 x (L-1)^2 XP in total, so each level costs more than the last.
 * Skill level:  15 x (L-1)^2 per skill.
 * Gear:         equipped gear adds a few % XP to the skill it belongs to (see Loot.kt), at most +25%.
 */
object Leveling {
    const val BASE_SET_XP = 5
    const val EFFORT_XP = 15
    const val WARMUP_XP = 2
    const val WARMUP_BELOW = 0.6
    const val PERSONAL_RECORD_XP = 20
    const val JUNK_AFTER_SETS = 24
    const val JUNK_XP = 3
    const val FINISH_WORKOUT_XP = 30
    const val WEEKLY_QUEST_XP = 150
    const val WEEKLY_QUEST_TARGET = 4
    const val MAX_CARDIO_MINUTES = 60
    const val MAX_CARDIO_KM = 15

    const val HERO_STEP = 80
    const val SKILL_STEP = 15

    /** Estimated 1-rep max (Epley). Reps above 12 are counted as 12, where the estimate stops being useful. */
    fun e1rm(weightKg: Double, reps: Int): Double =
        if (weightKg <= 0 || reps <= 0) 0.0 else weightKg * (1 + reps.coerceAtMost(12) / 30.0)

    data class SetScore(val xp: Int, val personalRecord: Boolean, val warmup: Boolean, val effort: Double)

    /**
     * Scores a strength set. [bestE1rm] is the best estimated 1RM before this set (null if none),
     * [bestReps] the best bodyweight reps before it (for sets without added weight).
     */
    fun scoreSet(reps: Int, weightKg: Double, bestE1rm: Double?, bestReps: Int?, setsBeforeInWorkout: Int = 0, gearBonus: Double = 0.0): SetScore {
        if (reps <= 0) return SetScore(0, false, false, 0.0)
        val weighted = weightKg > 0
        val now = if (weighted) e1rm(weightKg, reps) else reps.toDouble()
        val best = (if (weighted) bestE1rm else bestReps?.toDouble())?.takeIf { it > 0 }
        val effort = if (best == null) 1.0 else now / best
        val pr = best != null && now > best * 1.001
        val warmup = effort < WARMUP_BELOW
        var xp = if (warmup) WARMUP_XP.toDouble() else BASE_SET_XP + EFFORT_XP * effort.coerceAtMost(1.0)
        xp *= 1 + gearBonus.coerceIn(0.0, Loot.MAX_GEAR_BONUS)
        var total = xp.roundToInt() + if (pr) PERSONAL_RECORD_XP else 0
        if (setsBeforeInWorkout >= JUNK_AFTER_SETS) total = total.coerceAtMost(JUNK_XP)
        return SetScore(total, pr, warmup, effort)
    }

    fun xpForCardio(durationSec: Int, distanceM: Double, gearBonus: Double = 0.0): Int {
        val minutes = (durationSec / 60.0).coerceIn(0.0, MAX_CARDIO_MINUTES.toDouble())
        val km = (distanceM / 1000.0).coerceIn(0.0, MAX_CARDIO_KM.toDouble())
        return ((BASE_SET_XP + minutes * 0.6 + km) * (1 + gearBonus.coerceIn(0.0, Loot.MAX_GEAR_BONUS))).roundToInt()
    }

    /** Bonus for finishing a workout, given how many finished workouts this week includes it. */
    fun finishBonus(finishedThisWeekIncludingThis: Int): Int =
        FINISH_WORKOUT_XP + if (finishedThisWeekIncludingThis == WEEKLY_QUEST_TARGET) WEEKLY_QUEST_XP else 0

    /** Total XP needed to reach [level]. Level 1 starts at 0. */
    fun totalXpForLevel(level: Int, step: Int = HERO_STEP): Int = step * (level - 1) * (level - 1)

    fun levelFor(totalXp: Int, step: Int = HERO_STEP): Int {
        var level = 1
        while (totalXp >= totalXpForLevel(level + 1, step)) level++
        return level
    }

    data class Progress(
        val level: Int,
        val totalXp: Int,
        val xpIntoLevel: Int,
        val xpNeededForNext: Int,
    ) {
        val fraction: Float get() = if (xpNeededForNext == 0) 0f else xpIntoLevel.toFloat() / xpNeededForNext
        val percent: Int get() = (fraction * 100).roundToInt()
    }

    fun progress(totalXp: Int, step: Int = HERO_STEP): Progress {
        val level = levelFor(totalXp, step)
        val start = totalXpForLevel(level, step)
        val next = totalXpForLevel(level + 1, step)
        return Progress(level, totalXp, totalXp - start, next - start)
    }

    fun title(level: Int): String = when {
        level < 3 -> "Village Recruit"
        level < 5 -> "Squire of Iron"
        level < 8 -> "Barbell Knight"
        level < 12 -> "Plate Warden"
        level < 17 -> "Steel Champion"
        level < 25 -> "Titan of the Forge"
        level < 35 -> "Warlord of the Rack"
        else -> "Legend of Ashvale"
    }
}
