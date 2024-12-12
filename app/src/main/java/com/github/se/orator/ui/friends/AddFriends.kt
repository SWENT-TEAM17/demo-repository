package com.github.se.orator.ui.friends

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.ui.navigation.BottomNavigationMenu
import com.github.se.orator.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Route
import com.github.se.orator.ui.profile.ProfilePictureDialog
import com.github.se.orator.ui.theme.AppDimensions

/**
 * Composable function that displays the "Add Friends" screen, where users can:
 * - Search for other users to send friend requests.
 * - View and manage their sent friend requests.
 *
 * The screen includes a search bar, a list of filtered user profiles, and an expandable section
 * showing filtered sent friend requests.
 *
 * @param navigationActions Actions to handle navigation within the app.
 * @param userProfileViewModel ViewModel for managing user profile data and friend request logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AddFriendsScreen(
    navigationActions: NavigationActions,
    userProfileViewModel: UserProfileViewModel
) {
  val userProfile by userProfileViewModel.userProfile.collectAsState()
  val friendsProfiles by userProfileViewModel.friendsProfiles.collectAsState()
  var query by remember { mutableStateOf("") } // Holds the search query input
  var expanded by remember { mutableStateOf(false) } // Controls if search results are visible
  val allProfiles by userProfileViewModel.allProfiles.collectAsState() // All user profiles
  val focusRequester = FocusRequester() // Manages focus for the search field
  val sentReqProfiles by userProfileViewModel.sentReqProfiles.collectAsState()
  // Exclude the current user's profile and their friends' profiles from the list
  val filteredProfiles =
      allProfiles.filter { profile ->
        profile.uid != userProfile?.uid && // Exclude own profile
            friendsProfiles.none { friend -> friend.uid == profile.uid } && // Exclude friends
            sentReqProfiles.none { sent -> sent.uid == profile.uid } && // Exclude sent requests
            profile.name.contains(query, ignoreCase = true) // Match search query
      }
  val filteredSentReq =
      sentReqProfiles.filter { recReq -> recReq.name.contains(query, ignoreCase = true) }

  // State variable to keep track of the selected user's profile picture
  var selectedProfilePicUser by remember { mutableStateOf<UserProfile?>(null) }

  // State variable to control the expansion of Sent Friend Requests
  var isSentRequestsExpanded by remember { mutableStateOf(false) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Add a friend",
                  modifier = Modifier.testTag("addFriendTitle"),
                  color = MaterialTheme.colorScheme.onSurface)
            },
            navigationIcon = {
              IconButton(
                  onClick = {
                    navigationActions.goBack() // Navigate back
                  },
                  modifier = Modifier.testTag("addFriendBackButton")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer))
      },
      bottomBar = {
        BottomNavigationMenu(
            onTabSelect = { route -> navigationActions.navigateTo(route) },
            tabList = LIST_TOP_LEVEL_DESTINATION,
            selectedItem = Route.FRIENDS)
      }) { paddingValues ->
        // Replace Column with LazyColumn
        LazyColumn(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).padding(AppDimensions.paddingMedium),
            content = {
              // Search Field
              item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newValue ->
                      query = newValue
                      expanded = newValue.isNotEmpty()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .wrapContentHeight()
                            .focusRequester(focusRequester)
                            .testTag("addFriendSearchField"),
                    label = {
                      Text(
                          "Username",
                          modifier = Modifier.testTag("searchFieldLabel"),
                          color = MaterialTheme.colorScheme.onSurface)
                    },
                    leadingIcon = {
                      Icon(
                          Icons.Default.Search,
                          contentDescription = "Search Icon",
                          modifier = Modifier.testTag("searchIcon"),
                          tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                      if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.testTag("clearSearchButton")) {
                              Icon(
                                  Icons.Default.Clear,
                                  contentDescription = "Clear Icon",
                                  modifier = Modifier.testTag("clearIcon"))
                            }
                      }
                    },
                    colors =
                        TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                    singleLine = true,
                    keyboardActions = KeyboardActions.Default)
              }

              item { Spacer(modifier = Modifier.height(AppDimensions.paddingMedium)) }

              // Expandable Section: Sent Friend Requests
              if (filteredSentReq.isNotEmpty()) {
                // Header with Toggle Button
                item {
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .clickable { isSentRequestsExpanded = !isSentRequestsExpanded }
                              .padding(vertical = AppDimensions.smallPadding),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Sent Friend Requests",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f).testTag("sentFriendRequestsHeader"),
                            color = MaterialTheme.colorScheme.onSurface)
                        IconButton(
                            onClick = { isSentRequestsExpanded = !isSentRequestsExpanded },
                            modifier = Modifier.testTag("toggleSentRequestsButton")) {
                              Icon(
                                  imageVector =
                                      if (isSentRequestsExpanded) Icons.Default.ExpandLess
                                      else Icons.Default.ExpandMore,
                                  contentDescription =
                                      if (isSentRequestsExpanded) "Collapse Sent Requests"
                                      else "Expand Sent Requests",
                                  tint = MaterialTheme.colorScheme.onSurface)
                            }
                      }
                }

                // Sent Friend Requests List with AnimatedVisibility
                item {
                  AnimatedVisibility(
                      visible = isSentRequestsExpanded,
                      enter = expandVertically(),
                      exit = shrinkVertically()) {
                        Column {
                          Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
                          // Sent Friend Requests Items
                          for (sentRequest in filteredSentReq) {
                            SentFriendRequestItem(
                                sentRequest = sentRequest,
                                userProfileViewModel = userProfileViewModel)
                            Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
                          }
                          Spacer(modifier = Modifier.height(AppDimensions.paddingMedium))
                        }
                      }
                }
              }

              // Display search results if there is a query
              if (query.isNotEmpty()) {
                items(
                    filteredProfiles.filter { profile ->
                      profile.name.contains(query, ignoreCase = true)
                    }) { user ->
                      UserItem(
                          user = user,
                          userProfileViewModel = userProfileViewModel,
                          onProfilePictureClick = { selectedUser ->
                            selectedProfilePicUser = selectedUser
                          })
                      Spacer(modifier = Modifier.height(AppDimensions.paddingSmall))
                    }
              }
            })

        // Dialog to show the enlarged profile picture
        if (selectedProfilePicUser?.profilePic != null) {
          ProfilePictureDialog(
              profilePictureUrl = selectedProfilePicUser?.profilePic ?: "",
              onDismiss = { selectedProfilePicUser = null })
        }
      }
}

/**
 * Composable function that represents a single sent friend request item in the list.
 *
 * It displays the friend's profile picture, name, bio, and a button to cancel the request.
 *
 * @param sentRequest The [UserProfile] object representing the user to whom the request was sent.
 * @param userProfileViewModel The [UserProfileViewModel] that handles friend request cancellation.
 */
@Composable
fun SentFriendRequestItem(sentRequest: UserProfile, userProfileViewModel: UserProfileViewModel) {
  val context = LocalContext.current

  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = AppDimensions.smallPadding)
              .clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .testTag("sentFriendRequestItem#${sentRequest.uid}"),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shadowElevation = AppDimensions.elevationSmall // Subtle shadow with low elevation
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically) {
              // Friend's Profile Picture
              ProfilePicture(
                  profilePictureUrl = sentRequest.profilePic,
                  onClick = { /* Optionally, show enlarged picture */})
              Spacer(modifier = Modifier.width(AppDimensions.smallWidth))
              Column(
                  modifier = Modifier.weight(1f), // Expand to push Cancel button to the end
                  verticalArrangement = Arrangement.Center) {
                    // Friend's Name
                    Text(
                        text = sentRequest.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier.padding(bottom = AppDimensions.smallPadding)
                                .testTag("sentFriendRequestName#${sentRequest.uid}"),
                        color = MaterialTheme.colorScheme.primary)
                    // Friend's Bio
                    Text(
                        text = sentRequest.bio ?: "No bio available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("sentFriendRequestBio#${sentRequest.uid}"))
                  }
              // Cancel Friend Request Button
              IconButton(
                  onClick = {
                    userProfileViewModel.cancelFriendRequest(sentRequest)
                    Toast.makeText(
                            context,
                            "Friend request to ${sentRequest.name} has been canceled.",
                            Toast.LENGTH_SHORT)
                        .show()
                  },
                  modifier = Modifier.testTag("cancelFriendRequestButton#${sentRequest.uid}")) {
                    Icon(
                        imageVector = Icons.Default.Close, // Using Close icon for cancellation
                        contentDescription = "Cancel Friend Request",
                        tint = Color.Red)
                  }
            }
      }
}

/**
 * Composable function that represents a single user item in a list of search results.
 *
 * It displays the user's profile picture, name, and bio. Users can click on the profile picture to
 * view an enlarged version or click on the user item to send a friend request.
 *
 * If the user has already sent a friend request to the current user, a dialog will appear, giving
 * the option to accept, reject, or decide later.
 *
 * @param user The [UserProfile] object representing the user being displayed.
 * @param userProfileViewModel The [UserProfileViewModel] that handles friend request actions.
 * @param onProfilePictureClick Callback triggered when the profile picture is clicked.
 */
@Composable
fun UserItem(
    user: UserProfile,
    userProfileViewModel: UserProfileViewModel,
    onProfilePictureClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val recReqProfiles by userProfileViewModel.recReqProfiles.collectAsState()

  var showMutualRequestDialog by remember { mutableStateOf(false) }

  Surface(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = AppDimensions.smallPadding)
              .clip(RoundedCornerShape(AppDimensions.roundedCornerRadius))
              .testTag("userItem#${user.uid}"),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      shadowElevation = AppDimensions.elevationSmall) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(AppDimensions.paddingMedium)
                    .testTag("userItemRow#${user.uid}"),
            verticalAlignment = Alignment.CenterVertically) {
              ProfilePicture(
                  profilePictureUrl = user.profilePic,
                  onClick = { onProfilePictureClick(user) },
              )
              Spacer(modifier = Modifier.width(AppDimensions.smallWidth))
              Column(
                  modifier = Modifier.weight(1f), // This modifier makes the column expand
                  verticalArrangement = Arrangement.Center) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier.padding(bottom = AppDimensions.smallPadding)
                                .testTag("userName#${user.uid}"),
                        color = MaterialTheme.colorScheme.primary)

                    Text(
                        text = user.bio ?: "No bio available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("userBio#${user.uid}"))
                  }
              IconButton(
                  onClick = {
                    val hasIncomingRequest = recReqProfiles.any { it.uid == user.uid }

                    if (hasIncomingRequest) {
                      showMutualRequestDialog = true
                    } else {
                      userProfileViewModel.sendRequest(user)
                      Toast.makeText(
                              context,
                              "You have sent a friend request to ${user.name}.",
                              Toast.LENGTH_SHORT)
                          .show()
                    }
                  },
                  modifier = Modifier.testTag("sendFriendRequestButton#${user.uid}")) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Send Friend Request",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("sendFriendRequestIcon#${user.uid}"))
                  }
            }
        // Mutual request dialog remains unchanged
        if (showMutualRequestDialog) {
          AlertDialog(
              onDismissRequest = { showMutualRequestDialog = false },
              title = { Text("Friend Request") },
              text = {
                Text(
                    text =
                        "${user.name} has already sent you a friend request. Would you like to accept, reject, or decide later?")
              },
              confirmButton = {
                TextButton(
                    onClick = {
                      userProfileViewModel.acceptFriend(user)
                      Toast.makeText(
                              context, "You are now friends with ${user.name}.", Toast.LENGTH_SHORT)
                          .show()
                      showMutualRequestDialog = false
                    }) {
                      Text("Accept")
                    }
              },
              dismissButton = {
                Row {
                  TextButton(
                      onClick = {
                        userProfileViewModel.declineFriendRequest(user)
                        Toast.makeText(
                                context,
                                "Friend request from ${user.name} has been rejected.",
                                Toast.LENGTH_SHORT)
                            .show()
                        showMutualRequestDialog = false
                      }) {
                        Text("Reject")
                      }
                  TextButton(onClick = { showMutualRequestDialog = false }) { Text("Decide Later") }
                }
              })
        }
      }
}
