package com.github.se.orator.ui.offline

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.orator.model.symblAi.AndroidAudioPlayer
import com.github.se.orator.model.symblAi.AudioRecorder
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import com.github.se.orator.ui.theme.AppDimensions
import java.io.File

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun RecordingReviewScreen(
    navigationActions: NavigationActions,
    speakingViewModel: SpeakingViewModel = viewModel()
) {
  val context = LocalContext.current
  val recorder by lazy { AudioRecorder(context = context) }

  val player by lazy { AndroidAudioPlayer(context) }

  val audioFile: File = File(context.cacheDir, "${speakingViewModel.interviewPromptNb.value}.mp3")

  Log.d("rec screen review", "the file that you shall play is $audioFile")
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(AppDimensions.paddingMedium)
              .testTag("RecordingReviewScreen"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            modifier = Modifier.testTag("hear_recording_button"),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            onClick = { player.playFile(audioFile) }) {
              Text(text = "Play recording", color = MaterialTheme.colorScheme.surface)
            }

        Button(
            modifier = Modifier.testTag("stop_recording_button"),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            onClick = { player.stop() }) {
              Text(text = "Stop recording", color = MaterialTheme.colorScheme.surface)
            }

        Button(
            modifier = Modifier.testTag("try_again"),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            onClick = { navigationActions.navigateTo(Screen.OFFLINE_INTERVIEW_MODULE) }) {
              Text(
                  text = "Do another interview",
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.testTag("text_try_again"))
            }

        Row(
            modifier = Modifier.fillMaxWidth().testTag("Back"),
            verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Outlined.ArrowBackIosNew,
                  contentDescription = "Back",
                  modifier =
                      Modifier.size(AppDimensions.iconSizeSmall)
                          .clickable { navigationActions.goBack() }
                          .testTag("BackButton"),
                  tint = MaterialTheme.colorScheme.primary)
            }
      }
}
