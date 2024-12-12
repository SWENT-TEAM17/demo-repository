package com.github.se.orator.model.speechBattle

import android.util.Log
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.ui.network.Message
import com.github.se.orator.utils.mapToSpeechBattle
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

class BattleRepositoryFirestore(private val db: FirebaseFirestore) : BattleRepository {

  /** Generates a unique battle ID. */
  override fun generateUniqueBattleId(): String {
    return UUID.randomUUID().toString()
  }

  /**
   * Stores a battle request in the Firestore database.
   *
   * @param speechBattle The battle to store.
   * @param callback A callback function to indicate success or failure.
   */
  override fun storeBattleRequest(speechBattle: SpeechBattle, callback: (Boolean) -> Unit) {
    // Serialize the InterviewContext
    val interviewContextMap = interviewContextToMap(speechBattle.context)

    // Create a map to represent the battle data
    val battleMap =
        hashMapOf(
            "battleId" to speechBattle.battleId,
            "challenger" to speechBattle.challenger,
            "opponent" to speechBattle.opponent,
            "status" to speechBattle.status.name,
            "interviewContext" to interviewContextMap
            // Exclude "evaluationResult" as they are not set initially
            )

    // Store the battle in the "battles" collection
    db.collection("battles")
        .document(speechBattle.battleId)
        .set(battleMap)
        .addOnSuccessListener { callback(true) }
        .addOnFailureListener { e ->
          // Handle the error
          Log.e("BattleRepository", "Error storing battle request", e)
          callback(false)
        }
  }

  /**
   * Listens for pending battles for a specific user.
   *
   * @param userUid The UID of the user.
   * @param callback A callback function to handle the list of pending battles.
   */
  override fun listenForPendingBattles(
      userUid: String,
      callback: (List<SpeechBattle>) -> Unit
  ): ListenerRegistration {
    return db.collection("battles")
        .whereEqualTo("opponent", userUid)
        .whereEqualTo("status", BattleStatus.PENDING.name)
        .addSnapshotListener { snapshots, error ->
          if (error != null) {
            Log.e("BattleRepository", "Error listening for pending battles", error)
            return@addSnapshotListener
          }

          val battles =
              snapshots?.documents?.mapNotNull { doc -> documentToSpeechBattle(doc) } ?: emptyList()

          callback(battles)
        }
  }

  /**
   * Updates the status of a battle.
   *
   * @param battleId The ID of the battle.
   * @param status The new status.
   * @param callback A callback function to indicate success or failure.
   */
  override fun updateBattleStatus(
      battleId: String,
      status: BattleStatus,
      callback: (Boolean) -> Unit
  ) {
    val battleRef = db.collection("battles").document(battleId)

    battleRef
        .update("status", status.name)
        .addOnSuccessListener { callback(true) }
        .addOnFailureListener { e ->
          Log.e("BattleRepository", "Error updating battle status", e)
          callback(false)
        }
  }

  /**
   * Retrieves a SpeechBattle by its ID.
   *
   * @param battleId The ID of the battle.
   * @param callback A callback function to handle the retrieved SpeechBattle.
   */
  override fun getBattleById(battleId: String, callback: (SpeechBattle?) -> Unit) {
    val battleRef = db.collection("battles").document(battleId)

    battleRef
        .get()
        .addOnSuccessListener { document ->
          if (document != null && document.exists()) {
            val speechBattle = documentToSpeechBattle(document)
            callback(speechBattle)
          } else {
            callback(null)
          }
        }
        .addOnFailureListener { e ->
          Log.e("BattleRepository", "Error retrieving battle", e)
          callback(null)
        }
  }

  private fun documentToSpeechBattle(document: DocumentSnapshot): SpeechBattle? {
    val data = document.data ?: return null
    return mapToSpeechBattle(data)
  }

  /** Serializes an InterviewContext to a Map. */
  private fun interviewContextToMap(interviewContext: InterviewContext): Map<String, Any> {
    return mapOf(
        "targetPosition" to interviewContext.targetPosition,
        "companyName" to interviewContext.companyName,
        "interviewType" to interviewContext.interviewType,
        "experienceLevel" to interviewContext.experienceLevel,
        "jobDescription" to interviewContext.jobDescription,
        "focusArea" to interviewContext.focusArea)
  }

  override fun getPendingBattlesForUser(
      userUid: String,
      callback: (List<SpeechBattle>) -> Unit,
      onFailure: (Exception) -> Unit
  ) {
    db.collection("battles")
        .whereEqualTo("opponent", userUid)
        .whereEqualTo("status", BattleStatus.PENDING.name)
        .get()
        .addOnSuccessListener { querySnapshot ->
          val battles =
              querySnapshot.documents.mapNotNull { document -> documentToSpeechBattle(document) }
          callback(battles)
        }
        .addOnFailureListener { exception ->
          Log.e("BattleRepository", "Error fetching pending battles", exception)
          onFailure(exception)
        }
  }

  override fun listenToBattleUpdates(
      battleId: String,
      callback: (SpeechBattle?) -> Unit
  ): ListenerRegistration {
    return db.collection("battles").document(battleId).addSnapshotListener { snapshot, error ->
      if (error != null) {
        Log.e("BattleRepository", "Error listening to battle updates", error)
        callback(null)
        return@addSnapshotListener
      }

      if (snapshot != null && snapshot.exists()) {
        val battle = documentToSpeechBattle(snapshot)
        callback(battle)
      } else {
        callback(null)
      }
    }
  }

  /**
   * Updates the user's battle data with the given messages and marks the user as completed in the
   * battle.
   *
   * @param battleId The ID of the battle.
   * @param userId The ID of the user (either challenger or opponent).
   * @param messages The list of messages exchanged by the user during the battle.
   * @param callback A callback to indicate success or failure.
   */
  override fun updateUserBattleData(
      battleId: String,
      userId: String,
      messages: List<Message>,
      callback: (Boolean) -> Unit
  ) {
    val battleRef = db.collection("battles").document(battleId)

    battleRef
        .get()
        .addOnSuccessListener { document ->
          if (document.exists()) {
            val updates = mutableMapOf<String, Any>()

            // Fetch existing data
            val currentData = document.data ?: emptyMap<String, Any>()
            val challenger = currentData["challenger"] as? String
            val opponent = currentData["opponent"] as? String

            Log.d(
                "BattleRepository", "Challenger: $challenger, Opponent: $opponent, UserId: $userId")

            // Determine if the user is the challenger or opponent
            when (userId) {
              challenger -> {
                val existingMessages =
                    (currentData["challengerData"] as? List<Map<String, String>>).orEmpty()
                val newMessages = existingMessages + messages.map { it.toMap() }
                updates["challengerData"] = newMessages
                updates["challengerCompleted"] = true
              }
              opponent -> {
                val existingMessages =
                    (currentData["opponentData"] as? List<Map<String, String>>).orEmpty()
                val newMessages = existingMessages + messages.map { it.toMap() }
                updates["opponentData"] = newMessages
                updates["opponentCompleted"] = true
              }
              else -> {
                Log.e("BattleRepository", "User ID does not match challenger or opponent.")
                callback(false)
                return@addOnSuccessListener
              }
            }

            // Perform the update in Firestore
            battleRef
                .update(updates)
                .addOnSuccessListener {
                  Log.d("BattleRepository", "User battle data updated successfully.")
                  callback(true)
                }
                .addOnFailureListener { e ->
                  Log.e("BattleRepository", "Error updating battle data", e)
                  callback(false)
                }
          } else {
            Log.e("BattleRepository", "Battle document not found.")
            callback(false)
          }
        }
        .addOnFailureListener { e ->
          Log.e("BattleRepository", "Error fetching battle document.", e)
          callback(false)
        }
  }

  /**
   * Converts a Message object into a Map for Firestore storage.
   *
   * @return A Map representation of the Message object.
   */
  private fun Message.toMap(): Map<String, String> {
    return mapOf("role" to role, "content" to content)
  }

  /**
   * Updates the winner of a battle.
   *
   * @param battleId The ID of the battle.
   * @param winnerUid The UID of the winner.
   * @param evaluationText The evaluation text.
   */
  override fun updateBattleResult(
      battleId: String,
      winnerUid: String,
      evaluationText: String,
      callback: (Boolean) -> Unit
  ) {
    val battleRef = db.collection("battles").document(battleId)
    val updates =
        mapOf(
            "status" to BattleStatus.COMPLETED.name,
            "winner" to winnerUid,
            "evaluation" to evaluationText)
    battleRef
        .update(updates)
        .addOnSuccessListener {
          Log.d("BattleRepository", "Battle result updated")
          callback(true)
        }
        .addOnFailureListener { e ->
          Log.e("BattleRepository", "Error updating battle result", e)
          callback(false)
        }
  }

  /** Converts EvaluationResult to a Firestore-friendly map */
  private fun evaluationResultToMap(evaluation: EvaluationResult): Map<String, Any> {
    return mapOf(
        "winnerUid" to evaluation.winnerUid,
        "winnerMessage" to evaluation.winnerMessage.toMap(),
        "loserMessage" to evaluation.loserMessage.toMap())
  }

  /**
   * Updates the evaluation result for a given battle. This sets the status to COMPLETED, sets the
   * winner, and stores the evaluation on firebase.
   *
   * @param battleId The ID of the battle.
   * @param evaluation The evaluation result.
   * @param callback A callback function to execute.
   */
  fun updateEvaluationResult(
      battleId: String,
      evaluation: EvaluationResult,
      callback: (Boolean) -> Unit
  ) {
    val battleRef = db.collection("battles").document(battleId)
    val updates =
        mapOf(
            "evaluationResult" to evaluationResultToMap(evaluation),
            "status" to BattleStatus.COMPLETED.name,
            "winner" to evaluation.winnerUid)

    battleRef
        .update(updates)
        .addOnSuccessListener { callback(true) }
        .addOnFailureListener { e ->
          Log.e("BattleRepository", "Error updating evaluation result", e)
          callback(false)
        }
  }
}
