package com.github.se.orator.ui.friends

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import com.github.se.orator.R
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.speechBattle.BattleViewModel
import com.github.se.orator.model.speechBattle.SpeechBattle
import com.github.se.orator.ui.navigation.BottomNavigationMenu
import com.github.se.orator.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Route
import com.github.se.orator.ui.navigation.Screen
import com.github.se.orator.ui.profile.ProfilePictureDialog
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.COLOR_AMBER
import com.github.se.orator.utils.getCurrentDate
import com.github.se.orator.utils.getDaysDifference
import com.github.se.orator.utils.parseDate
import kotlinx.coroutines.launch

/**
 * Composable function that displays the "View Friends" screen.
 *
 * The screen includes:
 * - A search bar to filter both the friends list and received friend requests.
 * - An expandable section showing received friend requests with options to accept or decline.
 * - A list of friends with options to view their details or remove them.
 * - Navigation options to "Add Friends" and "Leaderboard."
 *
 * @param navigationActions Object to handle navigation within the app.
 * @param userProfileViewModel ViewModel providing user data and friend management functionality.
 * @param battleViewModel ViewModel for managing battles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFriendsScreen(
    navigationActions: NavigationActions,
    userProfileViewModel: UserProfileViewModel,
    battleViewModel: BattleViewModel? = null // Optional ViewModel for battle management
) {
  // State variables
  val friendsProfiles by userProfileViewModel.friendsProfiles.collectAsState()
  val recReqProfiles by userProfileViewModel.recReqProfiles.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  val filteredFriends =
      friendsProfiles.filter { friend -> friend.name.contains(searchQuery, ignoreCase = true) }
  val filteredRecReq =
      recReqProfiles.filter { recReq -> recReq.name.contains(searchQuery, ignoreCase = true) }

  // State variables for UI components
  val focusRequester = FocusRequester()
  val focusManager = LocalFocusManager.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  // State variable for selected friend
  var selectedFriend by remember { mutableStateOf<UserProfile?>(null) }

  // State variable for Snackbar
  val snackbarHostState = remember { SnackbarHostState() }

  // New state variable for Friend Requests expansion
  var isFriendRequestsExpanded by remember { mutableStateOf(true) }

  // Fetch pending battles when the screen is composed
  LaunchedEffect(Unit) { battleViewModel?.fetchPendingBattlesForUser() }

  // Collect the pending battles as state, fallback to an empty list if battleViewModel is null
  val pendingBattles by
      battleViewModel?.pendingBattles?.observeAsState(emptyList())
          ?: remember { mutableStateOf(emptyList<SpeechBattle>()) }

  // Create a HashMap of challengerUID to battleID for quick lookup
  val challengerBattleMap =
      remember(pendingBattles) { pendingBattles.associateBy({ it.challenger }, { it.battleId }) }
  ModalNavigationDrawer(
      modifier = Modifier.testTag("viewFriendsDrawerMenu"),
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet {
          Column(modifier = Modifier.fillMaxHeight().padding(AppDimensions.paddingMedium)) {
            Text(
                "Actions",
                modifier = Modifier.testTag("viewFriendsDrawerTitle"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(AppDimensions.heightMedium))
            TextButton(
                modifier = Modifier.testTag("viewFriendsAddFriendButton"),
                onClick = {
                  scope.launch {
                    drawerState.close()
                    navigationActions.navigateTo(Screen.ADD_FRIENDS)
                  }
                }) {
                  Text("➕ Add a friend", color = MaterialTheme.colorScheme.onSurface)
                }
            Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
            TextButton(
                modifier = Modifier.testTag("viewFriendsLeaderboardButton"),
                onClick = {
                  scope.launch {
                    drawerState.close()
                    navigationActions.navigateTo(Screen.LEADERBOARD)
                  }
                }) {
                  Text("⭐ Leaderboard", color = MaterialTheme.colorScheme.onSurface)
                }
          }
        }
      }) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
              TopAppBar(
                  modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                  title = {
                    Text(
                        "My Friends",
                        modifier = Modifier.testTag("myFriendsTitle"),
                        color = MaterialTheme.colorScheme.onSurface)
                  },
                  navigationIcon = {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.testTag("viewFriendsMenuButton")) {
                          Icon(
                              Icons.Default.Menu,
                              contentDescription = "Menu",
                              tint = MaterialTheme.colorScheme.onSurface)
                        }
                  },
                  backgroundColor = MaterialTheme.colorScheme.surface)
              HorizontalDivider()
            },
            bottomBar = {
              BottomNavigationMenu(
                  onTabSelect = { route ->
                    scope.launch {
                      drawerState.close()
                      navigationActions.navigateTo(route)
                    }
                  },
                  tabList = LIST_TOP_LEVEL_DESTINATION,
                  selectedItem = Route.FRIENDS)
            }) { innerPadding ->
              LazyColumn(
                  modifier =
                      Modifier.fillMaxSize()
                          .padding(innerPadding)
                          .padding(horizontal = AppDimensions.paddingMedium)
                          .clickable { focusManager.clearFocus() }) {
                    // Search bar for filtering friends
                    item {
                      Box(modifier = Modifier.padding(vertical = AppDimensions.paddingSmall)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search for a friend.") },
                            leadingIcon = {
                              Icon(
                                  imageVector = Icons.Default.Search,
                                  contentDescription = "Search Icon")
                            },
                            modifier =
                                Modifier.fillMaxWidth(1f)
                                    .horizontalScroll(rememberScrollState())
                                    .wrapContentHeight()
                                    .focusRequester(focusRequester)
                                    .testTag("viewFriendsSearch"),
                            colors =
                                TextFieldDefaults.outlinedTextFieldColors(
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedLabelColor =
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    leadingIconColor = MaterialTheme.colorScheme.primary))
                      }
                    }

                    // Spacer
                    item { Spacer(modifier = Modifier.height(AppDimensions.paddingMedium)) }

                    // Expandable Section: Received Friend Requests
                    if (filteredRecReq.isNotEmpty()) {
                      // Header with Toggle Button
                      item {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable {
                                      isFriendRequestsExpanded = !isFriendRequestsExpanded
                                    }
                                    .padding(vertical = AppDimensions.smallPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                              Text(
                                  text = "Friend Requests",
                                  style = MaterialTheme.typography.titleSmall,
                                  modifier = Modifier.weight(1f).testTag("friendRequestsHeader"),
                                  color = MaterialTheme.colorScheme.onSurface)
                              IconButton(
                                  onClick = {
                                    isFriendRequestsExpanded = !isFriendRequestsExpanded
                                  },
                                  modifier = Modifier.testTag("toggleFriendRequestsButton")) {
                                    Icon(
                                        imageVector =
                                            if (isFriendRequestsExpanded) Icons.Default.ExpandLess
                                            else Icons.Default.ExpandMore,
                                        contentDescription =
                                            if (isFriendRequestsExpanded) "Collapse Friend Requests"
                                            else "Expand Friend Requests",
                                        tint = MaterialTheme.colorScheme.onSurface)
                                  }
                            }
                      }

                      // Friend Requests List with AnimatedVisibility
                      item {
                        AnimatedVisibility(
                            visible = isFriendRequestsExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()) {
                              Column {
                                Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
                                // Friend Requests Items
                                for (friendRequest in filteredRecReq) {
                                  FriendRequestItem(
                                      friendRequest = friendRequest,
                                      userProfileViewModel = userProfileViewModel)
                                  Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
                                }
                                Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                              }
                            }
                      }
                    }

                    // Section for Friends List
                    item {
                      Text(
                          text = "Your Friends",
                          style = MaterialTheme.typography.titleSmall,
                          modifier =
                              Modifier.padding(bottom = AppDimensions.smallPadding)
                                  .testTag("viewFriendsList"),
                          color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Display message if no friends match the search query
                    if (filteredFriends.isEmpty()) {
                      item {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag("noFriendsFound"),
                            contentAlignment = Alignment.Center) {
                              Text(
                                  "No friends found.",
                                  style = MaterialTheme.typography.bodyLarge,
                                  modifier = Modifier.testTag("noFriendsFoundText"),
                                  color = MaterialTheme.colorScheme.onBackground)
                            }
                      }
                    } else {
                      // Display the list of friends if any match the search query
                      items(filteredFriends) { friend ->
                        val hasPendingBattle = friend.uid in challengerBattleMap.keys
                        FriendItem(
                            friend = friend,
                            hasPendingBattle = hasPendingBattle,
                            userProfileViewModel = userProfileViewModel,
                            onProfilePictureClick = { selectedFriend = it },
                            onClick = { selectedFriend ->
                              if (hasPendingBattle) {
                                // Accept battle and navigate to battle screen
                                battleViewModel?.acceptBattle(challengerBattleMap[friend.uid]!!)
                              } else {
                                // Navigate to create battle screen
                                navigationActions.navigateToSendBattleScreen(selectedFriend.uid)
                              }
                            })
                      }
                    }
                  }

              // Dialog to display the enlarged profile picture
              if (selectedFriend != null && !selectedFriend!!.profilePic.isNullOrEmpty()) {
                ProfilePictureDialog(
                    profilePictureUrl = selectedFriend!!.profilePic!!,
                    onDismiss = { selectedFriend = null })
              }
            }
      }
}

/**
 * Composable function that represents a single friend item in the list.
 *
 * It displays:
 * - The friend's profile picture, name, and bio.
 * - The friend's login streak or the last login date.
 * - An option to remove the friend from the user's friend list.
 * - A notification icon if there's a pending battle.
 *
 * @param friend The [UserProfile] object representing the friend being displayed.
 * @param hasPendingBattle A boolean indicating if the friend has challenged the user.
 * @param userProfileViewModel The [UserProfileViewModel] that handles friend deletion.
 * @param onProfilePictureClick Callback triggered when the friend's profile picture is clicked.
 * @param onClick Callback when the entire item is clicked.
 */
@Composable
fun FriendItem(
    friend: UserProfile,
    hasPendingBattle: Boolean,
    userProfileViewModel: UserProfileViewModel,
    onProfilePictureClick: (UserProfile) -> Unit,
    onClick: (UserProfile) -> Unit
) {
  // Compute the displayedStreak
  val displayedStreak = currentFriendStreak(friend.lastLoginDate, friend.currentStreak)
  Log.d("FriendItem", "Days Since Last Login: ${friend.lastLoginDate}")

  // Compute days since last login for broken streak
  val daysSinceLastLogin =
      remember(friend.lastLoginDate) {
        if (!friend.lastLoginDate.isNullOrEmpty()) {
          val lastLoginDate = parseDate(friend.lastLoginDate)
          if (lastLoginDate != null) {
            getDaysDifference(lastLoginDate, getCurrentDate())
          } else {
            0L
          }
        } else {
          0L
        }
      }

  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal = AppDimensions.smallPadding, vertical = AppDimensions.smallPadding)
              .clip(RoundedCornerShape(AppDimensions.paddingMediumSmall))
              .testTag("viewFriendsItem#${friend.uid}")
              .clickable { onClick(friend) },
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shadowElevation = AppDimensions.elevationSmall) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically) {
              ProfilePicture(
                  profilePictureUrl = friend.profilePic,
                  onClick = { onProfilePictureClick(friend) })
              Spacer(modifier = Modifier.width(AppDimensions.smallWidth))
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier.padding(bottom = AppDimensions.smallPadding)
                            .testTag("friendName#${friend.uid}"),
                    color = MaterialTheme.colorScheme.primary)
                Text(
                    text = friend.bio ?: "No bio available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("friendBio#${friend.uid}"))
                Spacer(modifier = Modifier.height(AppDimensions.smallPadding))
                // Display streak or last login message
                if (displayedStreak > 0) {
                  // Display Whatshot icon with streak count
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Filled.Whatshot,
                            contentDescription = "Active Streak",
                            tint = COLOR_AMBER,
                            modifier = Modifier.size(AppDimensions.iconSizeSmall))
                        Spacer(modifier = Modifier.width(AppDimensions.smallWidth))
                        Text(
                            text =
                                "$displayedStreak day${if (displayedStreak > 1) "s" else ""} streak",
                            style = MaterialTheme.typography.bodyLarge,
                            color = COLOR_AMBER,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("friendStreak").weight(1f))
                      }
                } else {
                  // Display last login message
                  Text(
                      text =
                          "Last login ${daysSinceLastLogin} day${if (daysSinceLastLogin > 1) "s" else ""} ago",
                      style = MaterialTheme.typography.bodyLarge,
                      color = MaterialTheme.colorScheme.secondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.testTag("friendLastLogin#${friend.uid}"))
                }
              }
              if (hasPendingBattle) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Pending Battle",
                    tint = Color.Red,
                    modifier =
                        Modifier.size(AppDimensions.iconSizeMedium)
                            .padding(end = AppDimensions.smallPadding)
                            .testTag("pendingBattleIcon#${friend.uid}"))
              }
              DeleteFriendButton(friend = friend, userProfileViewModel = userProfileViewModel)
            }
      }
}

/**
 * Composable function to display a profile picture with a circular shape. Uses Coil to load the
 * image asynchronously.
 *
 * @param profilePictureUrl The URL of the profile picture to display. Defaults to a placeholder if
 *   null.
 * @param onClick Action to be performed when the profile picture is clicked.
 */
@Composable
fun ProfilePicture(profilePictureUrl: String?, onClick: () -> Unit) {
  val painter = rememberAsyncImagePainter(model = profilePictureUrl ?: R.drawable.profile_picture)
  Image(
      painter = painter,
      contentDescription = "Profile Picture",
      contentScale = ContentScale.Crop,
      modifier =
          Modifier.size(AppDimensions.buttonHeight)
              .clip(CircleShape)
              .clickable(onClick = onClick)
              .testTag("profilePicture"))
}

/**
 * Composable function for the button to remove a friend from the user's friend list.
 * - Displays a delete icon.
 * - Shows a Toast message on successful removal of the friend.
 *
 * @param friend The [UserProfile] of the friend to be removed.
 * @param userProfileViewModel The [UserProfileViewModel] that handles friend deletion logic.
 */
@Composable
fun DeleteFriendButton(friend: UserProfile, userProfileViewModel: UserProfileViewModel) {
  val context = LocalContext.current

  IconButton(
      onClick = {
        userProfileViewModel.deleteFriend(friend)
        Toast.makeText(
                context, "${friend.name} has been removed from your friends.", Toast.LENGTH_SHORT)
            .show()
      },
      modifier = Modifier.testTag("deleteFriendButton#${friend.uid}")) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
      }
}

/**
 * Computes the current streak of a friend based on their last login date and current streak.
 * - A streak continues if the last login was on the same day or the following day.
 * - A broken streak resets to 0.
 *
 * @param lastLoginDateString The last login date as a string in "yyyy-MM-dd" format. Can be null.
 * @param currentStreak The current streak value.
 * @return The streak to be displayed: the `currentStreak` if active, otherwise 0.
 */
fun currentFriendStreak(lastLoginDateString: String?, currentStreak: Long): Long {
  if (!lastLoginDateString.isNullOrEmpty()) {
    val lastLoginDate = parseDate(lastLoginDateString)
    val currentDate = getCurrentDate()
    val daysDifference = getDaysDifference(lastLoginDate, currentDate)
    return when (daysDifference) {
      0L,
      1L -> currentStreak // Same day or consecutive day login
      else -> 0L // Streak broken
    }
  }
  return -1L // No last login date recorded
}

/**
 * Composable function that represents a single friend request item in the list.
 *
 * It displays:
 * - The requester's profile picture, name, and bio.
 * - Buttons to accept or decline the friend request.
 * - Handles user interactions and updates the state through the ViewModel.
 *
 * @param friendRequest The [UserProfile] object representing the user who sent the request.
 * @param userProfileViewModel The [UserProfileViewModel] that handles accepting or declining the
 *   request.
 */
@Composable
fun FriendRequestItem(friendRequest: UserProfile, userProfileViewModel: UserProfileViewModel) {
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = AppDimensions.smallPadding)
              .clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .testTag("friendRequestItem#${friendRequest.uid}"),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shadowElevation = AppDimensions.elevationSmall) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically) {
              // Friend's Profile Picture
              ProfilePicture(
                  profilePictureUrl = friendRequest.profilePic,
                  onClick = { /* Optionally, show enlarged picture */})
              Spacer(modifier = Modifier.width(AppDimensions.smallWidth))
              Column(modifier = Modifier.weight(1f)) {
                // Friend's Name
                Text(
                    text = friendRequest.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier.padding(bottom = AppDimensions.smallPadding)
                            .testTag("friendRequestName#${friendRequest.uid}"),
                    color = MaterialTheme.colorScheme.primary)
                // Friend's Bio
                Text(
                    text = friendRequest.bio ?: "No bio available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("friendRequestBio#${friendRequest.uid}"))
              }
              // Accept Friend Request Button
              IconButton(
                  onClick = { userProfileViewModel.acceptFriend(friendRequest) },
                  modifier =
                      Modifier.size(AppDimensions.iconSizeMedium)
                          .testTag("acceptFriendButton#${friendRequest.uid}")) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept Friend Request",
                        tint = Color.Green)
                  }
              // Decline Friend Request Button
              IconButton(
                  onClick = { userProfileViewModel.declineFriendRequest(friendRequest) },
                  modifier =
                      Modifier.size(AppDimensions.iconSizeMedium)
                          .testTag("declineFriendButton#${friendRequest.uid}")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Decline Friend Request",
                        tint = Color.Red)
                  }
            }
      }
}
