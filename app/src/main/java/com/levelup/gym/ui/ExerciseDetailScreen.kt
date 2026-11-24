package com.levelup.gym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.game.ExerciseGuides
import com.levelup.gym.game.Skills
import com.levelup.gym.game.UnitsFormat

/** "How to" screen: start and finish photos, form cues and the most common mistake. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(vm: GymViewModel, exerciseId: Long, onBack: () -> Unit) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val units by vm.units.collectAsStateWithLifecycle()
    val exercise = exercises.firstOrNull { it.id == exerciseId }
    val guide = exercise?.let { ExerciseGuides.forName(it.name) }
    val best by produceState<Double?>(null, exerciseId) { value = vm.bestWeight(exerciseId) }

    Column {
        TopAppBar(
            title = { Text(exercise?.name ?: "") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        if (exercise != null) ExerciseDetailBody(exercise, guide, best, units)
    }
}

@Composable
private fun ExerciseDetailBody(
    exercise: com.levelup.gym.data.ExerciseEntity,
    guide: com.levelup.gym.game.ExerciseGuide?,
    best: Double?,
    units: com.levelup.gym.data.Units,
) {
        val skill = Skills.forGroup(exercise.muscleGroup)

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (guide != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ExerciseImage(guide.startImage, exercise.muscleGroup, Modifier.aspectRatio(0.8f))
                            Text("START", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ExerciseImage(guide.finishImage, exercise.muscleGroup, Modifier.aspectRatio(0.8f))
                            Text("FINISH", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkillIcon(exercise.muscleGroup, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Trains ${skill.name}", fontWeight = FontWeight.Bold)
                        Text(exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                    }
                    val b = best
                    if (exercise.kind == ExerciseKind.STRENGTH && b != null && b > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("YOUR BEST", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                            Text(UnitsFormat.weight(b, units), color = GameColors.Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (guide == null) {
                item {
                    GameCard {
                        Text(
                            "This is your own exercise, so there are no photos or form notes for it yet.",
                            color = GameColors.Muted,
                        )
                    }
                }
            } else {
                item {
                    GameCard(accent = GameColors.Xp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("How to do it", style = MaterialTheme.typography.titleMedium)
                            guide.cues.forEachIndexed { i, cue ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("${i + 1}", color = GameColors.Xp, fontWeight = FontWeight.Bold)
                                    Text(cue, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item {
                    GameCard(accent = GameColors.Ember) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Avoid", style = MaterialTheme.typography.titleMedium, color = GameColors.Ember)
                            Text(guide.avoid, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (guide.bodyweight) {
                    item {
                        Text(
                            "Bodyweight exercise: leave weight at 0, or enter only the extra weight you add.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.Muted,
                        )
                    }
                }
                item {
                    Text(
                        "Photos: Free Exercise DB (public domain).",
                        style = MaterialTheme.typography.bodySmall,
                        color = GameColors.Muted,
                    )
                }
            }
        }
}
