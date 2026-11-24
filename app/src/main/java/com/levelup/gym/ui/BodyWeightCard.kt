package com.levelup.gym.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.levelup.gym.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.levelup.gym.data.BodyWeightEntity
import com.levelup.gym.data.Units
import com.levelup.gym.game.UnitsFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("d MMM")

/** Latest body weight, change over 30 days and a small trend line. Body weight never gives XP. */
@Composable
fun BodyWeightCard(
    latest: BodyWeightEntity?,
    last30Days: List<BodyWeightEntity>,
    units: Units,
    onLog: (Double) -> Unit,
) {
    var dialog by remember { mutableStateOf(false) }

    GameCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedievalIcon(R.drawable.ic_bodyweight_anvil, 26.dp)
                Spacer(Modifier.width(8.dp))
                Text("Body weight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (latest != null) {
                    Text(
                        dayFmt.format(Instant.ofEpochMilli(latest.loggedAt).atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = GameColors.Muted,
                    )
                }
            }
            if (latest == null) {
                Text(
                    "Weigh in to track your body weight here. It doesn't change your XP.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GameColors.Muted,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(UnitsFormat.weight(latest.weightKg, units), style = MaterialTheme.typography.headlineMedium)
                    if (last30Days.size >= 2) {
                        val diffKg = last30Days.last().weightKg - last30Days.first().weightKg
                        val diff = UnitsFormat.kgToDisplay(diffKg, units)
                        val sign = if (diff > 0.05) "+" else if (diff < -0.05) "−" else "±"
                        Text(
                            "$sign${UnitsFormat.number(kotlin.math.abs(diff))} ${UnitsFormat.weightUnit(units)} in 30 days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GameColors.Muted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                if (last30Days.size >= 2) Sparkline(last30Days.map { it.weightKg })
            }
            FilledTonalButton(onClick = { dialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Log body weight", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (dialog) {
        var text by remember {
            mutableStateOf(latest?.let { UnitsFormat.number(UnitsFormat.kgToDisplay(it.weightKg, units)) } ?: "")
        }
        val value = text.replace(',', '.').toDoubleOrNull()
        val focus = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Weigh-in") },
            text = {
                OutlinedTextField(
                    text, { text = it }, singleLine = true,
                    label = { Text("Body weight (${UnitsFormat.weightUnit(units)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = value != null && value > 0,
                    onClick = {
                        focus.clearFocus()
                        value?.let { onLog(UnitsFormat.displayToKg(it, units)) }
                        dialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Sparkline(values: List<Double>) {
    val color = GameColors.Arcane
    Canvas(Modifier.fillMaxWidth().height(48.dp)) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0.01 } ?: 1.0
        val pad = 4.dp.toPx()
        fun pt(i: Int) = Offset(
            pad + (size.width - 2 * pad) * i / (values.size - 1),
            pad + (size.height - 2 * pad) * (1 - ((values[i] - min) / range).toFloat()),
        )
        val path = Path().apply {
            values.indices.forEach { i -> val p = pt(i); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, 4.dp.toPx(), pt(values.lastIndex))
    }
}
