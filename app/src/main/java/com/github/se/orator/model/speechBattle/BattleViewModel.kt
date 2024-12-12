package com.github.se.orator.model.speechBattle

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.network.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * ViewModel for managing speech battles.
 *
 * @property userProfileViewModel ViewModel for user profile data.
 * @property navigationActions Navigation actions for navigating between screens.
 * @property apiLinkViewModel ViewModel for API link data.
 */
class BattleViewModel(
    internal val battleRepository: BattleRepository,
    private val userProfileViewModel: UserProfileViewModel,
    private val navigationActions: NavigationActions,
    private val apiLinkViewModel: ApiLinkViewModel,
    private val chatViewModel: ChatViewModel
) : ViewModel() {

  // List of all the incoming battles
  private val _pendingBattles = MutableLiveData<List<SpeechBattle>>(emptyList())
  val pendingBattles: LiveData<List<SpeechBattle>> = _pendingBattles

  /**
   * Creates a battle request between the current user and a friend.
   *
   * @param friendUid The UID of the friend to battle with.
   * @param context The context of the interview for the battle.
   * @return The ID of the created battle, or null if the user profile is not available.
   */
  fun createBattleRequest(friendUid: String, context: InterviewContext): String? {
    val battleId = battleRepository.generateUniqueBattleId()

    val challengerUid = userProfileViewModel.userProfile.value?.uid ?: return null

    val speechBattle =
        SpeechBattle(
            battleId = battleId,
            challenger = challengerUid,
            opponent = friendUid,
            status = BattleStatus.PENDING,
            context = context)

    battleRepository.storeBattleRequest(speechBattle) { success ->
      if (!success) {
        Log.e("BattleViewModel", "Failed to store battle request")
        return@storeBattleRequest
      }
    }

    apiLinkViewModel.updatePracticeContext(context)

    return battleId
  }

  /** Listens for pending battles for the current user. */
  fun listenForPendingBattles() {
    val currentUserUid = userProfileViewModel.userProfile.value?.uid ?: return
    battleRepository.listenForPendingBattles(currentUserUid) { battles ->
      _pendingBattles.value = battles
    }
  }

  /**
   * Accepts a battle request.
   *
   * @param battleId The ID of the battle to accept.
   */
  fun acceptBattle(battleId: String) {
    val currentUserUid = userProfileViewModel.userProfile.value?.uid ?: return

    // Use getFriendUid to determine the friend's UID
    getOpponentUid(battleId, currentUserUid) { friendUid ->
      if (friendUid != null) {
        // Update the battle status to IN_PROGRESS
        battleRepository.updateBattleStatus(battleId, BattleStatus.IN_PROGRESS) { success ->
          if (success) {
            // Fetch the battle details to update the context
            getBattleById(battleId) { battle ->
              if (battle != null) {
                // Set the practice context for the battle
                apiLinkViewModel.updatePracticeContext(battle.context)

                // Navigate to the battle chat screen with the battleId and userId
                navigationActions.navigateToBattleScreen(battleId, currentUserUid)

                Log.d("BattleViewModel", "Battle accepted successfully. Friend UID: $friendUid")
              } else {
                Log.e(
                    "BattleViewModel", "Failed to retrieve battle details for battleId: $battleId")
              }
            }
          } else {
            Log.e("BattleViewModel", "Failed to update battle status to IN_PROGRESS.")
          }
        }
      } else {
        Log.e("BattleViewModel", "Failed to determine friend UID for battleId: $battleId")
      }
    }
  }

  /**
   * Declines a battle request.
   *
   * @param battleId The ID of the battle to decline.
   */
  fun declineBattle(battleId: String) {
    battleRepository.updateBattleStatus(battleId, BattleStatus.CANCELLED) { success ->
      if (success) {
        // Battle declined successfully
      } else {
        Log.e("BattleViewModel", "Failed to decline battle")
      }
    }
  }

  /**
   * Retrieves a SpeechBattle by its ID.
   *
   * @param battleId The ID of the battle.
   * @param callback A callback function to handle the retrieved SpeechBattle.
   */
  fun getBattleById(battleId: String, callback: (SpeechBattle?) -> Unit) {
    battleRepository.getBattleById(battleId, callback)
  }

  /** Fetches pending battles for the current user. */
  fun fetchPendingBattlesForUser() {
    val currentUserUid = userProfileViewModel.userProfile.value?.uid ?: return
    battleRepository.getPendingBattlesForUser(
        currentUserUid,
        callback = { battles -> _pendingBattles.value = battles },
        onFailure = { exception ->
          Log.e("BattleViewModel", "Error fetching pending battles", exception)
        })
  }

  /**
   * Gets the status of a battle as a Flow.
   *
   * @param battleId The ID of the battle.
   * @return A Flow emitting the BattleStatus.
   */
  fun getBattleStatus(battleId: String): Flow<BattleStatus?> = callbackFlow {
    val listener =
        battleRepository.listenToBattleUpdates(battleId) { battle ->
          if (battle != null) {
            Log.d("BattleViewModel", "Battle status updated: ${battle.status}")
            trySend(battle.status)
          } else {
            Log.d("BattleViewModel", "No battle found or error occurred")
            trySend(null)
          }
        }

    awaitClose { listener.remove() }
  }

  /**
   * Marks the user's battle as completed.
   *
   * @param battleId The ID of the battle.
   * @param userId The ID of the user.
   * @param messages The list of messages exchanged in the battle.
   */
  fun markUserBattleCompleted(battleId: String, userId: String, messages: List<Message>) {
    battleRepository.updateUserBattleData(battleId, userId, messages) { success ->
      if (success) {
        battleRepository.getBattleById(battleId) { battle ->
          if (battle != null) {
            val isChallenger = userId == battle.challenger
            val otherUserCompleted =
                if (isChallenger) {
                  battle.opponentCompleted
                } else {
                  battle.challengerCompleted
                }

            if (otherUserCompleted) {
              // Both users are now completed. Update status to COMPLETED.
              battleRepository.updateBattleStatus(battleId, BattleStatus.COMPLETED) { statusSuccess
                ->
                if (statusSuccess) {
                  navigationActions.navigateToEvaluationScreen(battleId)
                } else {
                  Log.e("BattleViewModel", "Failed to update BattleStatus to COMPLETED.")
                }
              }
            } else {
              // Navigate to the waiting screen while waiting for the other user
              val friendUid = if (isChallenger) battle.opponent else battle.challenger
              navigationActions.navigateToWaitingForCompletion(battleId, friendUid)
            }
          } else {
            Log.e("BattleViewModel", "Failed to retrieve battle for battleId: $battleId")
          }
        }
      } else {
        Log.e("BattleViewModel", "Failed to update user battle data for userId: $userId")
      }
    }
  }

  /**
   * Retrieves the opponent's UID for a given battle.
   *
   * @param battleId The ID of the battle.
   * @param userId The ID of the current user.
   * @param callback A callback function to handle the friend's UID.
   */
  fun getOpponentUid(battleId: String, userId: String, callback: (String?) -> Unit) {
    getBattleById(battleId) { battle ->
      if (battle != null) {
        val friendUid =
            if (battle.challenger == userId) {
              battle.opponent
            } else if (battle.opponent == userId) {
              battle.challenger
            } else {
              null
            }
        callback(friendUid)
      } else {
        Log.e("BattleViewModel", "Battle not found for ID: $battleId")
        callback(null)
      }
    }
  }

  /**
   * Updates the result of a battle.
   *
   * @param battleId The ID of the battle.
   * @param winnerUid The UID of the winner.
   * @param evaluationText The evaluation text from ChatGPT.
   */
  fun updateBattleResult(battleId: String, winnerUid: String, evaluationText: String) {
    battleRepository.updateBattleResult(battleId, winnerUid, evaluationText) { success ->
      if (success) {
        Log.d("BattleViewModel", "Battle result updated successfully.")
        // Optionally notify observers via LiveData or StateFlow
      } else {
        Log.e("BattleViewModel", "Failed to update battle result.")
      }
    }
  }

  fun getBattleByIdFlow(battleId: String): Flow<SpeechBattle?> = callbackFlow {
    val listener = battleRepository.listenToBattleUpdates(battleId) { battle -> trySend(battle) }
    awaitClose { listener.remove() }
  }

  fun updateBattleStatus(battleId: String, status: BattleStatus, callback: (Boolean) -> Unit) {
    battleRepository.updateBattleStatus(battleId, status, callback)
  }

  fun startBattle(battleId: String) {
    battleRepository.updateBattleStatus(battleId, BattleStatus.IN_PROGRESS) { success ->
      if (success) {
        navigationActions.navigateToBattleScreen(
            battleId, userProfileViewModel.userProfile.value?.uid ?: "")
      } else {
        Log.e("BattleViewModel", "Failed to update battle status to IN_PROGRESS.")
      }
    }
  }
}
