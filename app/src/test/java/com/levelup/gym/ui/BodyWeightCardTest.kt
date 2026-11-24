package com.levelup.gym.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.gym.data.BodyWeightEntity
import com.levelup.gym.data.Units
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BodyWeightCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsLatestAndTrend() {
        val a = BodyWeightEntity(1, 1, 81.2, 0)
        val b = BodyWeightEntity(2, 1, 80.6, 1000)
        compose.setContent { GymTheme { BodyWeightCard(b, listOf(a, b), Units.METRIC, onLog = {}) } }
        compose.onNodeWithText("80.6 kg").assertExists()
        compose.onNodeWithText("in 30 days", substring = true).assertExists()
    }

    @Test fun singleValue() {
        val a = BodyWeightEntity(1, 1, 81.2, 0)
        compose.setContent { GymTheme { BodyWeightCard(a, listOf(a), Units.METRIC, onLog = {}) } }
        compose.onNodeWithText("81.2 kg").assertExists()
    }
}
