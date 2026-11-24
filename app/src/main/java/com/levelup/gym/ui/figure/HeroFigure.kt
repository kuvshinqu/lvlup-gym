package com.levelup.gym.ui.figure

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/** A still of the hero at [t] (0 = start, 1 = finish). */
@Composable
fun HeroFigure(art: ExerciseArt, modifier: Modifier = Modifier, t: Float = 1f, trail: Boolean = false, glow: Float = 0.7f) {
    Canvas(modifier) {
        drawHero(art, t, fitFor(art, size.width, size.height, size.minDimension * 0.04f), glow = glow, trail = trail)
    }
}

/** The hero moving from start to finish and back, pausing at each end, with glowing muscles. */
@Composable
fun AnimatedHeroFigure(art: ExerciseArt, modifier: Modifier = Modifier, trail: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "hero")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                0f at 0 using FastOutSlowInEasing
                0f at 500 using FastOutSlowInEasing
                1f at 1700 using LinearEasing
                1f at 2300 using FastOutSlowInEasing
                0f at 3600
            }
        ),
        label = "t",
    )
    val glow by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )
    Canvas(modifier) {
        drawHero(art, t, fitFor(art, size.width, size.height, size.minDimension * 0.04f), glow = glow, trail = trail)
    }
}
