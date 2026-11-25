package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.R
import com.levelup.gym.data.ExerciseEntity
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.SetEntity
import com.levelup.gym.data.SetWithExercise
import com.levelup.gym.data.Units
import com.levelup.gym.game.ExerciseGuides
import com.levelup.gym.game.Leveling
import com.levelup.gym.game.UnitsFormat
import com.levelup.gym.game.WorkoutDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private fun String.toNumber(): Double? = replace(',', '.').trim().toDoubleOrNull()

/** One exercise on the workout card: the sets already logged and how many are planned. */
private class ExerciseRows(
    val name: String,
    val exercise: ExerciseEntity?,
    val reps: String,
    val note: String,
    val logged: List<SetWithExercise>,
    target: Int,
) {
    val pending = (target - logged.size).coerceAtLeast(0)
    val total = logged.size + pending
    val complete = pending == 0 && logged.isNotEmpty()
    val cardio = exercise?.kind == ExerciseKind.CARDIO
    val guide = ExerciseGuides.forName(name)
    val bodyweight = guide?.bodyweight == true
    val timed = guide?.timed == true
}

private fun buildRows(draft: WorkoutDraft?, sets: List<SetWithExercise>, exercises: List<ExerciseEntity>, notes: Map<String, String>): List<ExerciseRows> {
    val byName = sets.groupBy { it.exerciseName }
    val planned = draft?.items.orEmpty().map { item ->
        ExerciseRows(item.exerciseName, exercises.firstOrNull { it.name == item.exerciseName }, item.reps, notes[item.exerciseName].orEmpty(), byName[item.exerciseName].orEmpty(), item.targetSets)
    }
    // Sets logged for something that isn't on the card (an older workout, or a removed row) still show.
    val extra = byName.filterKeys { name -> planned.none { it.name == name } }.map { (name, list) ->
        ExerciseRows(name, exercises.firstOrNull { it.name == name }, "", "", list, list.size)
    }
    return planned + extra
}

@Composable
fun WorkoutScreen(vm: GymViewModel, onClose: () -> Unit, onOpenExercise: (Long) -> Unit) {
    val workout by vm.activeWorkout.collectAsStateWithLifecycle()
    val sets by vm.activeSets.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()

    var pickerOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var editTimes by remember { mutableStateOf(false) }

    // Workout ended (finished or discarded): leave the screen.
    var hadWorkout by remember { mutableStateOf(false) }
    LaunchedEffect(workout) {
        if (workout != null) hadWorkout = true
        else if (hadWorkout) onClose()
    }

    val plan = vm.planDay(workout?.planDayId)
    val notes = remember(plan) { plan?.exercises.orEmpty().associate { it.exerciseName to it.note } }
    val rows = remember(draft, sets, exercises, notes) { buildRows(draft, sets, exercises, notes) }

    // What the hero typed into rows not logged yet, keyed "exercise#set#field".
    val edits = remember(workout?.id) { mutableStateMapOf<String, String>() }
    // The last set of each exercise from earlier workouts, to fill the rows in.
    val previous = remember(workout?.id) { mutableStateMapOf<Long, SetEntity?>() }
    LaunchedEffect(rows.mapNotNull { it.exercise?.id }) {
        rows.mapNotNull { it.exercise?.id }.filter { it !in previous }.forEach { previous[it] = vm.lastSet(it) }
    }
    val collapsed = remember(workout?.id) { mutableStateMapOf<String, Boolean>() }

    fun default(r: ExerciseRows, field: String): String {
        val last = r.logged.lastOrNull()?.set ?: r.exercise?.let { previous[it.id] }
        return when (field) {
            "reps" -> last?.reps?.takeIf { it > 0 }?.toString() ?: WorkoutDraft.lowerReps(r.reps)?.toString() ?: if (r.timed) "30" else "8"
            "weight" -> last?.weightKg?.takeIf { it > 0 }?.let { UnitsFormat.number(UnitsFormat.kgToDisplay(it, units)) } ?: ""
            "min" -> last?.durationSec?.takeIf { it > 0 }?.let { UnitsFormat.number(it / 60.0) } ?: "20"
            else -> last?.distanceM?.takeIf { it > 0 }?.let { UnitsFormat.number(UnitsFormat.metresToDisplay(it, units)) } ?: ""
        }
    }
    fun value(r: ExerciseRows, n: Int, field: String) = edits["${r.name}#$n#$field"] ?: default(r, field)

    // Guard against a double tap logging the same row twice before the database answers:
    // a tap waits until the last one shows up in the list (or two seconds pass).
    var waitingAt by remember { mutableStateOf(-1) }
    var waitingSince by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sets.size) { waitingAt = -1 }
    fun logRow(r: ExerciseRows, n: Int) {
        val ex = r.exercise ?: return
        val now = System.currentTimeMillis()
        if (waitingAt == sets.size && now - waitingSince < 2_000) return
        if (r.cardio) {
            val min = value(r, n, "min").toNumber() ?: 0.0
            if (min <= 0) return
            vm.logCardio(ex, (min * 60).toInt(), UnitsFormat.displayToMetres(value(r, n, "dist").toNumber() ?: 0.0, units))
            waitingAt = sets.size; waitingSince = now
        } else {
            val reps = value(r, n, "reps").toNumber()?.toInt() ?: 0
            if (reps <= 0) return
            // No weight is fine: bodyweight work, or a set the hero just wants ticked off.
            vm.logSet(ex, reps, UnitsFormat.displayToKg(value(r, n, "weight").toNumber() ?: 0.0, units))
            waitingAt = sets.size; waitingSince = now
        }
        edits.keys.filter { it.startsWith("${r.name}#$n#") }.forEach { edits.remove(it) }
    }

    val next = rows.firstOrNull { it.pending > 0 && it.exercise != null }
    val nextSet = next?.let { it.logged.size + 1 }
    val doneCount = rows.sumOf { it.logged.size }
    val totalCount = rows.sumOf { it.total }
    val pendingCount = rows.sumOf { it.pending }
    val sessionXp = sets.sumOf { it.set.xp }

    val listState = rememberLazyListState()
    var lastNext by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(next?.name) {
        val i = rows.indexOfFirst { it.name == next?.name }
        if (lastNext != null && i >= 0 && next?.name != lastNext) listState.animateScrollToItem(i)
        lastNext = next?.name
    }

    Column(Modifier.fillMaxSize()) {
        // Title bar
        Row(Modifier.fillMaxWidth().padding(end = 14.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                OutlinedText(plan?.name ?: "Free workout", style = MaterialTheme.typography.titleLarge, outlineWidth = 3.dp, maxLines = 1)
                Text(plan?.questName ?: "Your own quest", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted, maxLines = 1)
            }
            XpPill(sessionXp)
        }

        val w = workout
        if (w == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No quest in progress.")
                Button(onClick = { vm.startWorkout(null) }) { Text("Start a free workout") }
            }
            return@Column
        }

        TimerBar(w.startedAt, doneCount, totalCount, onEdit = { editTimes = true })

        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (rows.isEmpty()) {
                item {
                    ParchmentNote("A free quest. Add the exercises you're doing and tick each set off as you finish it.")
                }
            }
            items(rows, key = { it.name }) { r ->
                ExerciseCard(
                    r, units,
                    isNext = r === next,
                    expanded = collapsed[r.name] != true,
                    value = { n, f -> value(r, n, f) },
                    onEdit = { n, f, v -> edits["${r.name}#$n#$f"] = v },
                    onTick = { n -> logRow(r, n) },
                    onUntick = { vm.deleteSet(it.set.id) },
                    onToggle = { collapsed[r.name] = collapsed[r.name] != true },
                    onAddSet = { vm.addSetTo(r.name, r.logged.size) },
                    onRemoveSet = { vm.removeSetFrom(r.name, r.logged.size) },
                    onRemoveExercise = { vm.removeExerciseFromWorkout(r.name) },
                    onHowTo = { r.exercise?.let { onOpenExercise(it.id) } },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add exercise")
                    }
                    TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Abandon workout", color = GameColors.Ruby)
                    }
                }
            }
        }

        // Bottom bar: finish, and the big button that logs the next set.
        Row(
            Modifier.fillMaxWidth()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Color(0x000C1838), Color(0xF20C1838)), 0f, size.height * 0.35f))
                    drawRect(Color(0xF20C1838), Offset(0f, size.height * 0.35f), Size(size.width, size.height * 0.65f))
                }
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (next != null && nextSet != null) {
                GameButton(
                    onClick = { confirmFinish = true }, enabled = sets.isNotEmpty(), style = ButtonStyle.GREEN,
                    height = 62.dp, modifier = Modifier.width(96.dp),
                ) {
                    OutlinedText("Finish", style = MaterialTheme.typography.titleMedium, outlineWidth = 3.dp, fontSize = 17.sp)
                }
                val best by produceState<Pair<Double?, Int?>>(null to null, next.exercise?.id, sets.size) {
                    value = next.exercise?.let { vm.bestFor(it.id) } ?: (null to null)
                }
                val preview = if (next.cardio) {
                    val min = value(next, nextSet, "min").toNumber() ?: 0.0
                    Leveling.xpForCardio((min * 60).toInt(), UnitsFormat.displayToMetres(value(next, nextSet, "dist").toNumber() ?: 0.0, units))
                } else {
                    Leveling.scoreSet(
                        value(next, nextSet, "reps").toNumber()?.toInt() ?: 0,
                        UnitsFormat.displayToKg(value(next, nextSet, "weight").toNumber() ?: 0.0, units),
                        best.first, best.second, setsBeforeInWorkout = sets.size,
                    ).xp
                }
                GameButton(onClick = { logRow(next, nextSet) }, style = ButtonStyle.GOLD, height = 62.dp, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedText("LOG NEXT SET", style = MaterialTheme.typography.titleMedium, outlineWidth = 3.5.dp, fontSize = 19.sp)
                        Text(
                            "${next.name} · set $nextSet · +$preview XP", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = Color.White,
                        )
                    }
                }
            } else {
                RpgButton(
                    "Complete quest  ·  +${Leveling.FINISH_WORKOUT_XP} XP",
                    onClick = { vm.finishWorkout() },
                    enabled = sets.isNotEmpty(),
                    color = GameColors.Xp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (editTimes) {
        workout?.let { w ->
            DurationDialog(
                startedAt = w.startedAt, finishedAt = null,
                onDismiss = { editTimes = false },
                onSave = { start, _ -> vm.updateWorkoutTimes(w.id, start, null); editTimes = false },
            )
        }
    }
    if (pickerOpen) {
        ExercisePicker(
            exercises = exercises.filter { ex -> rows.none { it.name == ex.name } },
            onPick = { vm.addExerciseToWorkout(it); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            containerColor = GameColors.Panel,
            title = { Text("Finish the quest?") },
            text = {
                Text(
                    "$pendingCount ${if (pendingCount == 1) "set is" else "sets are"} not ticked off. " +
                        "You keep the XP from the $doneCount you did and get the finish bonus.",
                )
            },
            confirmButton = { Button(onClick = { confirmFinish = false; vm.finishWorkout() }) { Text("Finish quest") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep going", color = GameColors.Muted) } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = GameColors.Panel,
            title = { Text("Abandon this workout?") },
            text = { Text("Its sets and their XP will be removed.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discardWorkout() }) { Text("Abandon", color = GameColors.Ruby) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep going", color = GameColors.Muted) } },
        )
    }
}

/** The live clock: time since the start, which can be corrected, and how many sets are done. */
@Composable
private fun TimerBar(startedAt: Long, done: Int, total: Int, onEdit: () -> Unit) {
    // Ticks once a second. The wait runs off the main thread so it never holds up the UI.
    val now by produceState(System.currentTimeMillis()) {
        while (isActive) {
            value = System.currentTimeMillis()
            withContext(Dispatchers.Default) { delay(1000 - value % 1000) }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .drawBehind { chunkyBlock(GameColors.PanelDark, 14.dp.toPx(), outline = 2.5.dp.toPx(), shadow = 3.dp.toPx(), lip = 3.dp.toPx(), gloss = 0.06f) }
            .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable(onClick = onEdit).semantics { contentDescription = "Edit start and end times" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedievalIcon(R.drawable.ic_timer_hourglass, 34.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedText(TimeText.elapsed(now - startedAt), style = MaterialTheme.typography.headlineMedium, outlineWidth = 3.5.dp, fontSize = 28.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Edit, null, tint = GameColors.Gold, modifier = Modifier.size(18.dp))
                }
                Text("Started ${TimeText.clock(startedAt)}", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
            }
        }
        Column(Modifier.width(110.dp), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedText("$done/$total", style = MaterialTheme.typography.titleLarge, outlineWidth = 3.dp)
                Spacer(Modifier.width(4.dp))
                Text("SETS", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted, modifier = Modifier.padding(bottom = 4.dp))
            }
            XpBar(if (total == 0) 0f else done / total.toFloat(), height = 10.dp)
        }
    }
}

@Composable
private fun ExerciseCard(
    r: ExerciseRows,
    units: Units,
    isNext: Boolean,
    expanded: Boolean,
    value: (Int, String) -> String,
    onEdit: (Int, String, String) -> Unit,
    onTick: (Int) -> Unit,
    onUntick: (SetWithExercise) -> Unit,
    onToggle: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onRemoveExercise: () -> Unit,
    onHowTo: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    GameCard(accent = when { r.complete -> GameColors.Xp; isNext -> GameColors.Gold; else -> null }, ornate = false) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(r.name, r.exercise?.muscleGroup ?: "Other", Modifier.size(52.dp).clickable(onClick = onHowTo))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(
                        buildList {
                            add("${r.logged.size}/${r.total} done")
                            if (r.reps.isNotBlank()) add(if (r.timed || r.reps.endsWith("s")) r.reps else "${r.reps} reps")
                            if (r.note.isNotBlank()) add(r.note)
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (r.complete) GameColors.Xp else GameColors.Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                    Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (expanded) "Collapse ${r.name}" else "Expand ${r.name}", tint = GameColors.Muted)
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.MoreVert, "More for ${r.name}", tint = GameColors.Muted)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = GameColors.PanelDark) {
                        DropdownMenuItem(text = { Text("How to do it") }, onClick = { menu = false; onHowTo() })
                        if (r.pending > 0) DropdownMenuItem(text = { Text("Remove a set") }, onClick = { menu = false; onRemoveSet() })
                        if (r.logged.isEmpty()) DropdownMenuItem(text = { Text("Remove exercise") }, onClick = { menu = false; onRemoveExercise() })
                    }
                }
            }
            if (expanded) {
                r.logged.forEachIndexed { i, s -> SetLine(r, i + 1, units, done = s, next = false, value = value, onEdit = onEdit, onTick = { onUntick(s) }) }
                for (n in r.logged.size + 1..r.total) {
                    SetLine(r, n, units, done = null, next = isNext && n == r.logged.size + 1, value = value, onEdit = onEdit, onTick = { onTick(n) })
                }
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onAddSet).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, null, tint = GameColors.Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add a set", style = MaterialTheme.typography.labelLarge, color = GameColors.Gold)
                }
            }
        }
    }
}

/** One set: the tick circle, its number and the values. A done set turns green, with or without a weight. */
@Composable
private fun SetLine(
    r: ExerciseRows,
    n: Int,
    units: Units,
    done: SetWithExercise?,
    next: Boolean,
    value: (Int, String) -> String,
    onEdit: (Int, String, String) -> Unit,
    onTick: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .drawBehind {
                val cr = CornerRadius(12.dp.toPx())
                when {
                    done != null -> {
                        drawRoundRect(GameColors.Xp.copy(alpha = 0.22f), cornerRadius = cr)
                        drawRoundRect(GameColors.Xp.copy(alpha = 0.8f), cornerRadius = cr, style = Stroke(2.dp.toPx()))
                    }
                    next -> drawRoundRect(GameColors.Gold.copy(alpha = 0.9f), cornerRadius = cr, style = Stroke(2.dp.toPx()))
                    else -> drawRoundRect(Color.Black.copy(alpha = 0.16f), cornerRadius = cr)
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TickCircle(
            done = done != null, next = next, onClick = { focus.clearFocus(); onTick() },
            description = if (done != null) "Unmark ${r.name} set $n" else "Mark ${r.name} set $n done",
        )
        OutlinedText("$n", style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp, modifier = Modifier.width(16.dp))
        val tag = "${r.name}-$n"
        if (r.cardio) {
            val s = done?.set
            ValueBox(s?.let { UnitsFormat.number(it.durationSec / 60.0) } ?: value(n, "min"), "min", done != null, "min-$tag", { onEdit(n, "min", it) }, KeyboardType.Decimal)
            ValueBox(
                s?.let { if (it.distanceM > 0) UnitsFormat.number(UnitsFormat.metresToDisplay(it.distanceM, units)) else "—" } ?: value(n, "dist"),
                UnitsFormat.distanceUnit(units), done != null, "dist-$tag", { onEdit(n, "dist", it) }, KeyboardType.Decimal, placeholder = "—",
            )
        } else {
            val s = done?.set
            ValueBox(s?.reps?.toString() ?: value(n, "reps"), if (r.timed) "sec" else "reps", done != null, "reps-$tag", { onEdit(n, "reps", it) }, KeyboardType.Number, width = 50.dp)
            val weight = s?.let { if (it.weightKg > 0) UnitsFormat.number(UnitsFormat.kgToDisplay(it.weightKg, units)) else if (r.bodyweight) "BW" else "—" } ?: value(n, "weight")
            ValueBox(
                weight, (if (r.bodyweight) "+" else "") + UnitsFormat.weightUnit(units), done != null, "weight-$tag",
                { onEdit(n, "weight", it) }, KeyboardType.Decimal, placeholder = if (r.bodyweight) "BW" else "—",
            )
        }
        Spacer(Modifier.weight(1f))
        if (done != null) {
            // A new best shows the chalice in place of the gem.
            XpPill(
                done.set.xp, compact = true,
                icon = if (done.set.isPersonalRecord) R.drawable.ic_breakthrough_chalice else R.drawable.ic_xp_gem,
                modifier = if (done.set.isPersonalRecord) Modifier.semantics { contentDescription = "New best" } else Modifier,
            )
        }
    }
}

@Composable
private fun TickCircle(done: Boolean, next: Boolean, onClick: () -> Unit, description: String) {
    Canvas(
        Modifier.size(34.dp).clickable(onClick = onClick).semantics { contentDescription = description },
    ) {
        val r = size.minDimension / 2
        drawCircle(GameColors.Ink, r)
        if (done) {
            drawCircle(Brush.verticalGradient(listOf(Color(0xFF9BF06B), GameColors.Xp, Color(0xFF3E9A23))), r * 0.84f)
            drawCircle(Color.White.copy(alpha = 0.3f), r * 0.32f, center - Offset(r * 0.25f, r * 0.32f))
            val check = Path().apply {
                moveTo(center.x - r * 0.42f, center.y + r * 0.02f)
                lineTo(center.x - r * 0.1f, center.y + r * 0.34f)
                lineTo(center.x + r * 0.46f, center.y - r * 0.32f)
            }
            drawPath(check, GameColors.Ink, style = Stroke(r * 0.36f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(check, Color.White, style = Stroke(r * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawCircle(GameColors.PanelDark, r * 0.84f)
            drawCircle(if (next) GameColors.Gold else GameColors.Muted.copy(alpha = 0.55f), r * 0.66f, style = Stroke(r * 0.14f))
        }
    }
}

/** A number in a small dark slot. Editable until the set is done. */
@Composable
private fun ValueBox(
    value: String,
    unit: String,
    done: Boolean,
    tag: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType,
    placeholder: String = "",
    width: Dp = 58.dp,
) {
    val focus = LocalFocusManager.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(width).height(38.dp)
                .drawBehind {
                    chunkyBlock(
                        if (done) Color(0xFF2E7A2A) else Color(0xFF111E3D), 10.dp.toPx(),
                        outline = 2.dp.toPx(), shadow = 2.dp.toPx(), lip = 2.dp.toPx(), gloss = if (done) 0.12f else 0.04f,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val style = MaterialTheme.typography.titleMedium.copy(color = Color.White, textAlign = TextAlign.Center, shadow = null)
            if (done) {
                Text(value, style = style, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = { v -> onValue(v.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(GameColors.Gold),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 3.dp).testTag(tag),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (value.isEmpty()) Text(placeholder, style = style.copy(color = GameColors.Muted.copy(alpha = 0.6f)))
                            inner()
                        }
                    },
                )
            }
        }
        Spacer(Modifier.width(3.dp))
        Text(unit, style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
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
        containerColor = GameColors.Panel,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GameColors.Muted) } },
        title = { Text("Add exercise") },
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
