package com.levelup.gym.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.R
import com.levelup.gym.game.Badge
import com.levelup.gym.game.BadgeKind
import com.levelup.gym.game.BadgeTier
import com.levelup.gym.game.Badges
import com.levelup.gym.game.Streak
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** The torch: lit while the streak lives, cold grey when there is none. */
@Composable
fun StreakTorch(lit: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.ic_streak_torch), if (lit) "Streak torch lit" else "Streak torch out",
        modifier.size(size).alpha(if (lit) 1f else 0.5f), colorFilter = if (lit) null else grey,
    )
}

/** Small counter for the top of the hero screen. */
@Composable
fun StreakPill(days: Int, lit: Boolean) {
    ResourcePill("$days") { StreakTorch(lit, 32.dp) }
}

@Composable
fun StreakCard(s: Streak.State, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val alive = s.days > 0
    GameCard(modifier, accent = if (s.atRisk) GameColors.Ruby else if (alive) GameColors.Ember else null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StreakTorch(alive && (s.trainedToday || !s.atRisk), 54.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedText("${s.days}", style = MaterialTheme.typography.displaySmall, color = if (alive) GameColors.Ember else Color.White, outlineWidth = 4.dp, fontSize = 36.sp)
                        Spacer(Modifier.width(6.dp))
                        OutlinedText("DAY STREAK", Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp)
                    }
                    Text(
                        if (s.best > s.days) "Best: ${s.best} days" else if (alive) "Your best yet" else "Rest up to ${s.restAllowed} days in a row",
                        style = MaterialTheme.typography.labelMedium, color = GameColors.Muted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedievalIcon(R.drawable.ic_guard_shield, 26.dp, description = "Streak shields")
                    Spacer(Modifier.width(2.dp))
                    OutlinedText("${s.shields}", style = MaterialTheme.typography.titleMedium, outlineWidth = 3.dp)
                }
            }

            // This week, Monday to Sunday
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { i ->
                    val day = monday.plusDays(i.toLong())
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day == today) GameColors.Gold else GameColors.Muted,
                        )
                        DayMark(if (day.isAfter(today)) null else s.marks[day], day == today)
                    }
                }
            }

            Text(statusLine(s, today), style = MaterialTheme.typography.bodyMedium, color = if (s.atRisk) Color(0xFFFFB4A8) else Color.White)

            s.nextMilestone?.let { goal ->
                val reward = Badges.byKey("streak_$goal")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Next: $goal days" + (reward?.let { " · ${it.tier.chest.label.lowercase()}" } ?: "") +
                            if (goal in Streak.SHIELD_MILESTONES) " + shield" else "",
                        style = MaterialTheme.typography.labelMedium, color = GameColors.Gold, modifier = Modifier.weight(1f),
                    )
                    Text("${s.days}/$goal", style = MaterialTheme.typography.labelMedium, color = GameColors.Muted)
                }
                XpBar(s.days / goal.toFloat(), color = GameColors.Ember, height = 10.dp)
            }
        }
    }
}

private fun statusLine(s: Streak.State, today: LocalDate): String = when {
    s.days == 0 && s.brokenOn != null && !s.brokenOn.isBefore(today.minusDays(7)) ->
        "Your ${s.brokenLength} day streak ended ${endedWhen(s.brokenOn)}. Finish a workout to light a new one."
    s.days == 0 -> "Finish a workout to light your streak. Rest days count, up to ${s.restAllowed} in a row."
    s.trainedToday -> "Trained today. The torch burns bright."
    s.atRisk && s.shields > 0 -> "Your rest days are used up. Train today, or a shield will guard the streak."
    s.atRisk -> "Your rest days are used up. Train today or the streak ends at midnight!"
    s.restDaysLeft == 0 -> "Rest day. Tomorrow the torch needs you back at the gym."
    else -> "Rest day. You can rest ${s.restDaysLeft} more ${if (s.restDaysLeft == 1) "day" else "days"} before you need to train."
}

private fun endedWhen(day: LocalDate): String = when (val d = TimeText.dayOf(day)) {
    "Today" -> "today"
    "Yesterday" -> "yesterday"
    else -> "on $d"
}

/** One day of the week: a flame for training, a moon for rest, a shield when one was used, a cross when missed. */
@Composable
private fun DayMark(mark: Streak.Mark?, today: Boolean) {
    Canvas(Modifier.size(34.dp)) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(GameColors.Ink, r)
        val fill = when (mark) {
            Streak.Mark.TRAINED -> GameColors.Ember
            Streak.Mark.SHIELDED -> GameColors.Sky
            Streak.Mark.MISSED -> GameColors.Ruby
            Streak.Mark.REST -> GameColors.PanelLight
            else -> GameColors.PanelDark
        }
        drawCircle(Brush.verticalGradient(listOf(lerp(fill, Color.White, 0.3f), fill, lerp(fill, Color.Black, 0.25f))), r * 0.84f)
        when (mark) {
            Streak.Mark.TRAINED -> flame(c, r * 0.62f)
            Streak.Mark.REST -> {
                drawCircle(Color(0xFFF6E7C1), r * 0.36f, c)
                drawCircle(GameColors.PanelLight, r * 0.32f, c + Offset(r * 0.18f, -r * 0.14f))
            }
            Streak.Mark.SHIELDED -> shield(c, r * 0.5f)
            Streak.Mark.MISSED -> {
                val d = r * 0.3f
                listOf(Color.Black.copy(alpha = 0.5f) to r * 0.22f, Color.White to r * 0.13f).forEach { (col, w) ->
                    drawLine(col, c - Offset(d, d), c + Offset(d, d), w, StrokeCap.Round)
                    drawLine(col, c + Offset(d, -d), c + Offset(-d, d), w, StrokeCap.Round)
                }
            }
            else -> {}
        }
        if (today) drawCircle(GameColors.Gold, r * 0.92f, style = Stroke(r * 0.14f))
    }
}

private fun DrawScope.flame(c: Offset, s: Float) {
    fun drop(scale: Float, dy: Float) = Path().apply {
        val k = s * scale
        moveTo(c.x, c.y - k + dy)
        cubicTo(c.x + k * 0.9f, c.y - k * 0.1f + dy, c.x + k * 0.75f, c.y + k * 0.85f + dy, c.x, c.y + k * 0.85f + dy)
        cubicTo(c.x - k * 0.75f, c.y + k * 0.85f + dy, c.x - k * 0.9f, c.y - k * 0.1f + dy, c.x, c.y - k + dy)
        close()
    }
    drawPath(drop(1f, 0f), GameColors.Ink, style = Stroke(s * 0.16f, join = StrokeJoin.Round))
    drawPath(drop(1f, 0f), Color(0xFFFFE07A))
    drawPath(drop(0.55f, s * 0.3f), Color(0xFFFF9B3D))
}

private fun DrawScope.shield(c: Offset, s: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y - s)
        lineTo(c.x + s * 0.85f, c.y - s * 0.6f)
        quadraticTo(c.x + s * 0.8f, c.y + s * 0.6f, c.x, c.y + s)
        quadraticTo(c.x - s * 0.8f, c.y + s * 0.6f, c.x - s * 0.85f, c.y - s * 0.6f)
        close()
    }
    drawPath(p, Color.White)
    drawPath(p, GameColors.Ink, style = Stroke(s * 0.18f, join = StrokeJoin.Round))
}

// ------------------------------------------------------------------ Badges

@DrawableRes
private fun iconFor(kind: BadgeKind): Int = when (kind) {
    BadgeKind.STREAK -> R.drawable.ic_streak_torch
    BadgeKind.WORKOUTS -> R.drawable.ic_mission_swords
    BadgeKind.RECORDS -> R.drawable.ic_breakthrough_chalice
    BadgeKind.LEVEL -> R.drawable.ic_level_crown
    BadgeKind.VOLUME -> R.drawable.ic_bodyweight_anvil
    BadgeKind.CARDIO -> R.drawable.ic_stride_boot
    BadgeKind.EARLY -> R.drawable.ic_timer_hourglass
    BadgeKind.LATE -> R.drawable.ic_camp_tent
}

private fun tierColor(t: BadgeTier) = when (t) {
    BadgeTier.BRONZE -> Color(0xFFD9893F)
    BadgeTier.SILVER -> Color(0xFFCAD5E6)
    BadgeTier.GOLD -> GameColors.Gold
}

/** A medal on a ribbon: tier-coloured rim, the deed's icon in the middle. Locked medals are grey. */
@Composable
fun BadgeMedal(badge: Badge, earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val rimColor = if (earned) tierColor(badge.tier) else Color(0xFF7C869C)
            val ribbon = if (earned) GameColors.Ruby else Color(0xFF555E73)
            // ribbon tails behind the disc
            listOf(-1f, 1f).forEach { side ->
                val p = Path().apply {
                    moveTo(w * 0.5f + side * w * 0.06f, w * 0.5f)
                    lineTo(w * 0.5f + side * w * 0.34f, w * 0.98f)
                    lineTo(w * 0.5f + side * w * 0.2f, w * 0.9f)
                    lineTo(w * 0.5f + side * w * 0.1f, w * 1.0f)
                    lineTo(w * 0.5f - side * w * 0.08f, w * 0.55f)
                    close()
                }
                drawPath(p, lerp(ribbon, Color.Black, if (side < 0) 0.1f else 0.3f))
                drawPath(p, GameColors.Ink, style = Stroke(w * 0.025f, join = StrokeJoin.Round))
            }
            val c = Offset(w * 0.5f, w * 0.44f)
            val r = w * 0.38f
            drawCircle(GameColors.Ink.copy(alpha = 0.5f), r * 1.02f, c + Offset(0f, w * 0.03f))
            drawCircle(GameColors.Ink, r, c)
            drawCircle(Brush.verticalGradient(listOf(lerp(rimColor, Color.White, 0.5f), rimColor, lerp(rimColor, Color.Black, 0.4f)), c.y - r, c.y + r), r * 0.9f, c)
            drawCircle(GameColors.Ink, r * 0.7f, c)
            drawCircle(
                Brush.radialGradient(listOf(if (earned) Color(0xFF4A73C8) else Color(0xFF4B5468), if (earned) Color(0xFF1C3576) else Color(0xFF262C3A)), center = c - Offset(r * 0.2f, r * 0.3f), radius = r),
                r * 0.62f, c,
            )
            drawCircle(Color.White.copy(alpha = 0.18f), r * 0.28f, c - Offset(r * 0.25f, r * 0.32f))
        }
        Image(
            painterResource(iconFor(badge.kind)), null,
            Modifier.padding(bottom = size * 0.12f).size(size * 0.42f).alpha(if (earned) 1f else 0.45f),
            colorFilter = if (earned) null else grey,
        )
    }
}

/** A row of medals on the hero screen. */
@Composable
fun BadgeShelf(badges: List<GymViewModel.BadgeProgress>, onSeeAll: () -> Unit) {
    val earned = badges.filter { it.earned }.sortedByDescending { it.earnedAt }
    val show = (earned + badges.filter { !it.earned }.sortedByDescending { it.value / it.badge.goal.toFloat() }).take(4)
    GameCard(modifier = Modifier.clickable(onClick = onSeeAll)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                show.forEach { b ->
                    Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BadgeMedal(b.badge, b.earned, 60.dp)
                        Text(b.badge.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center, color = if (b.earned) Color.White else GameColors.Muted)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${earned.size} of ${badges.size} earned", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted, modifier = Modifier.weight(1f))
                Text("See all", style = MaterialTheme.typography.labelLarge, color = GameColors.Gold)
            }
        }
    }
}

@Composable
fun BadgesScreen(vm: GymViewModel, onBack: () -> Unit) {
    val badges by vm.badges.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            OutlinedText("Badges", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            OutlinedText("${badges.count { it.earned }}/${badges.size}", style = MaterialTheme.typography.titleLarge, color = GameColors.Gold, outlineWidth = 3.dp)
        }
        LazyVerticalGrid(
            GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { StreakCard(streak) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ParchmentNote("Every badge comes with a chest: bronze gives a wooden chest, silver an iron one, gold a golden one. Streak badges at 7, 30 and 100 days also give a shield.")
            }
            items(badges, key = { it.badge.key }) { b -> BadgeTile(b) }
        }
    }
}

@Composable
private fun BadgeTile(b: GymViewModel.BadgeProgress) {
    Column(
        Modifier.fillMaxWidth()
            .drawBehind { chunkyBlock(if (b.earned) GameColors.Panel else GameColors.PanelDark, 14.dp.toPx(), shadow = 3.dp.toPx(), lip = 3.dp.toPx(), gloss = 0.07f, rim = if (b.earned) tierColor(b.badge.tier).copy(alpha = 0.8f) else null) }
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BadgeMedal(b.badge, b.earned, 64.dp)
        OutlinedText(b.badge.name, style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp, textAlign = TextAlign.Center, maxLines = 2, fontSize = 14.sp)
        Text(b.badge.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp), color = GameColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.height(42.dp))
        if (b.earned) {
            Text("${b.badge.tier.label} · earned", style = MaterialTheme.typography.labelSmall, color = tierColor(b.badge.tier))
        } else {
            XpBar(b.value / b.badge.goal.toFloat(), color = GameColors.Gold, height = 8.dp)
            Text("${b.value.coerceAtMost(b.badge.goal)}/${b.badge.goal}", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
        }
    }
}
