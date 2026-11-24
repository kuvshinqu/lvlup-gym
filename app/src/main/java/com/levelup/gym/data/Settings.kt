package com.levelup.gym.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Units { METRIC, IMPERIAL }

/** Small app settings kept in SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _units = MutableStateFlow(
        runCatching { Units.valueOf(prefs.getString(KEY_UNITS, null) ?: "") }.getOrDefault(Units.METRIC)
    )
    val units: StateFlow<Units> = _units

    fun setUnits(units: Units) {
        prefs.edit().putString(KEY_UNITS, units.name).apply()
        _units.value = units
    }

    private val _programId = MutableStateFlow(prefs.getString(KEY_PROGRAM, null))
    /** The chosen quest line; null means the default program. */
    val programId: StateFlow<String?> = _programId

    fun setProgramId(id: String) {
        prefs.edit().putString(KEY_PROGRAM, id).apply()
        _programId.value = id
    }

    private val _customPrograms = MutableStateFlow(prefs.getString(KEY_CUSTOM, null))
    /** The hero's own programs, as JSON (see ProgramStore). */
    val customProgramsJson: StateFlow<String?> = _customPrograms

    fun setCustomProgramsJson(json: String) {
        prefs.edit().putString(KEY_CUSTOM, json).apply()
        _customPrograms.value = json
    }

    private val _quizDone = MutableStateFlow(prefs.getBoolean(KEY_QUIZ, false))
    val quizDone: StateFlow<Boolean> = _quizDone

    fun setQuizDone() {
        prefs.edit().putBoolean(KEY_QUIZ, true).apply()
        _quizDone.value = true
    }

    private companion object {
        const val KEY_UNITS = "units"
        const val KEY_PROGRAM = "program_id"
        const val KEY_CUSTOM = "custom_programs"
        const val KEY_QUIZ = "quiz_done"
    }
}
