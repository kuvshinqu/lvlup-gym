package com.levelup.gym.ui

import com.levelup.gym.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun skillColor(muscleGroup: String): Color = when (muscleGroup) {
    "Chest" -> Color(0xFFF5C451)
    "Back" -> Color(0xFF64B5F6)
    "Legs" -> Color(0xFF7CF29C)
    "Shoulders" -> Color(0xFFB388FF)
    "Arms" -> Color(0xFFFF8A65)
    "Core" -> Color(0xFF4DD0E1)
    "Cardio" -> Color(0xFFFF6B8A)
    else -> Color(0xFFB0BEC5)
}

/** Drawable for a muscle group's skill, from the medieval icon set. */
@DrawableRes
fun skillIconRes(muscleGroup: String): Int = when (muscleGroup) {
    "Chest" -> R.drawable.ic_guard_shield
    "Back" -> R.drawable.ic_pull_bow
    "Legs" -> R.drawable.ic_stride_boot
    "Shoulders" -> R.drawable.ic_might_sword
    "Arms" -> R.drawable.ic_grip_hammer
    "Core" -> R.drawable.ic_balance_scales
    "Cardio" -> R.drawable.ic_stamina_flask
    else -> R.drawable.ic_xp_gem
}

/** The skill's medieval icon on a dark stone tile, rimmed in the skill's colour. No emoji. */
@Composable
fun SkillIcon(muscleGroup: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val color = skillColor(muscleGroup)
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2E2A26), Color(0xFF1C1A18))))
            .border(1.dp, color.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(skillIconRes(muscleGroup)), contentDescription = null, modifier = Modifier.size(size * 0.8f))
    }
}

/** Objective marker: an empty ring, or a filled ring with a check when done. */
@Composable
fun ObjectiveMark(done: Boolean, size: Dp = 26.dp) {
    val color = if (done) GameColors.Xp else GameColors.Muted
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2 - 2.dp.toPx()
        if (done) {
            drawCircle(color.copy(alpha = 0.2f), r)
            drawCircle(color, r, style = Stroke(2.dp.toPx()))
            val w = this.size.width
            val check = Path().apply {
                moveTo(w * 0.3f, w * 0.52f); lineTo(w * 0.45f, w * 0.66f); lineTo(w * 0.72f, w * 0.36f)
            }
            drawPath(check, color, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawCircle(color, r, style = Stroke(2.dp.toPx()))
        }
    }
}
