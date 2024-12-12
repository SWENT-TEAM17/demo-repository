package com.github.se.orator.ui.navigation

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object Route {
  const val HOME = "Home"
  const val FRIENDS = "Friends"
  const val ADD_FRIENDS = "Add Friends Screen"
  const val PROFILE = "Profile"
  const val AUTH = "Auth"
  const val EDIT_PROFILE = "EditProfile"
  const val CREATE_PROFILE = "CreateProfile"
  const val SETTINGS = "Settings"
  const val SPEAKING_JOB_INTERVIEW = "SpeakingJobInterview"
  const val SPEAKING_PUBLIC_SPEAKING = "SpeakingPublicSpeaking"
  const val SPEAKING_SALES_PITCH = "SpeakingSalesPitch"
  const val SPEAKING_SCREEN = "SpeakingScreen"
  const val FEEDBACK = "Feedback"
  const val CHAT_SCREEN = "chat_screen"
  const val SPEAKING = "Speaking"
  const val OFFLINE = "Offline"
  const val PRACTICE_QUESTIONS = "PracticeQuestions"
  const val OFFLINE_RECORDING = "OfflineRecording"
  const val OFFLINE_RECORDING_REVIEW = "OfflineRecordingReview"
  const val STAT = "Statistics"
  const val BATTLE_SEND = "BattleSendScreen"
  const val BATTLE_REQUEST_SENT = "BattleRequestSentScreen"
  const val BATTLE_CHAT = "BattleChatScreen"
  const val WAITING_FOR_COMPLETION = "WaitingForCompletionScreen"
  const val EVALUATION = "EvaluationScreen"
  const val OFFLINE_INTERVIEW_MODULE = "OfflineInterviewModule"
  const val OFFLINE_RECORDING_PROFILE = "OfflineRecordingProfile"
}

object Screen {
  const val AUTH = "Auth Screen"
  const val HOME = "Home Screen"
  const val FRIENDS = "Friends Screen"
  const val ADD_FRIENDS = "Add Friends Screen"
  const val PROFILE = "Profile Screen"
  const val EDIT_PROFILE = "EditProfile Screen"
  const val CREATE_PROFILE = "CreateProfile Screen"
  const val SETTINGS = "Settings Screen"
  const val SPEAKING_JOB_INTERVIEW = "SpeakingJobInterview Screen"
  const val SPEAKING_PUBLIC_SPEAKING = "SpeakingPublicSpeaking Screen"
  const val SPEAKING_SALES_PITCH = "SpeakingSalesPitch Screen"
  const val SPEAKING_SCREEN = "SpeakingScreen"
  const val PRACTICE_SCREEN = "ViewPracticeScreen"
  const val LEADERBOARD = "LeaderBoard Screen"
  const val FEEDBACK = "Feedback Screen"
  const val CHAT_SCREEN = "chat_screen"
  const val FEEDBACK_SCREEN = "feedback_screen" // this one is the offline mode one !!!!
  const val SPEAKING = "Speaking"
  const val PRACTICE_QUESTIONS_SCREEN = "PracticeQuestions Screen"
  const val OFFLINE = "Offline Screen"
  const val OFFLINE_RECORDING_SCREEN = "OfflineRecording Screen"
  const val OFFLINE_RECORDING_REVIEW_SCREEN = "OfflineRecordingReview Screen"

  const val STAT = "Statistics screen"
  const val BATTLE_SEND_SCREEN = "Battle Send Screen"
  const val BATTLE_REQUEST_SENT_SCREEN = "Battle Request Sent Screen"
  const val BATTLE_CHAT_SCREEN = "Battle Chat Screen"
  const val WAITING_FOR_COMPLETION_SCREEN = "Waiting for Completion Screen"
  const val EVALUATION_SCREEN = "Evaluation Waiting Screen"
  const val OFFLINE_INTERVIEW_MODULE = "OfflineInterviewModule"
  const val OFFLINE_RECORDING_PROFILE = "OfflineRecordingProfile"
}

data class TopLevelDestination(
    val route: String,
    val outlinedIcon: ImageVector,
    val coloredIcon: ImageVector,
    val textId: String
)

object TopLevelDestinations {
  val HOME =
      TopLevelDestination(
          route = Route.HOME,
          outlinedIcon = Icons.Outlined.Home,
          coloredIcon = Icons.Filled.Home,
          textId = "Home")
  val FRIENDS =
      TopLevelDestination(
          route = Route.FRIENDS,
          outlinedIcon = Icons.Outlined.StarOutline,
          coloredIcon = Icons.Filled.Star,
          textId = "Friends")
  val PROFILE =
      TopLevelDestination(
          route = Route.PROFILE,
          outlinedIcon = Icons.Outlined.Person,
          coloredIcon = Icons.Filled.Person,
          textId = "Profile")
}

val LIST_TOP_LEVEL_DESTINATION =
    listOf(TopLevelDestinations.HOME, TopLevelDestinations.FRIENDS, TopLevelDestinations.PROFILE)

open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified [TopLevelDestination]
   *
   * @param destination The top level destination to navigate to Clear the back stack when
   *   navigating to a new destination This is useful when navigating to a new screen from the
   *   bottom navigation bar as we don't want to keep the previous screen in the back stack
   */
  open fun navigateTo(destination: TopLevelDestination) {

    navController.navigate(destination.route) {
      Log.d("inside of navigationActions!", "route is: ${destination.route}")
      // Pop up to the start destination of the graph to
      // avoid building up a large stack of destinations
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
        inclusive = true
      }

      // Avoid multiple copies of the same destination when reselecting same item
      launchSingleTop = true

      // Restore state when reselecting a previously selected item
      if (destination.route != Route.AUTH) {
        restoreState = true
      }
    }
  }

  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: String) {
    navController.navigate(screen)
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    Log.d("inside nav actions", "currentRoute called, returned ${navController.currentDestination}")
    return navController.currentDestination?.route ?: ""
  }
  // Lambda function to navigate to OfflineRecordingScreen with a selected question.
  // It constructs the navigation route by embedding the question text as a parameter
  val navigateToOfflineRecording: (String) -> Unit = { question ->
    navController.navigate("offline_recording/${question}")
  }

  // Wrapper function for navigating to OfflineRecordingScreen, allowing the lambda function to be
  // called directly. This wrapper is open so it can be overridden, making it suitable for mocking
  // and verifying in tests.
  open fun goToOfflineRecording(question: String) {
    navigateToOfflineRecording(question)
  }

  /**
   * Navigate to the Battle Screen, passing the friend's UID as an argument.
   *
   * @param friendUid The UID of the friend to battle with.
   */
  open fun navigateToSendBattleScreen(friendUid: String) {
    navController.navigate("${Route.BATTLE_SEND}/$friendUid")
  }

  /**
   * Navigate to the Battle Request Sent Screen, passing the friend's UID as an argument.
   *
   * @param friendUid The UID of the friend.
   * @param battleId The ID of the battle.
   */
  open fun navigateToBattleRequestSentScreen(friendUid: String, battleId: String) {
    navController.navigate("${Route.BATTLE_REQUEST_SENT}/$battleId/$friendUid")
  }

  /**
   * Navigate to the Battle Screen, passing the friend's UID as an argument.
   *
   * @param battleId The ID of the battle.
   */
  open fun navigateToBattleScreen(battleId: String, friendUid: String) {
    navController.navigate("${Route.BATTLE_CHAT}/$battleId/$friendUid")
  }

  /**
   * Navigate to the Waiting for Completion Screen, passing the battle ID as an argument.
   *
   * @param battleId The ID of the battle.
   */
  open fun navigateToWaitingForCompletion(battleId: String) {
    navController.navigate("${Route.WAITING_FOR_COMPLETION}/$battleId")
  }

  /**
   * Navigate to the Evaluation Screen, passing the battle ID as an argument.
   *
   * @param battleId The ID of the battle.
   */
  open fun navigateToEvaluationScreen(battleId: String) {
    navController.navigate("${Route.EVALUATION}/$battleId")
  }
}
