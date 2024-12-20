package com.github.se.orator.ui.offline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.symblAi.SpeakingRepository
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.overview.OfflineInterviewModule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class OfflineInterviewModuleTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var speakingViewModel: SpeakingViewModel
  private lateinit var speakingRepository: SpeakingRepository
  private lateinit var apiLinkViewModel: ApiLinkViewModel
  private lateinit var userProfileViewModel: UserProfileViewModel
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var mockOfflinePromptsFunctions: OfflinePromptsFunctionsInterface

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    speakingRepository = mock(SpeakingRepository::class.java)
    apiLinkViewModel = ApiLinkViewModel()
    userProfileRepository = mock(UserProfileRepository::class.java)
    userProfileViewModel = UserProfileViewModel(userProfileRepository)
    mockOfflinePromptsFunctions = mock(OfflinePromptsFunctionsInterface::class.java)

    speakingViewModel =
        SpeakingViewModel(speakingRepository, apiLinkViewModel, userProfileViewModel)

    // Mocking the response for getPromptMapElement
    `when`(mockOfflinePromptsFunctions.getPromptMapElement(anyString(), anyString(), any()))
        .thenReturn("Test Company")

    composeTestRule.setContent {
      OfflineInterviewModule(
          navigationActions = navigationActions,
          speakingViewModel = speakingViewModel,
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }
  }

  @Test
  fun testEverythingIsDisplayed() {
    composeTestRule.onNodeWithTag("company_field").assertIsDisplayed()
    composeTestRule.onNodeWithTag("job_field").assertIsDisplayed()
    composeTestRule.onNodeWithTag("question_field").assertIsDisplayed()
    composeTestRule.onNodeWithTag("doneButton").assertIsDisplayed()
    composeTestRule.onNodeWithText("What company are you applying to?").assertIsDisplayed()
    composeTestRule.onNodeWithText("What job are you applying to?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Go to recording screen").assertIsDisplayed()
  }

  @Test
  fun inputJobAndCompany() {
    // Input Company
    composeTestRule.onNodeWithTag("company_field", useUnmergedTree = true).performTextInput("Apple")
    composeTestRule
        .onNodeWithTag("company_field", useUnmergedTree = true)
        .assertTextContains("Apple")

    // Input Job Position
    composeTestRule.onNodeWithTag("job_field", useUnmergedTree = true).performTextInput("Engineer")
    composeTestRule
        .onNodeWithTag("job_field", useUnmergedTree = true)
        .assertTextContains("Engineer")

    // Input Question
    composeTestRule.onNodeWithTag("question_field").performTextInput("Focus on leadership skills")
    composeTestRule.onNodeWithTag("question_field").assertTextContains("Focus on leadership skills")
  }

  @Test
  fun testButtonFunctionality() {
    // Input necessary fields
    composeTestRule.onNodeWithTag("company_field").performTextInput("Apple")
    composeTestRule.onNodeWithTag("job_field").performTextInput("Engineer")
    composeTestRule.onNodeWithTag("question_field").performTextInput("Focus on leadership skills")

    // Click Done button
    composeTestRule.onNodeWithTag("doneButton").performClick()

    // Verify navigation to OfflineRecordingScreen with the correct question
    verify(navigationActions).goToOfflineRecording("Focus on leadership skills")
  }

  @Test
  fun testBackButton_navigation() {
    composeTestRule.onNodeWithTag("back_button").performClick()

    // Verify that navigation back was called
    verify(navigationActions).goBack()
  }
}
