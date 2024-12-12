package com.github.se.orator.ui.settings

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.se.orator.model.profile.UserProfileRepository
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.theme.AppThemeViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify

class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navigationActions: NavigationActions
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var userProfileViewModel: UserProfileViewModel

  @Before
  fun setUp() {
    navigationActions = mock(NavigationActions::class.java)
    userProfileRepository = mock(UserProfileRepository::class.java)
    userProfileViewModel = UserProfileViewModel(userProfileRepository)

    `when`(navigationActions.currentRoute()).thenReturn(Screen.HOME)
  }

  @Test
  fun testBackButtonExistsAndClickable() {
    composeTestRule.setContent {
      SettingsScreen(
          navigationActions = navigationActions, userProfileViewModel = userProfileViewModel)
    }

    composeTestRule.onNodeWithTag("back_button").assertExists()
    composeTestRule.onNodeWithTag("back_button").performClick()
    verify(navigationActions).goBack() // Verify navigation back action
  }

  @Test
  fun testSettingsButtonsExist() {
    composeTestRule.setContent {
      SettingsScreen(
          navigationActions = navigationActions, userProfileViewModel = userProfileViewModel)
    }

    // Test that each setting button exists and is clickable
    val settingsTags = listOf("theme", "about")

    settingsTags.forEach { tag ->
      composeTestRule.onNodeWithTag(tag).assertExists()
      composeTestRule.onNodeWithTag(tag).performClick()
    }

    // Handle the permission button on its own as clicking it will go to the device settings
    composeTestRule.onNodeWithTag("permissions").assertExists()
    composeTestRule.onNodeWithTag("permissions").assertHasClickAction()
  }

  @Test
  fun clickOnThemeButtonTriggersThemeSwitch() {
    var appThemeViewModel: AppThemeViewModel? = null

    composeTestRule.setContent {
      appThemeViewModel = AppThemeViewModel(LocalContext.current)
      appThemeViewModel?.saveTheme(false)
      SettingsScreen(
          navigationActions = navigationActions,
          userProfileViewModel = userProfileViewModel,
          themeViewModel = appThemeViewModel)
    }

    assert(appThemeViewModel?.isDark?.value == false)
    composeTestRule.onNodeWithTag("theme").performClick()
    assert(appThemeViewModel?.isDark?.value == true)
  }

  @Test
  fun noThemeViewModelDoesNotCauseACrash() {
    composeTestRule.setContent {
      SettingsScreen(
          navigationActions = navigationActions, userProfileViewModel = userProfileViewModel)
    }

    composeTestRule.onNodeWithTag("theme").performClick()
  }
}
