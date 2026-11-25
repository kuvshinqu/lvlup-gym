package com.levelup.gym.game

import java.time.LocalDate

/**
 * The daily streak, like a language app's, but made for lifting: rest days count, as long as you
 * don't rest more days in a row than your program allows (2, or 1 on a 5-6 day program).
 * Rest one day too many and the streak ends. A shield, if you hold one, guards that day instead.
 * Shields come from the 7, 30 and 100 day milestones, or the forge. Missing a day costs nothing else.
 */
object Streak {
    val MILESTONES = listOf(3, 7, 14, 30, 60, 100, 180, 365)
    /** Milestones that also hand out a shield. */
    val SHIELD_MILESTONES = setOf(7, 30, 100)
    const val MAX_SHIELDS = 2
    const val SHIELD_PRICE = 150

    fun restDaysAllowed(daysPerWeek: Int): Int = if (daysPerWeek >= 5) 1 else 2

    enum class Mark { TRAINED, REST, SHIELDED, MISSED, OPEN }

    data class State(
        /** Days in the current streak, rest days included. 0 when there is none. */
        val days: Int = 0,
        val best: Int = 0,
        val trainedToday: Boolean = false,
        /** Rest days in a row up to today. */
        val restRun: Int = 0,
        val restAllowed: Int = 2,
        val shields: Int = 0,
        /** Today hasn't been trained and the rest days are used up: train or the streak ends tonight. */
        val atRisk: Boolean = false,
        /** The day the last streak ended, and how long it was. */
        val brokenOn: LocalDate? = null,
        val brokenLength: Int = 0,
        val marks: Map<LocalDate, Mark> = emptyMap(),
        /** The longest streak ever, which decides the milestones reached. */
        val longestEver: Int = 0,
    ) {
        val nextMilestone: Int? get() = MILESTONES.firstOrNull { it > days }
        val milestonesReached: List<Int> get() = MILESTONES.filter { it <= longestEver }
        val restDaysLeft: Int get() = if (trainedToday) restAllowed else (restAllowed - restRun).coerceAtLeast(0)
    }

    fun compute(
        workoutDays: Set<LocalDate>,
        shieldsBought: List<LocalDate>,
        restAllowed: Int,
        today: LocalDate = LocalDate.now(),
    ): State {
        val bought = shieldsBought.sorted()
        if (workoutDays.isEmpty()) return State(restAllowed = restAllowed, shields = bought.size.coerceAtMost(MAX_SHIELDS))
        val marks = LinkedHashMap<LocalDate, Mark>()
        var shields = 0
        var bi = 0
        var streak = 0
        var best = 0
        var longest = 0
        var restRun = 0
        var brokenOn: LocalDate? = null
        var brokenLength = 0
        var day = minOf(workoutDays.min(), today)
        while (!day.isAfter(today)) {
            while (bi < bought.size && !bought[bi].isAfter(day)) { shields = (shields + 1).coerceAtMost(MAX_SHIELDS); bi++ }
            if (day in workoutDays) {
                streak++; restRun = 0; marks[day] = Mark.TRAINED
            } else if (streak > 0) {
                when {
                    restRun < restAllowed -> { restRun++; streak++; marks[day] = Mark.REST }
                    // Today isn't over: there's still time to train.
                    day == today -> marks[day] = Mark.OPEN
                    shields > 0 -> { shields--; streak++; marks[day] = Mark.SHIELDED }
                    else -> {
                        brokenOn = day; brokenLength = streak
                        streak = 0; restRun = 0; marks[day] = Mark.MISSED
                    }
                }
            } else {
                marks[day] = Mark.OPEN
            }
            if (streak > longest) {
                MILESTONES.filter { it in (longest + 1)..streak && it in SHIELD_MILESTONES }
                    .forEach { _ -> shields = (shields + 1).coerceAtMost(MAX_SHIELDS) }
                longest = streak
            }
            best = maxOf(best, streak)
            day = day.plusDays(1)
        }
        // Shields bought after today (clock changes) still count.
        while (bi < bought.size) { shields = (shields + 1).coerceAtMost(MAX_SHIELDS); bi++ }
        val trainedToday = today in workoutDays
        return State(
            days = streak, best = best, trainedToday = trainedToday, restRun = restRun, restAllowed = restAllowed,
            shields = shields, atRisk = streak > 0 && !trainedToday && marks[today] == Mark.OPEN,
            brokenOn = brokenOn, brokenLength = brokenLength, marks = marks, longestEver = longest,
        )
    }
}
