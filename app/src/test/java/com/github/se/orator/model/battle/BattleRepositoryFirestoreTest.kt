package com.github.se.orator.model.battle

import android.os.Looper
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.model.speechBattle.BattleRepositoryFirestore
import com.github.se.orator.model.speechBattle.BattleStatus
import com.github.se.orator.model.speechBattle.EvaluationResult
import com.github.se.orator.model.speechBattle.SpeechBattle
import com.github.se.orator.ui.network.Message
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BattleRepositoryFirestoreTest {

  @Mock private lateinit var mockFirestore: FirebaseFirestore

  @Mock private lateinit var mockCollectionReference: CollectionReference

  @Mock private lateinit var mockDocumentReference: DocumentReference

  @Mock private lateinit var mockDocumentSnapshot: DocumentSnapshot

  @Mock private lateinit var mockQuerySnapshot: QuerySnapshot

  @Mock private lateinit var mockListenerRegistration: ListenerRegistration

  private lateinit var repository: BattleRepositoryFirestore

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    // Initialize the repository with the mocked Firestore instance
    repository = BattleRepositoryFirestore(mockFirestore)

    // Mock Firestore collection and document references
    `when`(mockFirestore.collection(anyString())).thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference)
  }

  /** Test that generateUniqueBattleId returns a non-empty string */
  @Test
  fun generateUniqueBattleId_returnsNonEmptyString() {
    val battleId = repository.generateUniqueBattleId()
    assert(battleId.isNotEmpty())
  }

  /** Test storing a battle request successfully */
  @Test
  fun storeBattleRequest_success_callsCallbackWithTrue() {
    val speechBattle = createTestSpeechBattle()

    // Mock Firestore set operation to succeed
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forResult(null))

    var callbackCalled = false

    repository.storeBattleRequest(speechBattle) { success ->
      assert(success)
      callbackCalled = true
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Verify that set was called on the document reference
    verify(mockDocumentReference).set(any())
    assert(callbackCalled)
  }

  /** Test storing a battle request failure */
  @Test
  fun storeBattleRequest_failure_callsCallbackWithFalse() {
    val speechBattle = createTestSpeechBattle()

    // Mock Firestore set operation to fail
    val exception = Exception("Set failed")
    `when`(mockDocumentReference.set(any())).thenReturn(Tasks.forException(exception))

    var callbackCalled = false

    repository.storeBattleRequest(speechBattle) { success ->
      assert(!success)
      callbackCalled = true
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Verify that set was called on the document reference
    verify(mockDocumentReference).set(any())
    assert(callbackCalled)
  }

  /** Test retrieving a battle by ID successfully */
  @Test
  fun getBattleById_success_returnsSpeechBattle() {
    val battleId = "battle1"

    // Mock Firestore get operation to return a valid document
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockDocumentSnapshot))
    `when`(mockDocumentSnapshot.exists()).thenReturn(true)
    `when`(mockDocumentSnapshot.data).thenReturn(createTestBattleDataMap())

    var callbackBattle: SpeechBattle? = null

    repository.getBattleById(battleId) { battle -> callbackBattle = battle }

    shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).get()
    assertNotNull(callbackBattle)
    assertEquals("battle1", callbackBattle?.battleId)
    assertEquals("user1", callbackBattle?.challenger)
    assertEquals("user2", callbackBattle?.opponent)
    assertEquals(BattleStatus.PENDING, callbackBattle?.status)
  }

  /** Test retrieving a battle by ID when document does not exist */
  @Test
  fun getBattleById_documentDoesNotExist_returnsNull() {
    val battleId = "battle1"

    // Mock Firestore get operation to return a non-existent document
    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockDocumentSnapshot))
    `when`(mockDocumentSnapshot.exists()).thenReturn(false)

    var callbackBattle: SpeechBattle? = null

    repository.getBattleById(battleId) { battle -> callbackBattle = battle }

    shadowOf(Looper.getMainLooper()).idle()

    verify(mockDocumentReference).get()
    assertNull(callbackBattle)
  }

  /** Test listenForPendingBattles method */
  @Test
  fun listenForPendingBattles_receivesBattles_callsCallbackWithList() {
    val userUid = "user2"

    // Mock QuerySnapshot and DocumentSnapshot
    `when`(mockDocumentSnapshot.data).thenReturn(createTestBattleDataMap())
    `when`(mockQuerySnapshot.documents).thenReturn(listOf(mockDocumentSnapshot))

    // Mock Firestore query and snapshot listener
    `when`(mockCollectionReference.whereEqualTo("opponent", userUid))
        .thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.whereEqualTo("status", BattleStatus.PENDING.name))
        .thenReturn(mockCollectionReference)
    `when`(mockCollectionReference.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.getArgument<EventListener<QuerySnapshot>>(0)
      listener.onEvent(mockQuerySnapshot, null)
      mockListenerRegistration
    }

    var callbackBattles: List<SpeechBattle>? = null

    repository.listenForPendingBattles(userUid) { battles -> callbackBattles = battles }

    shadowOf(Looper.getMainLooper()).idle()

    verify(mockCollectionReference, times(2)).whereEqualTo(anyString(), any())
    verify(mockCollectionReference).addSnapshotListener(any())
    assertNotNull(callbackBattles)
    assertEquals(1, callbackBattles!!.size)
    val battle = callbackBattles!![0]
    assertEquals("battle1", battle.battleId)
    assertEquals("user2", battle.opponent)
  }

  /** Test updateBattleStatus successfully updates the status */
  @Test
  fun updateBattleStatus_success_callsCallbackWithTrue() {
    // Arrange
    val battleId = "battle1"
    val newStatus = BattleStatus.IN_PROGRESS

    // Mock Firestore update operation to succeed
    `when`(mockDocumentReference.update("status", newStatus.name)).thenReturn(Tasks.forResult(null))

    var callbackResult = false

    // Act
    repository.updateBattleStatus(battleId, newStatus) { success -> callbackResult = success }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).update("status", newStatus.name)
    assertTrue(callbackResult)
  }

  /** Test updateBattleStatus failure */
  @Test
  fun updateBattleStatus_failure_callsCallbackWithFalse() {
    // Arrange
    val battleId = "battle1"
    val newStatus = BattleStatus.IN_PROGRESS

    // Mock Firestore update operation to fail
    val exception = Exception("Update failed")
    `when`(mockDocumentReference.update("status", newStatus.name))
        .thenReturn(Tasks.forException(exception))

    var callbackResult = true // Initialize to true to check if it's set to false

    // Act
    repository.updateBattleStatus(battleId, newStatus) { success -> callbackResult = success }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).update("status", newStatus.name)
    assertFalse(callbackResult)
  }

  /** Test updateBattleResult successfully updates the battle result */
  @Test
  fun updateBattleResult_success_callsCallbackWithTrue() {
    // Arrange
    val battleId = "battle1"
    val winnerUid = "user1"
    val evaluationText = "Great performance!"

    val updates =
        mapOf(
            "status" to BattleStatus.COMPLETED.name,
            "winner" to winnerUid,
            "evaluation" to evaluationText)

    // Mock Firestore update operation to succeed
    `when`(mockDocumentReference.update(updates)).thenReturn(Tasks.forResult(null))

    var callbackResult = false

    // Act
    repository.updateBattleResult(battleId, winnerUid, evaluationText) { success ->
      callbackResult = success
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).update(updates)
    assertTrue(callbackResult)
  }

  /** Test updateBattleResult failure */
  @Test
  fun updateBattleResult_failure_callsCallbackWithFalse() {
    // Arrange
    val battleId = "battle1"
    val winnerUid = "user1"
    val evaluationText = "Great performance!"

    val updates =
        mapOf(
            "status" to BattleStatus.COMPLETED.name,
            "winner" to winnerUid,
            "evaluation" to evaluationText)

    // Mock Firestore update operation to fail
    val exception = Exception("Update failed")
    `when`(mockDocumentReference.update(updates)).thenReturn(Tasks.forException(exception))

    var callbackResult = true // Initialize to true to check if it's set to false

    // Act
    repository.updateBattleResult(battleId, winnerUid, evaluationText) { success ->
      callbackResult = success
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).update(updates)
    assertFalse(callbackResult)
  }

  /** Test listenToBattleUpdates successfully listens to updates */
  @Test
  fun listenToBattleUpdates_success_callsCallbackWithSpeechBattle() {
    // Arrange
    val battleId = "battle1"
    val mockBattleDocument = mock(DocumentSnapshot::class.java)
    val battleData = createTestBattleDataMap()
    `when`(mockBattleDocument.exists()).thenReturn(true)
    `when`(mockBattleDocument.data).thenReturn(battleData)

    // Mock Firestore addSnapshotListener to invoke callback with the mocked document
    `when`(mockDocumentReference.addSnapshotListener(any())).thenAnswer { invocation ->
      val listener = invocation.getArgument<EventListener<DocumentSnapshot>>(0)
      listener.onEvent(mockBattleDocument, null)
      mockListenerRegistration
    }

    var callbackBattle: SpeechBattle? = null

    // Act
    repository.listenToBattleUpdates(battleId) { battle -> callbackBattle = battle }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).addSnapshotListener(any())
    assertNotNull(callbackBattle)
    assertEquals("battle1", callbackBattle?.battleId)
    assertEquals("user1", callbackBattle?.challenger)
    assertEquals("user2", callbackBattle?.opponent)
    assertEquals(BattleStatus.PENDING, callbackBattle?.status)
  }

  /** Test updateUserBattleData successfully updates user data */
  @Test
  fun updateUserBattleData_success_callsCallbackWithTrue() {
    // Arrange
    val battleId = "battle1"
    val userId = "user1" // Assuming user1 is the challenger
    val messages =
        listOf(
            Message(role = "user", content = "Hello"),
            Message(role = "user", content = "How are you?"))

    // Mock Firestore get operation to return an existing battle
    val mockBattleDocument = mock(DocumentSnapshot::class.java)
    `when`(mockBattleDocument.exists()).thenReturn(true)
    `when`(mockBattleDocument.data).thenReturn(createTestBattleDataMap())

    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockBattleDocument))

    // Mock Firestore update operation to succeed
    `when`(mockDocumentReference.update(anyMap())).thenReturn(Tasks.forResult(null))

    var callbackResult = false

    // Act
    repository.updateUserBattleData(battleId, userId, messages) { success ->
      callbackResult = success
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).get()
    verify(mockDocumentReference).update(anyMap())
    assertTrue(callbackResult)
  }

  /** Test updateUserBattleData failure */
  @Test
  fun updateUserBattleData_failure_callsCallbackWithFalse() {
    // Arrange
    val battleId = "battle1"
    val userId = "user1" // Assuming user1 is the challenger
    val messages =
        listOf(
            Message(role = "user", content = "Hello"),
            Message(role = "user", content = "How are you?"))

    // Mock Firestore get operation to return an existing battle
    val mockBattleDocument = mock(DocumentSnapshot::class.java)
    `when`(mockBattleDocument.exists()).thenReturn(true)
    `when`(mockBattleDocument.data).thenReturn(createTestBattleDataMap())

    `when`(mockDocumentReference.get()).thenReturn(Tasks.forResult(mockBattleDocument))

    // Mock Firestore update operation to fail
    val exception = Exception("Update failed")
    `when`(mockDocumentReference.update(anyMap())).thenReturn(Tasks.forException(exception))

    var callbackResult = true // Initialize to true to check if it's set to false

    // Act
    repository.updateUserBattleData(battleId, userId, messages) { success ->
      callbackResult = success
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).get()
    verify(mockDocumentReference).update(anyMap())
    assertFalse(callbackResult)
  }

  /** New Test: updateEvaluationResult successfully updates the evaluation result */
  @Test
  fun updateEvaluationResult_success_callsCallbackWithTrue() {
    // Arrange
    val battleId = "battle1"
    val evaluationResult =
        EvaluationResult(
            winnerUid = "user1",
            winnerMessage =
                Message(role = "assistant", content = "Congratulations! You won the battle."),
            loserMessage =
                Message(
                    role = "assistant", content = "You lost the battle. Better luck next time."))

    val updates =
        mapOf(
            "evaluationResult" to
                mapOf(
                    "winnerUid" to "user1",
                    "winnerMessage" to
                        mapOf(
                            "role" to "assistant",
                            "content" to "Congratulations! You won the battle."),
                    "loserMessage" to
                        mapOf(
                            "role" to "assistant",
                            "content" to "You lost the battle. Better luck next time.")),
            "status" to BattleStatus.COMPLETED.name,
            "winner" to "user1")

    // Mock Firestore update operation to succeed
    `when`(mockDocumentReference.update(updates)).thenReturn(Tasks.forResult(null))

    var callbackResult = false

    // Act
    repository.updateEvaluationResult(battleId, evaluationResult) { success ->
      callbackResult = success
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).update(updates)
    assertTrue(callbackResult)
  }

  /** New Test: updateEvaluationResult failure */
  @Test
  fun updateEvaluationResult_failure_callsCallbackWithFalse() {
    // Arrange
    val battleId = "battle1"
    val evaluationResult =
        EvaluationResult(
            winnerUid = "user1",
            winnerMessage =
                Message(role = "assistant", content = "Congratulations! You won the battle."),
            loserMessage =
                Message(
                    role = "assistant", content = "You lost the battle. Better luck next time."))

    val updates =
        mapOf(
            "evaluationResult" to
                mapOf(
                    "winnerUid" to "user1",
                    "winnerMessage" to
                        mapOf(
                            "role" to "assistant",
                            "content" to "Congratulations! You won the battle."),
                    "loserMessage" to
                        mapOf(
                            "role" to "assistant",
                            "content" to "You lost the battle. Better luck next time.")),
            "status" to BattleStatus.COMPLETED.name,
            "winner" to "user1")

    // Mock Firestore update operation to fail
    val exception = Exception("Update failed")
    `when`(mockDocumentReference.update(updates)).thenReturn(Tasks.forException(exception))

    var callbackResult = true // Initialize to true to check if it's set to false

    // Act
    repository.updateEvaluationResult(battleId, evaluationResult) { success ->
      callbackResult = success
    }

    // Process any pending tasks
    shadowOf(Looper.getMainLooper()).idle()

    // Assert
    verify(mockDocumentReference).update(updates)
    assertFalse(callbackResult)
  }

  /** Helper methods to create test data */
  private fun createTestSpeechBattle(): SpeechBattle {
    return SpeechBattle(
        battleId = "battle1",
        challenger = "user1",
        opponent = "user2",
        status = BattleStatus.PENDING,
        context =
            InterviewContext(
                targetPosition = "Developer",
                companyName = "TechCorp",
                interviewType = "Technical",
                experienceLevel = "Junior",
                jobDescription = "Develop software",
                focusArea = "Backend"),
        challengerCompleted = false,
        opponentCompleted = false,
        challengerData =
            listOf(
                Message(role = "user", content = "Hello"),
                Message(role = "user", content = "How are you?")),
        opponentData =
            listOf(
                Message(role = "user", content = "Hi"),
                Message(role = "user", content = "I'm fine, thanks!")),
        evaluationResult = null)
  }

  private fun createTestBattleDataMap(): Map<String, Any> {
    return mapOf(
        "battleId" to "battle1",
        "challenger" to "user1",
        "opponent" to "user2",
        "status" to "PENDING",
        "winner" to "",
        "interviewContext" to
            mapOf(
                "targetPosition" to "Developer",
                "companyName" to "TechCorp",
                "interviewType" to "Technical",
                "experienceLevel" to "Junior",
                "jobDescription" to "Develop software",
                "focusArea" to "Backend"),
        "challengerCompleted" to false,
        "opponentCompleted" to false)
  }
}
