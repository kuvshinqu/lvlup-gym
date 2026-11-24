package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.drawHero
import com.levelup.gym.ui.figure.fitFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders single large hero poses on black and on white. The pair lets a script recover a
 * transparent cut-out (difference matting) for store artwork.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h800dp-xhdpi")
class StoreHeroTest {
    @get:Rule val compose = createComposeRule()

    private val poses = listOf(
        "press" to (ExerciseArts.forName("Overhead Press")!! to 1f),
        "deadlift" to (ExerciseArts.forName("Deadlift")!! to 1f),
        "squat" to (ExerciseArts.forName("Squat")!! to 0.6f),
        "curl" to (ExerciseArts.forName("Bicep Curl")!! to 1f),
        "idle" to (ExerciseArts.idle to 0f),
    )

    @Test fun heroes() {
        var current by mutableStateOf(0)
        var bg by mutableStateOf(Color.Black)
        compose.setContent {
            val (art, t) = poses[current].second
            Canvas(Modifier.size(600.dp, 800.dp).background(bg)) {
                drawHero(art, t, fitFor(art, size.width, size.height, 24f), glow = 1f)
            }
        }
        poses.forEachIndexed { i, (name, _) ->
            listOf("k" to Color.Black, "w" to Color.White).forEach { (suffix, c) ->
                current = i; bg = c
                compose.waitForIdle()
                compose.onRoot().captureRoboImage("build/screenshots/hero_${name}_$suffix.png")
            }
        }
    }
}
