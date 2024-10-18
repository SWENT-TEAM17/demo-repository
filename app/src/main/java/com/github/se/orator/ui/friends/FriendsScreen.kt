package com.github.se.orator.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.github.se.orator.R
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.ui.navigation.BottomNavigationMenu
import com.github.se.orator.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFriendsScreen(
    navigationActions: NavigationActions,
    userProfileViewModel: UserProfileViewModel
) {
  val friendsProfiles by userProfileViewModel.friendsProfiles.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  val filteredFriends =
      friendsProfiles.filter { friend -> friend.name.contains(searchQuery, ignoreCase = true) }

  // Manage focus for the search bar
  val focusRequester = FocusRequester()
  val focusManager = LocalFocusManager.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  // ModalDrawer Scaffold
  ModalNavigationDrawer(
      modifier = Modifier.testTag("viewFriendsDrawerMenu"),
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet {
          Column(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
            Text(
                "Actions",
                modifier = Modifier.testTag("viewFriendsDrawerTitle"),
                style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))

            // Option to Add Friend
            TextButton(
                modifier = Modifier.testTag("viewFriendsAddFriendButton"),
                onClick = {
                  // Navigate to Add Friend screen
                  scope.launch {
                    drawerState.close() // Close the drawer
                    navigationActions.navigateTo(Screen.ADD_FRIENDS)
                  }
                }) {
                  Text("➕ Add a friend")
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Option to Leaderboard
            TextButton(
                modifier = Modifier.testTag("viewFriendsLeaderboardButton"),
                onClick = {
                  // Close drawer and navigate to Leaderboard screen
                  scope.launch {
                    drawerState.close() // Close the drawer
                    navigationActions.navigateTo(Screen.LEADERBOARD)
                  }
                }) {
                  Text("⭐ Leaderboard")
                }
          }
        }
      }) {
        Scaffold(
            topBar = {
              TopAppBar(
                  title = { Text("My Friends") },
                  navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag("viewFriendsMenuButton"),
                        onClick = {
                          scope.launch {
                            drawerState.open() // Open the drawer
                          }
                        }) {
                          Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                  })
            },
            bottomBar = {
              BottomNavigationMenu(
                  onTabSelect = { route ->
                    scope.launch {
                      drawerState.close() // Close the drawer before navigating
                      navigationActions.navigateTo(route)
                    }
                  },
                  tabList = LIST_TOP_LEVEL_DESTINATION,
                  selectedItem = navigationActions.currentRoute())
            }) { innerPadding ->
              // Main container that will remove focus from the search bar when clicked
              Column(
                  modifier =
                      Modifier.fillMaxSize()
                          .padding(innerPadding)
                          .padding(horizontal = 16.dp, vertical = 8.dp)
                          .clickable {
                            focusManager.clearFocus()
                          } // Clear focus when clicking outside the search bar
                  ) {
                    // Search bar to filter friends by name
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search for a friend") },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .focusRequester(
                                    focusRequester) // Attach focusRequester to search bar
                                .testTag("viewFriendsSearch"))

                    if (filteredFriends.isEmpty()) {
                      // Show "No user found" when there are no matches
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No user found", style = MaterialTheme.typography.bodyLarge)
                      }
                    } else {
                      // LazyColumn for displaying friends
                      LazyColumn(
                          modifier = Modifier.testTag("viewFriendsList"),
                          contentPadding = PaddingValues(vertical = 8.dp),
                          verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredFriends) { friend -> FriendItem(friend = friend) }
                          }
                    }
                  }
            }
      }
}

@Composable
fun FriendItem(friend: UserProfile) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surface)
              .padding(16.dp)
              .testTag("viewFriendsItem#${friend.uid}"),
      verticalAlignment = Alignment.CenterVertically) {
        ProfilePicture(profilePictureUrl = friend.profilePic, onClick = {})
        Spacer(modifier = Modifier.width(16.dp))

        Column {
          Text(text = friend.name, style = MaterialTheme.typography.titleMedium)
          Text(
              text = friend.bio ?: "No bio available",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray)
        }
      }
}

/**
 * A composable function that displays a profile picture. If no profile picture URL is provided, a
 * default image is shown. The image is clickable, triggering the provided [onClick] function.
 *
 * @param profilePictureUrl The URL of the profile picture to be displayed. If null, a default image
 *   is shown.
 * @param onClick A lambda function that is triggered when the profile picture is clicked.
 */
@Composable
fun ProfilePicture(profilePictureUrl: String?, onClick: () -> Unit) {
  val painter = rememberAsyncImagePainter(model = profilePictureUrl ?: R.drawable.profile_picture)
  Image(
      painter = painter,
      contentDescription = "Profile Picture",
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(100.dp).clip(CircleShape).clickable(onClick = onClick))
}
