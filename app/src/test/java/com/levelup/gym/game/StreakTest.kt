package com.levelup.gym.game

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakTest {
    // Mon 2 Nov 2026
    private val mon = LocalDate.of(2026, 11, 2)
    private fun d(offset: Int) = mon.plusDays(offset.toLong())
    private val mwf = setOf(d(0), d(2), d(4))

    @Test fun noWorkoutsNoStreak() {
        val s = Streak.compute(emptySet(), emptyList(), 2, mon)
        assertEquals(0, s.days)
        assertNull(s.nextMilestone?.takeIf { it != 3 })
    }

    @Test fun restDaysCountTowardTheStreak() {
        val s = Streak.compute(mwf, emptyList(), 2, d(6)) // Sunday
        assertEquals(7, s.days)
        assertEquals(2, s.restRun)
        assertFalse(s.atRisk)
        assertEquals(Streak.Mark.REST, s.marks[d(5)])
        assertEquals(Streak.Mark.TRAINED, s.marks[d(4)])
    }

    @Test fun todayIsAtRiskOnceRestDaysAreUsedUp() {
        // The 7-day milestone was reached on Sunday, which also gave a shield.
        val s = Streak.compute(mwf, emptyList(), 2, d(7))
        assertEquals(7, s.days)
        assertTrue(s.atRisk)
        assertEquals(1, s.shields)
        assertFalse(s.trainedToday)
    }

    @Test fun aShieldGuardsAMissedDayThenTheStreakEnds() {
        val guarded = Streak.compute(mwf, emptyList(), 2, d(8))
        assertEquals(Streak.Mark.SHIELDED, guarded.marks[d(7)])
        assertEquals(8, guarded.days)
        assertEquals(0, guarded.shields)
        assertTrue(guarded.atRisk)

        val broken = Streak.compute(mwf, emptyList(), 2, d(9))
        assertEquals(0, broken.days)
        assertEquals(d(8), broken.brokenOn)
        assertEquals(8, broken.brokenLength)
        assertEquals(8, broken.best)
        assertEquals(Streak.Mark.MISSED, broken.marks[d(8)])
    }

    @Test fun trainingAgainStartsANewStreakAndKeepsMilestones() {
        val s = Streak.compute(mwf + d(10), emptyList(), 2, d(10))
        assertEquals(1, s.days)
        assertTrue(s.trainedToday)
        assertEquals(listOf(3, 7), s.milestonesReached)
        assertEquals(8, s.longestEver)
    }

    @Test fun boughtShieldsAreCapped() {
        val s = Streak.compute(setOf(mon), listOf(mon, mon, mon), 2, mon)
        assertEquals(Streak.MAX_SHIELDS, s.shields)
    }

    @Test fun sixDayProgramsGetOneRestDay() {
        assertEquals(1, Streak.restDaysAllowed(6))
        assertEquals(2, Streak.restDaysAllowed(4))
        val s = Streak.compute(setOf(mon), emptyList(), 1, d(2))
        assertEquals(Streak.Mark.REST, s.marks[d(1)])
        assertTrue(s.atRisk)
    }

    @Test fun badgesFollowDeeds() {
        val earned = Badges.earned(Deeds(longestStreak = 7, workouts = 1, records = 0, level = 5)).map { it.key }
        assertEquals(listOf("streak_3", "streak_7", "workouts_1", "level_5"), earned)
        assertEquals(ChestTier.IRON, Badges.byKey("streak_7")!!.tier.chest)
    }
}
