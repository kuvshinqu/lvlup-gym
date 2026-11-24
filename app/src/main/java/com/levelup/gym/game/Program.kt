package com.levelup.gym.game

/**
 * Starter program: 4-day upper/lower split.
 *
 * Why this shape (summary of the research it follows):
 * - Training each muscle about twice a week tends to beat once a week for growth
 *   (Schoenfeld et al. 2016 meta-analysis on frequency).
 * - Roughly 10+ hard sets per muscle per week is a good target for most lifters
 *   (Schoenfeld et al. 2017 dose-response meta-analysis).
 * - Mixing heavier sets (5-8 reps) with moderate ones (8-15) builds both strength and size.
 * - Big compound lifts go first while you're fresh; isolation work comes after.
 * - Rest 2-3 min on compounds and 1-2 min on isolation; add weight once you hit the top
 *   of the rep range on every set (double progression).
 */
data class PlannedExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: String,
    val note: String = "",
)

data class PlanDay(
    val id: String,
    val name: String,
    val questName: String,
    val suggestedDay: String,
    val focus: String,
    val exercises: List<PlannedExercise>,
)

object StarterProgram {
    const val NAME = "Upper / Lower · 4 days"

    val days = listOf(
        PlanDay(
            id = "upper_a", name = "Upper A", questName = "Siege of the Upper Keep",
            suggestedDay = "Mon", focus = "Heavy upper body",
            exercises = listOf(
                PlannedExercise("Bench Press", 4, "6-8", "Rest 2-3 min"),
                PlannedExercise("Barbell Row", 4, "6-8", "Rest 2-3 min"),
                PlannedExercise("Overhead Press", 3, "8-10"),
                PlannedExercise("Lat Pulldown", 3, "10-12"),
                PlannedExercise("Bicep Curl", 2, "10-12"),
                PlannedExercise("Tricep Pushdown", 2, "10-12"),
            ),
        ),
        PlanDay(
            id = "lower_a", name = "Lower A", questName = "Trial of the Mountain",
            suggestedDay = "Tue", focus = "Squat-focused legs",
            exercises = listOf(
                PlannedExercise("Squat", 4, "6-8", "Rest 2-3 min"),
                PlannedExercise("Romanian Deadlift", 3, "8-10"),
                PlannedExercise("Leg Press", 3, "10-12"),
                PlannedExercise("Leg Curl", 3, "10-12"),
                PlannedExercise("Calf Raise", 3, "12-15"),
                PlannedExercise("Plank (reps = seconds)", 3, "30-60 s"),
            ),
        ),
        PlanDay(
            id = "upper_b", name = "Upper B", questName = "Raid on the Sky Tower",
            suggestedDay = "Thu", focus = "Volume upper body",
            exercises = listOf(
                PlannedExercise("Incline Dumbbell Press", 4, "8-10"),
                PlannedExercise("Pull-up", 4, "6-10", "Use the pulldown if you can't do 6 yet"),
                PlannedExercise("Seated Cable Row", 3, "10-12"),
                PlannedExercise("Dumbbell Shoulder Press", 3, "10-12"),
                PlannedExercise("Lateral Raise", 3, "12-15"),
                PlannedExercise("Hammer Curl", 2, "10-12"),
                PlannedExercise("Overhead Tricep Extension", 2, "10-12"),
            ),
        ),
        PlanDay(
            id = "lower_b", name = "Lower B", questName = "Descent into the Deep Mines",
            suggestedDay = "Fri", focus = "Hinge-focused legs",
            exercises = listOf(
                PlannedExercise("Deadlift", 3, "5", "Rest 3 min"),
                PlannedExercise("Bulgarian Split Squat", 3, "8-10 each leg"),
                PlannedExercise("Leg Extension", 3, "12-15"),
                PlannedExercise("Leg Curl", 3, "10-12"),
                PlannedExercise("Calf Raise", 3, "12-15"),
                PlannedExercise("Hanging Leg Raise", 3, "10-15"),
            ),
        ),
    )

    const val CARDIO_TIP =
        "Optional: 20-30 min easy cardio (Zone 2, you can still talk) after a lower day or on a rest day."

    fun day(id: String?): PlanDay? = days.firstOrNull { it.id == id }

    /** The day after the last one done, wrapping around. */
    fun nextDay(lastDoneId: String?): PlanDay {
        val i = days.indexOfFirst { it.id == lastDoneId }
        return days[(i + 1).mod(days.size)]
    }
}
