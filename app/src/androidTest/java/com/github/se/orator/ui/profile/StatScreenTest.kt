package com.github.se.orator.ui.profile

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.profile.UserStatistics
import com.github.se.orator.model.speaking.AnalysisData
import com.github.se.orator.ui.navigation.NavigationActions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class StatScreenTest {

  private lateinit var userProfileViewModel: UserProfileViewModel
  @Mock private lateinit var navigationActions: NavigationActions
  @Mock private lateinit var userProfileRepository: UserProfileRepository

  private val mockedRecentData = createMockedRecentData()

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    userProfileViewModel = UserProfileViewModel(userProfileRepository)

    `when`(userProfileRepository.getUserProfile(any(), any(), any())).then {
      it.getArgument<(UserProfile) -> Unit>(1)(testUserProfile)
    }
    userProfileViewModel = UserProfileViewModel(userProfileRepository)
    userProfileViewModel.getUserProfile(testUserProfile.uid)
  }

  private val talkTimeSecMean =
      (mockedRecentData.toList().map { data -> data.talkTimeSeconds }).average()
  private val talkTimePercMean =
      (mockedRecentData.toList().map { data -> data.talkTimePercentage }).average()

  private val testUserProfile =
      UserProfile(
          uid = "testUid",
          name = "Test User",
          age = 25,
          statistics =
              UserStatistics(
                  recentData = mockedRecentData,
                  talkTimeSecMean = talkTimeSecMean,
                  talkTimePercMean = talkTimePercMean),
          friends = listOf("friend1", "friend2"),
          bio = "Test bio")

  @Test
  fun graphStats_displaysStatTitleAndBackButton() {
    composeTestRule.setContent {
      GraphStats(navigationActions = navigationActions, profileViewModel = userProfileViewModel)
    }

    // Check title
    composeTestRule.onNodeWithTag("statTitle").assertIsDisplayed().assert(hasText("Statistics"))

    // Check back button icon is displayed
    composeTestRule.onNodeWithTag("statBackButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("statBackIcon", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun graphStats_displaysGraphTitlesAndGraphs() {

    composeTestRule.setContent {
      GraphStats(navigationActions = navigationActions, profileViewModel = userProfileViewModel)
    }

    // Check Graph Screen Title
    composeTestRule
        .onNodeWithTag("graphScreenTitle", useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText("Statistics Graph"))

    // Check Talk Time Seconds Title
    composeTestRule
        .onNodeWithTag("talkTimeSecTitle")
        .assertIsDisplayed()
        .assert(hasText("Talk Time Seconds:"))

    // Check the first graph for talk time seconds
    composeTestRule.onNodeWithTag("talkTimeSecGraph").assertIsDisplayed()

    // Check mean text is displayed
    // The mean is calculated from the fake data in the ViewModel
    // If we used i * 10 (for i in 1..10): The mean of [10,20,...,100] is 55.0
    composeTestRule
        .onNodeWithTag("talkTimeSecMeanTitle")
        .assertIsDisplayed()
        .assert(hasText("Mean: ${talkTimeSecMean}")) // Adjust if your formatting differs

    // Check Talk Time Percentage Title
    composeTestRule
        .onNodeWithTag("talkTimePercTitle")
        .assertIsDisplayed()
        .assert(hasText("Talk Time Percentage:"))

    // Check the second graph for talk time percentage
    composeTestRule.onNodeWithTag("talkTimePercGraph").assertIsDisplayed()

    // For percentages (i * 5 for i in 1..10): The average would be [5,10,15,...,50] mean = 27.5
    composeTestRule
        .onNodeWithTag("talkTimePercMeanTitle")
        .assertIsDisplayed()
        .assert(hasText("Mean: ${talkTimePercMean}"))
  }
}

fun createMockedRecentData(): ArrayDeque<AnalysisData> {
  val mockedRecentData = ArrayDeque<AnalysisData>()
  for (i in 1..10) {
    mockedRecentData.addFirst(
        AnalysisData(
            transcription = "a",
            fillerWordsCount = 0,
            averagePauseDuration = 0.0,
            talkTimeSeconds = i.toDouble(),
            talkTimePercentage = i.toDouble() / 2.0,
            pace = 0))
  }
  return mockedRecentData
}
