package com.github.se.orator.ui.overview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.orator.ui.overview.SpeakingStart
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeakingStartTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSpeakingStartScreenElementsAreDisplayed() {
    composeTestRule.setContent { SpeakingStart() }
    // Check if the title is displayed
    composeTestRule.onNodeWithTag("titleText").assertIsDisplayed()

    // Check if the inputs are displayed
    composeTestRule.onNodeWithTag("levelInput").assertIsDisplayed()
    composeTestRule.onNodeWithTag("jobInput").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeInput").assertIsDisplayed()
    composeTestRule.onNodeWithTag("experienceInput").assertIsDisplayed()

    // Check if the Get Started button is displayed
    composeTestRule.onNodeWithTag("getStartedButton").assertIsDisplayed()
  }

  @Test
  fun testGetStartedButtonClick() {
    composeTestRule.setContent { SpeakingStart() }
    // Click the Get Started button
    composeTestRule.onNodeWithTag("getStartedButton").performClick()

    // Since no action is performed on click yet, we can assert it was clicked successfully.
    // Further logic for testing the navigation or state change would go here.
  }
}
