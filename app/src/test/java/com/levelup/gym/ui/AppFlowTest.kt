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
@Config(qualifiers = "w411dp-h822dp-xxhdpi")
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
        // First launch opens the Hero's Trial
        waitFor("Trial 1 of 5")
        shot("0a_trial")
        for (answer in listOf("Build muscle", "4 days", "A full gym", "6 months to 2 years")) {
            compose.onNodeWithText(answer).performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("About 60 min").performClick()
        waitFor("Accept the quest")
        shot("0b_trial_result")
        compose.onNodeWithText("Accept the quest").performClick()
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

        compose.onNodeWithText("Change quest line").performClick()
        waitFor("Choose a program")
        shot("6b_programs")
        compose.onNodeWithText("Forge your own").performClick()
        waitFor("Save and follow")
        // Robolectric never idles while a dialog is open, so step the clock by hand.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Add exercise").performClick()
        compose.mainClock.advanceTimeBy(500)
        shot("6c_pick_exercise")
        compose.onNodeWithText("Arnold Press").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.mainClock.autoAdvance = true
        waitFor("3 × 8-12")
        shot("6d_builder")
        compose.onNodeWithContentDescription("Back").performClick()
        waitFor("Choose a program")
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithText("Armory").performClick()
        waitFor("Add from the library")
        shot("7_exercises")
        compose.onNodeWithText("Add from the library").performClick()
        waitFor("Exercise library")
        compose.onNodeWithText("Dumbbell").performClick()
        compose.waitForIdle()
        shot("7b_library")
        compose.onNodeWithContentDescription("Back").performClick()
        waitFor("Add from the library")

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
