package com.github.se.orator

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.symblAi.SpeakingRepository
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.network.NetworkConnectivityObserver
import com.github.se.orator.network.ConnectivityObserver
import com.github.se.orator.ui.authentification.SignInScreen
import com.github.se.orator.ui.friends.AddFriendsScreen
import com.github.se.orator.ui.friends.LeaderboardScreen
import com.github.se.orator.ui.friends.ViewFriendsScreen
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Route
import com.github.se.orator.ui.navigation.Screen
import com.github.se.orator.ui.network.ChatGPTService
import com.github.se.orator.ui.network.createChatGPTService
import com.github.se.orator.ui.overview.ChatScreen
import com.github.se.orator.ui.overview.FeedbackScreen
import com.github.se.orator.ui.overview.SpeakingJobInterviewModule
import com.github.se.orator.ui.overview.SpeakingPublicSpeaking
import com.github.se.orator.ui.overview.SpeakingSalesPitchModule
import com.github.se.orator.ui.profile.CreateAccountScreen
import com.github.se.orator.ui.profile.EditProfileScreen
import com.github.se.orator.ui.profile.ProfileScreen
import com.github.se.orator.ui.screens.ViewConnectScreen
import com.github.se.orator.ui.screens.ViewFunScreen
import com.github.se.orator.ui.settings.SettingsScreen
import com.github.se.orator.ui.speaking.SpeakingScreen
import com.github.se.orator.ui.theme.ProjectTheme
import com.github.se.orator.ui.theme.mainScreen.MainScreen
import com.google.firebase.auth.FirebaseAuth

/** The MainActivity class is the main entry point for the OratorAI application. */

class MainActivity : ComponentActivity() {
  private lateinit var auth: FirebaseAuth
  private lateinit var networkConnectivityObserver: NetworkConnectivityObserver

  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize Firebase Auth
    auth = FirebaseAuth.getInstance()
    auth.currentUser?.let { auth.signOut() }

    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    val apiKey = appInfo.metaData.getString("GPT_API_KEY")
    val organizationId = appInfo.metaData.getString("GPT_ORGANIZATION_ID")
    requireNotNull(apiKey) { "GPT API Key is missing in the manifest" }
    requireNotNull(organizationId) { "GPT Organization ID is missing in the manifest" }

    val chatGPTService = createChatGPTService(apiKey, organizationId)

    // Initialize Connectivity Observer
    networkConnectivityObserver = NetworkConnectivityObserver(applicationContext)

    // Enable edge-to-edge display for modern UI layout
    enableEdgeToEdge()
    setContent {
      ProjectTheme {
        // Observe network connectivity status and update UI accordingly
        val networkStatus by networkConnectivityObserver.observe().collectAsState(
          initial = ConnectivityObserver.Status.Unavailable
        )
        Scaffold(modifier = Modifier.fillMaxSize()) {
          // Pass the connectivity status to OratorApp to handle network-based UI updates
          OratorApp(chatGPTService, networkStatus)
        }
      }
    }
  }
}

/**
 * The OratorApp composable is the main entry point for the OratorAI application.
 *
 * @param chatGPTService The ChatGPTService instance used for chat conversations.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OratorApp(chatGPTService: ChatGPTService, networkStatus: ConnectivityObserver.Status) {
  // Set up the navigation controller and actions
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)

  // Main layout using a Scaffold
  Scaffold(modifier = Modifier.fillMaxSize()) {
    // Check network status to adjust the UI accordingly
    if (networkStatus == ConnectivityObserver.Status.Unavailable) {
      // For now, display network status message if offline --> later it will be the full offline logic
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "Network status: $networkStatus")
      }
    } else {
      // Initialize necessary view models when network is available
      val userProfileViewModel: UserProfileViewModel = viewModel(factory = UserProfileViewModel.Factory)
      val apiLinkViewModel = ApiLinkViewModel()
      val speakingViewModel = SpeakingViewModel(SpeakingRepository(LocalContext.current), apiLinkViewModel)
      val chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

      // Replace the content of the Scaffold with the desired screen
      NavHost(navController = navController, startDestination = Route.AUTH) {
        navigation(
          startDestination = Screen.AUTH,
          route = Route.AUTH,
        ) {
          // Authentication Flow
          composable(Screen.AUTH) { SignInScreen(navigationActions, userProfileViewModel) }

          // Profile Creation Flow
          composable(Screen.CREATE_PROFILE) { CreateAccountScreen(navigationActions, userProfileViewModel) }
        }

        navigation(
          startDestination = Screen.HOME,
          route = Route.HOME,
        ) {
          composable(Screen.HOME) { MainScreen(navigationActions) }
          composable(Screen.SPEAKING_JOB_INTERVIEW) { SpeakingJobInterviewModule(navigationActions, apiLinkViewModel) }
          composable(Screen.SPEAKING_PUBLIC_SPEAKING) { SpeakingPublicSpeaking(navigationActions, apiLinkViewModel) }
          composable(Screen.SPEAKING_SALES_PITCH) { SpeakingSalesPitchModule(navigationActions, apiLinkViewModel) }
          composable(Screen.SPEAKING) { SpeakingScreen(navigationActions, speakingViewModel) }
          composable(Screen.CHAT_SCREEN) { ChatScreen(navigationActions = navigationActions, chatViewModel = chatViewModel) }
          composable(Screen.FEEDBACK) {
            FeedbackScreen(chatViewModel = chatViewModel, navController = navController, navigationActions = navigationActions)
          }
        }

        navigation(
          startDestination = Screen.FRIENDS,
          route = Route.FRIENDS,
        ) {
          composable(Screen.FRIENDS) { ViewFriendsScreen(navigationActions, userProfileViewModel) }
        }

        composable(Screen.FUN_SCREEN) { ViewFunScreen(navigationActions, userProfileViewModel) }
        composable(Screen.CONNECT_SCREEN) { ViewConnectScreen(navigationActions, userProfileViewModel) }

        navigation(startDestination = Screen.CREATE_PROFILE, route = Route.CREATE_PROFILE) {
          composable(Screen.CREATE_PROFILE) { CreateAccountScreen(navigationActions, userProfileViewModel) }
        }

        navigation(startDestination = Screen.EDIT_PROFILE, route = Route.EDIT_PROFILE) {
          composable(Screen.EDIT_PROFILE) { EditProfileScreen(navigationActions, userProfileViewModel) }
        }

        navigation(
          startDestination = Screen.PROFILE,
          route = Route.PROFILE,
        ) {
          composable(Screen.PROFILE) { ProfileScreen(navigationActions, userProfileViewModel) }
          composable(Screen.LEADERBOARD) { LeaderboardScreen(navigationActions, userProfileViewModel) }
          composable(Screen.ADD_FRIENDS) { AddFriendsScreen(navigationActions, userProfileViewModel) }
          composable(Screen.SETTINGS) { SettingsScreen(navigationActions, userProfileViewModel) }
        }
      }
    }
  }
}
