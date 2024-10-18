package com.github.se.orator.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.github.se.orator.model.profile.UserProfileRepositoryFirestore
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.ui.friends.ViewFriendsScreen
import com.github.se.orator.ui.theme.mainScreen.MainScreen
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class NavigationUITest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun correctNavigationFunctionsAreCalled() {
    val navigationActions = mock(NavigationActions::class.java)
    `when`(navigationActions.currentRoute()).thenReturn(Route.HOME)

    composeTestRule.setContent { MainScreen(navigationActions) }

    composeTestRule.onNodeWithTag("Friends").performClick()
    verify(navigationActions).navigateTo(eq(TopLevelDestinations.FRIENDS))
    composeTestRule.onNodeWithTag("Profile").performClick()
    verify(navigationActions).navigateTo(eq(TopLevelDestinations.PROFILE))
  }

  /**
   * This tests simulate the navigation from the home screen to the friends screen, As it can be
   * done by users.
   */
  @Test
  fun navigationIsDoneProperly() {
    var navigationActions: NavigationActions? = null
    composeTestRule.setContent {
      val navController = rememberNavController()
      navigationActions = NavigationActions(navController)

      NavHost(navController = navController, startDestination = Route.HOME) {
        navigation(
            startDestination = Screen.HOME,
            route = Route.HOME,
        ) {
          composable(Screen.HOME) { MainScreen(navigationActions!!) }
        }

        navigation(
            startDestination = Screen.FRIENDS,
            route = Route.FRIENDS,
        ) {
          composable(Screen.FRIENDS) {
            ViewFriendsScreen(
                navigationActions!!,
                UserProfileViewModel(
                    UserProfileRepositoryFirestore(mock(FirebaseFirestore::class.java))))
          }
        }
      }
      navigationActions!!.navigateTo(TopLevelDestinations.HOME)
    }

    composeTestRule.onNodeWithTag("Friends").performClick()

    assert(navigationActions!!.currentRoute() == Screen.FRIENDS)
  }
}
