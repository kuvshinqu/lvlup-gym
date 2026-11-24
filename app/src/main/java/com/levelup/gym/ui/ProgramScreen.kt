package com.levelup.gym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.game.StarterProgram

@Composable
fun ProgramScreen(vm: GymViewModel, onOpenWorkout: () -> Unit) {
    val next by vm.nextPlanDay.collectAsStateWithLifecycle()
    val active by vm.activeWorkout.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("QUEST LINE", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                Text(StarterProgram.NAME, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Each muscle twice a week, about 10 to 16 hard sets per muscle. Suggested days: " +
                        StarterProgram.days.joinToString(", ") { "${it.suggestedDay} ${it.name}" } + ". " +
                        "Finish each set 1 to 2 reps short of failure. When you hit the top of the rep range on every set, add weight next time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GameColors.Muted,
                )
            }
        }
        items(StarterProgram.days, key = { it.id }) { day ->
            val isNext = day.id == next.id
            GameCard(accent = if (isNext) GameColors.Gold else null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${day.suggestedDay.uppercase()} · ${day.name}", style = MaterialTheme.typography.labelSmall, color = GameColors.Gold)
                            Text(day.questName, style = MaterialTheme.typography.titleLarge)
                            Text(day.focus, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                        if (isNext) Text("NEXT", style = MaterialTheme.typography.labelSmall, color = GameColors.Xp)
                    }
                    day.exercises.forEach { ex ->
                        Row {
                            Text(ex.exerciseName, modifier = Modifier.weight(1f))
                            Text("${ex.sets} × ${ex.reps}", fontWeight = FontWeight.Bold)
                        }
                        if (ex.note.isNotEmpty()) {
                            Text(ex.note, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                    }
                    if (active == null) {
                        val start = { vm.startWorkout(day.id); onOpenWorkout() }
                        if (isNext) Button(onClick = start, modifier = Modifier.fillMaxWidth()) { Text("Begin quest") }
                        else OutlinedButton(onClick = start, modifier = Modifier.fillMaxWidth()) { Text("Begin quest") }
                    }
                }
            }
        }
        item {
            GameCard(accent = GameColors.Ember) {
                Text("🔥  ${StarterProgram.CARDIO_TIP}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
