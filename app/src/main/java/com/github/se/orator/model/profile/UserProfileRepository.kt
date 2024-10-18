package com.github.se.orator.model.profile

import android.net.Uri

interface UserProfileRepository {
  fun getFriendsProfiles(
      friendUids: List<String>,
      onSuccess: (List<UserProfile>) -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun getAllUserProfiles(onSuccess: (List<UserProfile>) -> Unit, onFailure: (Exception) -> Unit)

  fun updateUserProfilePicture(
      uid: String,
      downloadUrl: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun uploadProfilePicture(
      uid: String,
      imageUri: Uri,
      onSuccess: (String) -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun updateUserProfile(
      userProfile: UserProfile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun getUserProfile(uid: String, onSuccess: (UserProfile?) -> Unit, onFailure: (Exception) -> Unit)

  fun addUserProfile(
      userProfile: UserProfile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  )

  fun getCurrentUserUid(): String?
}
