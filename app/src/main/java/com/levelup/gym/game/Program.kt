package com.levelup.gym.game

/**
 * Training programs ("quest lines").
 *
 * The built-in programs follow current research (as of 2025):
 * - Weekly hard sets per muscle drive growth, with diminishing returns: about 10-20 sets per muscle per week
 *   suits most lifters (Pelland et al. 2024 dose-response meta-analysis; Schoenfeld et al. 2017).
 * - With weekly sets equal, how often you train a muscle matters little, so pick the number of days that fits
 *   your life; 2-3 sessions per muscle per week just makes the sets easier to spread (Pelland 2024; Schoenfeld 2019).
 * - Sets taken close to failure (about 0-3 reps in reserve) grow muscle best; strength needs less (Refalo et al. 2023/2024).
 * - Exercises that load a muscle while it is stretched tend to win: seated over lying leg curls, overhead triceps work,
 *   deep squats and lunges, incline curls, Romanian deadlifts (Maeo 2021, 2023; Pedrosa 2022; Wolf et al. 2023).
 * - Heavy compounds early in the session; 5-30 reps all build muscle when effort is high, heavier work builds strength.
 * - Progress with double progression: hit the top of the rep range on every set, then add weight.
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

data class Program(
    val id: String,
    val name: String,
    val daysPerWeek: Int,
    val summary: String,
    val days: List<PlanDay>,
    /** Needs only dumbbells and a bench. */
    val homeFriendly: Boolean = false,
    val custom: Boolean = false,
) {
    fun day(id: String?): PlanDay? = days.firstOrNull { it.id == id }

    /** The day after the last one done, wrapping around; the first day if the last one is from another program. */
    fun nextDay(lastDoneId: String?): PlanDay {
        val i = days.indexOfFirst { it.id == lastDoneId }
        return days[(i + 1).mod(days.size)]
    }
}


object Programs {
    const val CARDIO_TIP =
        "Optional: 20-30 min easy cardio (Zone 2, you can still talk) after a lower day or on a rest day."

    val fullBody3 = Program(
        id = "full_body_3", name = "Full Body · 3 days", daysPerWeek = 3,
        summary = "Every muscle three times a week, 9-12 hard sets each. Train Mon, Wed, Fri or any 3 days with a rest day between.",
        days = listOf(
            PlanDay(
                id = "fb_a", name = "Full Body A", questName = "The Gate of Three Towers",
                suggestedDay = "Mon", focus = "Squat, press, pull",
                exercises = listOf(
                PlannedExercise("Squat", 3, "6-8", "Rest 2-3 min"),
                PlannedExercise("Bench Press", 3, "6-8", "Rest 2-3 min"),
                PlannedExercise("Lat Pulldown", 3, "8-12"),
                PlannedExercise("Romanian Deadlift", 2, "8-10"),
                PlannedExercise("Lateral Raise", 2, "12-20"),
                PlannedExercise("Incline Dumbbell Curl", 2, "10-15"),
                ),
            ),
            PlanDay(
                id = "fb_b", name = "Full Body B", questName = "The Sunken Armory",
                suggestedDay = "Wed", focus = "Hinge, incline, row",
                exercises = listOf(
                PlannedExercise("Deadlift", 2, "5", "Rest 3 min"),
                PlannedExercise("Incline Dumbbell Press", 3, "8-12"),
                PlannedExercise("Chest-Supported Dumbbell Row", 3, "8-12"),
                PlannedExercise("Bulgarian Split Squat", 2, "8-12 each leg"),
                PlannedExercise("Overhead Tricep Extension", 2, "10-15"),
                PlannedExercise("Hanging Leg Raise", 2, "10-15"),
                ),
            ),
            PlanDay(
                id = "fb_c", name = "Full Body C", questName = "The Dragon's Stair",
                suggestedDay = "Fri", focus = "Legs, overhead, pull-ups",
                exercises = listOf(
                PlannedExercise("Leg Press", 3, "10-15"),
                PlannedExercise("Overhead Press", 3, "6-10"),
                PlannedExercise("Pull-up", 3, "6-10", "Use the pulldown if you can't do 6 yet"),
                PlannedExercise("Seated Leg Curl", 3, "10-12"),
                PlannedExercise("Cable Fly", 2, "12-15"),
                PlannedExercise("Calf Raise", 3, "10-15", "Pause 2 s at the bottom"),
                ),
            ),
        ),
    )

    val dumbbell3 = Program(
        id = "dumbbell_3", name = "Dumbbell Home · 3 days", daysPerWeek = 3, homeFriendly = true,
        summary = "Only dumbbells and a bench. Full body three times a week, with moves that stretch each muscle under load.",
        days = listOf(
            PlanDay(
                id = "db_a", name = "Home A", questName = "Siege of the Hearth",
                suggestedDay = "Mon", focus = "Squat, press, row",
                exercises = listOf(
                PlannedExercise("Goblet Squat", 3, "10-15"),
                PlannedExercise("Dumbbell Bench Press", 3, "8-12"),
                PlannedExercise("Dumbbell Row", 3, "10-12", "Each arm"),
                PlannedExercise("Dumbbell Romanian Deadlift", 2, "10-12"),
                PlannedExercise("Lateral Raise", 2, "12-20"),
                PlannedExercise("Incline Dumbbell Curl", 2, "10-15"),
                ),
            ),
            PlanDay(
                id = "db_b", name = "Home B", questName = "The Cellar Trial",
                suggestedDay = "Wed", focus = "Lunge, press, pull",
                exercises = listOf(
                PlannedExercise("Walking Lunge", 3, "8-12 each leg"),
                PlannedExercise("Dumbbell Shoulder Press", 3, "8-12"),
                PlannedExercise("Chest-Supported Dumbbell Row", 3, "10-12"),
                PlannedExercise("Glute Bridge", 2, "12-20"),
                PlannedExercise("Overhead Tricep Extension", 2, "10-15"),
                PlannedExercise("Plank (reps = seconds)", 2, "30-60 s"),
                ),
            ),
            PlanDay(
                id = "db_c", name = "Home C", questName = "The Watchtower",
                suggestedDay = "Fri", focus = "Split squat, push-ups, back",
                exercises = listOf(
                PlannedExercise("Bulgarian Split Squat", 3, "8-12 each leg"),
                PlannedExercise("Push-up", 3, "8-20", "Feet on a bench to make it harder"),
                PlannedExercise("Dumbbell Row", 3, "10-12", "Each arm"),
                PlannedExercise("Dumbbell Romanian Deadlift", 2, "10-12"),
                PlannedExercise("Dumbbell Fly", 2, "12-15"),
                PlannedExercise("Hammer Curl", 2, "10-12"),
                ),
            ),
        ),
    )

    val upperLower4 = Program(
        id = "upper_lower_4", name = "Upper / Lower · 4 days", daysPerWeek = 4,
        summary = "Each muscle twice a week, about 10-16 hard sets each. Suggested days: Mon, Tue, Thu, Fri.",
        days = listOf(
        PlanDay(
            id = "upper_a", name = "Upper A", questName = "Siege of the Upper Keep",
            suggestedDay = "Mon", focus = "Heavy upper body",
            exercises = listOf(
                PlannedExercise("Bench Press", 4, "6-8", "Rest 2-3 min"),
                PlannedExercise("Barbell Row", 4, "6-8", "Rest 2-3 min"),
                PlannedExercise("Overhead Press", 3, "8-10"),
                PlannedExercise("Lat Pulldown", 3, "10-12"),
                PlannedExercise("Incline Dumbbell Curl", 2, "10-12"),
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
                PlannedExercise("Seated Leg Curl", 3, "10-12"),
                PlannedExercise("Calf Raise", 3, "12-15"),
                PlannedExercise("Hanging Leg Raise", 3, "10-15"),
            ),
        ),
        ),
    )

    val pushPullLegs6 = Program(
        id = "ppl_6", name = "Push / Pull / Legs · 5-6 days", daysPerWeek = 6,
        summary = "Each muscle twice a week with more volume, 14-20 sets per muscle. Training 5 days? Keep cycling through the days in order.",
        days = listOf(
            PlanDay(
                id = "ppl_push_a", name = "Push A", questName = "Storm the Gate",
                suggestedDay = "Mon", focus = "Chest, shoulders, triceps",
                exercises = listOf(
                PlannedExercise("Bench Press", 3, "6-8", "Rest 2-3 min"),
                PlannedExercise("Smith Machine Incline Press", 3, "8-12"),
                PlannedExercise("Dumbbell Shoulder Press", 2, "8-12"),
                PlannedExercise("Cable Fly", 2, "12-15"),
                PlannedExercise("Lateral Raise", 3, "12-20"),
                PlannedExercise("Overhead Tricep Extension", 3, "10-15"),
                ),
            ),
            PlanDay(
                id = "ppl_pull_a", name = "Pull A", questName = "The Grappling Hook",
                suggestedDay = "Tue", focus = "Back, rear delts, biceps",
                exercises = listOf(
                PlannedExercise("Pull-up", 3, "6-10"),
                PlannedExercise("Barbell Row", 3, "6-10"),
                PlannedExercise("Lat Pulldown", 2, "10-12"),
                PlannedExercise("Face Pull", 2, "12-15"),
                PlannedExercise("Incline Dumbbell Curl", 3, "10-15"),
                ),
            ),
            PlanDay(
                id = "ppl_legs_a", name = "Legs A", questName = "Trial of the Peaks",
                suggestedDay = "Wed", focus = "Quads first",
                exercises = listOf(
                PlannedExercise("Squat", 3, "6-8", "Rest 3 min"),
                PlannedExercise("Romanian Deadlift", 3, "8-10"),
                PlannedExercise("Leg Extension", 3, "10-15"),
                PlannedExercise("Seated Leg Curl", 3, "10-12"),
                PlannedExercise("Calf Raise", 3, "10-15", "Pause at the bottom"),
                PlannedExercise("Cable Crunch", 2, "10-15"),
                ),
            ),
            PlanDay(
                id = "ppl_push_b", name = "Push B", questName = "Fire on the Ramparts",
                suggestedDay = "Thu", focus = "Shoulders first",
                exercises = listOf(
                PlannedExercise("Overhead Press", 3, "6-8"),
                PlannedExercise("Incline Dumbbell Press", 3, "8-12"),
                PlannedExercise("Dips", 2, "6-12"),
                PlannedExercise("Cable Lateral Raise", 3, "12-20"),
                PlannedExercise("Skull Crusher", 2, "10-12"),
                ),
            ),
            PlanDay(
                id = "ppl_pull_b", name = "Pull B", questName = "Raid on the High Keep",
                suggestedDay = "Fri", focus = "Rows and arms",
                exercises = listOf(
                PlannedExercise("Chest-Supported Dumbbell Row", 3, "8-12"),
                PlannedExercise("Lat Pulldown", 3, "8-12"),
                PlannedExercise("Straight-Arm Pulldown", 2, "12-15"),
                PlannedExercise("Rear Delt Fly", 2, "12-20"),
                PlannedExercise("Preacher Curl", 2, "10-12"),
                PlannedExercise("Hammer Curl", 2, "10-12"),
                ),
            ),
            PlanDay(
                id = "ppl_legs_b", name = "Legs B", questName = "The Sunless Mines",
                suggestedDay = "Sat", focus = "Hinge and glutes",
                exercises = listOf(
                PlannedExercise("Deadlift", 2, "5", "Rest 3 min"),
                PlannedExercise("Hip Thrust", 3, "8-12"),
                PlannedExercise("Walking Lunge", 2, "10 each leg"),
                PlannedExercise("Leg Press", 3, "10-15"),
                PlannedExercise("Seated Calf Raise", 3, "12-15"),
                PlannedExercise("Hanging Leg Raise", 2, "10-15"),
                ),
            ),
        ),
    )

    val builtIn: List<Program> = listOf(fullBody3, dumbbell3, upperLower4, pushPullLegs6)
    val default: Program = upperLower4

    /** Finds a day in any program, for history and the workout screen. */
    fun day(id: String?, custom: List<Program> = emptyList()): PlanDay? =
        if (id == null) null else (builtIn + custom).firstNotNullOfOrNull { it.day(id) }
}
