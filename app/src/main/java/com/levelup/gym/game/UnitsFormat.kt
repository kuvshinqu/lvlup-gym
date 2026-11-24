package com.levelup.gym.game

import com.levelup.gym.data.Units
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Converts stored metric values (kg, metres) to what the hero sees, and back. */
object UnitsFormat {
    const val LB_PER_KG = 2.2046226218
    const val M_PER_MILE = 1609.344

    fun weightUnit(u: Units) = if (u == Units.METRIC) "kg" else "lb"
    fun distanceUnit(u: Units) = if (u == Units.METRIC) "km" else "mi"
    fun weightStep(u: Units) = if (u == Units.METRIC) 2.5 else 5.0

    fun kgToDisplay(kg: Double, u: Units): Double = if (u == Units.METRIC) kg else kg * LB_PER_KG
    fun displayToKg(value: Double, u: Units): Double = if (u == Units.METRIC) value else value / LB_PER_KG

    fun metresToDisplay(m: Double, u: Units): Double = if (u == Units.METRIC) m / 1000.0 else m / M_PER_MILE
    fun displayToMetres(value: Double, u: Units): Double = if (u == Units.METRIC) value * 1000.0 else value * M_PER_MILE

    /** "60", "62.5", "132.3" */
    fun number(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (abs(rounded - rounded.roundToInt()) < 1e-9) rounded.roundToInt().toString()
        else String.format(Locale.US, "%.1f", rounded)
    }

    fun weight(kg: Double, u: Units) = "${number(kgToDisplay(kg, u))} ${weightUnit(u)}"
    fun distance(m: Double, u: Units) = "${number(metresToDisplay(m, u))} ${distanceUnit(u)}"

    fun duration(sec: Int): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            s == 0 -> "$m min"
            else -> "${m}m ${s}s"
        }
    }

    /** Big totals like lifetime volume: "12.4 t" or "27,300 lb". */
    fun volume(kg: Double, u: Units): String =
        if (u == Units.METRIC) {
            if (kg >= 1000) "${number(kg / 1000)} t" else "${kg.roundToInt()} kg"
        } else {
            String.format(Locale.US, "%,d lb", (kg * LB_PER_KG).roundToInt())
        }
}
