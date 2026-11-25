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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
        waitFor("LOG NEXT SET")
        compose.waitForIdle()
        shot("2a_workout_start")
        compose.onNodeWithTag("weight-Bench Press-1").performTextReplacement("60")
        compose.onNodeWithText("LOG NEXT SET").performClick()
        waitFor("1/4 done")
        // Set 2 is filled in from set 1.
        compose.onNodeWithText("LOG NEXT SET").performClick()
        waitFor("2/4 done")
        compose.onNodeWithTag("weight-Bench Press-3").performTextReplacement("62.5")
        compose.onNodeWithText("LOG NEXT SET").performClick()
        waitFor("LEVEL UP!")
        shot("2b_level_up")
        compose.onNodeWithText("Onward!").performClick()
        waitFor("3/4 done")
        compose.waitForIdle()
        shot("2_workout")

        // A set with no weight still ticks off and turns green.
        compose.onNodeWithContentDescription("Mark Barbell Row set 1 done").performScrollTo()
        compose.onNodeWithTag("weight-Barbell Row-1").performTextReplacement("")
        compose.onNodeWithContentDescription("Mark Barbell Row set 1 done").performClick()
        waitFor("1/4 done · 6-8")
        compose.onAllNodesWithText("Add a set")[0].performClick()
        waitFor("3/5 done")
        compose.onNodeWithContentDescription("Unmark Barbell Row set 1").performScrollTo()
        compose.waitForIdle()
        shot("3_workout_log")

        // Start and end times, set like a phone clock
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Edit start and end times").performClick()
        compose.mainClock.advanceTimeBy(600)
        shot("3b_duration")
        compose.onNodeWithText("Cancel").performClick()
        compose.mainClock.advanceTimeBy(600)

        compose.onNodeWithText("Finish").performClick()
        compose.mainClock.advanceTimeBy(600)
        shot("3c_finish_confirm")
        compose.onNodeWithText("Finish quest").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.mainClock.autoAdvance = true
        waitFor("Lower A")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        shot("4_hero_after")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("DAY STREAK"))
        compose.waitForIdle()
        shot("4a_hero_streak")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("See all"))
        waitFor("2 of 22 earned")
        compose.waitForIdle()
        shot("4a1_hero_badges")
        compose.onNodeWithText("See all").performClick()
        waitFor("Week of Iron")
        compose.waitForIdle()
        shot("4a2_badges")
        compose.onNodeWithContentDescription("Back").performClick()
        waitFor("See all")

        // Loot: the quest dropped an iron chest (it had a new best)
        compose.onNodeWithText("Treasury").performClick()
        waitFor("Chests")
        shot("4b_treasury")
        compose.onAllNodesWithText("Open")[0].performClick()
        compose.mainClock.advanceTimeBy(700)
        shot("4c0_chest_drop")
        waitFor("Claim")
        shot("4c_chest_open")
        compose.onNodeWithText("Claim").performClick()
        waitFor("Gear score")
        compose.waitForIdle()
        shot("4d_treasury_after")

        compose.onNodeWithText("Chronicle").performClick()
        waitFor("Siege of the Upper Keep")
        shot("5_history")
        compose.onNodeWithText("Siege of the Upper Keep").performClick()
        waitFor("Quest bonus")
        compose.waitForIdle()
        shot("5b_history_detail")
        compose.onNodeWithContentDescription("Back").performClick()
        waitFor("Export")

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
