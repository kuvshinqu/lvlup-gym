package com.levelup.gym.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.levelup.gym.R
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import com.levelup.gym.data.SetWithExercise
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.Units
import com.levelup.gym.game.ExerciseGuides
import com.levelup.gym.game.UnitsFormat

/** One of the full-colour medieval icons from res/drawable (ic_*), drawn as-is. */
@Composable
fun MedievalIcon(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier, description: String? = null) {
    androidx.compose.foundation.Image(painterResource(res), description, modifier.size(size))
}

/** "8 × 60 kg" for strength, "30 min · 5 km" for cardio. */
fun describeSet(s: SetWithExercise, units: Units): String =
    if (s.exerciseKind == ExerciseKind.CARDIO) {
        buildList {
            if (s.set.durationSec > 0) add(UnitsFormat.duration(s.set.durationSec))
            if (s.set.distanceM > 0) add(UnitsFormat.distance(s.set.distanceM, units))
        }.joinToString(" · ").ifEmpty { "Cardio" }
    } else {
        val guide = ExerciseGuides.forName(s.exerciseName)
        val amount = if (guide?.timed == true) "${s.set.reps} s" else "${s.set.reps}"
        when {
            s.set.weightKg > 0 && guide?.bodyweight == true -> "$amount × bodyweight + ${UnitsFormat.weight(s.set.weightKg, units)}"
            s.set.weightKg > 0 -> "$amount × ${UnitsFormat.weight(s.set.weightKg, units)}"
            guide?.bodyweight == true -> "$amount × bodyweight"
            else -> "$amount reps"
        }
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
            MedievalIcon(R.drawable.ic_breakthrough_chalice, 20.dp, description = "New best")
            Text("NEW BEST", style = MaterialTheme.typography.labelSmall, color = GameColors.Gold)
        }
        XpPill(set.set.xp)
        trailing()
    }
}
