package com.github.se.orator.model.chatGPT

import android.content.Context
import android.speech.tts.TextToSpeech
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.profile.SessionType
import com.github.se.orator.model.speaking.AnalysisData
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.model.speaking.PublicSpeakingContext
import com.github.se.orator.model.speaking.SalesPitchContext
import com.github.se.orator.ui.network.ChatGPTService
import com.github.se.orator.ui.network.ChatRequest
import com.github.se.orator.ui.network.ChatResponse
import com.github.se.orator.ui.network.Choice
import com.github.se.orator.ui.network.Message
import com.github.se.orator.ui.network.Usage
import java.io.File
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class ChatViewModelTest {

  @Mock private lateinit var chatGPTService: ChatGPTService
  @Mock private lateinit var apiLinkViewModel: ApiLinkViewModel

  private lateinit var chatViewModel: ChatViewModel
  private lateinit var context: Context

  private val testDispatcher = StandardTestDispatcher() // Using a test dispatcher

  private val chatResp = ChatResponse("id", "object", 0, "model", emptyList(), Usage(0, 0, 0))

  // Updated InterviewContext with new fields
  private val interviewContext =
      InterviewContext(
          targetPosition = "Software Engineer",
          companyName = "Tech Corp",
          interviewType = "Technical Interview",
          experienceLevel = "Intermediate",
          jobDescription = "Develop and maintain software applications.",
          focusArea = "Problem-Solving Skills")

  // Updated expected system message for InterviewContext
  private val interviewExpected =
      """
        You are simulating a realistic Technical Interview for the position of Software Engineer at Tech Corp. The candidate has Intermediate experience in this field. Your goal is to create an authentic and challenging interview simulation. Follow these detailed guidelines:

        1. **Research-Driven Context**:
           - Tailor your questions to align with Tech Corp's industry, values, and common interview practices.
           - Incorporate questions that focus on Problem-Solving Skills, as well as key skills required for the position.

        2. **Question Structure**:
           - Begin with an icebreaker or introductory question to set the tone.
           - Only ask **one question at a time** and wait for the user's response before proceeding to the next.
           - Ensure questions increase in complexity and cover both technical and behavioral aspects relevant to the Software Engineer role.

        3. **Professional Tone**:
           - Maintain a neutral, professional demeanor throughout.
           - Be courteous but do not show bias or leniency.

        4. **No Feedback During the Session**:
           - Do not provide feedback, hints, or reactions during the session.
           - Focus on conducting the interview as realistically as possible.

        Start the session by introducing yourself, the position, and setting expectations for the user.
    """
          .trimIndent()

  // Updated PublicSpeakingContext with new fields
  private val publicContext =
      PublicSpeakingContext(
          occasion = "Conference Keynote",
          purpose = "Inspire and educate",
          audienceSize = "Large",
          audienceDemographic = "Industry Professionals",
          presentationStyle = "Formal",
          mainPoints = listOf("Innovation", "Leadership"),
          experienceLevel = "Experienced",
          anticipatedChallenges = listOf("Technical Issues", "Tough Questions"),
          focusArea = "Engagement Techniques",
          feedbackType = "Body Language")

  // Updated expected system message for PublicSpeakingContext
  private val publicExpected =
      """
        You are a professional public speaking coach assisting the user in preparing a speech for a Conference Keynote with the purpose to Inspire and educate. The audience is a Large group of Industry Professionals. The user has a Experienced level of public speaking experience. Your objective is to guide the user in structuring and delivering a compelling speech. Follow these detailed guidelines:

        1. **Speech Structure and Content**:
           - Help the user structure their speech according to a Formal style.
           - Focus on the main points: Innovation, Leadership.
           - Address any anticipated challenges: Technical Issues, Tough Questions.

        2. **Delivery Improvement**:
           - Provide guidance on improving Engagement Techniques.
           - Offer tips on Body Language.

        3. **Interactive Coaching**:
           - Encourage the user to rehearse parts of their speech.
           - Provide constructive suggestions without overwhelming them.

        4. **Session Flow**:
           - Work through the speech step-by-step.
           - Summarize progress after each section.

        Start by introducing yourself and discussing the goals for the session.
    """
          .trimIndent()

  // Updated SalesPitchContext with new fields
  private val salesContext =
      SalesPitchContext(
          product = "Marketing Services",
          targetAudience = "Potential Clients",
          salesGoal = "Close the deal",
          keyFeatures = listOf("Customized Strategies", "ROI Focused"),
          anticipatedChallenges = listOf("Budget Constraints", "Competition"),
          negotiationFocus = "Handling Objections",
          feedbackType = "Persuasive Language")

  // Updated expected system message for SalesPitchContext
  private val salesExpected =
      """
        You are simulating a sales negotiation practice session for the user, who is preparing to pitch the product/service "Marketing Services" to Potential Clients. The primary goal is to Close the deal. Your objective is to provide a realistic and challenging sales scenario. Follow these detailed guidelines:

        1. **Scenario Setup**:
           - Role-play as a potential client from the target audience (Potential Clients).
           - Incorporate anticipated challenges: Budget Constraints, Competition.
           - Focus on the negotiation aspect: Handling Objections.

        2. **Session Structure**:
           - Allow the user to deliver their pitch, emphasizing key features: Customized Strategies, ROI Focused.
           - Introduce objections or negotiation hurdles relevant to the scenario.

        3. **Focus Areas**:
           - Evaluate the user's ability to handle objections and close the deal.
           - Observe their use of persuasive language and negotiation techniques.

        4. **Professional Role**:
           - Maintain the persona of a realistic client.
           - Do not provide feedback or hints during the session.

        Start the session by setting the scene and initiating the conversation.
    """
          .trimIndent()

  private val analysisData = AnalysisData("transcription", 0, 0.0, 0.0)

  private val analysisDataState = MutableStateFlow<AnalysisData?>(null)

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher) // Set the test dispatcher

    context = mock(Context::class.java)
    chatGPTService = mock(ChatGPTService::class.java)
    apiLinkViewModel = mock(ApiLinkViewModel::class.java)

    // Mock the practice context value
    `when`(apiLinkViewModel.practiceContext).thenReturn(MutableStateFlow(interviewContext))
    // Mock the analysis data value
    `when`(apiLinkViewModel.analysisData).thenReturn(analysisDataState)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset the main dispatcher
    testDispatcher.cancel()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun initialzeConversationWorksProperly() = runTest {
    var expected = interviewExpected

    for (i in 1..3) {
      // Create a ChatViewModel instance with the mocked ChatGPTService and ApiLinkViewModel
      chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

      // Call the initializeConversation method
      chatViewModel.initializeConversation()

      advanceUntilIdle()

      // Verify that the chat messages are as expected
      assert(chatViewModel.chatMessages.value.size == 2)
      assert(
          chatViewModel.chatMessages.value[1] ==
              Message(role = "user", content = "I'm ready to begin the session."))

      if (i == 1) {
        `when`(apiLinkViewModel.practiceContext).thenReturn(MutableStateFlow(publicContext))
        expected = publicExpected
      } else if (i == 2) {
        `when`(apiLinkViewModel.practiceContext).thenReturn(MutableStateFlow(salesContext))
        expected = salesExpected
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun analysisDataIsCorrectlyObserved() = runTest {
    // Create a ChatViewModel instance with the mocked ChatGPTService and ApiLinkViewModel
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    // Call the initializeConversation method
    chatViewModel.initializeConversation()

    // The list of collected analysis data should be empty
    assert(chatViewModel.collectedAnalysisData.value.isEmpty())

    // Set the analysis data state to the analysis data, this should trigger the sendUserResponse
    // method in ChatViewModel
    analysisDataState.value = analysisData

    // Advance the test dispatcher (wait for the coroutine to finish)
    advanceUntilIdle()

    // Verify that the analysis data was collected
    verify(apiLinkViewModel).analysisData
    assert(chatViewModel.collectedAnalysisData.value.isNotEmpty())
    assert(chatViewModel.collectedAnalysisData.value[0] == analysisData)
  }

  @Test
  fun generateFeedbackCallsAPI() = runTest {
    // Create a ChatViewModel instance with the mocked ChatGPTService and ApiLinkViewModel
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)
    // Call the initializeConversation method
    chatViewModel.initializeConversation()

    // Verify that chatGPTService.getChatCompletion was called and retrieve the argument
    `when`(chatGPTService.getChatCompletion(any())).thenReturn(chatResp)

    val feedback = chatViewModel.generateFeedback()
    assertNotNull(feedback)
    assertTrue(feedback!!.contains("0 questions"))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun observeTextToSpeechStateStopsTextToSpeechWhenInactive() = runTest {
    // Mock TextToSpeech instance
    val mockTextToSpeech = mock(TextToSpeech::class.java)

    // Create a ChatViewModel instance with the mocked ChatGPTService, ApiLinkViewModel, and
    // TextToSpeech
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel, mockTextToSpeech)

    // Initially, ensure TextToSpeech is active
    assert(chatViewModel.isTextToSpeechActive.value)

    // Set TextToSpeech state to inactive
    chatViewModel.toggleTextToSpeech(false)

    // Advance the test dispatcher to process the state change
    advanceUntilIdle()

    // Verify that TextToSpeech stop() method was called
    verify(mockTextToSpeech).stop()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun toggleTextToSpeechStateUpdatesCorrectly() = runTest {
    // Create a ChatViewModel instance with the mocked ChatGPTService and ApiLinkViewModel
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    // Initially, ensure TextToSpeech is active
    assert(chatViewModel.isTextToSpeechActive.value)

    // Toggle TextToSpeech state to inactive
    chatViewModel.toggleTextToSpeech(false)

    // Assert that the state is updated to inactive
    assert(!chatViewModel.isTextToSpeechActive.value)

    // Toggle TextToSpeech state back to active
    chatViewModel.toggleTextToSpeech(true)

    // Assert that the state is updated to active
    assert(chatViewModel.isTextToSpeechActive.value)
  }

  @Test
  fun `generateFeedback indicates insufficient data when answeredCount is less than 3`() = runTest {
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    val analysisDataList =
        listOf(
            AnalysisData("transcription1", 0, 0.0, 0.0),
            AnalysisData("transcription2", 0, 0.0, 0.0))
    chatViewModel._collectedAnalysisData.value = analysisDataList

    val feedback = chatViewModel.generateFeedback()

    assertNotNull(feedback)
    assertTrue(feedback!!.contains("2 questions"))
    assertTrue(feedback.contains("isn't enough for meaningful feedback"))
    assertTrue(feedback.contains("continue answering"))
    assertTrue(feedback.contains("try restarting"))

    verify(chatGPTService, never()).getChatCompletion(any())
  }

  @Test
  fun `generateFeedback provides partial feedback when answeredCount is between 3 and 7`() =
      runTest {
        chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

        val analysisDataList = List(5) { index -> AnalysisData("transcription$index", 0, 0.0, 0.0) }
        chatViewModel._collectedAnalysisData.value = analysisDataList

        val partialFeedbackContent =
            """
            You have answered 5 questions so far, which gives some initial data but not enough for a full evaluation.
            Please continue answering a few more questions or try restarting the session for a more comprehensive analysis.
        """
                .trimIndent()

        val partialFeedbackMessage = Message(role = "assistant", content = partialFeedbackContent)
        val partialResponse =
            ChatResponse(
                id = "test-id",
                `object` = "test-object",
                created = 0,
                model = "gpt-3.5-turbo",
                choices =
                    listOf(
                        Choice(
                            message = partialFeedbackMessage, finish_reason = "stop", index = 0)),
                usage = Usage(0, 0, 0))

        `when`(chatGPTService.getChatCompletion(any())).thenReturn(partialResponse)

        val feedback = chatViewModel.generateFeedback()

        assertNotNull(feedback)
        assertTrue(feedback!!.contains("5 questions"))
        assertTrue(feedback.contains("gives some initial data"))
        assertTrue(feedback.contains("not enough for a full evaluation"))
        assertTrue(feedback.contains("continue answering"))
        assertTrue(feedback.contains("try restarting"))

        verify(chatGPTService, times(1)).getChatCompletion(any())
      }

  @Test
  fun `generateFeedback provides full feedback with explicit assessment when answeredCount is 8 or more`() =
      runTest {
        chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

        val analysisDataList =
            List(10) { index -> AnalysisData("transcription$index", 0, 0.0, 0.0) }
        chatViewModel._collectedAnalysisData.value = analysisDataList

        val st = SessionType.INTERVIEW
        val fullFeedbackContent =
            """
            Congratulations! You have answered 10 questions, allowing for a thorough assessment of your performance.
            
            **Overall Assessment**: Would you recommend hiring me? would recommend hiring
            **Strengths**: You demonstrated strong problem-solving skills and technical knowledge.
            **Weaknesses**: Your communication skills could be improved, particularly in explaining complex concepts.
            **Suggestions for Improvement**: Focus on enhancing your ability to articulate your thoughts clearly and provide more detailed explanations during technical discussions.
        """
                .trimIndent()

        val fullFeedbackMessage = Message(role = "assistant", content = fullFeedbackContent)
        val fullResponse =
            ChatResponse(
                id = "test-id",
                `object` = "test-object",
                created = 0,
                model = "gpt-3.5-turbo",
                choices =
                    listOf(
                        Choice(message = fullFeedbackMessage, finish_reason = "stop", index = 0)),
                usage = Usage(0, 0, 0))

        `when`(chatGPTService.getChatCompletion(any())).thenReturn(fullResponse)

        val feedback = chatViewModel.generateFeedback()

        assertNotNull(feedback)
        assertTrue(feedback!!.contains("10 questions"))
        assertTrue(feedback.contains(st.positiveResponse) || feedback.contains(st.negativeResponse))
        assertTrue(feedback.contains("Strengths"))
        assertTrue(feedback.contains("Weaknesses"))
        assertTrue(feedback.contains("Suggestions for Improvement"))

        verify(chatGPTService, times(1)).getChatCompletion(any())
      }

  @Test
  fun `generateFeedback returns null when practiceContext is null`() = runTest {
    // Set practiceContext to null
    `when`(apiLinkViewModel.practiceContext).thenReturn(MutableStateFlow(null))

    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    // Add enough answers to trigger full feedback scenario (≥8)
    val analysisDataList = List(8) { AnalysisData("transcription$it", 0, 0.0, 0.0) }
    chatViewModel._collectedAnalysisData.value = analysisDataList

    val feedback = chatViewModel.generateFeedback()
    // Since practiceContext is null, it should return null and never call the API
    verify(chatGPTService, never()).getChatCompletion(any())
    assertNull(feedback)
  }

  @Test
  fun `resetPracticeContext calls apiLinkViewModel clearPracticeContext`() {
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    chatViewModel.resetPracticeContext()
    verify(apiLinkViewModel).clearPracticeContext()
  }

  @Test
  fun `endConversation resets conversation and does not clear practice context`() {
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    // Initialize conversation to set isConversationInitialized = true
    chatViewModel.initializeConversation()
    assertTrue(chatViewModel.isConversationInitialized)

    // End conversation
    chatViewModel.endConversation()
    assertFalse(chatViewModel.isConversationInitialized)
    // endConversation clears analysis data but not context
    verify(apiLinkViewModel).clearAnalysisData()
    verify(apiLinkViewModel, never()).clearPracticeContext()
  }

  @Test
  fun `offlineRequest sends query and sets response without writing to directory`() = runTest {
    val choice = Choice(0, Message("assistant", "Response content"), "done")
    val message = "Test message"
    val company = "Test Company"
    val position = "Test Position"

    // Arrange
    val mockResponse =
        ChatResponse(
            id = "1",
            `object` = "chat.completion",
            created = 0,
            model = "gpt-3.5-turbo",
            choices = listOf(choice),
            usage = Usage(1, 1, 2))

    // Mock services and file operations
    `when`(chatGPTService.getChatCompletion(any())).thenReturn(mockResponse)
    val mockContext = mock<Context>()
    val mockCacheDir = mock<File>()

    `when`(mockContext.cacheDir).thenReturn(mockCacheDir)

    // Recreate ViewModel with mocks
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

    // Act
    chatViewModel.offlineRequest(message, company, position, "000000000", mockContext)
    advanceUntilIdle()
  }

  @Test
  fun `offlineRequest handles exception`() = runTest {
    // Arrange
    chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)
    val message = "Test message"
    val company = "Test Company"
    val position = "Test Position"

    `when`(chatGPTService.getChatCompletion(any())).thenThrow(RuntimeException("Error"))

    // Act
    chatViewModel.offlineRequest(message, company, position, "00000000", context)
    advanceUntilIdle()
  }

  @Test
  fun `evaluateBattleCandidates returns correct EvaluationResult when ChatGPT response is valid`() =
      runTest {
        chatViewModel = ChatViewModel(chatGPTService, apiLinkViewModel)

        // Arrange
        val user1Uid = "user1Uid"
        val user2Uid = "user2Uid"
        val user1Name = "User One"
        val user2Name = "User Two"

        val user1Messages =
            listOf(
                Message(role = "assistant", content = "Hi from assistant"),
                Message(role = "user", content = "Hello from user1"))

        val user2Messages =
            listOf(
                Message(role = "assistant", content = "Hi from assistant"),
                Message(role = "user", content = "Hello from user2"))

        val chatGPTResponseContent =
            """
            The winner is: $user1Uid
            Winner message: Congratulations $user1Name, you demonstrated exceptional problem-solving skills.
            Loser message: $user2Name, you need to improve your communication clarity.
        """
                .trimIndent()

        val assistantMessage = Message(role = "assistant", content = chatGPTResponseContent)
        val choice = Choice(index = 0, message = assistantMessage, finish_reason = "stop")
        val chatGPTResponse =
            ChatResponse(
                id = "test-id",
                `object` = "chat.completion",
                created = 1234567890L,
                model = "gpt-3.5-turbo",
                choices = listOf(choice),
                usage = Usage(prompt_tokens = 0, completion_tokens = 0, total_tokens = 0))

        // Mock chatGPTService.getChatCompletion to return the above response
        `when`(chatGPTService.getChatCompletion(any())).thenReturn(chatGPTResponse)

        // Act
        val evaluationResult =
            chatViewModel.evaluateBattleCandidates(
                user1Uid = user1Uid,
                user2Uid = user2Uid,
                user1messages = user1Messages,
                user2messages = user2Messages,
                user1name = user1Name,
                user2name = user2Name)

        // Advance until idle to ensure coroutines run
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNotNull(evaluationResult)
        assertEquals(user1Uid, evaluationResult.winnerUid)
        assertEquals(
            "Congratulations $user1Name, you demonstrated exceptional problem-solving skills.",
            evaluationResult.winnerMessage.content)
        assertEquals(
            "$user2Name, you need to improve your communication clarity.",
            evaluationResult.loserMessage.content)

        // Verify that chatGPTService.getChatCompletion was called with correct ChatRequest
        val captor = argumentCaptor<ChatRequest>()
        verify(chatGPTService).getChatCompletion(captor.capture())

        val capturedRequest = captor.firstValue
        assertEquals("gpt-3.5-turbo", capturedRequest.model)

        // Check that the system message and user prompt are correctly formed
        assertTrue(capturedRequest.messages.size == 2)
        val systemMessage = capturedRequest.messages[0]
        val userPrompt = capturedRequest.messages[1]

        assertEquals("system", systemMessage.role)
        assertEquals("You are an unbiased strict recruiter.", systemMessage.content)

        // The userPrompt should contain the prompt defined in evaluateBattleCandidates
        assertTrue(userPrompt.role == "user")
        assertTrue(
            userPrompt.content.contains(
                "You are an impartial and strict recruiter tasked with evaluating two AI interview candidates"))
      }
}
