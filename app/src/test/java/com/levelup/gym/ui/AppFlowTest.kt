package com.levelup.gym.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.levelup.gym.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Drives the real app: start the first program day, log sets, finish, check history. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
    private fun shot(name: String) = captureScreenRoboImage("build/screenshots/$name.png")

    private fun waitFor(text: String, substring: Boolean = true) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    @Test fun fullQuestFlow() {
        waitFor("Start Upper A")
        shot("1_hero")

        compose.onNodeWithText("Start Upper A", substring = true).performClick()
        waitFor("Log set")
        compose.onNodeWithText("Weight (kg)").performTextReplacement("60")
        compose.onNodeWithText("Log set", substring = true).performClick()
        compose.onNodeWithText("Log set", substring = true).performClick()
        compose.onNodeWithText("Weight (kg)").performTextReplacement("62.5")
        compose.onNodeWithText("Log set", substring = true).performClick()
        waitFor("LEVEL UP!")
        shot("2b_level_up")
        compose.onNodeWithText("Onward!").performClick()
        waitFor("3/4")
        compose.waitForIdle()
        shot("2_workout")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("BATTLE LOG"))
        compose.waitForIdle()
        waitFor("NEW BEST")
        shot("3_workout_log")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Finish quest", substring = true))
        compose.onNodeWithText("Finish quest", substring = true).performClick()
        waitFor("Lower A")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        shot("4_hero_after")

        compose.onNodeWithText("History").performClick()
        waitFor("Siege of the Upper Keep")
        shot("5_history")

        compose.onNodeWithText("Program").performClick()
        waitFor("Upper / Lower")
        shot("6_program")

        compose.onNodeWithText("Exercises").performClick()
        waitFor("Add exercise")
        shot("7_exercises")
    }
}
