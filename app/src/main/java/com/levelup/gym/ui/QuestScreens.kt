package com.levelup.gym.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.R
import com.levelup.gym.data.MUSCLE_GROUPS
import com.levelup.gym.game.Equipment
import com.levelup.gym.game.ExerciseLibrary
import com.levelup.gym.game.LibraryExercise
import com.levelup.gym.game.PlanDay
import com.levelup.gym.game.PlannedExercise
import com.levelup.gym.game.Program
import com.levelup.gym.game.Programs
import com.levelup.gym.game.Quiz

@Composable
private fun ScreenHeader(kicker: String, title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Column {
            Text(kicker, style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

// ---------------------------------------------------------------- Program picker

@Composable
fun ProgramsScreen(vm: GymViewModel, onBack: () -> Unit, onOpenQuiz: () -> Unit, onBuild: (String?) -> Unit) {
    val active by vm.activeProgram.collectAsStateWithLifecycle()
    val custom by vm.customPrograms.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("QUEST LINES", "Choose a program", onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenQuiz, modifier = Modifier.weight(1f)) { Text("Take the trial") }
                OutlinedButton(onClick = { onBuild(null) }, modifier = Modifier.weight(1f)) { Text("Forge your own") }
            }
        }
        item { SectionLabel("Built-in") }
        items(Programs.builtIn, key = { it.id }) { p ->
            ProgramCard(p, selected = p.id == active.id, onPick = { vm.chooseProgram(p.id) })
        }
        if (custom.isNotEmpty()) {
            item { SectionLabel("Your own") }
            items(custom, key = { it.id }) { p ->
                ProgramCard(
                    p, selected = p.id == active.id, onPick = { vm.chooseProgram(p.id) },
                    onEdit = { onBuild(p.id) }, onDelete = { vm.deleteCustomProgram(p.id) },
                )
            }
        }
    }
}

@Composable
private fun ProgramCard(p: Program, selected: Boolean, onPick: () -> Unit, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    GameCard(accent = if (selected) GameColors.Gold else null, modifier = Modifier.clickable(onClick = onPick)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedievalIcon(if (p.custom) R.drawable.ic_grip_hammer else R.drawable.ic_campaign_map, 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${p.daysPerWeek} days a week" + if (p.homeFriendly) " · home friendly" else "",
                        style = MaterialTheme.typography.bodySmall, color = GameColors.Gold,
                    )
                }
                if (selected) Icon(Icons.Filled.Check, "Active", tint = GameColors.Gold)
            }
            Text(p.summary, style = MaterialTheme.typography.bodyMedium, color = GameColors.Muted)
            Text(p.days.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall)
            if (onEdit != null || onDelete != null) {
                Row {
                    onEdit?.let { TextButton(onClick = it) { Text("Edit") } }
                    onDelete?.let { TextButton(onClick = it) { Text("Delete", color = GameColors.Muted) } }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- The Hero's Trial

@Composable
fun QuizScreen(vm: GymViewModel, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    val total = Quiz.questions.size

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THE HERO'S TRIAL", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                    Text(
                        if (step < total) "Trial ${step + 1} of $total" else "Your quest line",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                IconButton(onClick = { vm.skipQuiz(); onDone() }) { Icon(Icons.Filled.Close, "Skip") }
            }
        }
        if (step < total) {
            val q = Quiz.questions[step]
            item {
                GameCard(accent = GameColors.Arcane) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MedievalIcon(R.drawable.ic_quest_scroll, 40.dp)
                        Text(q.title, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            items(q.options.size) { i ->
                val chosen = answers[q.id] == i
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (chosen) GameColors.Gold.copy(alpha = 0.15f) else GameColors.SurfaceHigh, RoundedCornerShape(12.dp))
                        .border(1.dp, if (chosen) GameColors.Gold else GameColors.Track, RoundedCornerShape(12.dp))
                        .clickable {
                            answers[q.id] = i
                            step++
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(q.options[i], style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
            if (step > 0) item { TextButton(onClick = { step-- }) { Text("Back") } }
        } else {
            val result = Quiz.recommend(answers)
            item {
                GameCard(accent = GameColors.Gold) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedievalIcon(R.drawable.ic_level_crown, 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(result.program.name, style = MaterialTheme.typography.titleLarge)
                        }
                        Text(result.program.summary, style = MaterialTheme.typography.bodyMedium, color = GameColors.Muted)
                        result.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        Text(result.tip, style = MaterialTheme.typography.bodyMedium, color = GameColors.Gold)
                    }
                }
            }
            item {
                RpgButton("Accept the quest", onClick = { vm.finishQuiz(result.program.id); onDone() }, modifier = Modifier.fillMaxWidth())
            }
            item { TextButton(onClick = { answers.clear(); step = 0 }) { Text("Retake the trial") } }
        }
    }
}

// ---------------------------------------------------------------- Forge your own program

private class DraftDay(name: String, exercises: List<PlannedExercise>) {
    var name by mutableStateOf(name)
    val exercises = mutableStateListOf<PlannedExercise>().apply { addAll(exercises) }
}

@Composable
fun ProgramBuilderScreen(vm: GymViewModel, programId: String?, onDone: () -> Unit) {
    val existing = vm.customPrograms.value.firstOrNull { it.id == programId }
    val id = remember { existing?.id ?: "custom_${System.currentTimeMillis()}" }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "My quest line") }
    val days = remember {
        mutableStateListOf<DraftDay>().apply {
            if (existing != null) existing.days.forEach { add(DraftDay(it.name, it.exercises)) }
            else add(DraftDay("Day 1", emptyList()))
        }
    }
    var pickingFor by remember { mutableStateOf<DraftDay?>(null) }
    val canSave = name.isNotBlank() && days.isNotEmpty() && days.all { it.exercises.isNotEmpty() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("FORGE", if (existing == null) "New program" else "Edit program", onDone) }
        item {
            OutlinedTextField(name, { name = it }, label = { Text("Program name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        days.forEachIndexed { di, day ->
            item(key = "day-$di-${days.size}") {
                GameCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                day.name, { day.name = it }, label = { Text("Day ${di + 1}") }, singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (days.size > 1) IconButton(onClick = { days.removeAt(di) }) {
                                Icon(Icons.Filled.Delete, "Remove day", tint = GameColors.Muted)
                            }
                        }
                        day.exercises.forEachIndexed { ei, e ->
                            PlannedRow(
                                e,
                                onChange = { day.exercises[ei] = it },
                                onRemove = { day.exercises.removeAt(ei) },
                            )
                        }
                        OutlinedButton(onClick = { pickingFor = day }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise")
                        }
                    }
                }
            }
        }
        if (days.size < 7) item {
            OutlinedButton(onClick = { days.add(DraftDay("Day ${days.size + 1}", emptyList())) }, modifier = Modifier.fillMaxWidth()) {
                Text("Add a day")
            }
        }
        item {
            Text(
                "Tip: aim for 10-20 hard sets per muscle each week, spread over 2 or more days. Put big lifts first.",
                style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
            )
        }
        item {
            RpgButton(
                "Save and follow", enabled = canSave, modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val program = Program(
                        id = id, name = name.trim(), daysPerWeek = days.size, summary = "Your own quest line.", custom = true,
                        days = days.mapIndexed { i, d ->
                            val n = d.name.ifBlank { "Day ${i + 1}" }
                            PlanDay("${id}_d$i", n, n, "", "", d.exercises.toList())
                        },
                    )
                    vm.saveCustomProgram(program)
                    vm.chooseProgram(program.id)
                    onDone()
                },
            )
        }
    }

    pickingFor?.let { day ->
        ExercisePickerDialog(
            vm,
            onDismiss = { pickingFor = null },
            onPick = { picked ->
                val reps = if (picked.second == "Cardio") "20-30 min" else "8-12"
                day.exercises.add(PlannedExercise(picked.first, if (picked.second == "Cardio") 1 else 3, reps))
                pickingFor = null
            },
        )
    }
}

@Composable
private fun PlannedRow(e: PlannedExercise, onChange: (PlannedExercise) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(e.exerciseName, style = MaterialTheme.typography.bodyLarge)
            Text("${e.sets} × ${e.reps}", style = MaterialTheme.typography.bodySmall, color = GameColors.Gold)
        }
        TextButton(onClick = { onChange(e.copy(sets = (e.sets - 1).coerceAtLeast(1))) }) { Text("−") }
        Text("${e.sets} sets", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { onChange(e.copy(sets = (e.sets + 1).coerceAtMost(10))) }) { Text("+") }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Remove", tint = GameColors.Muted) }
    }
    val ranges = listOf("3-5", "6-8", "8-12", "10-15", "12-20")
    if (e.reps in ranges) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ranges.forEach { r ->
                FilterChip(selected = e.reps == r, onClick = { onChange(e.copy(reps = r)) }, label = { Text(r) })
            }
        }
    }
}

/** Library plus the hero's own exercises; returns (name, muscle group). */
@Composable
private fun ExercisePickerDialog(vm: GymViewModel, onDismiss: () -> Unit, onPick: (Pair<String, String>) -> Unit) {
    val mine by vm.exercises.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val all = remember(mine) {
        (ExerciseLibrary.all.map { it.name to it.muscleGroup } + mine.map { it.name to it.muscleGroup })
            .distinctBy { it.first.lowercase() }.sortedBy { it.first }
    }
    val shown = all.filter { query.isBlank() || it.first.contains(query, true) || it.second.contains(query, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color(0xFF1A1F30),
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Pick an exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it.first }) { (n, grp) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(n to grp) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SkillIcon(grp, size = 24.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(n, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    ExerciseLibrary.find(n)?.equipment?.label?.let { "$grp · $it" } ?: "$grp · your exercise",
                                    style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------- Exercise library

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(vm: GymViewModel, onBack: () -> Unit) {
    val mine by vm.exercises.collectAsStateWithLifecycle()
    val have = mine.map { it.name.lowercase() }.toSet()
    var query by rememberSaveable { mutableStateOf("") }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var stretchOnly by rememberSaveable { mutableStateOf(false) }
    val shown = ExerciseLibrary.all.filter {
        (query.isBlank() || it.name.contains(query, true)) &&
            (equipment == null || it.equipment.name == equipment) &&
            (group == null || it.muscleGroup == group) &&
            (!stretchOnly || it.longLength)
    }.sortedWith(compareBy<LibraryExercise>({ MUSCLE_GROUPS.indexOf(it.muscleGroup) }, { it.name }))

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader("ARMORY", "Exercise library", onBack) }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Search ${ExerciseLibrary.all.size} exercises") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Equipment.entries.forEach { e ->
                    FilterChip(
                        selected = equipment == e.name,
                        onClick = { equipment = if (equipment == e.name) null else e.name },
                        label = { Text(e.label) },
                    )
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MUSCLE_GROUPS.forEach { g ->
                    FilterChip(
                        selected = group == g, onClick = { group = if (group == g) null else g },
                        label = { Text(g) }, leadingIcon = { SkillIcon(g, size = 18.dp) },
                    )
                }
                FilterChip(selected = stretchOnly, onClick = { stretchOnly = !stretchOnly }, label = { Text("Stretch focus") })
            }
        }
        item {
            Text(
                "Stretch focus marks moves that load the muscle while it is long. Recent studies found these grow muscle a bit better.",
                style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
            )
        }
        items(shown, key = { it.name }) { ex ->
            val added = ex.name.lowercase() in have
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(ex.name, ex.muscleGroup, Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${ex.muscleGroup} · ${ex.equipment.label}" + if (ex.longLength) " · stretch focus" else "",
                        style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
                    )
                }
                if (added) {
                    Text("Added", style = MaterialTheme.typography.labelMedium, color = GameColors.Gold, fontWeight = FontWeight.Bold)
                } else {
                    IconButton(onClick = { vm.addFromLibrary(ex) }) { Icon(Icons.Filled.Add, "Add ${ex.name}", tint = GameColors.Gold) }
                }
            }
        }
    }
}
