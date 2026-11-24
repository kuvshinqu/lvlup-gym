package com.levelup.gym.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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

    /**
     * Robolectric can't settle after closing a dialog whose text field has focus,
     * so the dialog is checked once with a screenshot and the weigh-ins go through the view model.
     */
    private fun logBodyWeight(value: String) {
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[GymViewModel::class.java]
        compose.runOnUiThread { vm.logBodyWeight(value.toDouble()) }
    }

    @Test fun fullQuestFlow() {
        waitFor("Begin Upper A")
        shot("1_hero")

        compose.onNodeWithText("Begin Upper A", substring = true).performClick()
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

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Complete quest", substring = true))
        compose.onNodeWithText("Complete quest", substring = true).performClick()
        waitFor("Lower A")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        shot("4_hero_after")

        compose.onNodeWithText("Chronicle").performClick()
        waitFor("Siege of the Upper Keep")
        shot("5_history")

        compose.onNodeWithText("Quests").performClick()
        waitFor("Upper / Lower")
        shot("6_program")

        compose.onNodeWithText("Armory").performClick()
        waitFor("Add exercise")
        shot("7_exercises")

        // How-to screen
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Pull-up"))
        compose.onNodeWithText("Pull-up").performClick()
        waitFor("How to do it")
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        shot("8_how_to")
        compose.onNodeWithContentDescription("Back").performClick()

        // Body weight on the hero screen
        compose.onNodeWithText("Hero").performClick()
        waitFor("Log body weight")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Log body weight"))
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Log body weight").performClick()
        compose.mainClock.advanceTimeBy(500)
        shot("9a_weigh_in")
        compose.onNodeWithText("Cancel").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.mainClock.autoAdvance = true
        logBodyWeight("81.2")
        waitFor("81.2 kg")
        logBodyWeight("80.6")
        waitFor("in 30 days")
        compose.waitForIdle()
        shot("9b_body_weight")
    }
}
