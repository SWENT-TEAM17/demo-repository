package com.github.se.orator.ui.profile

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.symblAi.AudioPlayer
import com.github.se.orator.model.symblAi.SpeakingRepository
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.network.ChatGPTService
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PreviousRecordingsFeedbackScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Use TestCoroutineDispatcher to control coroutine execution
  private val testDispatcher = UnconfinedTestDispatcher()

  private lateinit var navigationActions: NavigationActions
  private lateinit var speakingViewModel: SpeakingViewModel
  private lateinit var speakingRepository: SpeakingRepository
  private lateinit var apiLinkViewModel: ApiLinkViewModel
  private lateinit var chatGPTService: ChatGPTService
  private lateinit var chatViewModel: ChatViewModel
  private lateinit var userProfileViewModel: UserProfileViewModel
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var mockPlayer: AudioPlayer
  private lateinit var offlinePromptsFunctions: OfflinePromptsFunctionsInterface
  private lateinit var context: Context

  @Before
  fun setUp() {
    // Obtain the application context
    context = ApplicationProvider.getApplicationContext<Context>()

    // Initialize mocks
    navigationActions = mock(NavigationActions::class.java)
    speakingRepository = mock(SpeakingRepository::class.java)
    apiLinkViewModel = ApiLinkViewModel()
    chatGPTService = mock(ChatGPTService::class.java)
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)
    userProfileRepository = mock(UserProfileRepository::class.java)
    userProfileViewModel = UserProfileViewModel(userProfileRepository)
    offlinePromptsFunctions = mock(OfflinePromptsFunctionsInterface::class.java)
    mockPlayer = mock(AudioPlayer::class.java)

    // Initialize the SpeakingViewModel with the TestCoroutineDispatcher
    speakingViewModel =
        SpeakingViewModel(speakingRepository, apiLinkViewModel, userProfileViewModel)

    // Mock the fileData StateFlow to control the display text
    `when`(offlinePromptsFunctions.fileData).thenReturn(MutableStateFlow("Loading").asStateFlow())

    // Mock the response for getPromptMapElement to return "Test Company"
    `when`(offlinePromptsFunctions.getPromptMapElement(anyString(), anyString(), any()))
        .thenReturn("Test Company")

    // Optionally, mock loadPromptsFromFile if used
    `when`(offlinePromptsFunctions.loadPromptsFromFile(any())).thenReturn(emptyList())
  }

  @After
  fun tearDown() {
    // Clean up any created files in the cache directory to avoid test interference
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { it.delete() }
  }

  /**
   * Test Case 3: Verify that clicking the back button navigates back correctly when the audio file
   * exists.
   */
  @Test
  fun testBackButtonNavigation_withExistingFile() = runTest {
    // Set up a specific ID for the prompt
    val testID = "test_id_existing_navigation"
    val prompt =
        mapOf(
            "ID" to testID,
            "targetCompany" to "Test Company",
            "transcription" to "Sample transcription")
    `when`(offlinePromptsFunctions.loadPromptsFromFile(context)).thenReturn(listOf(prompt))

    // Create the audio file in the cache directory to simulate existence
    val audioFile = File(context.cacheDir, "$testID.mp3")
    audioFile.createNewFile()

    // Update the ViewModel's interviewPromptNb to the test ID
    speakingViewModel.interviewPromptNb.value = testID

    // Simulate that the GPT response is ready by updating fileData
    `when`(offlinePromptsFunctions.fileData)
        .thenReturn(MutableStateFlow("Interviewer's response").asStateFlow())

    // Set the content of the composable
    composeTestRule.setContent {
      PreviousRecordingsFeedbackScreen(
          navigationActions = navigationActions,
          viewModel = chatViewModel,
          speakingViewModel = speakingViewModel,
          player = mockPlayer,
          offlinePromptsFunctions = offlinePromptsFunctions)
    }

    // Allow LaunchedEffects to run
    advanceUntilIdle()

    // Click on the back button
    composeTestRule.onNodeWithTag("back_button").performClick()

    // Verify that the navigation back action was triggered
    verify(navigationActions).goBack()
  }

  /**
   * Test Case 4: Verify that clicking the back button navigates back correctly when the audio file
   * does not exist.
   */
  @Test
  fun testBackButtonNavigation_withMissingFile() = runTest {
    // Set up a specific ID for the prompt
    val testID = "test_id_missing_navigation"
    val prompt =
        mapOf(
            "ID" to testID,
            "targetCompany" to "Test Company",
            "transcription" to "Sample transcription")
    `when`(offlinePromptsFunctions.loadPromptsFromFile(context)).thenReturn(listOf(prompt))

    // Ensure the audio file does NOT exist by deleting it if it exists
    val audioFile = File(context.cacheDir, "$testID.mp3")
    if (audioFile.exists()) {
      audioFile.delete()
    }

    // Update the ViewModel's interviewPromptNb to the test ID
    speakingViewModel.interviewPromptNb.value = testID

    // Simulate that the GPT response is ready by updating fileData
    `when`(offlinePromptsFunctions.fileData)
        .thenReturn(MutableStateFlow("Interviewer's response").asStateFlow())

    // Set the content of the composable
    composeTestRule.setContent {
      PreviousRecordingsFeedbackScreen(
          navigationActions = navigationActions,
          viewModel = chatViewModel,
          speakingViewModel = speakingViewModel,
          player = mockPlayer,
          offlinePromptsFunctions = offlinePromptsFunctions)
    }

    // Allow LaunchedEffects to run
    advanceUntilIdle()

    // Click on the "Go Back" button in the error screen
    composeTestRule.onNodeWithTag("GoBackButton").performClick()

    // Verify that the navigation back action was triggered
    verify(navigationActions).goBack()
  }

  /**
   * Test Case 6: Verify that clicking the play button does not trigger the AudioPlayer when the
   * file does not exist.
   */
  @Test
  fun testPlayButton_withoutExistingFile() = runTest {
    // Set up a specific ID for the prompt
    val testID = "test_id_no_play_audio"
    val prompt =
        mapOf(
            "ID" to testID,
            "targetCompany" to "Test Company",
            "transcription" to "Sample transcription")
    `when`(offlinePromptsFunctions.loadPromptsFromFile(context)).thenReturn(listOf(prompt))

    // Ensure the audio file does NOT exist by deleting it if it exists
    val audioFile = File(context.cacheDir, "$testID.mp3")
    if (audioFile.exists()) {
      audioFile.delete()
    }

    // Update the ViewModel's interviewPromptNb to the test ID
    speakingViewModel.interviewPromptNb.value = testID

    // Simulate that the GPT response is ready by updating fileData
    `when`(offlinePromptsFunctions.fileData)
        .thenReturn(MutableStateFlow("Interviewer's response").asStateFlow())

    // Set the content of the composable
    composeTestRule.setContent {
      PreviousRecordingsFeedbackScreen(
          navigationActions = navigationActions,
          viewModel = chatViewModel,
          speakingViewModel = speakingViewModel,
          player = mockPlayer,
          offlinePromptsFunctions = offlinePromptsFunctions)
    }

    // Allow LaunchedEffects to run
    advanceUntilIdle()

    // Attempt to click on the play button
    // The play button should not be displayed, so assert it does not exist
    composeTestRule.onNodeWithTag("hear_recording_button").assertDoesNotExist()

    // Verify that playFile was never called
    verify(mockPlayer, never()).playFile(any())
  }

  /**
   * Test Case 8: Verify that clicking the stop button does not trigger the AudioPlayer when the
   * file does not exist.
   */
  @Test
  fun testStopButton_withoutExistingFile() = runTest {
    // Set up a specific ID for the prompt
    val testID = "test_id_no_stop_audio"
    val prompt =
        mapOf(
            "ID" to testID,
            "targetCompany" to "Test Company",
            "transcription" to "Sample transcription")
    `when`(offlinePromptsFunctions.loadPromptsFromFile(context)).thenReturn(listOf(prompt))

    // Ensure the audio file does NOT exist by deleting it if it exists
    val audioFile = File(context.cacheDir, "$testID.mp3")
    if (audioFile.exists()) {
      audioFile.delete()
    }

    // Update the ViewModel's interviewPromptNb to the test ID
    speakingViewModel.interviewPromptNb.value = testID

    // Simulate that the GPT response is ready by updating fileData
    `when`(offlinePromptsFunctions.fileData)
        .thenReturn(MutableStateFlow("Interviewer's response").asStateFlow())

    // Set the content of the composable
    composeTestRule.setContent {
      PreviousRecordingsFeedbackScreen(
          navigationActions = navigationActions,
          viewModel = chatViewModel,
          speakingViewModel = speakingViewModel,
          player = mockPlayer,
          offlinePromptsFunctions = offlinePromptsFunctions)
    }

    // Allow LaunchedEffects to run
    advanceUntilIdle()

    // Attempt to click on the stop button
    // The stop button should not be displayed, so assert it does not exist
    composeTestRule.onNodeWithTag("stop_recording_button").assertDoesNotExist()

    // Verify that stop was never called
    verify(mockPlayer, never()).stop()
  }

  @Test
  fun testErrorScreen_displayed_whenNoPromptFound() = runTest {
    // Mock loadPromptsFromFile to return empty list, meaning no prompt found
    `when`(offlinePromptsFunctions.loadPromptsFromFile(context)).thenReturn(emptyList())

    // Set the ViewModel's interviewPromptNb to an ID that does not exist
    speakingViewModel.interviewPromptNb.value = "non_existing_id"

    // Set the content of the composable
    composeTestRule.setContent {
      PreviousRecordingsFeedbackScreen(
          navigationActions = navigationActions,
          viewModel = chatViewModel,
          speakingViewModel = speakingViewModel,
          player = mockPlayer,
          offlinePromptsFunctions = offlinePromptsFunctions)
    }

    // Allow LaunchedEffects to run
    advanceUntilIdle()

    // Verify that the error screen is displayed
    composeTestRule.onNodeWithTag("ErrorScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ErrorText").assertIsDisplayed()
    composeTestRule.onNodeWithTag("GoBackButton").assertIsDisplayed()

    // Click on the Go Back button
    composeTestRule.onNodeWithTag("GoBackButton").performClick()

    // Verify that navigation back was called
    verify(navigationActions).goBack()
  }
}
