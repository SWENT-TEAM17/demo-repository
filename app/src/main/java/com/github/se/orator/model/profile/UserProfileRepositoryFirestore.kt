package com.github.se.orator.model.profile

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Repository class for managing user profiles in Firestore.
 *
 * @property db The Firestore database instance.
 */
class UserProfileRepositoryFirestore(private val db: FirebaseFirestore) : UserProfileRepository {

  private val collectionPath = "user_profiles"

  companion object {
    const val FIELD_PROFILE_PIC = "profilePic"
  }

  /**
   * Get the UID for the current authenticated user.
   *
   * @return The UID of the current user, or null if not authenticated.
   */
  override fun getCurrentUserUid(): String? {
    return FirebaseAuth.getInstance().currentUser?.uid
  }

  /**
   * Add a new user profile to Firestore.
   *
   * @param userProfile The user profile to be added.
   * @param onSuccess Callback to be invoked on successful addition.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun addUserProfile(
      userProfile: UserProfile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performFirestoreOperation(
        db.collection(collectionPath).document(userProfile.uid).set(userProfile),
        onSuccess,
        onFailure)
  }

  /**
   * Get user profile by UID.
   *
   * @param uid The UID of the user whose profile is to be fetched.
   * @param onSuccess Callback to be invoked with the fetched user profile.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun getUserProfile(
      uid: String,
      onSuccess: (UserProfile?) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath).document(uid).get().addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val userProfile = task.result?.let { documentToUserProfile(it) }
        onSuccess(userProfile)
      } else {
        task.exception?.let { e ->
          Log.e("UserProfileRepository", "Error getting user profile", e)
          onFailure(e)
        }
      }
    }
  }

  /**
   * Fetches all user profiles from the Firestore database. On success, it returns a list of
   * [UserProfile] objects through the [onSuccess] callback. On failure, it returns an exception
   * through the [onFailure] callback.
   *
   * @param onSuccess A lambda function that receives a list of [UserProfile] objects if the
   *   operation succeeds.
   * @param onFailure A lambda function that receives an [Exception] if the operation fails.
   */
  override fun getAllUserProfiles(
      onSuccess: (List<UserProfile>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection(collectionPath)
        .get()
        .addOnSuccessListener { querySnapshot ->
          val profiles = querySnapshot.documents.mapNotNull { documentToUserProfile(it) }
          onSuccess(profiles)
        }
        .addOnFailureListener { exception ->
          Log.e("UserProfileRepository", "Error fetching all user profiles", exception)
          onFailure(exception)
        }
  }

  /**
   * Update an existing user profile in Firestore.
   *
   * @param userProfile The user profile to be updated.
   * @param onSuccess Callback to be invoked on successful update.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun updateUserProfile(
      userProfile: UserProfile,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performFirestoreOperation(
        db.collection(collectionPath).document(userProfile.uid).set(userProfile),
        onSuccess,
        onFailure)
  }

  /**
   * Upload profile picture to Firebase Storage.
   *
   * @param uid The UID of the user.
   * @param imageUri The URI of the image to be uploaded.
   * @param onSuccess Callback to be invoked with the download URL of the uploaded image.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun uploadProfilePicture(
      uid: String,
      imageUri: Uri,
      onSuccess: (String) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    // Create a reference to the location where the profile picture will be stored
    val storageReference =
        FirebaseStorage.getInstance().reference.child("profile_pictures/$uid.jpg")

    Log.d("FirebaseStorage", "Uploading to: profile_pictures/$uid.jpg")

    // Upload the image to Firebase Storage
    storageReference
        .putFile(imageUri)
        .addOnSuccessListener {
          // Get the download URL after successful upload
          storageReference.downloadUrl
              .addOnSuccessListener { uri ->
                // Call onSuccess with the download URL
                onSuccess(uri.toString())
              }
              .addOnFailureListener { exception ->
                // Handle the failure to get the download URL
                onFailure(exception)
              }
        }
        .addOnFailureListener { exception ->
          // Handle failure of the image upload
          onFailure(exception)
        }
  }

  /**
   * Update user profile picture URL in Firestore.
   *
   * @param uid The UID of the user.
   * @param downloadUrl The download URL of the uploaded profile picture.
   * @param onSuccess Callback to be invoked on successful update.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun updateUserProfilePicture(
      uid: String,
      downloadUrl: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    Log.d(
        "UserProfileRepositoryFirestore",
        "Attempting to update Firestore for user: $uid with URL: $downloadUrl")

    // Update the user's Firestore document with the profile picture download URL
    db.collection(collectionPath)
        .document(uid)
        .update(FIELD_PROFILE_PIC, downloadUrl)
        .addOnSuccessListener {
          Log.d(
              "UserProfileRepositoryFirestore",
              "Profile picture URL updated in Firestore successfully.")
          onSuccess()
        }
        .addOnFailureListener { exception ->
          Log.e(
              "UserProfileRepositoryFirestore",
              "Failed to update profile picture URL in Firestore.",
              exception)
          onFailure(exception)
        }
  }

  /**
   * Convert Firestore document to UserProfile object.
   *
   * @param document The Firestore document to be converted.
   * @return The converted UserProfile object, or null if conversion fails.
   */
  private fun documentToUserProfile(document: DocumentSnapshot): UserProfile? {
    return try {
      val uid = document.id
      val name = document.getString("name") ?: return null
      val age = document.getLong("age")?.toInt() ?: return null
      val statisticsMap = document.get("statistics") as? Map<*, *>
      val statistics =
          statisticsMap?.let {
            UserStatistics(
                speechesGiven = it["speechesGiven"] as? Int ?: 0,
                improvement = it["improvement"] as? Float ?: 0.0f,
                previousRuns =
                    (it["previousRuns"] as? List<Map<String, Any>>)?.map { run ->
                      SpeechStats(
                          title = run["title"] as? String ?: "",
                          duration = run["duration"] as? Int ?: 0,
                          date = run["date"] as? Timestamp ?: Timestamp.now(),
                          accuracy = run["accuracy"] as? Float ?: 0.0f,
                          wordsPerMinute = run["wordsPerMinute"] as? Int ?: 0)
                    } ?: emptyList())
          } ?: UserStatistics()

      val friends = document.get("friends") as? List<String> ?: emptyList()
      val profilePic = document.getString(FIELD_PROFILE_PIC)
      val bio = document.getString("bio")
      UserProfile(
          uid = uid,
          name = name,
          age = age,
          statistics = statistics,
          friends = friends,
          profilePic = profilePic,
          bio = bio)
    } catch (e: Exception) {
      Log.e("UserProfileRepository", "Error converting document to UserProfile", e)
      null
    }
  }

  /**
   * Helper function to perform Firestore operations.
   *
   * @param task The Firestore task to be performed.
   * @param onSuccess Callback to be invoked on successful completion.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  private fun performFirestoreOperation(
      task: Task<Void>,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    task.addOnCompleteListener { result ->
      if (result.isSuccessful) {
        onSuccess()
      } else {
        result.exception?.let { e ->
          Log.e("UserProfileRepository", "Error performing Firestore operation", e)
          onFailure(e)
        }
      }
    }
  }

  /**
   * Get friends' profiles based on the UIDs stored in the user's profile.
   *
   * @param friendUids List of UIDs of the friends to be retrieved.
   * @param onSuccess Callback to be invoked with the list of friends' profiles.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun getFriendsProfiles(
      friendUids: List<String>,
      onSuccess: (List<UserProfile>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    if (friendUids.isEmpty()) {
      onSuccess(emptyList()) // Return an empty list if no friends
      return
    }

    db.collection(collectionPath)
        .whereIn("uid", friendUids)
        .get()
        .addOnSuccessListener { querySnapshot ->
          val friends = querySnapshot.documents.mapNotNull { documentToUserProfile(it) }
          onSuccess(friends)
        }
        .addOnFailureListener { exception ->
          Log.e("UserProfileRepository", "Error fetching friends profiles", exception)
          onFailure(exception)
        }
  }
}
