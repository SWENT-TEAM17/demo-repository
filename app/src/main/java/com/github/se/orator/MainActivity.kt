package com.github.se.orator

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.speechBattle.BattleViewModel
import com.github.se.orator.model.speechBattle.BattleViewModelFactory
import com.github.se.orator.model.symblAi.SpeakingRepositoryRecord
import com.github.se.orator.model.symblAi.SpeakingViewModel
import com.github.se.orator.model.theme.AppThemeViewModel
import com.github.se.orator.network.NetworkConnectivityObserver
import com.github.se.orator.network.OfflineViewModel
import com.github.se.orator.ui.authentification.SignInScreen
import com.github.se.orator.ui.battle.BattleChatScreen
import com.github.se.orator.ui.battle.BattleRequestSentScreen
import com.github.se.orator.ui.battle.BattleScreen
import com.github.se.orator.ui.battle.EvaluationScreen
import com.github.se.orator.ui.battle.WaitingForCompletionScreen
import com.github.se.orator.ui.friends.AddFriendsScreen
import com.github.se.orator.ui.friends.LeaderboardScreen
import com.github.se.orator.ui.friends.ViewFriendsScreen
import com.github.se.orator.ui.mainScreen.MainScreen
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Route
import com.github.se.orator.ui.navigation.Screen
import com.github.se.orator.ui.network.ChatGPTService
import com.github.se.orator.ui.network.createChatGPTService
import com.github.se.orator.ui.offline.OfflinePracticeQuestionsScreen
import com.github.se.orator.ui.offline.OfflineRecordingScreen
import com.github.se.orator.ui.offline.OfflineScreen
import com.github.se.orator.ui.offline.RecordingReviewScreen
import com.github.se.orator.ui.overview.ChatScreen
import com.github.se.orator.ui.overview.FeedbackScreen
import com.github.se.orator.ui.overview.OfflineInterviewModule
import com.github.se.orator.ui.overview.SpeakingJobInterviewModule
import com.github.se.orator.ui.overview.SpeakingPublicSpeakingModule
import com.github.se.orator.ui.overview.SpeakingSalesPitchModule
import com.github.se.orator.ui.profile.CreateAccountScreen
import com.github.se.orator.ui.profile.EditProfileScreen
import com.github.se.orator.ui.profile.GraphStats
import com.github.se.orator.ui.profile.OfflineRecordingsProfileScreen
import com.github.se.orator.ui.profile.PreviousRecordingsFeedbackScreen
import com.github.se.orator.ui.profile.ProfileScreen
import com.github.se.orator.ui.settings.SettingsScreen
import com.github.se.orator.ui.speaking.SpeakingScreen
import com.github.se.orator.ui.theme.ProjectTheme
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private lateinit var auth: FirebaseAuth
  lateinit var textToSpeech: TextToSpeech
  private lateinit var networkConnectivityObserver: NetworkConnectivityObserver
  private val offlineViewModel: OfflineViewModel by viewModels() // Initialize the OfflineViewModel

  private val themeViewModel: AppThemeViewModel = AppThemeViewModel(this)

  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize Firebase Auth
    auth = FirebaseAuth.getInstance()
    auth.currentUser?.let { auth.signOut() }

    // Retrieve API key and Organization ID from manifest metadata
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    val apiKey = appInfo.metaData.getString("GPT_API_KEY")
    val organizationId = appInfo.metaData.getString("GPT_ORGANIZATION_ID")

    requireNotNull(apiKey) { "GPT API Key is missing in the manifest" }
    requireNotNull(organizationId) { "GPT Organization ID is missing in the manifest" }

    // Create ChatGPT service instance
    val chatGPTService = createChatGPTService(apiKey, organizationId)

    // Initialize NetworkChangeReceiver and register it
    networkConnectivityObserver = NetworkConnectivityObserver()
    val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    registerReceiver(networkConnectivityObserver, filter)

    // Observe network status updates and update offline mode in ViewModel
    lifecycleScope.launch {
      NetworkConnectivityObserver.isNetworkAvailable.collect { isConnected ->
        offlineViewModel.setOfflineMode(!isConnected) // Update offline mode status in ViewModel
      }
    }

    textToSpeech =
        TextToSpeech(this) { status ->
          if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setSpeechRate(1.7f)
            textToSpeech.language = Locale.UK
          }
        }

    enableEdgeToEdge()
    setContent {
      ProjectTheme(themeViewModel = themeViewModel) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("mainActivityScaffold") // Tag for testing
            ) {
              // Observe offline mode state
              val isOffline by offlineViewModel.isOffline.observeAsState(false)
              OratorApp(chatGPTService, isOffline, themeViewModel, textToSpeech)
            }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // Unregister NetworkChangeReceiver to prevent leaks
    unregisterReceiver(networkConnectivityObserver) // Unregister receiver to prevent leaks
  }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OratorApp(
    chatGPTService: ChatGPTService,
    isOffline: Boolean,
    themeViewModel: AppThemeViewModel? = null,
    textToSpeech: TextToSpeech? = null
) {
  // Create NavController for navigation within the app
  val navController = rememberNavController()
  // Initialize NavigationActions to handle navigation events
  val navigationActions = NavigationActions(navController)
  val context = LocalContext.current

  // Initialize required ViewModels using ViewModel.compose APIs
  val userProfileViewModel: UserProfileViewModel = viewModel(factory = UserProfileViewModel.Factory)
  val apiLinkViewModel = ApiLinkViewModel()
  val speakingViewModel =
      SpeakingViewModel(SpeakingRepositoryRecord(context), apiLinkViewModel, userProfileViewModel)
  val chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel, textToSpeech)

  // Initialize BattleViewModel using the factory
  val battleViewModel: BattleViewModel =
      viewModel(
          factory =
              BattleViewModelFactory(
                  userProfileViewModel = userProfileViewModel,
                  navigationActions = navigationActions,
                  apiLinkViewModel = apiLinkViewModel,
                  chatViewModel = chatViewModel))

  // Scaffold composable to provide basic layout structure for the app
  Scaffold(modifier = Modifier.fillMaxSize().testTag("oratorScaffold")) {
    // NavHost composable manages navigation between screens
    NavHost(
        navController = navController,
        startDestination = Route.AUTH,
        modifier = Modifier.testTag("navHost")) {
          // Offline flow screens
          composable(Screen.OFFLINE) { OfflineScreen(navigationActions) }
          composable(Screen.PRACTICE_QUESTIONS_SCREEN) {
            OfflinePracticeQuestionsScreen(navigationActions)
          }
          composable(Screen.OFFLINE_RECORDING_REVIEW_SCREEN) {
            RecordingReviewScreen(navigationActions, speakingViewModel)
          }
          composable(Screen.OFFLINE_RECORDING_PROFILE) {
            OfflineRecordingsProfileScreen(navigationActions, speakingViewModel)
          }
          composable(Screen.OFFLINE_INTERVIEW_MODULE) {
            OfflineInterviewModule(navigationActions, speakingViewModel)
          }

          composable(
              route = "offline_recording/{question}",
              arguments = listOf(navArgument("question") { type = NavType.StringType })) {
                  backStackEntry ->
                val question = backStackEntry.arguments?.getString("question") ?: ""
                OfflineRecordingScreen(context, navigationActions, question, speakingViewModel)
              }

          // Online/auth flow
          navigation(startDestination = Screen.AUTH, route = Route.AUTH) {
            composable(Screen.AUTH) { SignInScreen(navigationActions, userProfileViewModel) }
            composable(Screen.CREATE_PROFILE) {
              CreateAccountScreen(navigationActions, userProfileViewModel)
            }
          }

          // Main/home flow
          navigation(startDestination = Screen.HOME, route = Route.HOME) {
            composable(Screen.HOME) { MainScreen(navigationActions) }
            composable(Screen.SPEAKING_JOB_INTERVIEW) {
              SpeakingJobInterviewModule(navigationActions, chatViewModel, apiLinkViewModel)
            }
            composable(Screen.SPEAKING_PUBLIC_SPEAKING) {
              SpeakingPublicSpeakingModule(navigationActions, chatViewModel, apiLinkViewModel)
            }
            composable(Screen.SPEAKING_SALES_PITCH) {
              SpeakingSalesPitchModule(navigationActions, chatViewModel, apiLinkViewModel)
            }
            composable(Screen.SPEAKING) {
              SpeakingScreen(navigationActions, speakingViewModel, apiLinkViewModel)
            }
            composable(Screen.CHAT_SCREEN) {
              ChatScreen(navigationActions = navigationActions, chatViewModel = chatViewModel)
            }
            composable(Screen.FEEDBACK) {
              FeedbackScreen(
                  chatViewModel = chatViewModel,
                  userProfileViewModel = userProfileViewModel,
                  apiLinkViewModel = apiLinkViewModel,
                  navigationActions = navigationActions)
            }
          }

          // Friends flow
          navigation(startDestination = Screen.FRIENDS, route = Route.FRIENDS) {
            composable(Screen.FRIENDS) {
              ViewFriendsScreen(navigationActions, userProfileViewModel, battleViewModel)
            }
          }

          // Profile flow
          navigation(startDestination = Screen.PROFILE, route = Route.PROFILE) {
            composable(Screen.PROFILE) { ProfileScreen(navigationActions, userProfileViewModel) }

            composable(Screen.FEEDBACK_SCREEN) {
              PreviousRecordingsFeedbackScreen(
                  context, navigationActions, chatViewModel, speakingViewModel)
            }

            composable(Screen.EDIT_PROFILE) {
              EditProfileScreen(navigationActions, userProfileViewModel)
            }
            composable(Screen.STAT) { GraphStats(navigationActions, userProfileViewModel) }
            composable(Screen.LEADERBOARD) {
              LeaderboardScreen(navigationActions, userProfileViewModel)
            }
            composable(Screen.ADD_FRIENDS) {
              AddFriendsScreen(navigationActions, userProfileViewModel)
            }
            composable(Screen.SETTINGS) {
              SettingsScreen(navigationActions, userProfileViewModel, themeViewModel)
            }
          }

          // Battle screen integration
          composable(
              route = "${Route.BATTLE_SEND}/{friendUid}",
              arguments = listOf(navArgument("friendUid") { type = NavType.StringType })) {
                  backStackEntry ->
                val friendUid = backStackEntry.arguments?.getString("friendUid") ?: ""
                BattleScreen(
                    friendUid = friendUid,
                    userProfileViewModel = userProfileViewModel,
                    navigationActions = navigationActions,
                    battleViewModel = battleViewModel)
              }

          // Battle Request Sent Screen
          composable(
              route = "${Route.BATTLE_REQUEST_SENT}/{battleId}/{friendUid}",
              arguments =
                  listOf(
                      navArgument("battleId") { type = NavType.StringType },
                      navArgument("friendUid") { type = NavType.StringType },
                  )) { backStackEntry ->
                val friendUid = backStackEntry.arguments?.getString("friendUid") ?: ""
                val battleId = backStackEntry.arguments?.getString("battleId") ?: ""
                BattleRequestSentScreen(
                    friendUid = friendUid,
                    battleId = battleId, // Pass battleId to the screen
                    navigationActions = navigationActions,
                    userProfileViewModel = userProfileViewModel,
                    battleViewModel = battleViewModel)
              }

          // Battle Chat Screen
          composable(
              route = "${Route.BATTLE_CHAT}/{battleId}/{userId}",
              arguments =
                  listOf(
                      navArgument("battleId") { type = NavType.StringType },
                      navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
                val battleId = backStackEntry.arguments?.getString("battleId") ?: ""
                val userId = userProfileViewModel.userProfile.value?.uid ?: ""
                BattleChatScreen(
                    battleId = battleId,
                    userId = userId,
                    navigationActions = navigationActions,
                    battleViewModel = battleViewModel,
                    chatViewModel = chatViewModel,
                    userProfileViewModel = userProfileViewModel)
              }

          composable(
              route = "${Route.WAITING_FOR_COMPLETION}/{battleId}",
              arguments = listOf(navArgument("battleId") { type = NavType.StringType })) {
                  backStackEntry ->
                val battleId = backStackEntry.arguments?.getString("battleId") ?: ""
                WaitingForCompletionScreen(
                    battleId = battleId,
                    navigationActions = navigationActions,
                    battleViewModel = battleViewModel,
                    userId = userProfileViewModel.userProfile.value?.uid ?: "")
              }
          composable(
              route = "${Route.EVALUATION}/{battleId}",
              arguments = listOf(navArgument("battleId") { type = NavType.StringType })) {
                  backStackEntry ->
                val battleId = backStackEntry.arguments?.getString("battleId") ?: ""
                EvaluationScreen(battleId = battleId, navigationActions = navigationActions)
              }
        }

    // Handle transitions based on network status and ensure smooth navigation.
    LaunchedEffect(isOffline) {
      if (isOffline) {
        // Navigate to the offline screen and clear the back stack to prevent returning to online
        // views
        navController.navigate(Screen.OFFLINE) { popUpTo(Screen.OFFLINE) { inclusive = true } }
      } else {
        // Return to the main authentication flow if returning online
        navController.navigate(Route.AUTH) { popUpTo(Screen.OFFLINE) { inclusive = true } }
      }
    }
  }
}
