package com.levelup.gym.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingTest {
    @Test fun firstSetCountsAsFullEffort() {
        val s = Leveling.scoreSet(reps = 8, weightKg = 60.0, bestE1rm = null, bestReps = null)
        assertEquals(20, s.xp)
        assertFalse(s.personalRecord)
    }

    @Test fun effortScalesWithYourBest() {
        val best = Leveling.e1rm(100.0, 5)
        // same strength as your best: full points
        assertEquals(20, Leveling.scoreSet(5, 100.0, best, null).xp)
        // 80% of your best: 5 + 15*0.8 = 17
        assertEquals(17, Leveling.scoreSet(5, 80.0, best, null).xp)
        // warm-up under 60%
        val warm = Leveling.scoreSet(5, 50.0, best, null)
        assertTrue(warm.warmup); assertEquals(2, warm.xp)
    }

    @Test fun newBestEarnsBonus() {
        val s = Leveling.scoreSet(8, 62.5, Leveling.e1rm(60.0, 8), null)
        assertTrue(s.personalRecord)
        assertEquals(20 + 20, s.xp)
    }

    @Test fun bodyweightComparesReps() {
        val s = Leveling.scoreSet(12, 0.0, null, 10)
        assertTrue(s.personalRecord)
        assertEquals(40, s.xp)
        assertEquals(17, Leveling.scoreSet(8, 0.0, null, 10).xp)
    }

    @Test fun junkVolumeIsCapped() {
        assertEquals(3, Leveling.scoreSet(8, 60.0, null, null, setsBeforeInWorkout = 24).xp)
    }

    @Test fun gearAddsAFewPercent() {
        assertEquals(22, Leveling.scoreSet(8, 60.0, null, null, gearBonus = 0.10).xp)
        assertEquals(25, Leveling.scoreSet(8, 60.0, null, null, gearBonus = 5.0).xp)
    }

    @Test fun levels_thresholds() {
        assertEquals(1, Leveling.levelFor(0))
        assertEquals(1, Leveling.levelFor(79))
        assertEquals(2, Leveling.levelFor(80))
        assertEquals(3, Leveling.levelFor(320))
        assertEquals(10, Leveling.levelFor(6480))
    }

    @Test fun progress_withinLevel() {
        val p = Leveling.progress(120)
        assertEquals(2, p.level)
        assertEquals(40, p.xpIntoLevel)
        assertEquals(240, p.xpNeededForNext)
    }

    @Test fun skillCurveIsCheaper() {
        assertEquals(2, Leveling.levelFor(15, Leveling.SKILL_STEP))
        assertEquals(3, Leveling.levelFor(60, Leveling.SKILL_STEP))
    }

    @Test fun programRotation() {
        assertEquals("upper_a", Programs.default.nextDay(null).id)
        assertEquals("lower_a", Programs.default.nextDay("upper_a").id)
        assertEquals("upper_a", Programs.default.nextDay("lower_b").id)
    }

    @Test fun unitsRoundTrip() {
        val kg = UnitsFormat.displayToKg(135.0, com.levelup.gym.data.Units.IMPERIAL)
        assertEquals("135 lb", UnitsFormat.weight(kg, com.levelup.gym.data.Units.IMPERIAL))
        assertEquals("62.5 kg", UnitsFormat.weight(62.5, com.levelup.gym.data.Units.METRIC))
    }
}

class ExerciseGuideTest {
    @Test fun everyStarterExerciseHasPhotosAndNotes() {
        com.levelup.gym.data.DEFAULT_EXERCISES.forEach { seed ->
            val guide = ExerciseGuides.forName(seed.name)
            assertTrue("${seed.name} has no guide", guide != null)
            assertTrue("${seed.name} has no cues", guide!!.cues.isNotEmpty() && guide.avoid.isNotBlank())
            assertTrue("${seed.name} has no drawing", com.levelup.gym.ui.figure.ExerciseArts.forName(seed.name) != null)
        }
    }
}
