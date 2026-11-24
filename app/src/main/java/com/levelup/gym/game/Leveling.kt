package com.levelup.gym.game

import kotlin.math.roundToInt

/**
 * All XP and level rules live here so they're easy to tune.
 *
 * Strength set: 10 base + 1 per rep (max 20) + 1 per 50 kg of volume (max 40)
 * Cardio:       10 base + 1 per minute (max 60) + 2 per km (max 30)
 * New best:     +25 when the weight beats your previous best on that exercise
 * Finish:       +50 for finishing a workout with at least one set
 * Weekly quest: +100 for the 4th finished workout of the week (Mon-Sun)
 * Hero level:   going from level L to L+1 costs 100 * L XP (100, 200, 300, ...)
 * Skill level:  each skill levels on its own, cheaper curve (50 * L per level)
 */
object Leveling {
    const val BASE_SET_XP = 10
    const val MAX_REP_XP = 20
    const val MAX_VOLUME_XP = 40
    const val PERSONAL_RECORD_XP = 25
    const val FINISH_WORKOUT_XP = 50
    const val WEEKLY_QUEST_XP = 100
    const val WEEKLY_QUEST_TARGET = 4
    const val MAX_CARDIO_MINUTE_XP = 60
    const val MAX_CARDIO_DISTANCE_XP = 30

    fun xpForSet(reps: Int, weightKg: Double, isPersonalRecord: Boolean): Int {
        val repXp = reps.coerceIn(0, MAX_REP_XP)
        val volumeXp = (reps * weightKg / 50.0).toInt().coerceIn(0, MAX_VOLUME_XP)
        val prXp = if (isPersonalRecord) PERSONAL_RECORD_XP else 0
        return BASE_SET_XP + repXp + volumeXp + prXp
    }

    fun xpForCardio(durationSec: Int, distanceM: Double): Int {
        val minuteXp = (durationSec / 60).coerceIn(0, MAX_CARDIO_MINUTE_XP)
        val distanceXp = (distanceM / 1000.0 * 2).toInt().coerceIn(0, MAX_CARDIO_DISTANCE_XP)
        return BASE_SET_XP + minuteXp + distanceXp
    }

    /** Bonus for finishing a workout, given how many finished workouts this week includes it. */
    fun finishBonus(finishedThisWeekIncludingThis: Int): Int =
        FINISH_WORKOUT_XP + if (finishedThisWeekIncludingThis == WEEKLY_QUEST_TARGET) WEEKLY_QUEST_XP else 0

    /** A set is a new best when it beats an earlier best weight (the very first set isn't). */
    fun isPersonalRecord(weightKg: Double, previousBestKg: Double?): Boolean =
        previousBestKg != null && previousBestKg > 0 && weightKg > previousBestKg

    /** Total XP needed to reach [level]. Level 1 starts at 0. */
    fun totalXpForLevel(level: Int, step: Int = HERO_STEP): Int = step / 2 * level * (level - 1)

    fun levelFor(totalXp: Int, step: Int = HERO_STEP): Int {
        var level = 1
        while (totalXp >= totalXpForLevel(level + 1, step)) level++
        return level
    }

    const val HERO_STEP = 100
    const val SKILL_STEP = 50

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
        else -> "Legend of the Rack"
    }
}
