package com.levelup.gym.ui

import com.levelup.gym.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.game.Leveling
import com.levelup.gym.game.UnitsFormat
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.HeroFigure
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind

@Composable
fun HeroScreen(vm: GymViewModel, onOpenWorkout: () -> Unit, onOpenSettings: () -> Unit, onOpenBadges: () -> Unit = {}) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val name by vm.heroName.collectAsStateWithLifecycle()
    val skills by vm.skills.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val week by vm.workoutsThisWeek.collectAsStateWithLifecycle()
    val active by vm.activeWorkout.collectAsStateWithLifecycle()
    val next by vm.nextPlanDay.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val bodyWeight by vm.latestBodyWeight.collectAsStateWithLifecycle()
    val bodyWeights by vm.bodyWeights30d.collectAsStateWithLifecycle()
    val treasury by vm.treasury.collectAsStateWithLifecycle()
    val look by vm.heroLook.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreakPill(streak.days, streak.days > 0)
                ResourcePill("${treasury.gold}") { Coin(32.dp) }
                if (treasury.unopened.isNotEmpty()) {
                    ResourcePill("${treasury.unopened.size}") {
                        androidx.compose.foundation.Canvas(Modifier.size(34.dp, 30.dp)) { drawChest(com.levelup.gym.game.ChestTier.valueOf(treasury.unopened.first().tier)) }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) { Icon(painterResource(R.drawable.ic_settings_key), "Settings", tint = Color.Unspecified, modifier = Modifier.size(30.dp)) }
            }
        }

        // Hero card
        item {
            GameCard(accent = GameColors.Gold) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeroFigure(ExerciseArts.idle, Modifier.size(width = 96.dp, height = 132.dp), t = 0f, look = look)
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedText(name, style = MaterialTheme.typography.headlineMedium, outlineWidth = 4.dp, maxLines = 1)
                            Text(Leveling.title(progress.level).uppercase(), color = GameColors.Gold, style = MaterialTheme.typography.labelSmall)
                            Text("${progress.totalXp} XP earned", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                        LevelBadge(progress.level, size = 78.dp)
                    }
                    XpBar(progress.fraction, label = "${progress.xpIntoLevel} / ${progress.xpNeededForNext}")
                    Text(
                        "${progress.xpNeededForNext - progress.xpIntoLevel} XP to level ${progress.level + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GameColors.Muted,
                    )
                }
            }
        }

        // Main action
        item {
            if (active != null) {
                RpgButton("Continue your quest", onClick = onOpenWorkout, modifier = Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RpgButton(
                        "Begin ${next.name}: ${next.questName}",
                        onClick = { vm.startWorkout(next.id); onOpenWorkout() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { vm.startWorkout(null); onOpenWorkout() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Free workout") }
                }
            }
        }

        // Streak
        item { StreakCard(streak) }

        // Weekly quest
        item {
            GameCard(accent = GameColors.Arcane) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedievalIcon(R.drawable.ic_quest_scroll, 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            OutlinedText("Weekly quest", style = MaterialTheme.typography.titleMedium, outlineWidth = 3.dp)
                            Text("Reward: golden chest + ${Leveling.WEEKLY_QUEST_XP} XP", color = GameColors.Gold, style = MaterialTheme.typography.labelMedium)
                        }
                        androidx.compose.foundation.Canvas(Modifier.size(58.dp, 48.dp)) { drawChest(com.levelup.gym.game.ChestTier.GOLDEN) }
                    }
                    Text(
                        if (week >= Leveling.WEEKLY_QUEST_TARGET) "Complete! The realm is safe this week."
                        else "Finish ${Leveling.WEEKLY_QUEST_TARGET} workouts this week (Mon to Sun).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GameColors.Muted,
                    )
                    XpBar(week.toFloat() / Leveling.WEEKLY_QUEST_TARGET, color = GameColors.Arcane, label = "${week.coerceAtMost(Leveling.WEEKLY_QUEST_TARGET)} / ${Leveling.WEEKLY_QUEST_TARGET}")
                }
            }
        }

        // Badges
        item { SectionLabel("Badges") }
        item { BadgeShelf(badges, onSeeAll = onOpenBadges) }

        // Body weight
        item { BodyWeightCard(bodyWeight, bodyWeights, units, onLog = vm::logBodyWeight) }

        // Skills
        item { SectionLabel("Skills") }
        items(skills.chunked(2), key = { row -> row.joinToString { it.skill.muscleGroup } }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { sp ->
                    Column(
                        Modifier.weight(1f)
                            .drawBehind { chunkyBlock(GameColors.Panel, 14.dp.toPx(), shadow = 4.dp.toPx(), lip = 4.dp.toPx(), gloss = 0.07f) }
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkillIcon(sp.skill.muscleGroup, size = 38.dp)
                            Spacer(Modifier.weight(1f))
                            LevelBadge(sp.progress.level, size = 38.dp)
                        }
                        OutlinedText(sp.skill.name, style = MaterialTheme.typography.titleSmall, outlineWidth = 2.5.dp, maxLines = 1)
                        Text(sp.skill.muscleGroup, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        XpBar(sp.progress.fraction, height = 10.dp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // Lifetime stats
        item { SectionLabel("Deeds so far") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("${stats.workoutCount}", "Workouts", Modifier.weight(1f))
                StatBox("${stats.setCount}", "Sets", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(UnitsFormat.volume(stats.volumeKg, units), "Lifted", Modifier.weight(1f))
                StatBox(UnitsFormat.duration(stats.cardioSec), "Cardio", Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.padding(8.dp)) }
    }
}
