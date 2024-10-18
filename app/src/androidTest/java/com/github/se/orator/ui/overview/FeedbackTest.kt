package com.github.se.orator.ui.overview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testFeedbackScreenElementsAreDisplayed() {
    composeTestRule.setContent { FeedbackScreen() }

    // Check if the feedback screen elements are displayed
    composeTestRule.onNodeWithTag("feedbackTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feedbackSubtitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("strengthsTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("strengthsContent").assertIsDisplayed()
    composeTestRule.onNodeWithTag("improvementsTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("improvementsContent").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tipsTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tipsContent").assertIsDisplayed()
    composeTestRule.onNodeWithTag("retryButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("reviewButton").assertIsDisplayed()
  }

  @Test
  fun testRetryButtonClick() {
    composeTestRule.setContent { FeedbackScreen() }

    // Perform click on Retry button
    composeTestRule.onNodeWithTag("retryButton").performClick()

    // Test the click logic (for now just ensuring the button works)
  }

  @Test
  fun testReviewButtonClick() {
    composeTestRule.setContent { FeedbackScreen() }

    // Perform click on Review button
    composeTestRule.onNodeWithTag("reviewButton").performClick()

    // Test the click logic (for now just ensuring the button works)
  }
}
