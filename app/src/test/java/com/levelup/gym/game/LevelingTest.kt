package com.levelup.gym.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingTest {
    @Test fun setXp_basicSet() {
        // 10 base + 8 reps + (8*60/50 = 9) volume
        assertEquals(27, Leveling.xpForSet(reps = 8, weightKg = 60.0, isPersonalRecord = false))
    }

    @Test fun setXp_bodyweightSet() {
        assertEquals(22, Leveling.xpForSet(reps = 12, weightKg = 0.0, isPersonalRecord = false))
    }

    @Test fun setXp_isCapped() {
        assertEquals(10 + 20 + 40, Leveling.xpForSet(reps = 100, weightKg = 500.0, isPersonalRecord = false))
    }

    @Test fun setXp_personalRecordBonus() {
        assertEquals(27 + 25, Leveling.xpForSet(reps = 8, weightKg = 60.0, isPersonalRecord = true))
    }

    @Test fun personalRecord_rules() {
        assertFalse(Leveling.isPersonalRecord(50.0, null))
        assertFalse(Leveling.isPersonalRecord(50.0, 50.0))
        assertTrue(Leveling.isPersonalRecord(52.5, 50.0))
    }

    @Test fun levels_thresholds() {
        assertEquals(1, Leveling.levelFor(0))
        assertEquals(1, Leveling.levelFor(99))
        assertEquals(2, Leveling.levelFor(100))
        assertEquals(2, Leveling.levelFor(299))
        assertEquals(3, Leveling.levelFor(300))
        assertEquals(4, Leveling.levelFor(600))
    }

    @Test fun progress_withinLevel() {
        val p = Leveling.progress(150)
        assertEquals(2, p.level)
        assertEquals(50, p.xpIntoLevel)
        assertEquals(200, p.xpNeededForNext)
        assertEquals(25, p.percent)
    }
}

class CardioAndQuestTest {
    @Test fun cardioXp() {
        // 10 base + 30 min + 2 per km * 5 km
        assertEquals(50, Leveling.xpForCardio(durationSec = 30 * 60, distanceM = 5000.0))
        assertEquals(10 + 60 + 30, Leveling.xpForCardio(durationSec = 300 * 60, distanceM = 100_000.0))
    }

    @Test fun weeklyQuestBonusOnlyOnFourth() {
        assertEquals(50, Leveling.finishBonus(3))
        assertEquals(150, Leveling.finishBonus(4))
        assertEquals(50, Leveling.finishBonus(5))
    }

    @Test fun skillCurveIsCheaper() {
        assertEquals(2, Leveling.levelFor(50, Leveling.SKILL_STEP))
        assertEquals(3, Leveling.levelFor(150, Leveling.SKILL_STEP))
    }

    @Test fun programRotation() {
        assertEquals("upper_a", StarterProgram.nextDay(null).id)
        assertEquals("lower_a", StarterProgram.nextDay("upper_a").id)
        assertEquals("upper_a", StarterProgram.nextDay("lower_b").id)
    }

    @Test fun programUsesOnlySeededExercises() {
        val seeded = com.levelup.gym.data.DEFAULT_EXERCISES.map { it.name }.toSet()
        StarterProgram.days.flatMap { it.exercises }.forEach {
            assertTrue("${it.exerciseName} missing from defaults", it.exerciseName in seeded)
        }
    }

    @Test fun unitsRoundTrip() {
        val kg = UnitsFormat.displayToKg(135.0, com.levelup.gym.data.Units.IMPERIAL)
        assertEquals("135 lb", UnitsFormat.weight(kg, com.levelup.gym.data.Units.IMPERIAL))
        assertEquals("62.5 kg", UnitsFormat.weight(62.5, com.levelup.gym.data.Units.METRIC))
    }
}
