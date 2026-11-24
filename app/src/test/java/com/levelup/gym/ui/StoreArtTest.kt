package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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

/** Renders large hero figures used to compose the Play Store feature graphic. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w900dp-h400dp-mdpi")
class StoreArtTest {
    @get:Rule val compose = createComposeRule()

    @Test fun featureFigures() {
        val figures = listOf(
            ExerciseArts.forName("Squat")!! to 1f,
            ExerciseArts.idle to 0f,
            ExerciseArts.forName("Overhead Press")!! to 1f,
        )
        compose.setContent {
            Row(Modifier.background(Color(0xFF0F1117))) {
                figures.forEach { (art, t) ->
                    Canvas(Modifier.size(300.dp, 400.dp)) {
                        drawHero(art, t, fitFor(art, size.width, size.height, 10f), glow = 1f)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/store_figures.png")
    }
}
