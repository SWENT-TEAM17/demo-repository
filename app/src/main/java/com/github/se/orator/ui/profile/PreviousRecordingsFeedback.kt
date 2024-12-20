package com.github.se.orator.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.symblAi.AndroidAudioPlayer
import com.github.se.orator.model.symblAi.AudioPlayer
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.TopNavigationMenu
import com.github.se.orator.ui.theme.AppDimensions
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "StateFlowValueCalledInComposition")
@Composable
fun PreviousRecordingsFeedbackScreen(
    context: Context = LocalContext.current,
    navigationActions: NavigationActions,
    viewModel: ChatViewModel,
    speakingViewModel: SpeakingViewModel,
    player: AudioPlayer = AndroidAudioPlayer(context),
    offlinePromptsFunctions: OfflinePromptsFunctionsInterface
) {
  // Load prompts from file
  val promptsList = offlinePromptsFunctions.loadPromptsFromFile(context)
  val prompt = promptsList?.find { it["ID"] == speakingViewModel.interviewPromptNb.value }

  // Handle case where prompt is not found
  if (prompt == null) {
    Scaffold(
        topBar = {
          TopNavigationMenu(
              title = "Feedback",
              navigationIcon = {
                IconButton(
                    onClick = { navigationActions.goBack() },
                    modifier = Modifier.testTag("back_button")) {
                      Icon(
                          Icons.Outlined.ArrowBackIosNew,
                          contentDescription = "Back button",
                          modifier = Modifier.size(AppDimensions.iconSizeMedium),
                          tint = MaterialTheme.colorScheme.onSurface)
                    }
              })
        }) { innerPadding ->
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(innerPadding)
                      .padding(AppDimensions.paddingMedium)
                      .testTag("ErrorScreen"),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No prompt found for the selected recording.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("ErrorText"))
                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                Button(
                    onClick = { navigationActions.goBack() },
                    modifier = Modifier.testTag("GoBackButton")) {
                      Text(text = "Go Back")
                    }
              }
        }
    return
  }

  val ID: String = prompt["ID"] ?: "unknown_id"
  val audioFile = File(context.cacheDir, "$ID.mp3")

  val fileData by offlinePromptsFunctions.fileData.collectAsState()

  // State to track if the file has been written to
  var writtenTo by remember { mutableStateOf(false) }

  // Initial setup: clear old display text and read the prompt text file
  LaunchedEffect(ID) {
    Log.d("PreviousRecordingsFeedbackScreen", "Launching setup for ID: $ID")
    offlinePromptsFunctions.clearDisplayText()
    offlinePromptsFunctions.readPromptTextFile(context, ID)
  }

  // Trigger transcript and GPT response if needed
  LaunchedEffect(fileData) {
    if (fileData == "Loading interviewer response..." || fileData.isNullOrEmpty()) {
      Log.d(
          "PreviousRecordingsFeedback",
          "Calling getTranscriptAndGetGPTResponse with fileData: $fileData")
      try {
        speakingViewModel.getTranscriptAndGetGPTResponse(
            audioFile, prompt, viewModel, context, offlinePromptsFunctions)
      } catch (e: Exception) {
        Log.e("PreviousRecordingsFeedback", "Error in getTranscriptAndGetGPTResponse", e)
        // Optionally, update UI to show error message
      }
    }
  }

  // Update writtenTo state when fileData changes
  LaunchedEffect(ID) {
    Log.d("PreviousRecordingsFeedbackScreen", "Starting polling for GPT response")
    while (!writtenTo) {
      offlinePromptsFunctions.readPromptTextFile(context, ID)
      Log.d("PreviousRecordingsFeedbackScreen", "Polling text file for ID: $ID")
      val currentFileData = offlinePromptsFunctions.fileData.value
      if (!currentFileData.isNullOrEmpty() &&
          currentFileData != "Loading interviewer response...") {
        Log.d("PreviousRecordingsFeedbackScreen", "File has been updated")
        writtenTo = true
        break
      }
      delay(500) // Polling interval
    }
  }

  // Determine the display text based on fileData
  val displayText =
      if (fileData == "Loading interviewer response..." || fileData.isNullOrEmpty()) {
        "Processing your audio, please wait..."
      } else {
        "Interviewer's response: $fileData"
      }

  Scaffold(
      topBar = {
        val company =
            offlinePromptsFunctions.getPromptMapElement(ID, "targetCompany", context)
                ?: "Unknown Company"
        TopNavigationMenu(
            title = "$company Interview",
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("back_button")) {
                    Icon(
                        Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Back button",
                        modifier = Modifier.size(AppDimensions.iconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            })
      }) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(AppDimensions.paddingMedium)
                    .testTag("RecordingReviewScreen"),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally) {
              if (!writtenTo) {
                LoadingUI()
              } else {
                // Check if audioFile exists before enabling play/stop buttons
                if (!audioFile.exists()) {
                  Text(
                      text = "Audio file not found.",
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.testTag("AudioNotFoundText"))
                } else {
                  Button(
                      modifier = Modifier.testTag("hear_recording_button"),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MaterialTheme.colorScheme.primary),
                      onClick = {
                        try {
                          player.playFile(audioFile)
                        } catch (e: Exception) {
                          Log.e("PreviousRecordingsFeedback", "Error playing file", e)
                          // Optionally, show a toast or error message to the user
                        }
                      }) {
                        Text(text = "Play Audio", color = MaterialTheme.colorScheme.surface)
                      }
                  Button(
                      modifier = Modifier.testTag("stop_recording_button"),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = MaterialTheme.colorScheme.primary),
                      onClick = {
                        try {
                          player.stop()
                        } catch (e: Exception) {
                          Log.e("PreviousRecordingsFeedback", "Error stopping player", e)
                          // Optionally, show a toast or error message to the user
                        }
                      }) {
                        Text(text = "Stop Audio", color = MaterialTheme.colorScheme.surface)
                      }
                }
                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                Divider()
                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                Text(
                    text =
                        "You said: ${offlinePromptsFunctions.getPromptMapElement(ID, "transcription", context).orEmpty()}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("ResponseText"))
                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                Divider()
                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                Text(
                    text = displayText,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("ResponseText"))
              }
            }
      }
}

@Composable
fun LoadingUI() {
  Column(
      modifier = Modifier.fillMaxSize().testTag("loadingColumn"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = AppDimensions.strokeWidth,
            modifier =
                Modifier.size(AppDimensions.loadingIndicatorSize).testTag("loadingIndicator"))
        Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
        Text(
            "Loading interviewer response...",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("interviewerResponse"))
      }
}
