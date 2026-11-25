package com.levelup.gym.game

import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.ExportRow
import com.levelup.gym.data.Units
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Writes the workout history as a CSV that opens in Excel, Google Sheets and Numbers: one row per set. */
object HistoryExport {
    private val date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val time = DateTimeFormatter.ofPattern("HH:mm")

    fun fileName(today: java.time.LocalDate = java.time.LocalDate.now()) = "level-up-gym-history-$today.csv"

    fun csv(
        rows: List<ExportRow>,
        units: Units,
        workoutName: (String?) -> String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val w = UnitsFormat.weightUnit(units)
        val d = UnitsFormat.distanceUnit(units)
        val out = StringBuilder("﻿") // byte order mark, so Excel reads the file as UTF-8
        line(out, listOf("Date", "Workout", "Start", "End", "Duration (min)", "Exercise", "Muscle group", "Set", "Reps", "Weight ($w)", "Time (s)", "Distance ($d)", "XP", "New best"))
        val setNumber = HashMap<Pair<Long, String>, Int>()
        rows.forEach { r ->
            val start = Instant.ofEpochMilli(r.startedAt).atZone(zone)
            val end = r.finishedAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
            val n = setNumber.merge(r.workoutId to r.exerciseName, 1, Int::plus)!!
            val timed = r.exerciseKind == ExerciseKind.STRENGTH && ExerciseGuides.forName(r.exerciseName)?.timed == true
            val seconds = when {
                r.exerciseKind == ExerciseKind.CARDIO -> r.durationSec
                timed -> r.reps
                else -> 0
            }
            line(
                out,
                listOf(
                    date.format(start),
                    workoutName(r.planDayId),
                    time.format(start),
                    end?.let { time.format(it) } ?: "",
                    r.finishedAt?.let { ((it - r.startedAt) / 60_000).toString() } ?: "",
                    r.exerciseName,
                    r.muscleGroup,
                    n.toString(),
                    if (r.exerciseKind == ExerciseKind.STRENGTH && !timed && r.reps > 0) r.reps.toString() else "",
                    if (r.weightKg > 0) UnitsFormat.number(UnitsFormat.kgToDisplay(r.weightKg, units)) else "",
                    if (seconds > 0) seconds.toString() else "",
                    if (r.distanceM > 0) String.format(java.util.Locale.US, "%.2f", UnitsFormat.metresToDisplay(r.distanceM, units)) else "",
                    r.xp.toString(),
                    if (r.isPersonalRecord) "yes" else "",
                ),
            )
        }
        return out.toString()
    }

    private fun line(out: StringBuilder, cells: List<String>) {
        cells.joinTo(out, ",") { c ->
            if (c.any { it == ',' || it == '"' || it == '\n' }) "\"" + c.replace("\"", "\"\"") + "\"" else c
        }
        out.append("\r\n")
    }
}
