package com.levelup.gym.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/** How times read across the app. */
object TimeText {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    private val day = DateTimeFormatter.ofPattern("EEE d MMM")
    private val fullDay = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
    private fun zoned(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    fun clock(ms: Long): String = clock.format(zoned(ms))

    /** "Today", "Yesterday" or "Mon 23 Nov". */
    fun day(ms: Long): String = dayOf(zoned(ms).toLocalDate())

    fun dayOf(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> if (date.year == today.year) day.format(date) else fullDay.format(date)
        }
    }

    /** "Wed 25 Nov 2026". */
    fun fullDay(ms: Long): String = fullDay.format(zoned(ms))

    /** The running clock: "04:09", or "1:04:09" past an hour. */
    fun elapsed(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }

    /** "01:09:23", as on the duration dialog. */
    fun total(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }

    /** "1h 9m" or "48 min". */
    fun length(ms: Long): String {
        val m = (ms / 60_000).coerceAtLeast(0)
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "$m min"
    }

    /** "11:32 – 12:41". */
    fun span(start: Long, end: Long?): String = clock(start) + " – " + (end?.let { clock(it) } ?: "now")
}

/**
 * Start and end of a workout, set like a phone's clock: pick Start or End, then spin the day, hour and minute.
 * While a workout is running only the start can move; the end is written when it is finished.
 */
@Composable
fun DurationDialog(
    startedAt: Long,
    finishedAt: Long?,
    onDismiss: () -> Unit,
    onSave: (start: Long, end: Long?) -> Unit,
) {
    val active = finishedAt == null
    var start by remember { mutableLongStateOf(startedAt) }
    var end by remember { mutableLongStateOf(finishedAt ?: System.currentTimeMillis()) }
    var editing by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()
    val shownEnd = if (active) now else end
    val error = when {
        start > now + 60_000 -> "The start can't be in the future."
        !active && end > now + 60_000 -> "The end can't be in the future."
        shownEnd <= start -> "The end has to be after the start."
        shownEnd - start > 24 * 3600_000L -> "A workout can't last more than 24 hours."
        else -> null
    }
    val shape = RoundedCornerShape(22.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GameColors.Panel,
        shape = shape,
        modifier = Modifier.border(3.dp, GameColors.Gold, shape),
        title = { OutlinedText("Duration", style = MaterialTheme.typography.headlineSmall, outlineWidth = 3.5.dp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedText(
                    TimeText.total(shownEnd - start), color = GameColors.Gold, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall, outlineWidth = 4.dp, fontSize = 38.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeTile("Start", start, editing == 0, true, { editing = 0 }, Modifier.weight(1f))
                    TimeTile("End", if (active) null else end, editing == 1, !active, { editing = 1 }, Modifier.weight(1f))
                }
                if (active) {
                    Text("The end is set when you finish the quest. You can change both later in the Chronicle.", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                }
                key(editing) {
                    DateTimeWheels(
                        millis = if (editing == 0) start else end,
                        onChange = { if (editing == 0) start = it else end = it },
                    )
                }
                error?.let { Text(it, color = GameColors.Ruby, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(start, if (active) null else end) }, enabled = error == null) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GameColors.Muted) } },
    )
}

@Composable
private fun TimeTile(label: String, millis: Long?, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .drawBehind {
                chunkyBlock(
                    if (selected) GameColors.PanelLight else GameColors.PanelDark, 12.dp.toPx(), outline = 2.dp.toPx(),
                    shadow = 3.dp.toPx(), lip = 3.dp.toPx(), rim = if (selected) GameColors.Gold else null, gloss = 0.08f,
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (selected) GameColors.Gold else GameColors.Muted)
        OutlinedText(millis?.let { TimeText.clock(it) } ?: "Now", style = MaterialTheme.typography.titleLarge, outlineWidth = 3.dp)
        Text(millis?.let { TimeText.day(it) } ?: "When you finish", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted, maxLines = 1)
    }
}

/** Day, hour and minute wheels. Seconds are kept from the original time. */
@Composable
private fun DateTimeWheels(millis: Long, onChange: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val initial = remember { Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime() }
    val days = remember {
        val today = LocalDate.now(zone)
        val first = minOf(initial.toLocalDate(), today).minusDays(30)
        val last = maxOf(initial.toLocalDate(), today)
        generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
    }
    var d by remember { mutableIntStateOf(days.indexOf(initial.toLocalDate()).coerceAtLeast(0)) }
    var h by remember { mutableIntStateOf(initial.hour) }
    var m by remember { mutableIntStateOf(initial.minute) }
    fun emit() = onChange(LocalDateTime.of(days[d], LocalTime.of(h, m, initial.second)).atZone(zone).toInstant().toEpochMilli())

    Column {
        Row {
            listOf("Day" to 1.8f, "Hour" to 1f, "Min" to 1f).forEach { (t, w) ->
                Text(t.uppercase(), Modifier.weight(w), style = MaterialTheme.typography.labelSmall, color = GameColors.Muted, textAlign = TextAlign.Center)
            }
        }
        Box {
            // the selection band behind all three wheels
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth().height(WHEEL_ROW)
                    .drawBehind { chunkyBlock(GameColors.PanelDark, 10.dp.toPx(), outline = 2.dp.toPx(), shadow = 0f, lip = 0f, rim = GameColors.Gold.copy(alpha = 0.7f), gloss = 0.05f) },
            )
            Row {
                WheelPicker(days.size, d, { TimeText.dayOf(days[it]) }, { d = it; emit() }, Modifier.weight(1.8f), "day")
                WheelPicker(24, h, { "%02d".format(it) }, { h = it; emit() }, Modifier.weight(1f), "hour")
                WheelPicker(60, m, { "%02d".format(it) }, { m = it; emit() }, Modifier.weight(1f), "minute")
            }
        }
    }
}

private val WHEEL_ROW: Dp = 40.dp

/** A phone-style spinning wheel: flick to scroll, it snaps to the middle row; tap a row to jump to it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    count: Int,
    initial: Int,
    label: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    name: String = "",
    rows: Int = 3,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial.coerceIn(0, (count - 1).coerceAtLeast(0)))
    val scope = rememberCoroutineScope()
    val rowPx = with(LocalDensity.current) { WHEEL_ROW.toPx() }
    val centre by remember { derivedStateOf { (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0).coerceIn(0, count - 1) } }
    val select by rememberUpdatedState(onSelected)
    LaunchedEffect(state) { snapshotFlow { centre }.collect { select(it) } }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = WHEEL_ROW * (rows / 2)),
        modifier = modifier.height(WHEEL_ROW * rows).semantics { contentDescription = "$name wheel" },
    ) {
        items(count) { i ->
            val selected = i == centre
            Box(
                Modifier.fillMaxWidth().height(WHEEL_ROW).clickable { scope.launch { state.animateScrollToItem(i) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(i), maxLines = 1,
                    style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = if (selected) Color.White else GameColors.Muted.copy(alpha = 0.6f),
                )
            }
        }
    }
}
