package com.github.se.orator.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.symblAi.AndroidAudioPlayer
import com.github.se.orator.model.symblAi.AudioRecorder
import com.github.se.orator.model.symblAi.SpeakingRepositoryRecord
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.model.symblAi.SymblApiClient
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.network.ChatGPTService
import com.github.se.orator.ui.network.ChatRequest
import com.github.se.orator.ui.theme.AppColors
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.AppFontSizes
import com.github.se.orator.ui.theme.AppShapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import loadPromptsFromFile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "StateFlowValueCalledInComposition")
@Composable
fun PreviousRecordingsFeedbackScreen(
    context: Context,
    navigationActions: NavigationActions,
    viewModel: ChatViewModel,
    speakingViewModel: SpeakingViewModel
) {

    val recorder by lazy {
        AudioRecorder(context = context)
    }

    val player by lazy {
        AndroidAudioPlayer(context)
    }
    val prompts = loadPromptsFromFile(context)?.find { it["ID"] == speakingViewModel.interviewPromptNb.value}

    val ID = prompts?.get("ID") ?: "audio.mp3"

    val audioFile: File = File(context.cacheDir, "$ID.mp3")

    val offlineAnalysisData by speakingViewModel.offlineAnalysisData.collectAsState()

    speakingViewModel.getTranscript(audioFile)


    Log.d("hi", "hello!")
//    symblApiClient.getTranscription(audioFile, {analysisData -> whatUserSaid.value = analysisData.transcription
//                                               Log.d("this is inside getTranscription in feedback", whatUserSaid.value)}, {})

    val response by viewModel.response.collectAsState("")

    if (offlineAnalysisData != null) {
        viewModel.offlineRequest(offlineAnalysisData!!.transcription.removePrefix("You said:").trim(), prompts?.get("targetCompany") ?: "Apple", prompts?.get("targetPosition") ?: "engineer")
        Log.d("testing offline chat view model", "the gpt model offline value response is $response")
        //Text(text = "What you said: ${what_has_been_said.value}")
        Text(text = "Interviewer's response: $response", color = Color.Black)
        Log.d("d", "Hello! This is has been said: ${offlineAnalysisData!!.transcription}")
    }
    //prompts?.get("targetPosition") ?: "Default Value"
    //val jobPosition = prompts?.get("jobPosition")
    Log.d("prompts are: ", prompts?.get("targetPosition") ?: "Default Value")



    //val chatMessages by chatViewModel.chatMessages.collectAsState()

    Column(
        modifier =
        Modifier.fillMaxSize()
            .padding(AppDimensions.paddingMedium)
            .testTag("RecordingReviewScreen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Button (
            onClick = {
                player.playFile(audioFile)
            },
            shape = AppShapes.circleShape,
            colors =
            ButtonDefaults.buttonColors(Color.White),
            contentPadding = PaddingValues(0.dp)) {
            androidx.compose.material.Icon(
                Icons.Outlined.PlayCircleOutline,
                contentDescription = "Edit button",
                modifier = Modifier.size(30.dp),
                tint = AppColors.primaryColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().testTag("Back"),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                modifier =
                Modifier.size(AppDimensions.iconSizeSmall)
                    .clickable { navigationActions.goBack() }
                    .testTag("BackButton"),
                tint = MaterialTheme.colorScheme.primary)


        }
    }
}
