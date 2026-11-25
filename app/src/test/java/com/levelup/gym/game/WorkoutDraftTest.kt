package com.levelup.gym.game

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Runs under Robolectric for org.json. */
@RunWith(AndroidJUnit4::class)
class WorkoutDraftTest {
    private val day = Programs.upperLower4.days.first()

    @Test fun startsFromThePlan() {
        val d = WorkoutDraft.fromPlan(7, day)
        assertEquals(day.exercises.size, d.items.size)
        assertEquals(4, d.item("Bench Press")!!.targetSets)
        assertEquals(6, WorkoutDraft.lowerReps(d.item("Bench Press")!!.reps))
    }

    @Test fun addAndRemoveSetsNeverDropBelowWhatIsLogged() {
        val d = WorkoutDraft.fromPlan(7, day)
        assertEquals(5, d.addSet("Bench Press", logged = 2).item("Bench Press")!!.targetSets)
        // Logged more than planned: the new set goes after them.
        assertEquals(7, d.addSet("Bench Press", logged = 6).item("Bench Press")!!.targetSets)
        assertEquals(3, d.removeSet("Bench Press", logged = 0).item("Bench Press")!!.targetSets)
        assertEquals(4, d.removeSet("Bench Press", logged = 4).item("Bench Press")!!.targetSets)
        assertEquals(1, d.addSet("Pull-up", logged = 0).item("Pull-up")!!.targetSets)
    }

    @Test fun survivesBeingSaved() {
        val d = WorkoutDraft.fromPlan(7, day).addExercise("Pull-up").addSet("Bench Press", 0)
        assertEquals(d, WorkoutDraft.decode(d.encode()))
        assertEquals(null, WorkoutDraft.decode("not json"))
    }
}
