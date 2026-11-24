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

    private companion object {
        const val KEY_UNITS = "units"
    }
}
