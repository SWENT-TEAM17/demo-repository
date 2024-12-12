package com.github.se.orator.model.profile

import android.net.Uri
import android.util.Log
import com.github.se.orator.utils.formatDate
import com.github.se.orator.utils.getCurrentDate
import com.github.se.orator.utils.getDaysDifference
import com.github.se.orator.utils.mapToSpeechBattle
import com.github.se.orator.utils.parseDate
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

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
   * Deletes a user profile from Firestore.
   *
   * @param uid The UID of the user to delete.
   * @param onSuccess Callback invoked on successful deletion.
   * @param onFailure Callback invoked with an [Exception] on failure.
   */
  override fun deleteUserProfile(
      uid: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performFirestoreOperation(
        db.collection(collectionPath).document(uid).delete(), onSuccess, onFailure)
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
      val lastLoginDate = document.getString("lastLoginDate") ?: "1970-01-01"
      val currentStreak = document.getLong("currentStreak") ?: 0L

      // Retrieve 'statistics' map
      val statisticsMap = document.get("statistics") as? Map<String, Any>
      val statistics =
          statisticsMap?.let {
            val improvement = (it["improvement"] as? Number)?.toFloat() ?: 0.0f

            // Extract 'sessionsGiven' map
            val sessionsGivenMap = it["sessionsGiven"] as? Map<String, Long> ?: emptyMap()
            val sessionsGiven = sessionsGivenMap.mapValues { entry -> entry.value.toInt() }

            // Extract 'successfulSessions' map
            val successfulSessionsMap = it["successfulSessions"] as? Map<String, Long> ?: emptyMap()
            val successfulSessions =
                successfulSessionsMap.mapValues { entry -> entry.value.toInt() }

            // Extract 'previousRuns' list
            val previousRunsList = it["previousRuns"] as? List<Map<String, Any>>
            val previousRuns =
                previousRunsList?.map { run ->
                  SpeechStats(
                      title = run["title"] as? String ?: "",
                      duration = (run["duration"] as? Number)?.toInt() ?: 0,
                      date = run["date"] as? Timestamp ?: Timestamp.now(),
                      accuracy = (run["accuracy"] as? Number)?.toFloat() ?: 0.0f,
                      wordsPerMinute = (run["wordsPerMinute"] as? Number)?.toInt() ?: 0)
                } ?: emptyList()

            // Extract 'battleStats' list
            val battleStatsList = it["battleStats"] as? List<Map<String, Any>>
            val battleStats =
                battleStatsList?.mapNotNull { battle -> mapToSpeechBattle(battle) } ?: emptyList()

            UserStatistics(
                sessionsGiven = sessionsGiven,
                successfulSessions = successfulSessions,
                improvement = improvement,
                previousRuns = previousRuns,
                battleStats = battleStats)
          } ?: UserStatistics()

      // Retrieve other fields from the document
      val friends = document.get("friends") as? List<String> ?: emptyList()
      val recReq = document.get("recReq") as? List<String> ?: emptyList()
      val sentReq = document.get("sentReq") as? List<String> ?: emptyList()
      val profilePic = document.getString(FIELD_PROFILE_PIC)
      val bio = document.getString("bio")

      // Construct and return the 'UserProfile' object
      UserProfile(
          uid = uid,
          name = name,
          age = age,
          statistics = statistics,
          friends = friends,
          recReq = recReq,
          sentReq = sentReq,
          profilePic = profilePic,
          currentStreak = currentStreak,
          lastLoginDate = lastLoginDate,
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
   * Helper function to fetch user profiles based on a list of UIDs.
   *
   * @param uids List of UIDs to fetch profiles for.
   * @param errorMessage Custom error message for logging.
   * @param onSuccess Callback invoked with the list of fetched profiles.
   * @param onFailure Callback invoked with an exception if the operation fails.
   */
  private fun fetchUserProfilesByUids(
      uids: List<String>,
      errorMessage: String,
      onSuccess: (List<UserProfile>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    if (uids.isEmpty()) {
      onSuccess(emptyList()) // Return an empty list if no UIDs are provided
      return
    }

    db.collection(collectionPath)
        .whereIn("uid", uids)
        .get()
        .addOnSuccessListener { querySnapshot ->
          val profiles = querySnapshot.documents.mapNotNull { documentToUserProfile(it) }
          onSuccess(profiles)
        }
        .addOnFailureListener { exception ->
          Log.e("UserProfileRepository", errorMessage, exception)
          onFailure(exception)
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
    fetchUserProfilesByUids(
        uids = friendUids,
        errorMessage = "Error fetching friends profiles",
        onSuccess = onSuccess,
        onFailure = onFailure)
  }

  /**
   * Get profiles the user received requests from based on the UIDs stored in the user's profile.
   *
   * @param recReqUIds List of UIDs of the users the user received requests from to be retrieved.
   * @param onSuccess Callback to be invoked with the list of received friend requests profiles.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun getRecReqProfiles(
      recReqUIds: List<String>,
      onSuccess: (List<UserProfile>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    fetchUserProfilesByUids(
        uids = recReqUIds,
        errorMessage = "Error fetching received friend requests profiles",
        onSuccess = onSuccess,
        onFailure = onFailure)
  }

  /**
   * Get sent requests profiles based on their UIDs.
   *
   * @param sentReqProfiles List of UIDs of the sent friend requests.
   * @param onSuccess Callback to be invoked with the list of sent requests profiles.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun getSentReqProfiles(
      sentReqProfiles: List<String>,
      onSuccess: (List<UserProfile>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    fetchUserProfilesByUids(
        uids = sentReqProfiles,
        errorMessage = "Error fetching sent friend requests profiles",
        onSuccess = onSuccess,
        onFailure = onFailure)
  }

  /**
   * Helper function to perform Firestore transactions with user documents.
   *
   * @param currentUid The UID of the current user.
   * @param friendUid The UID of the other user involved.
   * @param transactionBlock The block of code to execute within the transaction.
   * @param onSuccess Callback invoked on successful transaction.
   * @param onFailure Callback invoked with an [Exception] on failure.
   */
  private fun performUserTransaction(
      currentUid: String,
      friendUid: String,
      transactionBlock: (Transaction, DocumentSnapshot, DocumentSnapshot) -> Unit,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    val currentUserRef = db.collection(collectionPath).document(currentUid)
    val friendUserRef = db.collection(collectionPath).document(friendUid)

    db.runTransaction { transaction ->
          val currentUserSnapshot = transaction.get(currentUserRef)
          val friendUserSnapshot = transaction.get(friendUserRef)

          transactionBlock(transaction, currentUserSnapshot, friendUserSnapshot)
        }
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { exception -> onFailure(exception) }
  }

  /**
   * Sends a friend request from the current user to another user.
   *
   * @param currentUid The UID of the current user.
   * @param friendUid The UID of the user to send the friend request to.
   * @param onSuccess Callback invoked on successful operation.
   * @param onFailure Callback invoked with an [Exception] on failure.
   */
  override fun sendFriendRequest(
      currentUid: String,
      friendUid: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performUserTransaction(
        currentUid,
        friendUid,
        { transaction, currentUserSnapshot, friendUserSnapshot ->
          val currentSentReq = currentUserSnapshot.get("sentReq") as? List<String> ?: emptyList()
          val friendRecReq = friendUserSnapshot.get("recReq") as? List<String> ?: emptyList()

          if (currentSentReq.contains(friendUid)) {
            throw Exception("Friend request already sent.")
          }

          val updatedSentReq = currentSentReq + friendUid
          transaction.update(currentUserSnapshot.reference, "sentReq", updatedSentReq)

          val updatedRecReq = friendRecReq + currentUid
          transaction.update(friendUserSnapshot.reference, "recReq", updatedRecReq)
        },
        onSuccess,
        onFailure)
  }

  /**
   * Accepts a friend request, establishing a friendship between two users.
   *
   * @param currentUid The UID of the current user.
   * @param friendUid The UID of the user who sent the request.
   * @param onSuccess Callback invoked on successful operation.
   * @param onFailure Callback invoked with an [Exception] on failure.
   */
  override fun acceptFriendRequest(
      currentUid: String,
      friendUid: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performUserTransaction(
        currentUid,
        friendUid,
        { transaction, currentUserSnapshot, friendUserSnapshot ->
          val currentFriends = currentUserSnapshot.get("friends") as? List<String> ?: emptyList()
          val currentRecReq = currentUserSnapshot.get("recReq") as? List<String> ?: emptyList()
          val friendSentReq = friendUserSnapshot.get("sentReq") as? List<String> ?: emptyList()
          val friendFriends = friendUserSnapshot.get("friends") as? List<String> ?: emptyList()

          if (!currentRecReq.contains(friendUid)) {
            throw Exception("No friend request from this user to accept.")
          }

          if (!friendSentReq.contains(currentUid)) {
            throw Exception("No sent friend request from this user to this user.")
          }

          val updatedCurrentFriends = currentFriends + friendUid
          transaction.update(currentUserSnapshot.reference, "friends", updatedCurrentFriends)

          val updatedCurrentRecReq = currentRecReq - friendUid
          transaction.update(currentUserSnapshot.reference, "recReq", updatedCurrentRecReq)

          val updatedFriendSentReq = friendSentReq - currentUid
          transaction.update(friendUserSnapshot.reference, "sentReq", updatedFriendSentReq)

          val updatedFriendFriends = friendFriends + currentUid
          transaction.update(friendUserSnapshot.reference, "friends", updatedFriendFriends)
        },
        onSuccess,
        onFailure)
  }

  /**
   * Declines a friend request from another user.
   *
   * @param currentUid The UID of the current user.
   * @param friendUid The UID of the user who sent the request.
   * @param onSuccess Callback invoked on successful operation.
   * @param onFailure Callback invoked with an [Exception] on failure.
   */
  override fun declineFriendRequest(
      currentUid: String,
      friendUid: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performUserTransaction(
        currentUid,
        friendUid,
        { transaction, currentUserSnapshot, friendUserSnapshot ->
          val currentRecReq = currentUserSnapshot.get("recReq") as? List<String> ?: emptyList()
          val friendSentReq = friendUserSnapshot.get("sentReq") as? List<String> ?: emptyList()

          if (!currentRecReq.contains(friendUid)) {
            throw Exception("No friend request from this user to decline.")
          }

          if (!friendSentReq.contains(currentUid)) {
            throw Exception("No sent friend request from current user to this user.")
          }

          val updatedCurrentRecReq = currentRecReq - friendUid
          transaction.update(currentUserSnapshot.reference, "recReq", updatedCurrentRecReq)

          val updatedFriendSentReq = friendSentReq - currentUid
          transaction.update(friendUserSnapshot.reference, "sentReq", updatedFriendSentReq)
        },
        onSuccess,
        onFailure)
  }

  /**
   * Cancels a previously sent friend request.
   *
   * This function removes the `friendUid` from the current user's `sentReq` list and removes the
   * `currentUid` from the friend's `recReq` list, effectively canceling the friend request.
   *
   * @param currentUid The UID of the current user who sent the friend request.
   * @param friendUid The UID of the friend to whom the request was sent.
   * @param onSuccess Callback to be invoked on successful cancellation.
   * @param onFailure Callback to be invoked on failure with the exception.
   */
  override fun cancelFriendRequest(
      currentUid: String,
      friendUid: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performUserTransaction(
        currentUid,
        friendUid,
        { transaction, currentUserSnapshot, friendUserSnapshot ->
          val currentSentReq = currentUserSnapshot.get("sentReq") as? List<String> ?: emptyList()
          val friendRecReq = friendUserSnapshot.get("recReq") as? List<String> ?: emptyList()

          if (!currentSentReq.contains(friendUid)) {
            throw Exception("No sent friend request to cancel.")
          }

          if (!friendRecReq.contains(currentUid)) {
            throw Exception("Friend does not have you in their received requests.")
          }

          val updatedSentReq = currentSentReq - friendUid
          transaction.update(currentUserSnapshot.reference, "sentReq", updatedSentReq)

          val updatedRecReq = friendRecReq - currentUid
          transaction.update(friendUserSnapshot.reference, "recReq", updatedRecReq)
        },
        {
          // Additional logging can be handled here if needed
          Log.d("UserProfileRepositoryFirestore", "Friend request canceled successfully.")
          onSuccess()
        },
        { exception ->
          Log.e("UserProfileRepositoryFirestore", "Failed to cancel friend request.", exception)
          onFailure(exception)
        })
  }

  /**
   * Deletes an existing friendship between two users.
   *
   * @param currentUid The UID of the current user.
   * @param friendUid The UID of the friend to remove.
   * @param onSuccess Callback invoked on successful operation.
   * @param onFailure Callback invoked with an [Exception] on failure.
   */
  override fun deleteFriend(
      currentUid: String,
      friendUid: String,
      onSuccess: () -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    performUserTransaction(
        currentUid,
        friendUid,
        { transaction, currentUserSnapshot, friendUserSnapshot ->
          val currentFriends = currentUserSnapshot.get("friends") as? List<String> ?: emptyList()
          val friendFriends = friendUserSnapshot.get("friends") as? List<String> ?: emptyList()

          if (!currentFriends.contains(friendUid)) {
            throw Exception("Users are not friends.")
          }

          val updatedCurrentFriends = currentFriends - friendUid
          transaction.update(currentUserSnapshot.reference, "friends", updatedCurrentFriends)

          val updatedFriendFriends = friendFriends - currentUid
          transaction.update(friendUserSnapshot.reference, "friends", updatedFriendFriends)
        },
        onSuccess,
        onFailure)
  }

  /**
   * Calculates the mean (average) of the elements in a given list.
   *
   * This function takes a list of numerical values (`List<Double>`) and returns the mean of its
   * elements. If the list is empty or contains only invalid numbers (e.g., NaN, Infinity), the
   * function returns 0.0. If the list contains any NaN or infinite values, an
   * IllegalArgumentException is thrown to alert the caller.
   *
   * @param values A `List<Double>` containing the numerical values. Defaults to an empty list if
   *   not provided.
   * @return The mean of the values, or 0.0 if the list is empty or contains only invalid numbers.
   * @throws IllegalArgumentException If the list contains NaN or infinite values.
   */
  override fun getMetricMean(values: List<Double>): Double {
    // Check if the list is empty to avoid division by zero
    if (values.isEmpty()) return 0.0

    // Check for invalid values (NaN or infinite)
    if (values.any { it.isNaN() || it.isInfinite() }) {
      throw IllegalArgumentException("Input contains NaN or infinite values")
    }

    // Sum the elements and divide by the size of the list
    val sum = values.sum()
    return sum / values.size
  }

  override fun updateLoginStreak(uid: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
    val userRef = db.collection(collectionPath).document(uid)
    db.runTransaction { transaction ->
          val snapshot = transaction.get(userRef)
          val currentDate = getCurrentDate()
          val lastLoginDateString = snapshot.getString("lastLoginDate")
          val currentStreak = snapshot.getLong("currentStreak") ?: 0L
          val updatedStreak: Long
          val lastLoginDate: Date?
          if (lastLoginDateString != null) {
            lastLoginDate = parseDate(lastLoginDateString)
            val daysDifference = getDaysDifference(lastLoginDate, currentDate)
            updatedStreak =
                when (daysDifference) {
                  0L -> currentStreak // Same day login
                  1L -> currentStreak + 1 // Consecutive day
                  else -> 1L // Streak broken
                }
          } else {
            // First-time login
            updatedStreak = 1L
          }
          // Update the fields
          transaction.update(
              userRef,
              mapOf("lastLoginDate" to formatDate(currentDate), "currentStreak" to updatedStreak))
        }
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { exception ->
          Log.e("UserProfileRepository", "Error updating login streak", exception)
          onFailure()
        }
  }
}
