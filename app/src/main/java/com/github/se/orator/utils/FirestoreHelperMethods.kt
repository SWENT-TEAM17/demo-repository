package com.github.se.orator.utils

import android.util.Log
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.model.speechBattle.BattleStatus
import com.github.se.orator.model.speechBattle.EvaluationResult
import com.github.se.orator.model.speechBattle.SpeechBattle
import com.github.se.orator.ui.network.Message

/**
 * Converts a Firestore map to a SpeechBattle object.
 *
 * @param data The map containing SpeechBattle data.
 * @return A SpeechBattle object, or null if conversion fails.
 */
fun mapToSpeechBattle(data: Map<String, Any>): SpeechBattle? {
  return try {
    val battleId = data["battleId"] as? String ?: return null
    val challenger = data["challenger"] as? String ?: return null
    val opponent = data["opponent"] as? String ?: return null
    val statusString = data["status"] as? String ?: "PENDING"
    val status = BattleStatus.valueOf(statusString)

    // Convert InterviewContext
    val interviewContextMap = data["interviewContext"] as? Map<String, Any> ?: return null
    val interviewContext = convertInterviewContext(interviewContextMap) ?: return null

    // Extract the challengerCompleted and opponentCompleted fields
    val challengerCompleted = data["challengerCompleted"] as? Boolean ?: false
    val opponentCompleted = data["opponentCompleted"] as? Boolean ?: false

    // Extract challengerData and opponentData
    val challengerDataList =
        (data["challengerData"] as? List<Map<String, String>>)?.mapNotNull { messageFromMap(it) }
            ?: emptyList()
    val opponentDataList =
        (data["opponentData"] as? List<Map<String, String>>)?.mapNotNull { messageFromMap(it) }
            ?: emptyList()

    // Extract evaluationResult if available
    val evaluationMap = data["evaluationResult"] as? Map<String, Any>
    val evaluationResult = evaluationMap?.let { evaluationResultFromMap(it) }

    SpeechBattle(
        battleId = battleId,
        challenger = challenger,
        opponent = opponent,
        status = status,
        context = interviewContext,
        challengerCompleted = challengerCompleted,
        opponentCompleted = opponentCompleted,
        challengerData = challengerDataList,
        opponentData = opponentDataList,
        evaluationResult = evaluationResult)
  } catch (e: Exception) {
    Log.e("UserProfileRepository", "Error converting map to SpeechBattle", e)
    null
  }
}

/**
 * Converts a map to an InterviewContext object.
 *
 * @param contextMap The map representation of an InterviewContext.
 * @return The corresponding InterviewContext object, or null if conversion fails.
 */
private fun convertInterviewContext(contextMap: Map<String, Any>?): InterviewContext? {
  return contextMap?.let {
    InterviewContext(
        targetPosition = it["targetPosition"] as? String ?: "",
        companyName = it["companyName"] as? String ?: "",
        interviewType = it["interviewType"] as? String ?: "",
        experienceLevel = it["experienceLevel"] as? String ?: "",
        jobDescription = it["jobDescription"] as? String ?: "",
        focusArea = it["focusArea"] as? String ?: "")
  }
}

/** Converts a Firestore map to a Message object */
private fun messageFromMap(map: Map<String, String>): Message? {
  val role = map["role"] ?: return null
  val content = map["content"] ?: return null
  return Message(role, content)
}

/** Converts a Firestore map to EvaluationResult */
private fun evaluationResultFromMap(data: Map<String, Any>): EvaluationResult? {
  val winnerUid = data["winnerUid"] as? String ?: return null
  val winnerMessageMap = data["winnerMessage"] as? Map<String, String> ?: return null
  val loserMessageMap = data["loserMessage"] as? Map<String, String> ?: return null

  val winnerMessage = messageFromMap(winnerMessageMap) ?: return null
  val loserMessage = messageFromMap(loserMessageMap) ?: return null

  return EvaluationResult(
      winnerUid = winnerUid, winnerMessage = winnerMessage, loserMessage = loserMessage)
}
