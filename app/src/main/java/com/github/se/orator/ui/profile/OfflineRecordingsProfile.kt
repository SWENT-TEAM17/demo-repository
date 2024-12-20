package com.github.se.orator.ui.profile

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import com.github.se.orator.ui.navigation.TopNavigationMenu
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.AppFontSizes
import com.github.se.orator.ui.theme.AppTypography

/**
 * Prompt card for offline mode
 *
 * @param prompt the prompt
 * @param index
 * @param navigationActions
 * @param speakingViewModel
 * @param promptID
 */
@Composable
fun PromptCard(
    prompt: Map<String, String>,
    index: Int,
    navigationActions: NavigationActions,
    speakingViewModel: SpeakingViewModel,
    promptID: String
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .height(AppDimensions.cardSectionHeightMedium)
              .padding(AppDimensions.paddingSmall)
              .testTag("prompt_card_$index"),
      // colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onSurface),
      onClick = {
        speakingViewModel.interviewPromptNb.value = promptID
        Log.d(
            "OfflineRecordingsProfile: ",
            "opening the file : ${speakingViewModel.interviewPromptNb.value}.mp3")
        navigationActions.navigateTo(Screen.FEEDBACK_SCREEN)
      }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppDimensions.paddingMedium),
            verticalArrangement = Arrangement.Center) {
              Text(
                  text = "Interview ${index + 1}",
                  fontSize = AppFontSizes.bodyLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.testTag("prompt_title_$index"))
              Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
              Text(
                  text = "Company: ${prompt["targetCompany"]}",
                  fontSize = AppFontSizes.bodySmall,
                  color = MaterialTheme.colorScheme.primary)
              Text(
                  text = "Job position: ${prompt["jobPosition"]}",
                  fontSize = AppFontSizes.bodySmall,
                  color = MaterialTheme.colorScheme.primary)
            }
      }
}

@Composable
fun PromptCardsSection(
    context: Context,
    navigationActions: NavigationActions,
    speakingViewModel: SpeakingViewModel,
    offlinePromptsFunctions: OfflinePromptsFunctionsInterface
) {
  val prompts =
      offlinePromptsFunctions.loadPromptsFromFile(context) // Load the prompts from the file

  LazyColumn(
      modifier = Modifier.fillMaxSize().padding(AppDimensions.paddingMedium),
      horizontalAlignment = Alignment.CenterHorizontally) {
        if (prompts.isNullOrEmpty()) {
          // If no prompts exist, show a placeholder text
          item {
            Text(
                text = "No prompts found.",
                style = AppTypography.bodyLargeStyle,
                modifier = Modifier.testTag("no_prompts_text"))
          }
        } else {
          // Display a card for each prompt
          prompts.forEachIndexed { index, prompt ->
            val promptID = prompt.get("ID") ?: "audio.mp3"
            item {
              PromptCard(
                  prompt = prompt, index = index, navigationActions, speakingViewModel, promptID)
            }
            item { Spacer(modifier = Modifier.height(AppDimensions.paddingSmall)) }
          }
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRecordingsProfileScreen(
    navigationActions: NavigationActions,
    speakingViewModel: SpeakingViewModel,
    offlinePromptsFunctions: OfflinePromptsFunctionsInterface
) {
  val context = LocalContext.current
  Column {
    TopNavigationMenu(
        textTestTag = "previous_sessions_test",
        title = "Previous sessions",
        navigationIcon = {
          IconButton(
              onClick = { navigationActions.goBack() },
              modifier = Modifier.testTag("back_button")) {
                androidx.compose.material.Icon(
                    Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "Back button",
                    modifier = Modifier.size(AppDimensions.iconSizeMedium),
                    tint = MaterialTheme.colorScheme.onSurface)
              }
        })
    Column(modifier = Modifier.fillMaxSize().padding(AppDimensions.paddingMedium)) {
      Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
      PromptCardsSection(context, navigationActions, speakingViewModel, offlinePromptsFunctions)
    }
  }
}
