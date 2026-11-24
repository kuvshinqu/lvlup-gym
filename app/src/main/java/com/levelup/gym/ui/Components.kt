package com.levelup.gym.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.levelup.gym.data.SetWithExercise
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.Units
import com.levelup.gym.game.UnitsFormat

@Composable
fun XpBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = GameColors.Xp,
    height: Dp = 12.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "xp")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(GameColors.Track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.65f), color)))
        )
    }
}

@Composable
fun LevelBadge(level: Int, size: Dp = 64.dp, color: Color = GameColors.Gold) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), GameColors.Surface)))
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LV", style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                "$level",
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Black,
                color = color,
            )
        }
    }
}

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (accent != null) it.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else it },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GameColors.SurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun XpPill(xp: Int, modifier: Modifier = Modifier) {
    Text(
        "+$xp XP",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(GameColors.Xp.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = GameColors.Xp,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

/** "8 × 60 kg" for strength, "30 min · 5 km" for cardio. */
fun describeSet(s: SetWithExercise, units: Units): String =
    if (s.exerciseKind == ExerciseKind.CARDIO) {
        buildList {
            if (s.set.durationSec > 0) add(UnitsFormat.duration(s.set.durationSec))
            if (s.set.distanceM > 0) add(UnitsFormat.distance(s.set.distanceM, units))
        }.joinToString(" · ").ifEmpty { "Cardio" }
    } else if (s.set.weightKg > 0) {
        "${s.set.reps} × ${UnitsFormat.weight(s.set.weightKg, units)}"
    } else {
        "${s.set.reps} reps"
    }

@Composable
fun SetRow(
    index: Int,
    set: SetWithExercise,
    units: Units,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "$index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(describeSet(set, units), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (set.set.isPersonalRecord) {
            Text("NEW BEST", style = MaterialTheme.typography.labelSmall, color = GameColors.Gold)
        }
        XpPill(set.set.xp)
        trailing()
    }
}
