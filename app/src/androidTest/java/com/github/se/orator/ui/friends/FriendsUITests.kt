package com.github.se.orator.ui.friends

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.profile.UserStatistics
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class FriendsUITests {

  @get:Rule val composeTestRule = createComposeRule()

  // Friends List
  private val profile1 = UserProfile("1", "John Doe", 99, statistics = UserStatistics())
  private val profile2 = UserProfile("2", "Jane Doe", 100, statistics = UserStatistics())

  @Mock private lateinit var mockNavigationActions: NavigationActions
  @Mock private lateinit var mockUserProfileRepository: UserProfileRepository
  private lateinit var userProfileViewModel: UserProfileViewModel

  private val testProfile = UserProfile("1", "John Doe", 99, statistics = UserStatistics())

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    // When getUserProfiles is called on the repository, a custom list of profiles is passed.
    `when`(mockUserProfileRepository.getFriendsProfiles(any(), any(), any())).then {
      it.getArgument<(List<UserProfile>) -> Unit>(1)(listOf(profile1, profile2))
    }
    `when`(mockUserProfileRepository.getUserProfile(any(), any(), any())).then {
      it.getArgument<(UserProfile) -> Unit>(1)(testProfile)
    }
    `when`(mockUserProfileRepository.getAllUserProfiles(any(), any())).then {
      it.getArgument<(List<UserProfile>) -> Unit>(0)(listOf(profile1, profile2))
    }

    userProfileViewModel = UserProfileViewModel(mockUserProfileRepository)
    userProfileViewModel.getUserProfile(testProfile.uid)
  }


  /**
   * Tests that the action button works correctly on the View Friends screen.
   * It verifies that the menu button exists, is displayed, and can be clicked.
   * After clicking, it checks that the drawer menu and its elements are displayed.
   */
  @Test
  fun testActionButtonWorks() {
    viewFriendsTestsSetup()

    composeTestRule
        .onNodeWithTag("viewFriendsMenuButton")
        .assertExists("The menu button on the view friends screen does not exist")
        .assertIsDisplayed()
        .performClick()

    composeTestRule.onNodeWithTag("viewFriendsDrawerMenu").assertIsDisplayed()
    composeTestRule.onNodeWithTag("viewFriendsDrawerTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("viewFriendsAddFriendButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("viewFriendsLeaderboardButton").assertIsDisplayed()
  }

  /**
   * Tests that the friends list is displayed correctly on the View Friends screen.
   * It verifies that the friends list exists and is displayed.
   * It also checks that the first friend item is displayed.
   */
  @Test
  fun testFriendsListIsDisplayed() {
    viewFriendsTestsSetup()

    composeTestRule.onNodeWithTag("viewFriendsList", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("viewFriendsItem#1", useUnmergedTree = true).assertIsDisplayed()
  }


  /**
   * Tests that the friends list is displayed correctly on the View Friends screen.
   * It verifies that the friends list exists and is displayed.
   * It also checks that the first friend item is displayed.
   */
  @Test
  fun testCanGoToAddFriendAndLeaderboardScreens() {
    viewFriendsTestsSetup()

    composeTestRule.onNodeWithTag("viewFriendsMenuButton").performClick()

    composeTestRule
        .onNodeWithTag("viewFriendsAddFriendButton", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    verify(mockNavigationActions).navigateTo(eq(Screen.ADD_FRIENDS))

    composeTestRule
        .onNodeWithTag("viewFriendsLeaderboardButton", useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    verify(mockNavigationActions).navigateTo(eq(Screen.LEADERBOARD))
  }

  /**
   * Tests that the search functionality works correctly on the View Friends screen.
   * It verifies that the search field exists and is displayed, can be clicked and text
   * can be inputted. After inputting text, it checks that the first friend item is displayed
   * and the second is not.
   */
  @Test
  fun testCanSearchForFriends() {
    viewFriendsTestsSetup()

    composeTestRule.onNodeWithTag("viewFriendsSearch").performClick()
    composeTestRule.onNodeWithTag("viewFriendsSearch").performTextInput("John")

    composeTestRule.onNodeWithTag("viewFriendsItem#1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("viewFriendsItem#2", useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  /**
   * Tests that the friends list is displayed correctly on the View Friends screen.
   * It also checks that the first friend item is displayed.
   */
  @Test
  fun testAddFriendsScreenElementsAreDisplayed() {

    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.ADD_FRIENDS)
    composeTestRule.setContent { AddFriendsScreen(mockNavigationActions, userProfileViewModel) }

    // Check if the add friend screen elements are displayed
    composeTestRule.onNodeWithTag("addFriendTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addFriendSearchField").assertIsDisplayed()
  }


  /**
   * Tests that the search functionality works correctly on the Add Friends screen.
   * After inputting text, it checks that the first friend item is displayed
   * and the second is not.
   */
  @Test
  fun testAddFriendSearch() {
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.ADD_FRIENDS)
    composeTestRule.setContent { AddFriendsScreen(mockNavigationActions, userProfileViewModel) }

    composeTestRule.onNodeWithTag("addFriendSearchField").performTextInput("John")

    composeTestRule.onNodeWithTag("addFriendUserItem#1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addFriendUserItem#2").assertIsNotDisplayed()
  }

  /**
   * Tests that the leaderboard screen elements are displayed correctly.
   */
  @Test
  fun testLeaderboardScreenElementsAreDisplayed() {
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.LEADERBOARD)
    composeTestRule.setContent { LeaderboardScreen(mockNavigationActions, userProfileViewModel) }

    // Check if the leaderboard screen elements are displayed
    composeTestRule.onNodeWithTag("leaderboardTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("leaderboardList").assertIsDisplayed()
  }

    /**
     * Tests that the leaderboard item elements are displayed correctly.
     */
  @Test
  fun testLeaderboardItemElementsAreDisplayed() {
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.LEADERBOARD)
    composeTestRule.setContent { LeaderboardScreen(mockNavigationActions, userProfileViewModel) }

    // Check if the leaderboard item elements are displayed
    composeTestRule.onNodeWithTag("leaderboardItem#1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("leaderboardItem#2", useUnmergedTree = true).assertIsDisplayed()
  }

  /** Function used to setup a testing environment for the ViewFriendsScreen tests */
  private fun viewFriendsTestsSetup() {
    `when`(mockNavigationActions.currentRoute()).thenReturn(Screen.FRIENDS)
    composeTestRule.setContent { ViewFriendsScreen(mockNavigationActions, userProfileViewModel) }
  }
}
