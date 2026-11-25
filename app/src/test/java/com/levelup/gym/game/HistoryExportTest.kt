package com.levelup.gym.game

import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.ExportRow
import com.levelup.gym.data.Units
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryExportTest {
    private val zone = ZoneOffset.UTC
    private val start = LocalDateTime.of(2026, 11, 25, 11, 32).toInstant(zone).toEpochMilli()
    private val end = start + 69 * 60_000L

    private fun row(name: String, kind: ExerciseKind = ExerciseKind.STRENGTH, reps: Int = 0, kg: Double = 0.0, sec: Int = 0, m: Double = 0.0, pr: Boolean = false) =
        ExportRow(1, start, end, "upper_a", 30, name, if (kind == ExerciseKind.CARDIO) "Cardio" else "Chest", kind, reps, kg, sec, m, 20, pr, start)

    @Test fun oneRowPerSetWithTimes() {
        val csv = HistoryExport.csv(
            listOf(row("Bench Press", reps = 8, kg = 60.0), row("Bench Press", reps = 8, kg = 62.5, pr = true), row("Plank (reps = seconds)", reps = 45), row("Running", ExerciseKind.CARDIO, sec = 1200, m = 3500.0)),
            Units.METRIC, { "Upper A, heavy" }, zone,
        )
        val lines = csv.removePrefix("﻿").trimEnd().split("\r\n")
        assertEquals("Date,Workout,Start,End,Duration (min),Exercise,Muscle group,Set,Reps,Weight (kg),Time (s),Distance (km),XP,New best", lines[0])
        assertEquals("2026-11-25,\"Upper A, heavy\",11:32,12:41,69,Bench Press,Chest,1,8,60,,,20,", lines[1])
        assertEquals("2026-11-25,\"Upper A, heavy\",11:32,12:41,69,Bench Press,Chest,2,8,62.5,,,20,yes", lines[2])
        // Planks are timed: the reps are seconds.
        assertTrue(lines[3].contains(",Plank (reps = seconds),Chest,1,,,45,,"))
        assertTrue(lines[4].contains(",Running,Cardio,1,,,1200,3.50,"))
    }

    @Test fun poundsWhenTheHeroUsesThem() {
        val csv = HistoryExport.csv(listOf(row("Bench Press", reps = 5, kg = 100.0)), Units.IMPERIAL, { "Upper A" }, zone)
        assertTrue(csv.contains("Weight (lb)"))
        assertTrue(csv.contains(",5,220.5,"))
    }
}
