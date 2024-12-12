package com.github.se.orator.endtoend

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.profile.UserStatistics
import com.github.se.orator.model.theme.AppThemeViewModel
import com.github.se.orator.ui.friends.AddFriendsScreen
import com.github.se.orator.ui.friends.LeaderboardScreen
import com.github.se.orator.ui.friends.ViewFriendsScreen
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import com.github.se.orator.ui.profile.CreateAccountScreen
import com.github.se.orator.ui.profile.EditProfileScreen
import com.github.se.orator.ui.profile.ProfileScreen
import com.github.se.orator.ui.settings.SettingsScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.verify

class EndToEndAppTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var userProfileViewModel: UserProfileViewModel
  private lateinit var mockThemeContext: Context
  private lateinit var appThemeViewModel: AppThemeViewModel
  private lateinit var mockSharedPreferences: SharedPreferences
  private lateinit var mockEditor: SharedPreferences.Editor
  private val bio = "Test bio"
  private val testUserProfile =
      UserProfile(uid = "testUid", name = "", age = 25, statistics = UserStatistics(), bio = bio)

  private val profile1 = UserProfile("1", "John Doe", 99, statistics = UserStatistics())
  private val profile2 = UserProfile("2", "Jane Doe", 100, statistics = UserStatistics())

  private val screenList =
      listOf(
          Screen.HOME,
          Screen.SETTINGS,
          Screen.EDIT_PROFILE,
          Screen.ADD_FRIENDS,
          Screen.FRIENDS,
          Screen.LEADERBOARD,
          Screen.CREATE_PROFILE)

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    userProfileRepository = mock(UserProfileRepository::class.java)
    userProfileViewModel = UserProfileViewModel(userProfileRepository)
    userProfileViewModel = UserProfileViewModel(userProfileRepository)
    mockSharedPreferences = mock(SharedPreferences::class.java)
    mockThemeContext = mock(Context::class.java)
    mockEditor = mock(SharedPreferences.Editor::class.java)
    appThemeViewModel = AppThemeViewModel(mockThemeContext)

    `when`(
            mockThemeContext.getSharedPreferences(
                org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .thenReturn(mockSharedPreferences)
    `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putBoolean(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .thenReturn(mockEditor)

    // mocking the request for the user who is using the app
    `when`(
            userProfileRepository.getUserProfile(
                org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .then { it.getArgument<(UserProfile) -> Unit>(1)(testUserProfile) }

    `when`(
            userProfileRepository.getFriendsProfiles(
                org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .then { it.getArgument<(List<UserProfile>) -> Unit>(1)(listOf(profile1, profile2)) }

    `when`(
            userProfileRepository.getAllUserProfiles(
                org.mockito.kotlin.any(), org.mockito.kotlin.any()))
        .then { it.getArgument<(List<UserProfile>) -> Unit>(0)(listOf(profile1, profile2)) }

    // mocking navigation functions
    `when`(navigationActions.currentRoute()).thenReturn(Screen.FRIENDS)
    `when`(navigationActions.goBack()).then { navController?.popBackStack() ?: {} }

    for (screen in screenList) {
      `when`(navigationActions.navigateTo(screen)).then { navController?.navigate(screen) }
    }

    userProfileViewModel = UserProfileViewModel(userProfileRepository)
    userProfileViewModel.getUserProfile(testUserProfile.uid)
  }

  var navController: NavHostController? = null

  @Test
  fun testEndToEndNavigationAndUI() {
    // Set up NavHost for navigation and initialize different screens within one setContent
    composeTestRule.setContent {
      navController = rememberNavController()

      NavHost(navController = navController!!, startDestination = Screen.HOME) {
        composable(Screen.HOME) { ProfileScreen(navigationActions, userProfileViewModel) }
        composable(Screen.SETTINGS) {
          SettingsScreen(navigationActions, userProfileViewModel, appThemeViewModel)
        }
        composable(Screen.FRIENDS) { ViewFriendsScreen(navigationActions, userProfileViewModel) }
        composable(Screen.ADD_FRIENDS) { AddFriendsScreen(navigationActions, userProfileViewModel) }
        composable(Screen.EDIT_PROFILE) {
          EditProfileScreen(navigationActions, userProfileViewModel)
        }
        composable(Screen.CREATE_PROFILE) {
          CreateAccountScreen(navigationActions, userProfileViewModel)
        }
        composable(Screen.PROFILE) { ProfileScreen(navigationActions, userProfileViewModel) }
        composable(Screen.LEADERBOARD) {
          LeaderboardScreen(navigationActions, userProfileViewModel)
        }
      }
    }
    // manually going to create profile to simulate what a new user would go through
    // cannot have a sign in screen then a create profile screen since mocking the google
    // auth response would take too long, so we manually go to create profile, see if it works
    // then go back.

    composeTestRule.runOnUiThread {
      navController?.navigate(
          Screen.CREATE_PROFILE) // this here forces us to navigate to the create_profile screen
    }
    composeTestRule.onNodeWithTag("save_profile_button").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("save_profile_button")
        .assertIsNotEnabled() // clicking save profile without a username to see if the button is
    // enabled (it shouldn't be)

    composeTestRule
        .onNodeWithTag("username_input")
        .performTextInput("TestUser") // add a username then the button should be enabled
    composeTestRule
        .onNodeWithTag("username_input")
        .assertTextContains("TestUser") // making sure username is shown correctly
    composeTestRule
        .onNodeWithTag("save_profile_button")
        .assertIsEnabled() // now the save profile button should be enabled
    composeTestRule.runOnUiThread {
      navController?.navigate(
          Screen.HOME) // forcing back to home where the user would be once he finishes creating his
      // account
    }

    // navigating to profile
    composeTestRule.onNodeWithTag("Profile").performClick()
    composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Sign out").assertIsDisplayed()
    composeTestRule.onNodeWithTag("edit_button").assertExists()
    composeTestRule.onNodeWithTag("statistics_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("previous_sessions_section").assertIsDisplayed()
    // go to settings
    composeTestRule
        .onNodeWithContentDescription("Settings")
        .performClick() // Simulate click on Settings button
    verify(navigationActions).navigateTo(Screen.SETTINGS)

    // test that each setting button exists and is clickable
    val settingsTags = listOf("theme", "about")

    settingsTags.forEach { tag ->
      composeTestRule.onNodeWithTag(tag).assertExists()
      composeTestRule.onNodeWithTag(tag).performClick()
    }
    // Handle the permission button on its own as clicking it will go to the device settings
    composeTestRule.onNodeWithTag("permissions").assertExists()
    composeTestRule.onNodeWithTag("permissions").assertHasClickAction()

    // testing that the theme switch is triggered
    verify(mockSharedPreferences).edit()
    verify(mockEditor).putBoolean(org.mockito.kotlin.any(), org.mockito.kotlin.any())

    // go back to profile and test the features there
    composeTestRule.onNodeWithTag("back_button").performClick()
    verify(navigationActions).goBack() // ensure back button brings user back to profile
    clearInvocations(navigationActions)

    // navigate to edit profile
    composeTestRule.onNodeWithTag("edit_button").performClick()
    verify(navigationActions).navigateTo(Screen.EDIT_PROFILE)
    composeTestRule.onNodeWithTag("back_button", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_button", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("username_field").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bio_field").assertIsDisplayed()
    composeTestRule.onNodeWithText("Save changes").assertIsDisplayed()

    // testing that adding text to username and bio is shown fine on screen
    composeTestRule.onNodeWithTag("username_field").performTextInput("TestName")
    composeTestRule.onNodeWithTag("username_field").assertTextContains("TestName")
    composeTestRule.onNodeWithTag("bio_field").assertTextContains(bio)
    composeTestRule.onNodeWithTag("bio_field").performTextInput("adding text ")

    // making sure adding profile pictures works as expected
    composeTestRule.onNodeWithTag("upload_profile_picture_button").performClick()
    composeTestRule.onNodeWithText("Choose Profile Picture").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.onNodeWithText("Choose Profile Picture").assertIsNotDisplayed()

    // go back to profile screen
    composeTestRule.onNodeWithTag("back_button").performClick()
    verify(navigationActions).goBack()
    clearInvocations(navigationActions)

    // navigate from profile to friends
    composeTestRule.onNodeWithTag("Friends").performClick()
    composeTestRule.runOnUiThread { navController?.navigate(Screen.FRIENDS) }

    composeTestRule.onNodeWithTag("viewFriendsMenuButton").performClick()
    composeTestRule.onNodeWithTag("viewFriendsLeaderboardButton").performClick()
    composeTestRule.onNodeWithTag("leaderboardTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("leaderboardBackButton", useUnmergedTree = true).performClick()
    verify(navigationActions).goBack()
    clearInvocations(navigationActions)

    composeTestRule.onNodeWithTag("viewFriendsAddFriendButton").performClick()
    // Check if the add friend screen elements are displayed
    composeTestRule.onNodeWithTag("addFriendTitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("addFriendSearchField").assertIsDisplayed()

    composeTestRule.onNodeWithTag("addFriendBackButton", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("viewFriendsSearch").performClick()
    composeTestRule.onNodeWithTag("viewFriendsSearch").performTextInput("John")

    composeTestRule.onNodeWithTag("viewFriendsItem#1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("viewFriendsItem#2", useUnmergedTree = true)
        .assertIsNotDisplayed()

    verify(navigationActions).goBack()
    clearInvocations(navigationActions)

    // Step 7: Navigate back to Home from Friends
    composeTestRule.onNodeWithTag("Home").performClick() // Simulate clicking Home button
  }
}
