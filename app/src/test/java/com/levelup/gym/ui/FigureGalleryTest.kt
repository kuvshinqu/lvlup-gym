package com.levelup.gym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.levelup.gym.data.DEFAULT_EXERCISES
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.drawHero
import com.levelup.gym.ui.figure.fitFor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders every exercise's start and finish pose into contact sheets (build/screenshots/figures_N.png). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1000dp-mdpi")
class FigureGalleryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allExercisesHaveArt() {
        DEFAULT_EXERCISES.forEach { assertTrue("${it.name} has no art", ExerciseArts.forName(it.name) != null) }
    }

    @Test fun contactSheets() {
        val names = DEFAULT_EXERCISES.map { it.name }
        val pages = names.chunked(8)
        var page by mutableIntStateOf(0)
        compose.setContent {
            GymTheme {
                Column(Modifier.background(Color(0xFF0F1117))) {
                    pages[page].chunked(2).forEach { pair ->
                        Row {
                            pair.forEach { name ->
                                val art = ExerciseArts.forName(name)!!
                                Column(Modifier.width(400.dp)) {
                                    Text(name, color = Color.White, fontSize = 14.sp)
                                    Row {
                                        listOf(0f, 1f).forEach { t ->
                                            Canvas(Modifier.size(200.dp, 210.dp)) {
                                                drawHero(art, t, fitFor(art, size.width, size.height, 8f), trail = t == 1f)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        pages.indices.forEach { i ->
            page = i
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/screenshots/figures_$i.png")
        }
    }
}
