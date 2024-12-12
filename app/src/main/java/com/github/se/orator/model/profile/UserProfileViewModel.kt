package com.github.se.orator.model.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.se.orator.model.speaking.AnalysisData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing user profiles and their interactions, including:
 * - Fetching and updating user profiles.
 * - Managing friends, friend requests, and session statistics.
 * - Handling profile picture uploads and login streaks.
 *
 * @param repository The repository for accessing user profile data.
 */
class UserProfileViewModel(internal val repository: UserProfileRepository) : ViewModel() {

  // Mutable state flow to hold the user profile
  private val userProfile_ = MutableStateFlow<UserProfile?>(null)
  val userProfile: StateFlow<UserProfile?> = userProfile_.asStateFlow()

  // Mutable state flow to hold the list of all profiles
  private val allProfiles_ = MutableStateFlow<List<UserProfile>>(emptyList())
  val allProfiles: StateFlow<List<UserProfile>> = allProfiles_.asStateFlow()

  // Mutable state flow to hold the list of friends' profiles
  private val friendsProfiles_ = MutableStateFlow<List<UserProfile>>(emptyList())
  val friendsProfiles: StateFlow<List<UserProfile>> = friendsProfiles_.asStateFlow()

  // Mutable state flow to hold the list of friends' profiles
  private val recReqProfiles_ = MutableStateFlow<List<UserProfile>>(emptyList())
  val recReqProfiles: StateFlow<List<UserProfile>> = recReqProfiles_.asStateFlow()
  // Mutable state flow to hold the list of friends' profiles
  private val sentReqProfiles_ = MutableStateFlow<List<UserProfile>>(emptyList())
  val sentReqProfiles: StateFlow<List<UserProfile>> = sentReqProfiles_.asStateFlow()
  // Selected friend's profile
  private val selectedFriend_ = MutableStateFlow<UserProfile?>(null)
  val selectedFriend: StateFlow<UserProfile?> = selectedFriend_.asStateFlow()

  // Loading state to indicate if the profile is being fetched
  private val isLoading_ = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = isLoading_.asStateFlow()

  private val pendingBattles_ = MutableStateFlow<Map<String, String>>(emptyMap())
  val pendingBattles: StateFlow<Map<String, String>> = pendingBattles_.asStateFlow()

  private val friendsWithPendingBattles_ =
      MutableStateFlow<List<Pair<UserProfile, String>>>(emptyList())
  val friendsWithPendingBattles: StateFlow<List<Pair<UserProfile, String>>> =
      friendsWithPendingBattles_.asStateFlow()

  // Queue of the last ten analysis data
  private val recentData_ = MutableStateFlow<ArrayDeque<AnalysisData>>(ArrayDeque())
  val recentData: StateFlow<ArrayDeque<AnalysisData>> = recentData_.asStateFlow()

  // Max size for a recentData queue
  private val MAX_RECENT_DATA_QUEUE_SIZE = 10

  // Init block to fetch user profile automatically after authentication
  init {
    val uid = repository.getCurrentUserUid()
    if (uid != null) {
      getUserProfile(uid)
    } else {
      isLoading_.value = false
    }
  }

  // Factory for creating UserProfileViewModel with Firestore dependency
  companion object {
    val Factory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserProfileViewModel(
                UserProfileRepositoryFirestore(FirebaseFirestore.getInstance()))
                as T
          }
        }
  }

  /**
   * Adds a new user profile or updates an existing one.
   *
   * @param userProfile The user profile to be created or updated.
   */
  fun createOrUpdateUserProfile(userProfile: UserProfile) {
    if (userProfile_.value == null) {
      // Create a new profile if none exists
      Log.d("UserProfileViewModel", "Creating new user profile.")
      addUserProfile(userProfile)
    } else {
      // Update the existing profile
      Log.d("UserProfileViewModel", "Updating existing user profile.")
      updateUserProfile(userProfile)
    }
  }

  /**
   * Adds a new user profile to Firestore.
   *
   * @param userProfile The user profile to be added.
   */
  fun addUserProfile(userProfile: UserProfile) {
    repository.addUserProfile(
        userProfile = userProfile,
        onSuccess = {
          userProfile_.value = userProfile // Set the newly added profile
          Log.d("UserProfileViewModel", "Profile added successfully.")
          // Add the profile to the list containing all profiles
          allProfiles_.value += userProfile
        },
        onFailure = { Log.e("UserProfileViewModel", "Failed to add user profile.", it) })
  }

  /**
   * Fetches the user profile for the current user and updates the state flow.
   *
   * @param uid The UID of the user whose profile is to be fetched.
   */
  fun getUserProfile(uid: String) {
    isLoading_.value = true
    repository.getUserProfile(
        uid = uid,
        onSuccess = { profile ->
          userProfile_.value = profile
          profile?.let {
            // Fetch Friends Profiles
            fetchFriendsProfiles(it.friends)

            // Fetch Received Friend Requests Profiles
            fetchRecReqProfiles(it.recReq)

            // Fetch Sent Friend Requests Profiles
            fetchSentReqProfiles(it.sentReq)

            // Optionally, fetch all user profiles if needed
            fetchAllUserProfiles()
          }
          isLoading_.value = false
          Log.d("UserProfileViewModel", "User profile fetched successfully.")
          Log.d("UserProfileViewModel", "Friends: ${profile?.name}")
        },
        onFailure = {
          // Handle error
          Log.e("UserProfileViewModel", "Failed to fetch user profile.", it)
          isLoading_.value = false
        })
  }

  /**
   * Fetches the friends' profiles based on the UIDs stored in the user's profile.
   *
   * @param friendUids List of UIDs of the friends to be retrieved.
   */
  private fun fetchFriendsProfiles(friendUids: List<String>) {
    repository.getFriendsProfiles(
        friendUids = friendUids,
        onSuccess = { profiles -> friendsProfiles_.value = profiles },
        onFailure = {
          // Handle error
          Log.e("UserProfileViewModel", "Failed to fetch friends' profiles.", it)
        })
  }

  /**
   * Fetches the friends' profiles based on the UIDs stored in the user's profile.
   *
   * @param recReqUIds List of UIDs of the friends to be retrieved.
   */
  private fun fetchRecReqProfiles(recReqUIds: List<String>) {
    repository.getRecReqProfiles(
        recReqUIds = recReqUIds,
        onSuccess = { profiles -> recReqProfiles_.value = profiles },
        onFailure = {
          // Handle error
          Log.e("UserProfileViewModel", "Failed to fetch friends' profiles.", it)
        })
  }
  /**
   * Fetches the friends' profiles based on the UIDs stored in the user's profile.
   *
   * @param friendUids List of UIDs of the friends to be retrieved.
   */
  private fun fetchSentReqProfiles(friendUids: List<String>) {
    repository.getSentReqProfiles(
        sentReqProfiles = friendUids,
        onSuccess = { profiles -> sentReqProfiles_.value = profiles },
        onFailure = {
          // Handle error
          Log.e("UserProfileViewModel", "Failed to fetch friends' profiles.", it)
        })
  }

  /** Fetches all the user profiles */
  private fun fetchAllUserProfiles() {
    repository.getAllUserProfiles(
        onSuccess = { profiles -> allProfiles_.value = profiles },
        onFailure = {
          // Handle error
          Log.e("UserProfileViewModel", "Failed to fetch friends' profiles.", it)
        })
  }

  /**
   * Adds a user profile to the current user's list of friends.
   *
   * @param friend The user profile of the friend to be added.
   */
  /**
   * Accepts a friend request from the specified friend.
   *
   * @param friend The `UserProfile` of the user whose request is being accepted.
   */
  fun acceptFriend(friend: UserProfile) {
    val currentUid = repository.getCurrentUserUid()
    if (currentUid != null) {
      repository.acceptFriendRequest(
          currentUid = currentUid,
          friendUid = friend.uid,
          onSuccess = {
            Log.d("UserProfileViewModel", "Friend request accepted from ${friend.name}")

            // Update local state for friends
            val updatedFriendsList = userProfile_.value?.friends?.plus(friend.uid)
            if (updatedFriendsList != null) {
              val updatedProfile = userProfile_.value!!.copy(friends = updatedFriendsList)
              userProfile_.value = updatedProfile
              friendsProfiles_.value += friend

              // Remove from received requests
              val updatedRecReq = userProfile_.value?.recReq?.minus(friend.uid)
              if (updatedRecReq != null) {
                val updatedProfileRecReq = userProfile_.value!!.copy(recReq = updatedRecReq)
                userProfile_.value = updatedProfileRecReq
                recReqProfiles_.value = recReqProfiles_.value.filter { it.uid != friend.uid }
              }
            }
          },
          onFailure = { exception ->
            Log.e("UserProfileViewModel", "Failed to accept friend request.", exception)
            // Optionally, notify the UI about the failure
          })
    } else {
      Log.e("UserProfileViewModel", "Cannot accept friend request: User is not authenticated.")
      // Optionally, handle unauthenticated state here (e.g., prompt user to log in)
    }
  }

  /**
   * Declines a friend request from the specified friend.
   *
   * @param friend The `UserProfile` of the user whose request is being declined.
   */
  fun declineFriendRequest(friend: UserProfile) {
    val currentUid = repository.getCurrentUserUid()
    if (currentUid != null) {
      repository.declineFriendRequest(
          currentUid = currentUid,
          friendUid = friend.uid,
          onSuccess = {
            Log.d("UserProfileViewModel", "Friend request declined from ${friend.name}")

            // Update local state by removing the declined request
            val updatedRecReq = userProfile_.value?.recReq?.minus(friend.uid)
            if (updatedRecReq != null) {
              val updatedProfile = userProfile_.value!!.copy(recReq = updatedRecReq)
              userProfile_.value = updatedProfile
              recReqProfiles_.value = recReqProfiles_.value.filter { it.uid != friend.uid }
            }
          },
          onFailure = { exception ->
            Log.e("UserProfileViewModel", "Failed to decline friend request.", exception)
            // Optionally, notify the UI about the failure
          })
    } else {
      Log.e("UserProfileViewModel", "Cannot decline friend request: User is not authenticated.")
      // Optionally, handle unauthenticated state here (e.g., prompt user to log in)
    }
  }

  /**
   * Sends a friend request to the specified user if the user did not already sent one
   *
   * @param friend The [UserProfile] of the user to send a request to.
   */
  fun sendRequest(friend: UserProfile) {
    val currentUid = repository.getCurrentUserUid()
    if (currentUid != null) {
      // Check if there's already an incoming request from this user
      if (recReqProfiles.value.any { it.uid == friend.uid }) {
        // Handle mutual request scenario
        // This logic will be managed in the UI, so no action here
      } else {
        repository.sendFriendRequest(
            currentUid = currentUid,
            friendUid = friend.uid,
            onSuccess = {
              Log.d("UserProfileViewModel", "Friend request sent to ${friend.name}")

              // Update local state for sent requests
              val updatedSentReq = userProfile_.value?.sentReq?.plus(friend.uid)
              if (updatedSentReq != null) {
                val updatedProfile = userProfile_.value!!.copy(sentReq = updatedSentReq)
                userProfile_.value = updatedProfile
                sentReqProfiles_.value += friend
              }
            },
            onFailure = { exception ->
              Log.e("UserProfileViewModel", "Failed to send friend request.", exception)
              // Optionally, notify the UI about the failure (e.g., via another StateFlow or
              // LiveData)
            })
      }
    } else {
      Log.e("UserProfileViewModel", "Cannot send friend request: User is not authenticated.")
      // Optionally, handle unauthenticated state here (e.g., prompt user to log in)
    }
  }
  /**
   * Cancels a previously sent friend request to the specified friend.
   *
   * @param friend The `UserProfile` of the friend whose request is to be canceled.
   */
  fun cancelFriendRequest(friend: UserProfile) {
    val currentUid = repository.getCurrentUserUid()
    if (currentUid != null) {
      repository.cancelFriendRequest(
          currentUid = currentUid,
          friendUid = friend.uid,
          onSuccess = {
            Log.d("UserProfileViewModel", "Friend request to ${friend.name} canceled successfully.")

            // Update the sent requests list by removing the friend
            val updatedSentReq = userProfile_.value?.sentReq?.minus(friend.uid)
            if (updatedSentReq != null) {
              // Update the userProfile state
              val updatedProfile = userProfile_.value!!.copy(sentReq = updatedSentReq)
              userProfile_.value = updatedProfile

              // Update the sentReqProfiles state by removing the canceled friend
              sentReqProfiles_.value = sentReqProfiles_.value.filter { it.uid != friend.uid }
            }

            // Optionally, remove the friend from allProfiles if necessary
            // allProfiles_.value = allProfiles_.value // No change needed here
          },
          onFailure = { exception ->
            Log.e(
                "UserProfileViewModel",
                "Failed to cancel friend request to ${friend.name}.",
                exception)
          })
    } else {
      Log.e("UserProfileViewModel", "Cannot cancel friend request: User is not authenticated.")
    }
  }

  /**
   * Updates the user profile.
   *
   * @param profile The user profile to be updated.
   */
  private fun updateUserProfile(profile: UserProfile) {
    repository.updateUserProfile(
        userProfile = profile,
        onSuccess = {
          getUserProfile(profile.uid) // Re-fetch profile after updating
          Log.d("UserProfileViewModel", "Profile updated successfully.")
        },
        onFailure = { Log.e("UserProfileViewModel", "Failed to update user profile.", it) })
  }

  /**
   * Selects a friend's profile to view in detail.
   *
   * @param friend The friend's profile to select.
   */
  fun selectFriend(friend: UserProfile) {
    selectedFriend_.value = friend
  }

  /**
   * Uploads a profile picture.
   *
   * @param uid The UID of the user.
   * @param imageUri The URI of the image to be uploaded.
   */
  fun uploadProfilePicture(uid: String, imageUri: Uri) {
    Log.d("UserProfileViewModel", "Uploading profile picture for user: $uid with URI: $imageUri")
    repository.uploadProfilePicture(
        uid,
        imageUri,
        onSuccess = { downloadUrl ->
          Log.d(
              "UserProfileViewModel",
              "Profile picture uploaded successfully. Download URL: $downloadUrl")
          updateUserProfilePicture(uid, downloadUrl)
        },
        onFailure = { exception ->
          Log.e("UserProfileViewModel", "Failed to upload profile picture.", exception)
        })
  }

  /**
   * Updates Firestore with the profile picture URL.
   *
   * @param uid The UID of the user.
   * @param downloadUrl The download URL of the uploaded profile picture.
   */
  private fun updateUserProfilePicture(uid: String, downloadUrl: String) {
    Log.d(
        "UserProfileViewModel",
        "Updating Firestore for user: $uid with profile picture URL: $downloadUrl")
    repository.updateUserProfilePicture(
        uid,
        downloadUrl,
        onSuccess = {
          Log.d("UserProfileViewModel", "Profile picture URL updated successfully in Firestore.")
          // Optionally, fetch the profile again to check
          getUserProfile(uid)
        },
        onFailure = { exception ->
          Log.e(
              "UserProfileViewModel",
              "Failed to update profile picture URL in Firestore.",
              exception)
        })
  }

  /**
   * Checks if the user profile is incomplete. This method checks if essential fields (like name)
   * are missing or blank.
   *
   * @return True if the profile is incomplete, false otherwise.
   */
  fun isProfileIncomplete(): Boolean {
    // Check if the profile is still loading
    if (isLoading_.value) {
      Log.d("UserProfileViewModel", "Profile is still loading.")
      return false // While loading, return false to prevent redirection
    }

    val profile = userProfile_.value
    Log.d("UserProfileViewModel", "Checking profile completeness. Profile: $profile")

    return profile == null || profile.name.isBlank()
  }

  /**
   * Deletes a friend from both the current user's and friend's list of friends.
   *
   * @param friend The `UserProfile` of the friend to be deleted.
   */
  fun deleteFriend(friend: UserProfile) {
    val currentUid = repository.getCurrentUserUid()
    if (currentUid != null) {
      repository.deleteFriend(
          currentUid = currentUid,
          friendUid = friend.uid,
          onSuccess = {
            Log.d("UserProfileViewModel", "Friend ${friend.name} removed successfully.")

            // Update the sent requests list by removing the friend
            val updatedFriends = userProfile_.value?.recReq?.minus(friend.uid)
            if (updatedFriends != null) {
              // Update the userProfile state
              val updatedProfile = userProfile_.value!!.copy(friends = updatedFriends)
              userProfile_.value = updatedProfile

              // Update the sentReqProfiles state by removing the canceled friend
              friendsProfiles_.value = friendsProfiles_.value.filter { it.uid != friend.uid }
            }

            // Optionally, remove the friend from allProfiles if necessary
            // allProfiles_.value = allProfiles_.value // No change needed here
          },
          onFailure = { exception ->
            Log.e("UserProfileViewModel", "Failed to delete friend.", exception)
            // Optionally, notify the UI about the failure (e.g., via another StateFlow or LiveData)
          })
    } else {
      Log.e("UserProfileViewModel", "Cannot delete friend: User is not authenticated.")
      // Optionally, handle unauthenticated state here (e.g., prompt user to log in)
    }
  }

  /**
   * Adds an AnalysisData object to the queue while ensuring the queue maintains a maximum size of
   * 10 elements.
   *
   * This function adds the given data to the end of the queue. If the queue already contains 10
   * elements, the oldest element (at the front of the queue) is removed before adding the new
   * metric. It also validates the AnalysisData object before adding it to ensure data integrity.
   *
   * @param queue The MutableStateFlow containing the ArrayDeque of AnalysisData.
   * @param value The new AnalysisData object to be added to the queue.
   * @return The updated ArrayDeque with the new value if valid, otherwise the original queue.
   */
  private fun addData(
      queue: MutableStateFlow<ArrayDeque<AnalysisData>>,
      value: AnalysisData
  ): ArrayDeque<AnalysisData> {
    // Validate the AnalysisData object
    if (!value.isValid()) {
      Log.e("addData", "Attempted to add invalid AnalysisData: $value")
      return queue.value // Return the original queue without adding invalid data
    }

    // Add data to the queue while maintaining a maximum size of 10
    val updatedQueue =
        queue.value.apply {
          if (size >= MAX_RECENT_DATA_QUEUE_SIZE) {
            removeFirst() // Remove the oldest element if the queue is full
          }
          addLast(value) // Add the new data to the end of the queue
        }

    // Update the MutableStateFlow with the new queue
    queue.value = updatedQueue

    return updatedQueue
  }

  /**
   * Adds the latest analysis data to its respective queue and update the profile to save the
   * updated queue
   *
   * @param value The new value to be added to the queue.
   */
  fun addNewestData(value: AnalysisData) {
    val currentUserProfile = userProfile_.value
    if (currentUserProfile != null) {
      val currentStats = currentUserProfile.statistics
      val updatedQueue = addData(recentData_, value)

      // Create a new statistics object with the updated queue
      val updatedStats = currentStats.copy(recentData = updatedQueue)

      // Create a new profile object with the updated queue
      val updatedProfile = currentUserProfile.copy(statistics = updatedStats)

      // Updates the user profile with the new one
      updateUserProfile(updatedProfile)

      userProfile_.value = updatedProfile
    } else {
      Log.e("UserProfileViewModel", "Failed to add new metric value: Current user profile is null.")
    }
  }

  /**
   * Calculates the means of the values of talk time seconds and percentage queues and update the
   * profile with the new means
   */
  fun updateMetricMean() {
    val currentUserProfile = userProfile_.value

    if (currentUserProfile != null) {
      val currentStats = currentUserProfile.statistics
      // Calculate the mean of the values of the metric queues
      val updatedTalkTimeSecMean =
          repository.getMetricMean(recentData_.value.map { data -> data.talkTimeSeconds })
      val updatedTalkTimePercMean =
          repository.getMetricMean(recentData_.value.map { data -> data.talkTimePercentage })

      // Create a new statistics object with the updated means
      val updatedStats =
          currentStats.copy(
              talkTimeSecMean = updatedTalkTimeSecMean, talkTimePercMean = updatedTalkTimePercMean)

      // Create a new profile object with the updated stats
      val updatedProfile = currentUserProfile.copy(statistics = updatedStats)

      // Updates the user profile with the new one
      updateUserProfile(updatedProfile)

      userProfile_.value = updatedProfile
    } else {
      Log.e("UserProfileViewModel", "Failed to update metric means: Current user profile is null.")
    }
  }

  /**
   * Updates the session result in the user profile statistics.
   *
   * @param isSuccess The result of the session.
   * @param sessionType The type of session.
   */
  fun updateSessionResult(isSuccess: Boolean, sessionType: SessionType) {
    val currentUserProfile = userProfile_.value
    if (currentUserProfile != null) {
      val currentStats = currentUserProfile.statistics

      val sessionTypeKey = sessionType.name

      // Update sessions given
      val updatedSessionsGiven = currentStats.sessionsGiven.toMutableMap()
      updatedSessionsGiven[sessionTypeKey] = (updatedSessionsGiven[sessionTypeKey] ?: 0) + 1

      // Update successful sessions if applicable
      val updatedSuccessfulSessions = currentStats.successfulSessions.toMutableMap()
      if (isSuccess) {
        updatedSuccessfulSessions[sessionTypeKey] =
            (updatedSuccessfulSessions[sessionTypeKey] ?: 0) + 1
      }

      val updatedStats =
          currentStats.copy(
              sessionsGiven = updatedSessionsGiven, successfulSessions = updatedSuccessfulSessions)

      val updatedProfile = currentUserProfile.copy(statistics = updatedStats)

      // Save the updated profile to the database
      updateUserProfile(updatedProfile)

      // Update the StateFlow
      userProfile_.value = updatedProfile
    } else {
      Log.e("UserProfileViewModel", "Current user profile is null.")
    }
  }

  fun updateLoginStreak() {
    val uid = repository.getCurrentUserUid()
    if (uid != null) {
      repository.updateLoginStreak(
          uid = uid,
          onSuccess = {
            // Optionally, fetch the updated profile
            getUserProfile(uid)
            Log.d("UserProfileViewModel", "Login streak updated successfully.")
          },
          onFailure = { Log.e("UserProfileViewModel", "Failed to update login streak.") })
    } else {
      Log.e("UserProfileViewModel", "Cannot update streak: User is not authenticated.")
    }
  }

  /**
   * <<<<<<< HEAD Ensures that a given list contains exactly 10 elements. If the list has fewer than
   * 10 elements, the missing elements are filled with zeros. If the list has more than 10 elements,
   * only the first 10 elements are returned.
   *
   * @param inputList The input list of floats to process.
   * @return A list of exactly 10 integers.
   */
  fun ensureListSizeTen(inputList: List<Float>): List<Float> {
    // Calculate the number of missing elements to make the list size 10
    val missingElements = MAX_RECENT_DATA_QUEUE_SIZE - inputList.size

    // If the list already has 10 or more elements, return the first 10 elements
    if (missingElements <= 0) {
      return inputList.take(MAX_RECENT_DATA_QUEUE_SIZE)
    }
    // Otherwise, append the required number of zeros
    return inputList + List(missingElements) { 0f }
  }

  /**
   * Fetches the name of a user based on their UID.
   *
   * @param uid The UID of the user.
   * @return The name of the user.
   */
  fun getName(uid: String): String {
    val profile = allProfiles_.value.find { it.uid == uid }
    return profile?.name ?: "Unknown"
  }

  /**
   * Calculates the success ratio for a given practice mode based on the user's statistics.
   *
   * The success ratio is computed as the ratio of successful sessions to total sessions for the
   * specified practice mode. If the number of failed sessions is zero or the data for the practice
   * mode is unavailable, the function returns -1.0.
   *
   * @param userStatistics The user's statistics containing session data.
   * @param practiceMode The practice mode for which to calculate the success ratio.
   * @return The success ratio as a [Double], or -1.0 if the ratio cannot be calculated.
   */
  fun getSuccessRatioForMode(userStatistics: UserStatistics, practiceMode: SessionType): Double {
    if (userStatistics.successfulSessions.contains(practiceMode.toString())) {
      val nbrSuccess = userStatistics.successfulSessions[practiceMode.toString()]
      val totalNbrSessions = userStatistics.sessionsGiven[practiceMode.toString()]
      if (nbrSuccess != null &&
          totalNbrSessions != null &&
          nbrSuccess >= 0 &&
          totalNbrSessions > 0) {
        return nbrSuccess.toDouble() / totalNbrSessions.toDouble()
      }
      return -1.0
    } else {
      return -1.0
    }
  }

  /**
   * Retrieves the number of successful sessions for a given practice mode from the user's
   * statistics.
   *
   * If the data for the specified practice mode is unavailable or the number of successful sessions
   * is null, the function returns -1.
   *
   * @param userStatistics The user's statistics containing session data.
   * @param practiceMode The practice mode for which to retrieve the number of successful sessions.
   * @return The number of successful sessions as an [Int], or -1 if the data is unavailable.
   */
  fun getSuccessForMode(userStatistics: UserStatistics, practiceMode: SessionType): Int {
    if (userStatistics.successfulSessions.contains(practiceMode.toString())) {
      val nbrSuccess = userStatistics.successfulSessions[practiceMode.toString()]
      if (nbrSuccess != null) {
        return nbrSuccess
      }
      return -1
    }
    return -1
  }
}
