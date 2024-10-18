package com.github.se.orator.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.orator.model.profile.UserProfile
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.ui.navigation.BottomNavigationMenu
import com.github.se.orator.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navigationActions: NavigationActions,
    userProfileViewModel: UserProfileViewModel
) {
  // Collect the user profile and friends' profiles from the ViewModel
  val userProfile by userProfileViewModel.userProfile.collectAsState()
  val friendsProfiles by userProfileViewModel.friendsProfiles.collectAsState()

  // Combine user and friends into a single list and sort by improvement (highest to lowest)
  val leaderboardEntries =
      remember(userProfile, friendsProfiles) {
        (listOfNotNull(userProfile) + friendsProfiles).sortedByDescending {
          it.statistics.improvement
        }
      }

  val focusManager = LocalFocusManager.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

  ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet {
          Column(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))

            // Option to Add Friend
            TextButton(onClick = { navigationActions.navigateTo(Screen.ADD_FRIENDS) }) {
              Text("➕ Add a friend")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option to Friends List
            TextButton(onClick = { navigationActions.navigateTo(Screen.FRIENDS) }) {
              Text("👥 View Friends")
            }
          }
        }
      }) {
        Scaffold(
            topBar = {
              TopAppBar(
                  title = { Text("Leaderboard", modifier = Modifier.testTag("leaderboardTitle")) },
                  navigationIcon = {
                    IconButton(
                        onClick = {
                          navigationActions.goBack() // Only navigate back, no drawer action
                        }) {
                          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                  })
            },
            bottomBar = {
              BottomNavigationMenu(
                  onTabSelect = { route -> navigationActions.navigateTo(route) },
                  tabList = LIST_TOP_LEVEL_DESTINATION,
                  selectedItem = navigationActions.currentRoute())
            }) { innerPadding ->
              Column(
                  modifier =
                      Modifier.fillMaxSize()
                          .padding(innerPadding)
                          .padding(horizontal = 16.dp, vertical = 8.dp)
                          .clickable { focusManager.clearFocus() }
                          .testTag("leaderboardList")) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                          // Use itemsIndexed to get the index for ranks
                          itemsIndexed(leaderboardEntries) { index, profile ->
                            LeaderboardItem(rank = index + 1, profile = profile)
                          }
                        }
                  }
            }
      }
}

@Composable
fun LeaderboardItem(rank: Int, profile: UserProfile) {
  val rankColor =
      when (rank) {
        1 -> Color(0xFFFFD700) // Gold color
        2 -> Color(0xFFC0C0C0) // Silver color
        3 -> Color(0xFFCD7F32) // Bronze color
        else -> MaterialTheme.colorScheme.onSurface // Normal color for the rest
      }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surface)
              .padding(16.dp)
              .testTag("leaderboardItem#$rank"),
      verticalAlignment = Alignment.CenterVertically) {
        ProfilePicture(profilePictureUrl = profile.profilePic, onClick = {})
        Spacer(modifier = Modifier.width(16.dp))

        Column {
          // Show the rank with the specified color
          Text(
              text = "$rank. ${profile.name}",
              style = MaterialTheme.typography.titleMedium,
              color = rankColor // Apply the color based on rank
              )
          Text(
              text = "Improvement: ${profile.statistics.improvement}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray)
        }
      }
}
