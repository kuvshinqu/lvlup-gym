package com.levelup.gym.game

import org.json.JSONArray
import org.json.JSONObject

/**
 * The plan for the workout in progress: which exercises are on the card and how many sets each should have.
 * Logged sets live in the database; this only holds the rows still to do, so "+ Add a set" and added
 * exercises survive the app being closed mid-workout.
 */
data class WorkoutDraft(val workoutId: Long, val items: List<Item>) {
    data class Item(val exerciseName: String, val targetSets: Int, val reps: String = "")

    fun item(name: String): Item? = items.firstOrNull { it.exerciseName == name }

    /** One more set for [name], counting sets already logged beyond the plan. */
    fun addSet(name: String, logged: Int): WorkoutDraft {
        val current = item(name)
        val next = maxOf(current?.targetSets ?: 0, logged) + 1
        return withItem(current?.copy(targetSets = next) ?: Item(name, next))
    }

    /** One set fewer, but never below what is already logged. */
    fun removeSet(name: String, logged: Int): WorkoutDraft {
        val current = item(name) ?: return this
        return withItem(current.copy(targetSets = maxOf(logged, current.targetSets - 1)))
    }

    fun addExercise(name: String, sets: Int = 3, reps: String = ""): WorkoutDraft =
        if (item(name) != null) this else copy(items = items + Item(name, sets, reps))

    fun removeExercise(name: String): WorkoutDraft = copy(items = items.filter { it.exerciseName != name })

    private fun withItem(item: Item): WorkoutDraft =
        if (items.any { it.exerciseName == item.exerciseName }) copy(items = items.map { if (it.exerciseName == item.exerciseName) item else it })
        else copy(items = items + item)

    fun encode(): String = JSONObject().apply {
        put("workout", workoutId)
        put("items", JSONArray().apply {
            items.forEach { put(JSONObject().apply { put("name", it.exerciseName); put("sets", it.targetSets); put("reps", it.reps) }) }
        })
    }.toString()

    companion object {
        fun fromPlan(workoutId: Long, plan: PlanDay?): WorkoutDraft =
            WorkoutDraft(workoutId, plan?.exercises.orEmpty().map { Item(it.exerciseName, it.sets, it.reps) })

        fun decode(json: String?): WorkoutDraft? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val o = JSONObject(json)
                val arr = o.getJSONArray("items")
                WorkoutDraft(
                    o.getLong("workout"),
                    (0 until arr.length()).map { i ->
                        val it = arr.getJSONObject(i)
                        Item(it.getString("name"), it.getInt("sets"), it.optString("reps", ""))
                    },
                )
            }.getOrNull()
        }

        /** "6-8" -> 6, "12" -> 12, "30 s" -> 30, "" -> null. */
        fun lowerReps(range: String): Int? = Regex("\\d+").find(range)?.value?.toIntOrNull()
    }
}
