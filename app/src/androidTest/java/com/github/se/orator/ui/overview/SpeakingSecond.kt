package com.github.se.orator.ui.overview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.orator.ui.overview.SpeakingSecond
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeakingSecondTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSpeakingSecondScreenElementsAreDisplayed() {
    composeTestRule.setContent { SpeakingSecond() }
    // Check if the chat bubble is displayed
    composeTestRule.onNodeWithTag("chatBubbleMessage").assertIsDisplayed()

    // Check if the mic button is displayed and clickable
    composeTestRule.onNodeWithTag("micButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("micButton").performClick()

    // Check if the feedback button is displayed and clickable
    composeTestRule.onNodeWithTag("feedbackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feedbackButton").performClick()
  }
}
