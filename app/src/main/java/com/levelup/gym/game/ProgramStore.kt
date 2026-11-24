package com.levelup.gym.game

import org.json.JSONArray
import org.json.JSONObject

/** Saves the hero's own programs as JSON (kept in settings). */
object ProgramStore {
    fun encode(programs: List<Program>): String = JSONArray().apply {
        programs.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id); put("name", p.name)
                put("days", JSONArray().apply {
                    p.days.forEach { d ->
                        put(JSONObject().apply {
                            put("id", d.id); put("name", d.name); put("quest", d.questName)
                            put("exercises", JSONArray().apply {
                                d.exercises.forEach { e ->
                                    put(JSONObject().apply { put("name", e.exerciseName); put("sets", e.sets); put("reps", e.reps) })
                                }
                            })
                        })
                    }
                })
            })
        }
    }.toString()

    fun decode(json: String?): List<Program> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val daysArr = o.getJSONArray("days")
                val days = (0 until daysArr.length()).map { k ->
                    val d = daysArr.getJSONObject(k)
                    val exArr = d.getJSONArray("exercises")
                    PlanDay(
                        id = d.getString("id"), name = d.getString("name"), questName = d.optString("quest", d.getString("name")),
                        suggestedDay = "", focus = "",
                        exercises = (0 until exArr.length()).map { x ->
                            val e = exArr.getJSONObject(x)
                            PlannedExercise(e.getString("name"), e.getInt("sets"), e.getString("reps"))
                        },
                    )
                }
                Program(
                    id = o.getString("id"), name = o.getString("name"), daysPerWeek = days.size,
                    summary = "Your own quest line.", days = days, custom = true,
                )
            }
        }.getOrDefault(emptyList())
    }
}
