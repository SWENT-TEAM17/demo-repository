package com.github.se.orator.ui.offline

import android.Manifest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.GrantPermissionRule
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.symblAi.SpeakingRepository
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class OfflineRecordingScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

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
  }

  @Test
  fun testEverythingIsDisplayed() {
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }
    composeTestRule.onNodeWithTag("OfflineRecordingScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("BackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("BackButtonRow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("RecordingColumn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("MicIconContainer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("DoneButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mic_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("targetCompany").assertIsDisplayed()
    composeTestRule.onNodeWithTag("QuestionText").assertIsDisplayed()
  }

  @Test
  fun testMicButton_startAndStopRecording() {
    // Recompose with permission granted
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }

    // Click mic button to start recording
    composeTestRule.onNodeWithTag("mic_button").performClick()

    // Verify startRecording was called
    verify(speakingRepository).startRecording(any())

    // Click mic button to stop recording
    composeTestRule.onNodeWithTag("mic_button").performClick()

    // Verify stopRecording was called
    verify(speakingRepository).stopRecording()
  }

  @Test
  fun testDoneButton_enabledAfterRecording() {
    // Recompose with permission granted and recording finished
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }

    // Start recording
    composeTestRule.onNodeWithTag("mic_button").performClick()
    // Stop recording
    composeTestRule.onNodeWithTag("mic_button").performClick()

    // Click Done button
    composeTestRule.onNodeWithTag("DoneButton").performClick()

    // Verify navigation to review screen
    verify(navigationActions).navigateTo(Screen.OFFLINE_RECORDING_REVIEW_SCREEN)
  }

  @Test
  fun testDoneButton_disabledDuringRecording() {
    // Recompose with permission granted and recording in progress
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }

    // Start recording
    composeTestRule.onNodeWithTag("mic_button").performClick()

    // Attempt to click "Done" while recording
    composeTestRule.onNodeWithTag("DoneButton").performClick()

    // Verify that navigation does not occur
    verify(navigationActions, never()).navigateTo(Screen.OFFLINE_RECORDING_REVIEW_SCREEN)
  }

  @Test
  fun testNavigationOnDone() {
    // Recompose with permission granted and recording completed
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }

    // Start and stop recording
    composeTestRule.onNodeWithTag("mic_button").performClick()
    composeTestRule.onNodeWithTag("mic_button").performClick()

    // Click Done button
    composeTestRule.onNodeWithTag("DoneButton").performClick()

    // Verify navigation to review screen
    verify(navigationActions).navigateTo(Screen.OFFLINE_RECORDING_REVIEW_SCREEN)
  }

  @Test
  fun testRecordingFeedbackMessage_displayed() {
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }
    composeTestRule.onNodeWithTag("mic_text").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Tap once to record, tap again to stop returning.")
        .assertIsDisplayed()
  }

  @Test
  fun testQuestionText_displayedCorrectly() {
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }
    composeTestRule
        .onNodeWithTag("QuestionText")
        .assertTextContains("Make sure to focus on: What are your greatest strengths?")
  }
  //
  //  @Test
  //  fun testTargetCompany_displayedCorrectly() {
  //    composeTestRule.setContent {
  //      OfflineRecordingScreen(
  //          navigationActions = navigationActions,
  //          question = "What are your greatest strengths?",
  //          viewModel = speakingViewModel,
  //          permissionGranted = mutableStateOf(true), // Grant permission
  //          offlinePromptsFunctions = mockOfflinePromptsFunctions)
  //    }
  //    composeTestRule.onNodeWithTag("targetCompany").assertTextContains("Test Company")
  //  }

  @Test
  fun testDoneButton_notNavigatingWhenNotSaved() {
    // Recompose with permission granted and start recording without stopping
    composeTestRule.setContent {
      OfflineRecordingScreen(
          navigationActions = navigationActions,
          question = "What are your greatest strengths?",
          viewModel = speakingViewModel,
          permissionGranted = mutableStateOf(true), // Grant permission
          offlinePromptsFunctions = mockOfflinePromptsFunctions)
    }

    // Start recording
    composeTestRule.onNodeWithTag("mic_button").performClick()

    // Attempt to click "Done" without stopping
    composeTestRule.onNodeWithTag("DoneButton").performClick()

    // Verify that navigation does not happen
    verify(navigationActions, never()).navigateTo(Screen.OFFLINE_RECORDING_REVIEW_SCREEN)
  }
}
