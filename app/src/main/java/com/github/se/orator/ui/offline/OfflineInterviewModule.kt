package com.github.se.orator.ui.overview

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.TopNavigationMenu
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.AppFontSizes
import kotlin.random.Random

fun generateRandomString(length: Int = 8): String {
  val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
  return (1..length).map { charset[Random.nextInt(charset.length)] }.joinToString("")
}

/**
 * The SpeakingJobInterviewModule composable is a composable screen that displays the job interview
 * module.
 *
 * @param navigationActions The navigation actions that can be performed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun OfflineInterviewModule(
    navigationActions: NavigationActions,
    speakingViewModel: SpeakingViewModel,
    offlinePromptsFunctions: OfflinePromptsFunctionsInterface
) {
  var targetCompany by remember { mutableStateOf("") }
  var jobPosition by remember { mutableStateOf("") }
  var question by remember { mutableStateOf("") }
  val ID = generateRandomString()
    var lastQuestion by remember { mutableStateOf<String?>(null) } // Local state for the last question
  val context = LocalContext.current

  Scaffold(
      topBar = {
        TopNavigationMenu(
            textTestTag = "offlineModuleTitle",
            title = "Offline interview",
            navigationIcon = {
              IconButton(
                  onClick = {
                    navigationActions.goBack() // Navigate back
                  },
                  modifier = Modifier.testTag("back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            })
      }) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = AppDimensions.paddingMedium)
                    .padding(top = AppDimensions.paddingSmall)
                    .verticalScroll(rememberScrollState())
                    .testTag("content"),
            verticalArrangement = Arrangement.Top) {
              OutlinedTextField(
                  value = targetCompany,
                  onValueChange = { newCompany -> targetCompany = newCompany },
                  label = {
                    Text("What company are you applying to?", modifier = Modifier.testTag(""))
                  },
                  modifier = Modifier.fillMaxWidth().testTag("company_field"),
                  singleLine = true,
                  colors =
                      androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                          backgroundColor = MaterialTheme.colorScheme.surface,
                          textColor = MaterialTheme.colorScheme.onSurface,
                          focusedBorderColor = MaterialTheme.colorScheme.outline,
                          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                          cursorColor = MaterialTheme.colorScheme.primary,
                          focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                          unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))

              OutlinedTextField(
                  value = jobPosition,
                  onValueChange = { newJobPosition -> jobPosition = newJobPosition },
                  label = { Text("What job are you applying to?") },
                  modifier = Modifier.fillMaxWidth().testTag("job_field"),
                  singleLine = true,
                  colors =
                      androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                          backgroundColor = MaterialTheme.colorScheme.surface,
                          textColor = MaterialTheme.colorScheme.onSurface,
                          focusedBorderColor = MaterialTheme.colorScheme.outline,
                          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                          cursorColor = MaterialTheme.colorScheme.primary,
                          focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                          unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))

              OutlinedTextField(
                  value = question,
                  onValueChange = { newQuestion -> question = newQuestion },
                  label = {
                    Text("What do you want to focus on?", modifier = Modifier.testTag("question"))
                  },
                  modifier = Modifier.fillMaxWidth().testTag("question_field"),
                  singleLine = false,
                  colors =
                      androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                          backgroundColor = MaterialTheme.colorScheme.surface,
                          textColor = MaterialTheme.colorScheme.onSurface,
                          focusedBorderColor = MaterialTheme.colorScheme.outline,
                          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                          cursorColor = MaterialTheme.colorScheme.primary,
                          focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                          unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))

              Spacer(modifier = Modifier.height(AppDimensions.paddingLarge))

              Button(
                  onClick = {
                    // this is for the profile screen to see previous interviews
                    offlinePromptsFunctions.savePromptsToFile(
                        context = context,
                        prompts =
                            mapOf(
                                "targetCompany" to targetCompany,
                                "jobPosition" to jobPosition,
                                "question" to question,
                                "ID" to ID,
                                "transcribed" to "0",
                                "GPTresponse" to "0",
                                "transcription" to ""),
                    )
                    offlinePromptsFunctions.createEmptyPromptFile(context, ID)
                    speakingViewModel.interviewPromptNb.value = ID
                     navigationActions.goToOfflineRecording(question.ifEmpty { lastQuestion ?: "" })

                  },
                  modifier =
                      Modifier.fillMaxWidth(0.8f)
                          .padding(AppDimensions.paddingSmall)
                          .testTag("doneButton")
                          .align(Alignment.CenterHorizontally),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                           enabled = question.isNotEmpty() || !lastQuestion.isNullOrEmpty(), // Disable if no valid question     
                          ) {
                    Text(
                        text = "Go to recording screen",
                        fontSize = AppFontSizes.buttonText,
                        color = MaterialTheme.colorScheme.surface)
                  }
            }
      }
}
