package com.github.se.orator.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.github.se.orator.model.profile.SessionType
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.ui.navigation.BottomNavigationMenu
import com.github.se.orator.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Route
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.AppFontSizes
import com.github.se.orator.ui.theme.ProjectTheme

var currentPracticeMode = mutableStateOf(SessionType.SPEECH)
var currentRankMetric = mutableStateOf("Ratio")
/**
 * Composable function that displays the "Leaderboard" screen, which shows a ranked list of friends
 * based on their improvement statistics. Users can also select different practice modes.
 *
 * @param navigationActions Actions to handle navigation within the app.
 * @param userProfileViewModel ViewModel for managing user profile data and fetching leaderboard
 *   entries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navigationActions: NavigationActions,
    userProfileViewModel: UserProfileViewModel
) {
  // Fetch user profile and friends' profiles to create leaderboard entries
  val userProfile by userProfileViewModel.userProfile.collectAsState()
  val friendsProfiles by userProfileViewModel.friendsProfiles.collectAsState()

  val usersForRanking = listOfNotNull(userProfile) + friendsProfiles

  // Combine and sort profiles by improvement for leaderboard display
  val leaderboardEntriesRatio =
      remember(userProfile, friendsProfiles, currentPracticeMode, currentRankMetric) {
        (usersForRanking).sortedByDescending {
          userProfileViewModel.getSuccessRatioForMode(it.statistics, currentPracticeMode.value)
        }
      }
  val leaderboardEntriesSuccess =
      remember(userProfile, friendsProfiles, currentPracticeMode, currentRankMetric) {
        (usersForRanking).sortedByDescending {
          userProfileViewModel.getSuccessForMode(it.statistics, currentPracticeMode.value)
        }
      }
  val leaderboardEntriesImprovement =
      remember(userProfile, friendsProfiles, currentPracticeMode, currentRankMetric) {
        (usersForRanking).sortedByDescending { it.statistics.improvement }
      }

  ProjectTheme {
    Scaffold(
        topBar = {
          TitleAppBar(
              navigationActions,
              "Leaderboard",
              "leaderboardTitle",
              "leaderboardBackButton",
              "leaderboardBackIcon")
        },
        bottomBar = {
          BottomNavigationMenu(
              onTabSelect = { route -> navigationActions.navigateTo(route) },
              tabList = LIST_TOP_LEVEL_DESTINATION,
              selectedItem = Route.FRIENDS)
        }) { innerPadding ->
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(innerPadding)
                      .padding(
                          horizontal = AppDimensions.paddingMedium,
                          vertical = AppDimensions.paddingSmall)
                      .testTag("leaderboardList"),
              horizontalAlignment = Alignment.CenterHorizontally) {
                // Dropdown selector for choosing practice mode
                ButtonRow()

                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))

                // Leaderboard list displaying ranked profiles
                LazyColumn(
                    contentPadding = PaddingValues(vertical = AppDimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.paddingSmall),
                    modifier = Modifier.testTag("leaderboardLazyColumn")) {
                      if (currentRankMetric.value == "Ratio") {
                        itemsIndexed(leaderboardEntriesRatio) { index, profile ->
                          LeaderboardItem(
                              rank = index + 1, profile = profile, "Ratio", userProfileViewModel)
                        }
                      } else if (currentRankMetric.value == "Success") {
                        itemsIndexed(leaderboardEntriesSuccess) { index, profile ->
                          LeaderboardItem(
                              rank = index + 1, profile = profile, "Success", userProfileViewModel)
                        }
                      } else {
                        itemsIndexed(leaderboardEntriesImprovement) { index, profile ->
                          LeaderboardItem(
                              rank = index + 1,
                              profile = profile,
                              "Improvement",
                              userProfileViewModel)
                        }
                      }
                    }
              }
        }
  }
}

/**
 * Composable function that displays a dropdown menu for selecting different practice modes. The
 * selected mode is shown and can be changed by the user.
 */
@Composable
fun PracticeModeSelector() {
  var expanded by remember { mutableStateOf(false) } // Controls dropdown menu visibility
  var selectedMode by remember { mutableStateOf("Practice mode 1") } // Holds the selected mode

  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .clickable { expanded = true }
              .padding(AppDimensions.paddingSmallMedium)
              .testTag("practiceModeSelector"),
      contentAlignment = Alignment.Center) {
        Text(
            text = "Practice mode",
            fontSize = AppFontSizes.subtitle, // 16.0sp
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("selectedMode"))

        // Dropdown menu options for selecting a practice mode
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          DropdownMenuItem(
              text = { Text("Interview", color = MaterialTheme.colorScheme.onSecondaryContainer) },
              onClick = {
                selectedMode = "Practice mode 1"
                currentPracticeMode.value = SessionType.INTERVIEW
                expanded = false
              },
              modifier = Modifier.testTag("practiceModeOption1"))
          DropdownMenuItem(
              text = { Text("Speech") },
              onClick = {
                selectedMode = "Practice mode 2"
                currentPracticeMode.value = SessionType.SPEECH
                expanded = false
              },
              modifier = Modifier.testTag("practiceModeOption2"))
          DropdownMenuItem(
              text = { Text("Negotiation") },
              onClick = {
                selectedMode = "Practice mode 3"
                currentPracticeMode.value = SessionType.NEGOTIATION
                expanded = false
              },
              modifier = Modifier.testTag("practiceModeOption3"))
        }
      }
}

/**
 * Composable function that represents a single leaderboard item, displaying the profile's rank,
 * name, and improvement statistics.
 *
 * @param rank The rank position of the user in the leaderboard.
 * @param profile The [UserProfile] object containing user information and improvement statistics.
 */
@Composable
fun LeaderboardItem(
    rank: Int,
    profile: UserProfile,
    selectedMetric: String,
    userProfileViewModel: UserProfileViewModel
) {
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = AppDimensions.paddingExtraSmall) // Side padding for each item
              .clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .testTag("leaderboardItem#$rank"),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shadowElevation = AppDimensions.elevationSmall // Subtle shadow with low elevation
      ) {
        Row(modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingMedium)) {
          ProfilePicture(profilePictureUrl = profile.profilePic, onClick = {})
          Spacer(modifier = Modifier.width(AppDimensions.paddingSmallMedium))

          Column {
            // Display user's name in bold
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary)
            // Display user's improvement statistics
            // Display the selected metric's value
            val metricValue =
                when (selectedMetric) {
                  "Ratio" ->
                      if (userProfileViewModel.getSuccessRatioForMode(
                          profile.statistics, currentPracticeMode.value) >= 0) {
                        "Success Ratio: ${userProfileViewModel.getSuccessRatioForMode(profile.statistics, currentPracticeMode.value)}"
                      } else {
                        "Sucess Ratio: Not Ranked"
                      }
                  "Success" ->
                      if (userProfileViewModel.getSuccessForMode(
                          profile.statistics, currentPracticeMode.value) >= 0) {
                        "Success: ${
                              userProfileViewModel.getSuccessForMode(profile.statistics, currentPracticeMode.value)
                          }"
                      } else {
                        "Sucess: Not Ranked"
                      }
                  else ->
                      if (profile.statistics.improvement >= 0) {
                        "Improvement: ${profile.statistics.improvement}"
                      } else {
                        "Improvement: Not Ranked"
                      }
                }
            Text(
                text = metricValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag("leaderboardItemImprovement#$rank"))
          }

          Spacer(modifier = Modifier.weight(AppDimensions.FULL))

          // Display rank as a badge on the left side
          Text(
              text = "#$rank",
              fontWeight = FontWeight.Bold,
              modifier =
                  Modifier.align(Alignment.CenterVertically).testTag("leaderboardItemName#$rank"),
              color = MaterialTheme.colorScheme.secondary)
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleAppBar(
    navigationActions: NavigationActions,
    title: String,
    titleTestTAg: String,
    buttonTestTag: String,
    iconTestTag: String
) {
  TopAppBar(
      title = {
        Text(
            title,
            modifier = Modifier.testTag(titleTestTAg),
            color = MaterialTheme.colorScheme.onSurface)
      },
      navigationIcon = {
        IconButton(
            onClick = {
              navigationActions.goBack() // Navigate back
            },
            modifier = Modifier.testTag(buttonTestTag)) {
              Icon(
                  Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  modifier = Modifier.testTag(iconTestTag),
                  tint = MaterialTheme.colorScheme.onSurface)
            }
      },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer))
}

/**
 * Composable function that displays a dropdown menu for selecting different rank metrics. The
 * selected metric is shown and can be changed by the user.
 */
@Composable
fun RankMetricSelector() {
  var expanded by remember { mutableStateOf(false) } // Controls dropdown menu visibility
  var selectedMetric by remember { mutableStateOf("Ratio") } // Holds the selected metric

  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .clickable { expanded = true }
              .padding(AppDimensions.paddingSmallMedium)
              .testTag("rankMetricSelector"),
      contentAlignment = Alignment.Center) {
        Text(
            text = "Rank metric",
            fontSize = AppFontSizes.subtitle,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("selectedMetric"))

        // Dropdown menu options for selecting a rank metric
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          DropdownMenuItem(
              text = {
                Text("Success ratio", color = MaterialTheme.colorScheme.onSecondaryContainer)
              },
              onClick = {
                selectedMetric = "Ratio"
                currentRankMetric.value = "Ratio"
                expanded = false
              },
              modifier = Modifier.testTag("rankMetricOption1"))
          DropdownMenuItem(
              text = { Text("Success") },
              onClick = {
                selectedMetric = "Success"
                currentRankMetric.value = "Success"
                expanded = false
              },
              modifier = Modifier.testTag("rankMetricOption2"))
          DropdownMenuItem(
              text = { Text("Improvement") },
              onClick = {
                selectedMetric = "Improvement"
                currentRankMetric.value = "Improvement"
                expanded = false
              },
              modifier = Modifier.testTag("rankMetricOption3"))
        }
      }
}

/**
 * The implementation of the toolbar containing the practice mode button and the rank metric button
 */
@Composable
fun ButtonRow() {
  Row(
      modifier =
          Modifier.testTag("buttonRowLeaderboard")
              .fillMaxWidth()
              .padding(top = AppDimensions.paddingMedium),
      horizontalArrangement =
          Arrangement.spacedBy(AppDimensions.spacingXLarge, Alignment.CenterHorizontally),
  ) {
    PracticeModeSelector()
    RankMetricSelector()
  }
}
