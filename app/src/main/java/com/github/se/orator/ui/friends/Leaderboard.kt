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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.github.se.orator.model.profile.SessionType
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.ui.navigation.BottomNavigationMenu
import com.github.se.orator.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Route
import com.github.se.orator.ui.navigation.TopNavigationMenu
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.AppFontSizes
import java.util.Locale

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
  val userProfile by userProfileViewModel.userProfile.collectAsState()
  val friendsProfiles by userProfileViewModel.friendsProfiles.collectAsState()

  val usersForRanking = listOfNotNull(userProfile) + friendsProfiles

  val leaderboardEntriesRatio =
      remember(userProfile, friendsProfiles, currentPracticeMode, currentRankMetric) {
        usersForRanking.sortedByDescending {
          userProfileViewModel.getSuccessRatioForMode(it.statistics, currentPracticeMode.value)
        }
      }

  val leaderboardEntriesSuccess =
      remember(userProfile, friendsProfiles, currentPracticeMode, currentRankMetric) {
        usersForRanking.sortedByDescending {
          userProfileViewModel.getSuccessForMode(it.statistics, currentPracticeMode.value)
        }
      }

  val leaderboardEntriesImprovement =
      remember(userProfile, friendsProfiles, currentPracticeMode, currentRankMetric) {
        usersForRanking.sortedByDescending { it.statistics.improvement }
      }

  Scaffold(
      // No topBar here, we will place a custom row instead
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
              // Row at the top for back button and title
              Row(
                  modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingMedium),
                  verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navigationActions.goBack() },
                        modifier = Modifier.testTag("leaderboardBackButton")) {
                          Icon(
                              Icons.AutoMirrored.Filled.ArrowBack,
                              contentDescription = "Back",
                              modifier = Modifier.testTag("leaderboardBackIcon"),
                              tint = MaterialTheme.colorScheme.onSurface)
                        }

                    Spacer(modifier = Modifier.width(AppDimensions.paddingSmall))

                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("leaderboardTitle"))
                  }

              // Dropdown selectors and leaderboard list
              ButtonRow()

              Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))

              LazyColumn(
                  contentPadding = PaddingValues(vertical = AppDimensions.paddingSmall),
                  verticalArrangement = Arrangement.spacedBy(AppDimensions.paddingSmall),
                  modifier = Modifier.testTag("leaderboardLazyColumn")) {
                    val selectedMetric = currentRankMetric.value
                    when (selectedMetric) {
                      "Ratio" -> {
                        itemsIndexed(leaderboardEntriesRatio) { index, profile ->
                          LeaderboardItem(
                              rank = index + 1,
                              profile = profile,
                              selectedMetric = "Ratio",
                              userProfileViewModel = userProfileViewModel)
                        }
                      }
                      "Success" -> {
                        itemsIndexed(leaderboardEntriesSuccess) { index, profile ->
                          LeaderboardItem(
                              rank = index + 1,
                              profile = profile,
                              selectedMetric = "Success",
                              userProfileViewModel = userProfileViewModel)
                        }
                      }
                      else -> {
                        // Improvement
                        itemsIndexed(leaderboardEntriesImprovement) { index, profile ->
                          LeaderboardItem(
                              rank = index + 1,
                              profile = profile,
                              selectedMetric = "Improvement",
                              userProfileViewModel = userProfileViewModel)
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
  var selectedMode by remember {
    mutableStateOf(
        currentPracticeMode.value.toString().lowercase(Locale.ROOT).capitalize(Locale.ROOT))
  } // Holds the selected mode

  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .clickable { expanded = true }
              .padding(AppDimensions.paddingSmallMedium)
              .testTag("practiceModeSelector"),
      contentAlignment = Alignment.Center) {
        Text(
            text = "Mode : $selectedMode",
            fontSize = AppFontSizes.subtitle, // 16.0sp
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("selectedMode"))

        // Dropdown menu options for selecting a practice mode
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          PracticeModeDropDownMenuCustomItem(
              sessionType = SessionType.INTERVIEW,
              onClickSet = {
                selectedMode = it
                expanded = false
              },
              testTag = "practiceModeOption1")

          PracticeModeDropDownMenuCustomItem(
              sessionType = SessionType.SPEECH,
              onClickSet = {
                selectedMode = it
                expanded = false
              },
              testTag = "practiceModeOption2")
          PracticeModeDropDownMenuCustomItem(
              sessionType = SessionType.NEGOTIATION,
              onClickSet = {
                selectedMode = it
                expanded = false
              },
              testTag = "practiceModeOption3")
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
    titleTestTag: String,
    buttonTestTag: String,
    iconTestTag: String
) {
  TopNavigationMenu(
      textTestTag = titleTestTag,
      title = title,
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
      })
}

/**
 * Composable function that displays a dropdown menu for selecting different rank metrics. The
 * selected metric is shown and can be changed by the user.
 */
@Composable
fun RankMetricSelector() {
  var expanded by remember { mutableStateOf(false) } // Controls dropdown menu visibility
  var selectedMetric by remember {
    mutableStateOf(currentRankMetric.value)
  } // Holds the selected metric

  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .clickable { expanded = true }
              .padding(AppDimensions.paddingSmallMedium)
              .testTag("rankMetricSelector"),
      contentAlignment = Alignment.Center) {
        Text(
            text = "Metric : $selectedMetric",
            fontSize = AppFontSizes.subtitle,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("selectedMetric"))

        // Dropdown menu options for selecting a rank metric
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          MetricDropDownMenuCustomItem(
              text = "Ratio",
              metric = "Ratio",
              onClickSet = {
                selectedMetric = "Ratio"
                expanded = false
              },
              testTag = "rankMetricOption1")
          MetricDropDownMenuCustomItem(
              text = "Success",
              metric = "Success",
              onClickSet = {
                selectedMetric = "Success"
                expanded = false
              },
              testTag = "rankMetricOption2")
          MetricDropDownMenuCustomItem(
              text = "Improvement",
              metric = "Improvement",
              onClickSet = {
                selectedMetric = "Improvement"
                expanded = false
              },
              testTag = "rankMetricOption3")
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

/**
 * Custom dropdown menu item for selecting a practice mode
 *
 * @param sessionType The session type to set when the item is clicked
 * @param onClickSet The function to call when the item is clicked
 * @param testTag The test tag for the dropdown menu item
 */
@Composable
private fun PracticeModeDropDownMenuCustomItem(
    sessionType: SessionType,
    onClickSet: (String) -> Unit,
    testTag: String
) {
  DropdownMenuItem(
      text = {
        Text(
            sessionType.toString().lowercase(Locale.ROOT).capitalize(Locale.ROOT),
            color = colorWhenSelected(currentPracticeMode.value == sessionType))
      },
      onClick = {
        currentPracticeMode.value = sessionType
        onClickSet(
            currentPracticeMode.value.toString().lowercase(Locale.ROOT).capitalize(Locale.ROOT))
      },
      modifier =
          Modifier.testTag(testTag)
              .background(
                  color =
                      MaterialTheme.colorScheme.primary.copy(
                          alpha = if (currentPracticeMode.value == sessionType) 0.4f else 0.0f)))
}

/**
 * Custom dropdown menu item for selecting a rank metric
 *
 * @param text The text to display in the dropdown menu item
 * @param metric The metric to set when the item is clicked
 * @param onClickSet The function to call when the item is clicked
 * @param testTag The test tag for the dropdown menu item
 */
@Composable
private fun MetricDropDownMenuCustomItem(
    text: String,
    metric: String,
    onClickSet: () -> Unit,
    testTag: String
) {
  DropdownMenuItem(
      text = { Text(text, color = colorWhenSelected(currentRankMetric.value == metric)) },
      onClick = {
        currentRankMetric.value = metric
        onClickSet()
      },
      modifier =
          Modifier.testTag(testTag)
              .background(
                  color =
                      MaterialTheme.colorScheme.primary.copy(
                          alpha = if (currentRankMetric.value == metric) 0.4f else 0.0f)))
}

/**
 * Composable function that returns the primary app color when the item is selected, and the
 * onSecondaryContainer color when it is not selected.
 *
 * @param selected The boolean value that determines if the item is selected
 * @return The color to use for the item
 */
@Composable
private fun colorWhenSelected(selected: Boolean): Color {
  return if (selected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.onSecondaryContainer
  }
}
