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
import com.levelup.gym.game.ExerciseGuides
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisesScreen(vm: GymViewModel, onOpenExercise: (Long) -> Unit) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("Chest") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column {
                Text("ARMORY", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
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
                                label = { Text(g) },
                                leadingIcon = { SkillIcon(g, size = 18.dp) },
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    SkillIcon(g, size = 26.dp)
                    Spacer(Modifier.width(10.dp))
                    SectionLabel("$g · ${skill.name}")
                }
            }
            items(list, key = { it.id }) { ex ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenExercise(ex.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseThumb(ex.name, ex.muscleGroup, Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (ExerciseGuides.forName(ex.name) != null) "Tap for how to" else "Your exercise",
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.Muted,
                        )
                    }
                    IconButton(onClick = { vm.deleteExercise(ex) }) {
                        Icon(Icons.Filled.Delete, "Delete ${ex.name}", tint = GameColors.Muted)
                    }
                }
            }
        }
    }
}
