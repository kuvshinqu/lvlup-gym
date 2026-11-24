package com.levelup.gym.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.HeroFigure

/** Small framed drawing of the hero doing the exercise (finish position). Falls back to the skill sigil. */
@Composable
fun ExerciseThumb(name: String, muscleGroup: String, modifier: Modifier = Modifier) {
    val art = ExerciseArts.forName(name)
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(GameColors.SurfaceHigh, GameColors.Background)))
            .border(1.dp, GameColors.Gold.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) HeroFigure(art, Modifier.fillMaxSize(), t = 1f, glow = 0.8f)
        else SkillIcon(muscleGroup)
    }
}
