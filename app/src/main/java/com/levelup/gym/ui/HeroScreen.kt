package com.levelup.gym.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun HeroScreen(vm: GymViewModel, onOpenWorkout: () -> Unit, onOpenSettings: () -> Unit) {
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HERO'S JOURNAL", style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, color = GameColors.Gold.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings", tint = GameColors.Muted) }
            }
        }

        // Hero card
        item {
            GameCard(accent = GameColors.Gold) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeroFigure(ExerciseArts.idle, Modifier.size(width = 92.dp, height = 120.dp), t = 0f)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(name, style = MaterialTheme.typography.headlineMedium)
                            Text(Leveling.title(progress.level).uppercase(), color = GameColors.Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Total ${progress.totalXp} XP", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                        LevelBadge(progress.level, size = 76.dp)
                    }
                    XpBar(progress.fraction, height = 14.dp)
                    Row {
                        Text(
                            "${progress.xpIntoLevel} / ${progress.xpNeededForNext} XP",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${progress.xpNeededForNext - progress.xpIntoLevel} XP to level ${progress.level + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.Muted,
                        )
                    }
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

        // Weekly quest
        item {
            GameCard(accent = GameColors.Arcane) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Weekly quest", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            "+${Leveling.WEEKLY_QUEST_XP} XP",
                            color = GameColors.Arcane,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        if (week >= Leveling.WEEKLY_QUEST_TARGET) "Complete! The realm is safe this week."
                        else "Finish ${Leveling.WEEKLY_QUEST_TARGET} workouts this week (Mon to Sun).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GameColors.Muted,
                    )
                    XpBar(week.toFloat() / Leveling.WEEKLY_QUEST_TARGET, color = GameColors.Arcane)
                    Text("${week.coerceAtMost(Leveling.WEEKLY_QUEST_TARGET)} / ${Leveling.WEEKLY_QUEST_TARGET}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Body weight
        item { BodyWeightCard(bodyWeight, bodyWeights, units, onLog = vm::logBodyWeight) }

        // Skills
        item { SectionLabel("Skills") }
        items(skills, key = { it.skill.muscleGroup }) { sp ->
            GameCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkillIcon(sp.skill.muscleGroup, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sp.skill.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("Lv ${sp.progress.level}", color = GameColors.Gold, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${sp.skill.muscleGroup} · ${sp.skill.flavor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.Muted,
                        )
                        XpBar(sp.progress.fraction, height = 8.dp)
                    }
                }
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
