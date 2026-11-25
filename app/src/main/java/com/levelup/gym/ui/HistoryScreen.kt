package com.levelup.gym.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.levelup.gym.R
import com.levelup.gym.game.HistoryExport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.game.UnitsFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm")
private fun formatDate(millis: Long) = dateFmt.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** "Wed 25 Nov · 11:32 – 12:41 · 1h 9m" */
private fun whenText(start: Long, end: Long?): String =
    listOfNotNull(TimeText.day(start), TimeText.span(start, end), end?.let { TimeText.length(it - start) }).joinToString(" · ")

@Composable
fun HistoryScreen(vm: GymViewModel, onOpen: (Long) -> Unit) {
    val history by vm.history.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.exportHistory(it) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CHRONICLE", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                    Text("Workout history", style = MaterialTheme.typography.headlineMedium)
                }
                GameButton(
                    onClick = { export.launch(HistoryExport.fileName()) }, enabled = history.isNotEmpty(),
                    style = ButtonStyle.GOLD, height = 46.dp,
                ) {
                    MedievalIcon(R.drawable.ic_quest_scroll, 22.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Export")
                }
            }
        }
        if (history.isNotEmpty()) {
            item {
                ParchmentNote(
                    "Every quest is kept on this phone with its start and end time. Export saves all of it as a " +
                        "spreadsheet file (CSV) you can open in Google Sheets or Excel, or keep as a backup.",
                )
            }
        }
        if (history.isEmpty()) {
            item {
                GameCard {
                    Text(
                        "No quests completed yet. Finish a workout and it will be written here.",
                        color = GameColors.Muted,
                    )
                }
            }
        }
        items(history, key = { it.id }) { w ->
            val plan = vm.planDay(w.planDayId)
            GameCard(modifier = Modifier.clickable { onOpen(w.id) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plan?.questName ?: "Free workout", style = MaterialTheme.typography.titleMedium)
                            Text(whenText(w.startedAt, w.finishedAt), style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                        XpPill(w.xp)
                    }
                    Text(
                        buildList {
                            add(if (w.exerciseCount == 1) "1 exercise" else "${w.exerciseCount} exercises")
                            add(if (w.setCount == 1) "1 set" else "${w.setCount} sets")
                            if (w.volumeKg > 0) add(UnitsFormat.volume(w.volumeKg, units))
                            if (w.cardioSec > 0) add("${UnitsFormat.duration(w.cardioSec)} cardio")
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(vm: GymViewModel, workoutId: Long, onBack: () -> Unit) {
    val workout by remember(workoutId) { vm.workout(workoutId) }.collectAsStateWithLifecycle(null)
    val sets by remember(workoutId) { vm.setsFor(workoutId) }.collectAsStateWithLifecycle(emptyList())
    val units by vm.units.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var editTimes by remember { mutableStateOf(false) }
    val plan = vm.planDay(workout?.planDayId)

    Column {
        TopAppBar(
            title = {
                Column {
                    Text(plan?.questName ?: "Free workout", style = MaterialTheme.typography.titleMedium)
                    workout?.let { Text(TimeText.fullDay(it.startedAt), style = MaterialTheme.typography.bodySmall, color = GameColors.Muted) }
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete workout") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val bonus = workout?.bonusXp ?: 0
            workout?.let { w ->
                item {
                    GameCard(modifier = Modifier.clickable { editTimes = true }.semantics { contentDescription = "Edit start and end times" }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MedievalIcon(R.drawable.ic_timer_hourglass, 34.dp)
                            TimeStat("Start", TimeText.clock(w.startedAt), Modifier.weight(1f))
                            TimeStat("End", w.finishedAt?.let { TimeText.clock(it) } ?: "-", Modifier.weight(1f))
                            TimeStat("Time", w.finishedAt?.let { TimeText.length(it - w.startedAt) } ?: "-", Modifier.weight(1.2f))
                            Icon(Icons.Filled.Edit, null, tint = GameColors.Gold)
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    XpPill(sets.sumOf { it.set.xp } + bonus)
                }
            }
            sets.groupBy { it.exerciseName }.forEach { (name, list) ->
                item(key = name) {
                    GameCard {
                        Column {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            list.forEachIndexed { i, s ->
                                SetRow(i + 1, s, units)
                                if (i < list.lastIndex) HorizontalDivider(color = GameColors.Track)
                            }
                        }
                    }
                }
            }
            if (bonus > 0) {
                item {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Quest bonus", modifier = Modifier.weight(1f), color = GameColors.Muted)
                        XpPill(bonus)
                    }
                }
            }
        }
    }

    if (editTimes) {
        workout?.let { w ->
            DurationDialog(
                startedAt = w.startedAt, finishedAt = w.finishedAt ?: System.currentTimeMillis(),
                onDismiss = { editTimes = false },
                onSave = { start, end -> vm.updateWorkoutTimes(w.id, start, end ?: w.finishedAt); editTimes = false },
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            text = { Text("Its sets and XP will be removed from your hero.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteWorkout(workoutId); onBack() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TimeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
        OutlinedText(value, style = MaterialTheme.typography.titleLarge, outlineWidth = 3.dp, maxLines = 1)
    }
}
