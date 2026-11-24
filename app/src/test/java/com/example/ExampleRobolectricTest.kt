package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Albie's Chess", appName)
    }

    @Test
    fun `main activity launches and supports continuous guess mode puzzles studio and history`() {
        composeRule.onNodeWithTag("opening_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("chess_board").assertIsDisplayed()
        composeRule.onNodeWithTag("evaluation_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("start_guess_the_move_button").performScrollTo().assertIsDisplayed()

        // Tap "Start Guess the Move" -> enters continuous mode with Waiting banner & Stop button
        composeRule.onNodeWithTag("start_guess_the_move_button").performClick()
        composeRule.onNodeWithTag("guess_waiting_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("stop_guess_the_move_button").performScrollTo().assertIsDisplayed()

        // Tap "Stop Guess the Move" -> seamlessly returns to normal play
        composeRule.onNodeWithTag("stop_guess_the_move_button").performClick()
        composeRule.onNodeWithTag("start_guess_the_move_button").performScrollTo().assertIsDisplayed()

        // Navigate to Train (Historical Tactics & Strategy Puzzles) tab
        composeRule.onNodeWithTag("nav_puzzles").performClick()
        composeRule.onNodeWithTag("tab_tactics").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_strategy").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_strategy").performClick()

        // Navigate to History tab
        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.onNodeWithTag("tab_paused_games").assertIsDisplayed()

        // Navigate to Studio tab -> verify Arrow Opacity slider, Player Name editor & Board Themes
        composeRule.onNodeWithTag("nav_studio").performClick()
        composeRule.onNodeWithTag("studio_list").performScrollToNode(hasTestTag("arrow_opacity_slider"))
        composeRule.onNodeWithTag("arrow_opacity_slider").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_list").performScrollToNode(hasTestTag("studio_player_name_input"))
        composeRule.onNodeWithTag("studio_player_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_list").performScrollToNode(hasTestTag("board_theme_tournament_walnut"))
        composeRule.onNodeWithTag("board_theme_tournament_walnut").assertIsDisplayed()
    }
}
