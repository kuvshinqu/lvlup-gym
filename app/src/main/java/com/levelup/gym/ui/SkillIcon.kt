package com.levelup.gym.ui

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

/** A round sigil with a simple line drawing for each skill. No emoji. */
@Composable
fun SkillIcon(muscleGroup: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val color = skillColor(muscleGroup)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.5.dp, color.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size).padding(size * 0.22f)) { drawSigil(muscleGroup, color) }
    }
}

private fun DrawScope.drawSigil(group: String, color: Color) {
    val u = size.minDimension / 24f // draw on a 24x24 grid
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    val stroke = Stroke(width = 2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun path(vararg pts: Pair<Float, Float>, close: Boolean = false) = Path().apply {
        pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        if (close) close()
    }
    when (group) {
        "Chest" -> { // shield
            val shield = Path().apply {
                moveTo(12 * u, 2 * u); lineTo(21 * u, 5 * u); lineTo(20 * u, 13 * u)
                quadraticTo(18 * u, 19 * u, 12 * u, 22 * u)
                quadraticTo(6 * u, 19 * u, 4 * u, 13 * u); lineTo(3 * u, 5 * u); close()
            }
            drawPath(shield, color.copy(alpha = 0.25f))
            drawPath(shield, color, style = stroke)
            drawLine(color, p(12f, 6f), p(12f, 18f), stroke.width, StrokeCap.Round)
        }
        "Back" -> { // bow and arrow
            drawArc(color, 90f, 180f, false, p(4f, 2f), Size(14 * u, 20 * u), style = stroke)
            drawLine(color, p(11f, 2f), p(11f, 22f), 1.4f * u)
            drawLine(color, p(5f, 12f), p(22f, 12f), stroke.width, StrokeCap.Round)
            drawPath(path(18f to 8.5f, 22f to 12f, 18f to 15.5f), color, style = stroke)
        }
        "Legs" -> { // mountains
            drawPath(path(1f to 21f, 9f to 6f, 14f to 15f, 17f to 10f, 23f to 21f, close = true), color.copy(alpha = 0.25f))
            drawPath(path(1f to 21f, 9f to 6f, 14f to 15f, 17f to 10f, 23f to 21f, close = true), color, style = stroke)
        }
        "Shoulders" -> { // lightning bolt
            val bolt = path(14f to 1f, 4f to 14f, 11f to 14f, 9f to 23f, 20f to 9f, 13f to 9f, close = true)
            drawPath(bolt, color.copy(alpha = 0.25f))
            drawPath(bolt, color, style = stroke)
        }
        "Arms" -> { // dumbbell
            drawLine(color, p(6f, 12f), p(18f, 12f), stroke.width, StrokeCap.Round)
            drawRoundRect(color, p(2f, 6f), Size(4 * u, 12 * u), androidx.compose.ui.geometry.CornerRadius(u, u))
            drawRoundRect(color, p(18f, 6f), Size(4 * u, 12 * u), androidx.compose.ui.geometry.CornerRadius(u, u))
        }
        "Core" -> { // hexagon stone
            val hex = path(12f to 2f, 21f to 7f, 21f to 17f, 12f to 22f, 3f to 17f, 3f to 7f, close = true)
            drawPath(hex, color.copy(alpha = 0.25f))
            drawPath(hex, color, style = stroke)
            drawPath(path(3f to 7f, 12f to 12f, 21f to 7f), color, style = stroke)
            drawLine(color, p(12f, 12f), p(12f, 22f), stroke.width, StrokeCap.Round)
        }
        "Cardio" -> { // heart
            val heart = Path().apply {
                moveTo(12 * u, 21 * u)
                cubicTo(2 * u, 14 * u, 1 * u, 7 * u, 6.5f * u, 4 * u)
                cubicTo(9.5f * u, 2.5f * u, 12 * u, 5 * u, 12 * u, 7 * u)
                cubicTo(12 * u, 5 * u, 14.5f * u, 2.5f * u, 17.5f * u, 4 * u)
                cubicTo(23 * u, 7 * u, 22 * u, 14 * u, 12 * u, 21 * u)
                close()
            }
            drawPath(heart, color.copy(alpha = 0.25f))
            drawPath(heart, color, style = stroke)
        }
        else -> { // four-point star
            val star = path(12f to 2f, 14.5f to 9.5f, 22f to 12f, 14.5f to 14.5f, 12f to 22f, 9.5f to 14.5f, 2f to 12f, 9.5f to 9.5f, close = true)
            drawPath(star, color.copy(alpha = 0.25f))
            drawPath(star, color, style = stroke)
        }
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
