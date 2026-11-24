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
import com.levelup.gym.ui.figure.AnimatedHeroFigure
import com.levelup.gym.ui.figure.ExerciseArt
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.FigurePalette
import com.levelup.gym.ui.figure.HeroFigure
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
            val art = ExerciseArts.forName(exercise.name)
            if (art != null) {
                item {
                    GameCard(accent = GameColors.Gold, ornate = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AnimatedHeroFigure(art, Modifier.fillMaxWidth().aspectRatio(1.15f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PosePanel("A", if (art.hold) "Hold" else "Start", art, 0f, Modifier.weight(1f))
                                if (!art.hold) PosePanel("B", "Finish", art, 1f, Modifier.weight(1f))
                            }
                            MuscleLegend(art)
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
                            "This is your own exercise, so there is no drawing or form notes for it yet.",
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
            }
        }
}

@Composable
private fun PosePanel(letter: String, label: String, art: ExerciseArt, t: Float, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GameColors.Background)
            .border(1.dp, GameColors.Gold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(GameColors.Xp.copy(alpha = 0.18f))
                    .border(1.dp, GameColors.Xp, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(letter, color = GameColors.Xp, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
        }
        HeroFigure(art, Modifier.fillMaxWidth().aspectRatio(1f), t = t)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleLegend(art: ExerciseArt) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        art.primary.forEach { MuscleChip(it.label, FigurePalette.musclePrimary) }
        art.secondary.forEach { MuscleChip(it.label, FigurePalette.muscleSecondary.copy(alpha = 0.8f)) }
    }
}

@Composable
private fun MuscleChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
