package com.github.se.orator.model.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*

/**
 * ViewModel for managing user profiles and friends' profiles.
 *
 * @property repository The repository for accessing user profile data.
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

  // Selected friend's profile
  private val selectedFriend_ = MutableStateFlow<UserProfile?>(null)
  val selectedFriend: StateFlow<UserProfile?> = selectedFriend_.asStateFlow()

  // Loading state to indicate if the profile is being fetched
  private val isLoading_ = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = isLoading_.asStateFlow()

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
          profile?.friends?.let {
            fetchFriendsProfiles(it)
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
  fun addFriend(friend: UserProfile) {
    val currentUserProfile = userProfile_.value
    if (currentUserProfile != null) {
      // Check if the friend is already in the list to avoid duplicates
      if (!currentUserProfile.friends.contains(friend.uid)) {
        val updatedFriendsList =
            currentUserProfile.friends.toMutableList().apply { add(friend.uid) }

        // Create a new profile object with the updated friends list
        val updatedProfile = currentUserProfile.copy(friends = updatedFriendsList)

        // Update the user profile with the new friends list
        updateUserProfile(updatedProfile)

        // Optionally: Update the state with the new friend in friendsProfiles
        friendsProfiles_.value += friend
      } else {
        Log.d("UserProfileViewModel", "Friend ${friend.name} is already in the list.")
      }
    } else {
      Log.e("UserProfileViewModel", "Failed to add friend: Current user profile is null.")
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
}
