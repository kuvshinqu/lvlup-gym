package com.levelup.gym.game

/** The Hero's Trial: a short quiz that picks a starting program. */
object Quiz {
    data class Question(val id: String, val title: String, val options: List<String>)

    val questions = listOf(
        Question("goal", "What is your main quest?", listOf("Build muscle", "Get stronger", "Get fit and healthy")),
        Question("days", "How many days a week can you train?", listOf("2-3 days", "4 days", "5-6 days")),
        Question("place", "Where do you train?", listOf("A full gym", "At home with dumbbells")),
        Question("experience", "How long have you been lifting?", listOf("Under 6 months", "6 months to 2 years", "Over 2 years")),
        Question("time", "How long can a session be?", listOf("About 45 min", "About 60 min", "75 min or more")),
    )

    data class Result(val program: Program, val reasons: List<String>, val tip: String)

    /** [answers] maps question id to the chosen option index. */
    fun recommend(answers: Map<String, Int>): Result {
        val goal = answers["goal"] ?: 0
        val days = answers["days"] ?: 1
        val home = (answers["place"] ?: 0) == 1
        val experience = answers["experience"] ?: 0
        val time = answers["time"] ?: 1
        val reasons = mutableListOf<String>()

        val program = when {
            home -> {
                reasons += "You train at home, so every move uses only dumbbells and a bench."
                Programs.dumbbell3
            }
            days == 0 -> {
                reasons += "With 2-3 days, full-body sessions hit every muscle often enough to grow."
                Programs.fullBody3
            }
            days == 2 && experience == 0 -> {
                reasons += "You're new, so 4 days of upper/lower builds a base without beating you up. Add a 5th day of cardio if you like."
                Programs.upperLower4
            }
            days == 2 -> {
                reasons += "With 5-6 days, push/pull/legs fits more weekly sets per muscle, which helps experienced lifters keep growing."
                Programs.pushPullLegs6
            }
            else -> {
                reasons += "4 days of upper/lower trains each muscle twice a week, the sweet spot for most lifters."
                Programs.upperLower4
            }
        }
        if (time == 0) reasons += "Short on time? Rest 90 s between sets and drop the last exercise of a day when you need to."
        if (experience == 2) reasons += "With your experience, take most sets to 0-1 reps short of failure."

        val tip = when (goal) {
            1 -> "Strength: keep the first lift of each day heavy (3-6 reps) and rest 2-3 min between those sets."
            2 -> "Fitness: add 2-3 easy cardio sessions of 20-30 min a week, and keep lifting 1-3 reps short of failure."
            else -> "Muscle: finish most sets 1-3 reps short of failure. When you hit the top of the rep range on every set, add weight."
        }
        return Result(program, reasons, tip)
    }
}
