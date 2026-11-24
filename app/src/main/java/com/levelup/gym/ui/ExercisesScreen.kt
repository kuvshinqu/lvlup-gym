package com.levelup.gym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.MUSCLE_GROUPS
import com.levelup.gym.game.Skills

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisesScreen(vm: GymViewModel) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("Chest") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column {
                Text("ARSENAL", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                Text("Exercises", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            GameCard(accent = GameColors.Gold) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add exercise", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        name, { name = it }, label = { Text("Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Skill it trains", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MUSCLE_GROUPS.forEach { g ->
                            FilterChip(
                                selected = g == group,
                                onClick = { group = g },
                                label = { Text("${Skills.forGroup(g).icon} $g") },
                            )
                        }
                    }
                    Button(
                        enabled = name.isNotBlank(),
                        onClick = {
                            val kind = if (group == "Cardio") ExerciseKind.CARDIO else ExerciseKind.STRENGTH
                            vm.addExercise(name, group, kind)
                            name = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add") }
                    Text(
                        "Cardio exercises log time and distance; all others log reps and weight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GameColors.Muted,
                    )
                }
            }
        }
        exercises.groupBy { it.muscleGroup }.forEach { (g, list) ->
            item(key = "h-$g") {
                val skill = Skills.forGroup(g)
                SectionLabel("${skill.icon} $g · ${skill.name}")
            }
            items(list, key = { it.id }) { ex ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ex.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { vm.deleteExercise(ex) }) {
                        Icon(Icons.Filled.Delete, "Delete ${ex.name}", tint = GameColors.Muted)
                    }
                }
            }
        }
    }
}
