package com.levelup.gym.data

import com.levelup.gym.data.ExerciseKind.CARDIO
import com.levelup.gym.data.ExerciseKind.STRENGTH

data class ExerciseSeed(val name: String, val muscleGroup: String, val kind: ExerciseKind = STRENGTH)

/** Starter exercises for a new hero, covering everything the starter program uses. */
val DEFAULT_EXERCISES = listOf(
    ExerciseSeed("Bench Press", "Chest"),
    ExerciseSeed("Incline Dumbbell Press", "Chest"),
    ExerciseSeed("Push-up", "Chest"),
    ExerciseSeed("Barbell Row", "Back"),
    ExerciseSeed("Pull-up", "Back"),
    ExerciseSeed("Lat Pulldown", "Back"),
    ExerciseSeed("Seated Cable Row", "Back"),
    ExerciseSeed("Deadlift", "Back"),
    ExerciseSeed("Squat", "Legs"),
    ExerciseSeed("Romanian Deadlift", "Legs"),
    ExerciseSeed("Leg Press", "Legs"),
    ExerciseSeed("Bulgarian Split Squat", "Legs"),
    ExerciseSeed("Leg Curl", "Legs"),
    ExerciseSeed("Leg Extension", "Legs"),
    ExerciseSeed("Calf Raise", "Legs"),
    ExerciseSeed("Overhead Press", "Shoulders"),
    ExerciseSeed("Dumbbell Shoulder Press", "Shoulders"),
    ExerciseSeed("Lateral Raise", "Shoulders"),
    ExerciseSeed("Bicep Curl", "Arms"),
    ExerciseSeed("Hammer Curl", "Arms"),
    ExerciseSeed("Tricep Pushdown", "Arms"),
    ExerciseSeed("Overhead Tricep Extension", "Arms"),
    ExerciseSeed("Plank (reps = seconds)", "Core"),
    ExerciseSeed("Hanging Leg Raise", "Core"),
    ExerciseSeed("Running", "Cardio", CARDIO),
    ExerciseSeed("Cycling", "Cardio", CARDIO),
    ExerciseSeed("Rowing Machine", "Cardio", CARDIO),
    ExerciseSeed("Incline Walk", "Cardio", CARDIO),
    ExerciseSeed("Elliptical", "Cardio", CARDIO),
)

val MUSCLE_GROUPS = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio")
