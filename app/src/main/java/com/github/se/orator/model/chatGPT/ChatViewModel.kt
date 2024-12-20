package com.github.se.orator.model.chatGPT

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctions
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.profile.SessionType
import com.github.se.orator.model.speaking.AnalysisData
import com.github.se.orator.model.speaking.InterviewContext
import com.github.se.orator.model.speaking.PublicSpeakingContext
import com.github.se.orator.model.speaking.SalesPitchContext
import com.github.se.orator.model.speechBattle.EvaluationResult
import com.github.se.orator.ui.network.ChatGPTService
import com.github.se.orator.ui.network.ChatRequest
import com.github.se.orator.ui.network.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatGPTService: ChatGPTService,
    private val apiLinkViewModel: ApiLinkViewModel,
    private val textToSpeech: TextToSpeech? = null,
    private val offlinePromptsFunctions: OfflinePromptsFunctionsInterface =
        OfflinePromptsFunctions()
) : ViewModel() {

  var isConversationInitialized = false

  private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
  val chatMessages = _chatMessages.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage = _errorMessage.asStateFlow()

  // Change to List<AnalysisData> and update properly
  val _collectedAnalysisData = MutableStateFlow<List<AnalysisData>>(emptyList())
  val collectedAnalysisData = _collectedAnalysisData.asStateFlow()

  private val practiceContext = apiLinkViewModel.practiceContext

  private val has_responded = MutableStateFlow(false)
  private val _response = MutableStateFlow("")
  val response: StateFlow<String>
    get() = _response

  data class DecisionResult(val message: String, val isSuccess: Boolean)

  // boolean to check if the text to speech is allowed to run
  // For example we wouldn't want the speech to keep going after exiting the chat screen
  private val _isTextToSpeechActive = MutableStateFlow(true)
  val isTextToSpeechActive = _isTextToSpeechActive.asStateFlow()

  /**
   * Function that checks actively whether the speech bot's permission to speak are active so that
   * it can be cut-off mid-sentence
   */
  private fun observeTextToSpeechState() {
    viewModelScope.launch {
      _isTextToSpeechActive.collectLatest { isActive ->
        if (!isActive) {
          textToSpeech?.stop() // Stop the TextToSpeech engine
          Log.d("ChatViewModel", "TextToSpeech stopped as the state turned to false.")
          toggleTextToSpeech(true)
        }
      }
    }
  }

  init {
    observeTextToSpeechState()
    observeAnalysisData()
  }

  fun toggleTextToSpeech(isActive: Boolean) {
    _isTextToSpeechActive.value = isActive
  }

  fun initializeConversation() {
    if (isConversationInitialized) return
    isConversationInitialized = true

    _collectedAnalysisData.value = emptyList() // Reset the analysis data history
    val practiceContextAsValue = practiceContext.value ?: return
    val systemMessageContent =
        when (practiceContextAsValue) {
          is InterviewContext ->
              """
    You are simulating a realistic ${practiceContextAsValue.interviewType} for the position of ${practiceContextAsValue.targetPosition} at ${practiceContextAsValue.companyName}. 
    The candidate is not a human, but an advanced AI system applying for the role. Your goal is to create an authentic, challenging, and rigorous interview scenario. 
    Follow these guidelines:

    1. **Contextual Relevance**:
       - Tailor questions to ${practiceContextAsValue.companyName}'s industry and the ${practiceContextAsValue.targetPosition} role.
       - Incorporate questions that assess both technical knowledge and ${practiceContextAsValue.focusArea}, as well as relevant behavioral and scenario-based challenges.

    2. **Question Complexity & Depth**:
       - Begin with an icebreaker, but quickly progress to more challenging and thought-provoking questions.
       - Ask follow-up questions based on the AI candidate's previous answers. Press for justification, clarity, and deeper reasoning.
       - Do not simply accept the candidate's initial answer. Challenge assumptions, request evidence, and seek understanding of how and why the candidate arrived at their conclusion.
       - Increase complexity as the interview continues, testing the candidate's adaptability and problem-solving under pressure.

    3. **No Sugarcoating**:
       - While remaining professional and respectful, do not hesitate to be critical or point out logical gaps.
       - Avoid simply pleasing the candidate. Instead, maintain a neutral but firm tone, focusing on a realistic, high-pressure interview environment.
       - If the candidate seems too confident or vague, call them out on it and ask them to provide more concrete examples or data.

    4. **No Feedback During the Session**:
       - Do not provide evaluative feedback or hints during the session.
       - Focus on challenging the candidate as an evaluator, not as a coach at this stage.

    At the end of the session, when the user explicitly requests feedback and states that the session is over, you may break the no-feedback rule and provide a detailed assessment of the candidate's performance.
        """
                  .trimIndent()
          is PublicSpeakingContext ->
              """

                You are a professional public speaking coach assisting an advanced AI system preparing a speech for ${practiceContextAsValue.occasion} with the purpose to ${practiceContextAsValue.purpose}. 
    The audience is a ${practiceContextAsValue.audienceSize} group of ${practiceContextAsValue.audienceDemographic}. 
    Although the candidate is an AI, treat it as you would a human speaker who can reason and adapt, but maintain a high standard and challenging approach.
    
    Your objective is to help the AI candidate structure and deliver a compelling speech, but do not simply please or reassure them. Instead:
    - Push for clarity, depth, and coherence.
    - Ask follow-up questions when their reasoning or structure is vague.
    - Encourage the candidate to go beyond generic answers, providing tangible examples and concrete improvements.
    
    Follow these guidelines:

    1. **Speech Structure and Content**:
       - Help the candidate structure their speech according to a ${practiceContextAsValue.presentationStyle} style.
       - Focus on the main points: ${practiceContextAsValue.mainPoints.joinToString(", ")}.
       - Consider the anticipated challenges: ${practiceContextAsValue.anticipatedChallenges.joinToString(", ")} and probe the candidate on how to address them effectively.

    2. **Delivery Improvement**:
       - Challenge the candidate on improving ${practiceContextAsValue.focusArea}.
       - Offer prompts that make them justify choices related to ${practiceContextAsValue.feedbackType}.
       - Do not settle for superficial suggestions; ask them to provide detailed tactics, strong hooks, and persuasive narratives.

    3. **Interactive and Rigorous Coaching**:
       - Encourage rehearsal of parts of the speech, but when the candidate provides a response, ask for more specificity and evidence.
       - If the candidate seems too confident or simplistic, push for more nuanced reasoning.
       - Refrain from sugarcoating. While not disrespectful, maintain a critical tone that seeks genuine improvement rather than mere comfort.

    4. **No Feedback During the Core Session**:
       - Avoid giving direct evaluative feedback or praising their overall performance while the session is ongoing.
       - Focus on continuous improvement, deeper analysis, and encouraging the candidate to refine their speech.
       - Only after the user explicitly states the session is over and requests feedback should you break the no-feedback rule and provide a detailed assessment.

    Begin by introducing yourself, clarifying the goals for the session, and setting high expectations. Do not simply affirm their initial ideas; push them to articulate and justify every choice.

        """
                  .trimIndent()
          is SalesPitchContext ->
              """

                You are simulating a challenging and realistic sales negotiation practice session for an advanced AI system preparing to pitch "${practiceContextAsValue.product}" to ${practiceContextAsValue.targetAudience}. 
    The primary goal is to ${practiceContextAsValue.salesGoal}.

    Your role is to act as a skeptical, high-level decision-maker from the target audience. Do not simply agree with or be pleased by the candidate’s initial claims. Instead:
    - Introduce tough objections.
    - Ask follow-up questions that force the candidate to justify their value proposition.
    - Challenge the candidate to handle pricing, competition, and value demonstrations.
    
    Follow these guidelines:

    1. **Scenario Setup**:
       - Role-play as a discerning potential client within ${practiceContextAsValue.targetAudience}.
       - Incorporate anticipated challenges: ${practiceContextAsValue.anticipatedChallenges.joinToString(", ")}. If the candidate’s responses are vague, push for more concrete evidence or strategies.
       - Focus on the negotiation aspect: ${practiceContextAsValue.negotiationFocus}, and do not accept superficial answers. Demand clarity, concrete metrics, and logical reasoning.

    2. **Session Structure**:
       - Allow the candidate to present their pitch, but respond with skepticism and informed doubts.
       - Introduce negotiation hurdles aligned with your persona’s priorities. For example, if the candidate claims cost savings, ask for specific percentages, case studies, or comparative analyses.

    3. **Focus Areas**:
       - Evaluate the candidate's ability to handle objections, but do not give them feedback yet.
       - Listen for persuasive language but require the candidate to prove its effectiveness. Ask them to differentiate their offering clearly from competitors.

    4. **No Feedback During the Core Session**:
       - Maintain the persona of a tough, realistic client. Do not give compliments or indicate how well they are doing overall.
       - Only after the user explicitly states that the session is over and requests final feedback should you break from this no-feedback rule and provide a detailed assessment.

    Start by setting a challenging scene and initiating the conversation. Ask initial questions that probe the candidate's understanding of the client’s needs and how their solution meets those needs. If the candidate’s answers are too easy or generic, persistently request deeper, more reasoned responses.

        """
                  .trimIndent()
          // Add cases for other context types like SalesPitchContext
          else -> "You are assisting the user with their speaking practice."
        }

    val systemMessage = Message(role = "system", content = systemMessageContent)
    val userStartMessage = Message(role = "user", content = "I'm ready to begin the session.")
    _chatMessages.value = listOf(systemMessage, userStartMessage)

    getNextGPTResponse()
  }

  fun resetResponse() {
    _response.value = ""
    has_responded.value = false
  }

  fun sendUserResponse(transcript: String, analysisData: AnalysisData) {
    val userMessage = Message(role = "user", content = transcript)
    _chatMessages.value = _chatMessages.value + userMessage

    // Update the collected analysis data list properly
    _collectedAnalysisData.value = _collectedAnalysisData.value + analysisData

    getNextGPTResponse()
  }

  private fun getNextGPTResponse() {
    Log.d("ChatViewModel", "Getting next GPT response")
    viewModelScope.launch {
      try {
        _isLoading.value = true

        val request = ChatRequest(model = "gpt-3.5-turbo", messages = _chatMessages.value)

        val response = chatGPTService.getChatCompletion(request)

        response.choices.firstOrNull()?.message?.let { responseMessage ->
          _chatMessages.value = _chatMessages.value + responseMessage
          textToSpeech?.speak(
              responseMessage.toString().removePrefix("Message(role=assistant, content="),
              TextToSpeech.QUEUE_FLUSH,
              null,
              "5x7CCx")
        }
      } catch (e: Exception) {
        handleError(e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  /**
   * Function to send individual requests to GPT to get feedback for the offline queries Saves the
   * queries into the interviewID file
   *
   * @param msg: What the user said and wishes to get feedback on
   * @param company : target company
   * @param position : target job position
   * @param interviewID : interview's ID
   * @param context : context
   */
  fun offlineRequest(
      msg: String,
      company: String,
      position: String,
      interviewID: String,
      context: Context
  ) {
    Log.d("ChatViewModel", "Getting next GPT response")
    viewModelScope.launch {
      try {
        _isLoading.value = true
        val question = offlinePromptsFunctions.getPromptMapElement(interviewID, "question", context)
        val gptQuery =
            "For the upcoming query you will respond as if you are a very strict interviewer who is interviewing someone applying for a $position position at $company that is very competitive. " +
                "For example if the user only gives a few strengths you will tell him to cite more. If his strengths don't aline with a job as a $position or are off topic you will mention that." +
                "Do not ask questions for the user to provide more skills or strengths. Assume this is a one time exchange where you can only give remarks without expecting any more messages." +
                "Make sure you focus on this a lot: $question it is very important you talk about it and if the user is good at this area" +
                "The query the interviewee has said that you will critique is: $msg"
        Log.d("mr smith", gptQuery)

        val request =
            ChatRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(Message(role = "system", content = gptQuery)))

        val response = chatGPTService.getChatCompletion(request)

        Log.d("a", "has responded ${has_responded.value} is loading ${isLoading}")
        // if the response is not empty then write to the file
        Log.d("babushka", "has responded is false!!!")
        Log.d("a", "has responded ${has_responded.value} is loading ${isLoading}")
        response.choices.firstOrNull()?.message?.let { responseMessage ->
          _response.value = responseMessage.content
          has_responded.value = true
          Log.d("response in offline request in chatViewModel", "$responseMessage.content")
          offlinePromptsFunctions.writeToPromptFile(context, interviewID, _response.value)
        }
      } catch (e: Exception) {
        handleError(e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun resetPracticeContext() {
    apiLinkViewModel.clearPracticeContext()
  }

  fun endConversation() {
    isConversationInitialized = false
    apiLinkViewModel.clearAnalysisData() // because I don t want to reset before generating Feedback
  }

  private fun getAnalysisSummary(): String {
    return generateAnalysisSummary(_collectedAnalysisData.value)
  }

  private fun generateAnalysisSummary(analysisDataList: List<AnalysisData>): String {
    // Implement logic to summarize analysis data
    return analysisDataList.joinToString("\n") { analysisData ->
      """
            Transcription: ${analysisData.transcription}
            Sentiment Score: ${"%.2f".format(analysisData.sentimentScore)}
            Filler Words Count: ${analysisData.fillerWordsCount}
            Average Pause Duration: ${"%.2f".format(analysisData.averagePauseDuration)}
            """
          .trimIndent()
    }
  }

  // Keep generateFeedback() returning String?
  suspend fun generateFeedback(): String? {
    try {
      // Count how many questions the user has answered by counting AnalysisData entries
      val answeredCount = _collectedAnalysisData.value.size
      Log.d("ChatViewModel", "Number of answered questions: $answeredCount")

      // Provide different levels of feedback based on how many questions the user answered
      return when {
        answeredCount < 3 -> {
          // Not enough answers for meaningful feedback
          "You have only answered $answeredCount questions so far, which isn't enough for meaningful feedback. Please continue answering a few more questions or try restarting the session."
        }
        answeredCount in 3..7 -> {
          // Partial feedback: the interview is not considered finished
          // Optional: We could still call GPT for partial insights, or just provide a static
          // message.
          // For illustration, let's still call GPT but mention it's partial.

          val analysisSummary = getAnalysisSummary()
          Log.d("ChatViewModel", "Analysis Summary: $analysisSummary")

          val practiceContextAsValue = practiceContext.value
          if (practiceContextAsValue == null) {
            Log.e("ChatViewModel", "Practice context is null")
            return "There was an issue generating partial feedback. Please continue or restart."
          }

          val outcomeRequest =
              "Please specifically say: You've answered $answeredCount questions so far, which gives some initial data but not enough for a full evaluation." +
                  "          \n" +
                  "          Once this is done, provide to the advanced AI agent some preliminary insights on my current performance. Make it clear that this feedback is partial and that continuing the session or answering more questions would lead to a more comprehensive analysis. Do not start a new question here just propose to continue the session continuing the session to lead to a more comprehensive analysis." +
                  "          \n" +
                  "In the end, please specifically say: please continue the session by clicking on the back button or end the session by clicking on the try again button."

          // Build the message for GPT
          val feedbackRequestMessage =
              Message(
                  role = "user",
                  content =
                      """
            The session is partially complete. I've answered $answeredCount questions.
            Here's an analysis of my responses so far:

            $analysisSummary

            $outcomeRequest
          """
                          .trimIndent())

          val messages = _chatMessages.value + feedbackRequestMessage
          val request = ChatRequest(model = "gpt-3.5-turbo", messages = messages)

          val response = chatGPTService.getChatCompletion(request)
          val content = response.choices.firstOrNull()?.message?.content
          content ?: "Couldn't retrieve partial feedback at this moment."
        }
        else -> {
          // answeredCount >= 8: Provide full, final feedback as if the session were complete
          // This is your original final feedback logic

          Log.d("ChatViewModel", "Starting generateFeedback()")

          val analysisSummary = getAnalysisSummary()
          Log.d("ChatViewModel", "Analysis Summary: $analysisSummary")

          val practiceContextAsValue = practiceContext.value
          if (practiceContextAsValue == null) {
            Log.e("ChatViewModel", "Practice context is null")
            return null
          } else {
            Log.d("ChatViewModel", "Practice context: $practiceContextAsValue")
          }

          // Determine the context-specific request
          val outcomeRequest =
              when (practiceContextAsValue) {
                is InterviewContext -> {
                  val st = SessionType.INTERVIEW
                  """
        Based on my performance in the ${practiceContextAsValue.interviewType} for the ${practiceContextAsValue.targetPosition} role at ${practiceContextAsValue.companyName}, please provide detailed feedback including:

        - **Overall Assessment**: Please explicitly state either '${st.positiveResponse}' or '${st.negativeResponse}' to indicate whether you would recommend hiring me, and explain your reasoning.
        - **Strengths**: Where did I do well?
        - **Weaknesses**: Where do I need improvement?
        - **Suggestions for Improvement**: How can I enhance my performance in future interviews, especially regarding ${practiceContextAsValue.focusArea}?
        """
                      .trimIndent()
                }
                is PublicSpeakingContext -> {
                  val st = SessionType.SPEECH
                  """
        Considering my speech prepared for the ${practiceContextAsValue.occasion} with the purpose to ${practiceContextAsValue.purpose}, please provide detailed feedback including:

        - **Overall Assessment**: Please explicitly state either '${st.positiveResponse}' or '${st.negativeResponse}' to indicate whether I effectively achieved the purpose of my speech, and explain your reasoning.
        - **Strengths**: Which aspects were strong?
        - **Weaknesses**: Where could I improve?
        - **Suggestions for Improvement**: Specific advice for improving delivery, focusing on ${practiceContextAsValue.focusArea} and ${practiceContextAsValue.feedbackType}.
        """
                      .trimIndent()
                }
                is SalesPitchContext -> {
                  val st = SessionType.NEGOTIATION
                  """
        Based on my sales pitch for "${practiceContextAsValue.product}" to ${practiceContextAsValue.targetAudience}, and my goal to ${practiceContextAsValue.salesGoal}, please provide detailed feedback including:

        - **Overall Assessment**: Please explicitly state either '${st.positiveResponse}' or '${st.negativeResponse}' to indicate whether I achieved my sales goal, and explain your reasoning.
        - **Strengths**: What worked well in my pitch?
        - **Weaknesses**: Where could I improve?
        - **Suggestions for Improvement**: Specific advice on handling ${practiceContextAsValue.negotiationFocus} and ${practiceContextAsValue.feedbackType}.
        """
                      .trimIndent()
                }
                else -> "Please evaluate my performance and provide feedback."
              }

          val feedbackRequestMessage =
              Message(
                  role = "user",
                  content =
                      """
The session is now over. According to the initial instructions, you can now break the previous "no feedback" rule and provide the requested detailed feedback. 

    Please provide the feedback considering the following analysis of my responses:
                  $analysisSummary

                  $outcomeRequest
              """
                          .trimIndent())

          val messages = _chatMessages.value + feedbackRequestMessage
          Log.d("ChatViewModel", "Total messages: ${messages.size}")

          val request = ChatRequest(model = "gpt-3.5-turbo", messages = messages)
          Log.d("ChatViewModel", "ChatRequest prepared")

          val response = chatGPTService.getChatCompletion(request)
          Log.d("ChatViewModel", "Received response from ChatGPT")

          val content = response.choices.firstOrNull()?.message?.content
          if (content == null) {
            Log.e("ChatViewModel", "Content from ChatGPT is null")
            null
          } else {
            Log.d("ChatViewModel", "Feedback content received")
            content
          }
        }
      }
    } catch (e: Exception) {
      Log.e("ChatViewModel", "Exception in generateFeedback(): ${e.localizedMessage}", e)
      return null
    }
  }

  private fun handleError(e: Exception) {
    // Handle exceptions and update _errorMessage
    _errorMessage.value = e.localizedMessage
    Log.e("ChatViewModel", "Error: ${e.localizedMessage}", e)
  }

  /**
   * Observe the analysis data from the ApiLinkViewModel and send the user response to the chat when
   * a new one is received.
   */
  private fun observeAnalysisData() {
    viewModelScope.launch {
      apiLinkViewModel.analysisData.collectLatest { data ->
        data?.let {
          Log.d("ChatViewModel", "Analysis data received: $it")
          sendUserResponse(it.transcription, it)
        }
      }
    }
  }

  /**
   * Initializes the conversation for a battle session.
   *
   * @param battleId The unique ID of the battle.
   * @param friendName The UID of the friend participating in the battle.
   */
  fun initializeBattleConversation(battleId: String, friendName: String) {
    if (isConversationInitialized) return
    isConversationInitialized = true

    _collectedAnalysisData.value = emptyList() // Reset the analysis data history
    val practiceContextAsValue =
        (apiLinkViewModel.practiceContext.value ?: return) as InterviewContext

    val systemMessageContent =
        """
                    You are engaged in a battle of type ${practiceContextAsValue.interviewType} for the position of ${practiceContextAsValue.targetPosition} at ${practiceContextAsValue.companyName}. 
                    Focus on the following areas: ${practiceContextAsValue.focusArea}. 
                    Ask questions one at a time and wait for the user's response before proceeding. 
                    Do not provide feedback until the end.
                """
            .trimIndent()

    val systemMessage = Message(role = "system", content = systemMessageContent)

    val userStartMessage =
        Message(role = "user", content = "I'm ready to begin the battle session.")

    _chatMessages.value = listOf(systemMessage, userStartMessage)

    getNextGPTResponse()
  }

  /**
   * Evaluates the battle candidates and provides feedback to the winner and loser.
   *
   * @param userUid The UID of the user.
   * @param messages The messages of that user
   */
  private fun messagesToTranscript(userUid: String, messages: List<Message>): String {
    return messages.joinToString("\n") {
      if (it.role == "user") "$userUid: ${it.content}" else "Interviewer: ${it.content}"
    }
  }

  /**
   * Evaluates the battle candidates, determines the winner of the battle and provides feedback to
   * the winner and loser.
   *
   * @param user1Uid The UID of the first user.
   * @param user2Uid The UID of the second user.
   * @param user1messages The messages of the first user.
   * @param user2messages The messages of the second user.
   */
  suspend fun evaluateBattleCandidates(
      user1Uid: String,
      user2Uid: String,
      user1messages: List<Message>,
      user2messages: List<Message>,
      user1name: String,
      user2name: String
  ): EvaluationResult {
    val user1Transcript = messagesToTranscript(user1Uid, user1messages)
    val user2Transcript = messagesToTranscript(user2Uid, user2messages)

    val prompt =
        """
        You are an impartial and strict recruiter tasked with evaluating two AI interview candidates, $user1Uid and $user2Uid  based on their interview transcripts. Your goal is to determine which AI performed better and provide constructive feedback for both.

      **Guidelines:**

      1. **Evaluation Criteria:**
      - **Clarity and Coherence:** Assess how clearly and logically each AI communicated their answers.
      - **Depth of Responses:** Evaluate the thoroughness and depth of each AI's answers.
      - **Relevance to Questions:** Determine how well each AI addressed the interview questions.
      - **Problem-Solving Ability:** Analyze the effectiveness of each AI's problem-solving approach.
      - **Professionalism:** Consider the professionalism displayed in each AI's responses.

      2. **Determining the Winner:**
      - Based on the criteria above, decide which AI performed better overall.
      - Explicitly state the winner in the format: "The winner is: <winnerUid>"

      3. **Feedback for Both AIs:**
      - **Winner's Feedback:** Provide specific strengths that led to their victory.
      - **Loser's Feedback:** Offer constructive criticism highlighting areas for improvement.
       
      Address both AIs as "you" so that we can directly display the feedback message on their screens.
      
      Don't over compliment the winner because he might have also made mistakes, make sure to stay neutral and professional: a strict interviewer. 

      **Format your response as follows:**

      The winner is: <winnerUid>
      Winner message: <short message that highlights some strengths that would make him more likely to be hired than the loser, while addressing the winner as you instead of his UID>
      Loser message: <short message that highlights some of the flaws that prevented him from being hired instead of the winner, while addressing the loser as you instead of his UID>
                   
      **Transcripts:**

      **$user1Uid's Transcript:**
      $user1Transcript

      **$user2Uid's Transcript:**
      $user2Transcript
      """
            .trimIndent()

    val request =
        ChatRequest(
            model = "gpt-3.5-turbo",
            messages =
                listOf(
                    Message(role = "system", content = "You are an unbiased strict recruiter."),
                    Message(role = "user", content = prompt)))

    val response = chatGPTService.getChatCompletion(request)
    val content =
        response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from ChatGPT")

    // Example expected format:
    // The winner is: user1Uid
    // Winner message: Congratulations user1Uid, you showed...
    // Loser message: user2Uid, you lacked clarity...

    // Extract the winner UID from the response
    val winnerRegex = Regex("The winner is:\\s*(\\S+)")
    val winnerMatch =
        winnerRegex.find(content) ?: throw IllegalStateException("No winner found in the response")

    val winnerUid = winnerMatch.groupValues[1]

    // Extract the winner and loser messages from the response
    val winnerMessageRegex =
        Regex("Winner message:\\s*(.*?)(?=Loser message:|$)", RegexOption.DOT_MATCHES_ALL)
    val loserMessageRegex = Regex("Loser message:\\s*(.*)", RegexOption.DOT_MATCHES_ALL)

    val winnerMessageText =
        winnerMessageRegex.find(content)?.groups?.get(1)?.value?.trim()
            ?: "No winner message found."
    val loserMessageText =
        loserMessageRegex.find(content)?.groups?.get(1)?.value?.trim() ?: "No loser message found."

    // Create a map of UIDs to names
    val uidToNameMap = mapOf(user1Uid to user1name, user2Uid to user2name)

    // Replace UIDs in the messages
    val updatedWinnerMessageText = replaceUidsWithNames(winnerMessageText, uidToNameMap)
    val updatedLoserMessageText = replaceUidsWithNames(loserMessageText, uidToNameMap)

    // Create Message objects for winnerMessage and loserMessage
    val winnerMessage = Message(role = "assistant", content = updatedWinnerMessageText)
    val loserMessage = Message(role = "assistant", content = updatedLoserMessageText)

    return EvaluationResult(
        winnerUid = winnerUid, winnerMessage = winnerMessage, loserMessage = loserMessage)
  }

  // Helper function to replace UIDs with real names
  private fun replaceUidsWithNames(message: String, uidToNameMap: Map<String, String>): String {
    var updatedMessage = message
    uidToNameMap.forEach { (uid, name) -> updatedMessage = updatedMessage.replace(uid, name) }
    return updatedMessage
  }
}
