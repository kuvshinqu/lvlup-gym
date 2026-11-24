package com.levelup.gym.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgramsTest {
    @Test fun everyProgramExerciseIsInTheLibrary() {
        Programs.builtIn.flatMap { it.days }.flatMap { it.exercises }.forEach {
            assertNotNull("Missing from library: ${it.exerciseName}", ExerciseLibrary.find(it.exerciseName))
        }
    }

    @Test fun libraryNamesAreUniqueAndHaveGuides() {
        val names = ExerciseLibrary.all.map { it.name.lowercase() }
        assertEquals(names.size, names.toSet().size)
        ExerciseLibrary.all.forEach { assertNotNull("No guide: ${it.name}", ExerciseGuides.forName(it.name)) }
    }

    @Test fun dayIdsAreUniqueAcrossPrograms() {
        val ids = Programs.builtIn.flatMap { p -> p.days.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun weeklySetsPerMuscleStayInResearchRange() {
        // Pelland 2024: about 10-20 hard sets per muscle per week. Allow a little slack either side.
        Programs.builtIn.forEach { p ->
            val perGroup = p.days.flatMap { it.exercises }
                .groupBy { ExerciseLibrary.find(it.exerciseName)!!.muscleGroup }
                .mapValues { (_, l) -> l.sumOf { it.sets } }
            listOf("Chest", "Back").forEach { g ->
                val sets = perGroup[g] ?: 0
                assertTrue("${p.name} $g has $sets sets", sets in 8..24)
            }
            // Legs covers quads, hamstrings, glutes and calves together.
            val legs = perGroup["Legs"] ?: 0
            assertTrue("${p.name} legs has $legs sets", legs in 14..40)
        }
    }

    @Test fun quizPicksSensiblePrograms() {
        assertEquals(Programs.dumbbell3, Quiz.recommend(mapOf("place" to 1, "days" to 2)).program)
        assertEquals(Programs.fullBody3, Quiz.recommend(mapOf("days" to 0)).program)
        assertEquals(Programs.upperLower4, Quiz.recommend(mapOf("days" to 1)).program)
        assertEquals(Programs.upperLower4, Quiz.recommend(mapOf("days" to 2, "experience" to 0)).program)
        assertEquals(Programs.pushPullLegs6, Quiz.recommend(mapOf("days" to 2, "experience" to 2)).program)
    }

    @Test fun nextDayWraps() {
        val p = Programs.fullBody3
        assertEquals("fb_a", p.nextDay(null).id)
        assertEquals("fb_b", p.nextDay("fb_a").id)
        assertEquals("fb_a", p.nextDay("fb_c").id)
        assertEquals("fb_a", p.nextDay("upper_a").id)
    }

    @Test fun customProgramsRoundTrip() {
        val p = Program(
            "custom_1", "Mine", 2, "Your own quest line.", custom = true,
            days = listOf(
                PlanDay("custom_1_d0", "Push", "Push", "", "", listOf(PlannedExercise("Smith Machine Bench Press", 3, "6-8"))),
                PlanDay("custom_1_d1", "Legs", "Legs", "", "", listOf(PlannedExercise("Hack Squat", 4, "8-12"))),
            ),
        )
        assertEquals(listOf(p), ProgramStore.decode(ProgramStore.encode(listOf(p))))
        assertEquals(emptyList<Program>(), ProgramStore.decode("not json"))
    }
}
