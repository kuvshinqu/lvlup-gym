package com.levelup.gym.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.size
import com.levelup.gym.game.ExerciseGuides
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.data.ExerciseEntity
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.game.Leveling
import com.levelup.gym.game.Skills
import com.levelup.gym.game.StarterProgram
import com.levelup.gym.game.UnitsFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun String.toNumber(): Double? = replace(',', '.').trim().toDoubleOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(vm: GymViewModel, onClose: () -> Unit, onOpenExercise: (Long) -> Unit) {
    val workout by vm.activeWorkout.collectAsStateWithLifecycle()
    val sets by vm.activeSets.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()

    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reps by rememberSaveable { mutableStateOf("8") }
    var weight by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("20") }
    var distance by rememberSaveable { mutableStateOf("") }
    var pickerOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val plan = StarterProgram.day(workout?.planDayId)
    val selected = exercises.firstOrNull { it.id == selectedId }

    // Pick the first planned exercise by default.
    LaunchedEffect(plan?.id, exercises.size) {
        if (selectedId == null && plan != null) {
            selectedId = exercises.firstOrNull { it.name == plan.exercises.first().exerciseName }?.id
        }
    }
    // Prefill from the last time this exercise was done.
    LaunchedEffect(selectedId, units) {
        val id = selectedId ?: return@LaunchedEffect
        val last = vm.lastSet(id) ?: return@LaunchedEffect
        if (last.reps > 0) reps = last.reps.toString()
        weight = if (last.weightKg > 0) UnitsFormat.number(UnitsFormat.kgToDisplay(last.weightKg, units)) else ""
        if (last.durationSec > 0) minutes = UnitsFormat.number(last.durationSec / 60.0)
        distance = if (last.distanceM > 0) UnitsFormat.number(UnitsFormat.metresToDisplay(last.distanceM, units)) else ""
    }

    // Workout ended (finished or discarded): leave the screen.
    var hadWorkout by remember { mutableStateOf(false) }
    LaunchedEffect(workout) {
        if (workout != null) hadWorkout = true
        else if (hadWorkout) onClose()
    }

    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val sessionXp = sets.sumOf { it.set.xp }

    Column {
        TopAppBar(
            title = {
                Column {
                    Text(plan?.questName ?: "Free workout", style = MaterialTheme.typography.titleMedium)
                    workout?.let {
                        Text(
                            "${plan?.name ?: "Quest"} · started " +
                                timeFmt.format(Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.Muted,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            actions = { XpPill(sessionXp, Modifier.padding(end = 12.dp)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )

        if (workout == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No quest in progress.")
                Button(onClick = { vm.startWorkout(null) }) { Text("Start a free workout") }
            }
        } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Log a set
            item {
                GameCard(accent = GameColors.Gold) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (selected != null) {
                                ExerciseThumb(
                                    selected.name,
                                    selected.muscleGroup,
                                    Modifier.size(56.dp).clickable { onOpenExercise(selected.id) },
                                )
                            }
                            OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.weight(1f)) {
                                Text(selected?.name ?: "Choose exercise")
                            }
                            if (selected != null) {
                                IconButton(onClick = { onOpenExercise(selected.id) }) {
                                    Icon(Icons.Filled.Info, "How to do ${selected.name}", tint = GameColors.Gold)
                                }
                            }
                        }
                        if (selected?.kind == ExerciseKind.CARDIO) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    minutes, { minutes = it }, label = { Text("Minutes") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true, modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    distance, { distance = it }, label = { Text("Distance (${UnitsFormat.distanceUnit(units)})") },
                                    placeholder = { Text("optional") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true, modifier = Modifier.weight(1f),
                                )
                            }
                            val min = minutes.toNumber() ?: 0.0
                            val km = distance.toNumber() ?: 0.0
                            val preview = Leveling.xpForCardio((min * 60).toInt(), UnitsFormat.displayToMetres(km, units))
                            RpgButton(
                                "Log cardio  ·  +$preview XP",
                                enabled = min > 0,
                                onClick = { vm.logCardio(selected, (min * 60).toInt(), UnitsFormat.displayToMetres(km, units)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Stepper(
                                    label = "Reps", value = reps, onValue = { reps = it },
                                    step = 1.0, keyboard = KeyboardType.Number, modifier = Modifier.weight(1f),
                                )
                                val bodyweight = selected?.let { ExerciseGuides.forName(it.name)?.bodyweight } == true
                                Stepper(
                                    label = (if (bodyweight) "Added weight" else "Weight") + " (${UnitsFormat.weightUnit(units)})",
                                    value = weight, onValue = { weight = it },
                                    step = UnitsFormat.weightStep(units), keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f),
                                )
                            }
                            val r = reps.toNumber()?.toInt() ?: 0
                            val kg = UnitsFormat.displayToKg(weight.toNumber() ?: 0.0, units)
                            RpgButton(
                                "Strike!  Log set  ·  +${Leveling.xpForSet(r, kg, false)} XP",
                                enabled = selected != null && r > 0,
                                onClick = { selected?.let { vm.logSet(it, r, kg) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (selected != null) {
                            val skill = Skills.forGroup(selected.muscleGroup)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SkillIcon(selected.muscleGroup, size = 22.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Trains ${skill.name}" +
                                        if (ExerciseGuides.forName(selected.name)?.bodyweight == true) " · bodyweight exercise" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GameColors.Muted,
                                )
                            }
                        }
                    }
                }
            }

            // Planned objectives
            if (plan != null) {
                item { SectionLabel("Objectives") }
                items(plan.exercises, key = { "plan-" + it.exerciseName }) { pe ->
                    val ex = exercises.firstOrNull { it.name == pe.exerciseName }
                    val done = sets.count { it.exerciseName == pe.exerciseName }
                    val complete = done >= pe.sets
                    GameCard(
                        modifier = Modifier.clickable(enabled = ex != null) { selectedId = ex?.id },
                        accent = when {
                            ex != null && ex.id == selectedId -> GameColors.Gold
                            complete -> GameColors.Xp
                            else -> null
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ObjectiveMark(complete)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pe.exerciseName, fontWeight = FontWeight.Bold)
                                Text("${pe.sets} × ${pe.reps}", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                            }
                            Text("$done/${pe.sets}", color = if (complete) GameColors.Xp else GameColors.Muted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Logged sets, grouped by exercise
            val groups = sets.groupBy { it.exerciseName }
            if (groups.isNotEmpty()) item { SectionLabel("Battle log") }
            groups.forEach { (name, list) ->
                item(key = "group-$name") {
                    GameCard {
                        Column {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            list.forEachIndexed { i, s ->
                                SetRow(i + 1, s, units) {
                                    IconButton(onClick = { vm.deleteSet(s.set.id) }) {
                                        Icon(Icons.Filled.Delete, "Delete set", tint = GameColors.Muted)
                                    }
                                }
                                if (i < list.lastIndex) HorizontalDivider(color = GameColors.Track)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RpgButton(
                        "Complete quest  ·  +${Leveling.FINISH_WORKOUT_XP} XP",
                        onClick = { vm.finishWorkout() },
                        enabled = sets.isNotEmpty(),
                        color = GameColors.Xp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Abandon workout", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        ExercisePicker(
            exercises = exercises,
            onPick = { selectedId = it.id; pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Abandon this workout?") },
            text = { Text("Its sets and their XP will be removed.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discardWorkout() }) {
                    Text("Abandon", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep going") } },
        )
    }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    step: Double,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value, onValue, label = { Text(label) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            TextButton(onClick = {
                val v = ((value.toNumber() ?: 0.0) - step).coerceAtLeast(0.0)
                onValue(UnitsFormat.number(v))
            }) { Text("−", style = MaterialTheme.typography.titleLarge) }
            TextButton(onClick = {
                onValue(UnitsFormat.number((value.toNumber() ?: 0.0) + step))
            }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun ExercisePicker(
    exercises: List<ExerciseEntity>,
    onPick: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Choose exercise") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                exercises.groupBy { it.muscleGroup }.forEach { (group, list) ->
                    item(key = "h-$group") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            SkillIcon(group, size = 22.dp)
                            Spacer(Modifier.width(8.dp))
                            SectionLabel(group)
                        }
                    }
                    items(list, key = { it.id }) { ex ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseThumb(ex.name, ex.muscleGroup, Modifier.size(44.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(ex.name)
                        }
                    }
                }
            }
        },
    )
}
