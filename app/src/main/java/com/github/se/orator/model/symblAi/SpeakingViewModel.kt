package com.github.se.orator.model.symblAi

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.orator.model.apiLink.ApiLinkViewModel
import com.github.se.orator.model.chatGPT.ChatViewModel
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctions
import com.github.se.orator.model.offlinePrompts.OfflinePromptsFunctionsInterface
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.speaking.AnalysisData
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeakingViewModel(
    private val repository: SpeakingRepository,
    private val apiLinkViewModel: ApiLinkViewModel,
    private val userProfileViewModel: UserProfileViewModel
) : ViewModel() {
  /** The analysis data collected. It is not final as the user can still re-record another audio. */
  val interviewPromptNb = MutableStateFlow("")

  /** The analysis data collected. It is not final as the user can still re-record another audio. */
  private val _analysisData = MutableStateFlow<AnalysisData?>(null)
  val analysisData: StateFlow<AnalysisData?> = _analysisData.asStateFlow()

  /** The result of the analysis of the user's speech. */
  val analysisState: StateFlow<SpeakingRepository.AnalysisState> = repository.analysisState

  /** The error that occurred during processing of the user's speech. */
  private val _analysisError = MutableStateFlow(SpeakingError.NO_ERROR)
  val analysisError = _analysisError.asStateFlow()

  private val _isRecording = MutableStateFlow(false)

  /** True if the user is currently recording their speech, false otherwise. */
  val isRecording: StateFlow<Boolean> = _isRecording

  val fileSaved: StateFlow<Boolean> = repository.fileSaved

  fun setFileSaved(newVal: Boolean) {
    repository.setFileSaved(newVal)
  }
  /** To be called when the speaking screen is closed or the "Done" button is pressed. */
  fun endAndSave() {
    if (_isRecording.value) {
      repository.stopRecording() // Ensure the recording stops
      _isRecording.value = false
    }
    if (_analysisData.value != null) {
      apiLinkViewModel.updateAnalysisData(_analysisData.value!!)
    }
    repository.resetRecorder()
    _analysisData.value = null
  }
  // Suspend function to handle transcript fetching
  suspend fun getTranscript(
      audioFile: File,
      offlinePromptsFunctions: OfflinePromptsFunctionsInterface = OfflinePromptsFunctions(),
      id: String,
      context: Context
  ) {
    // Suspend until the transcript is available
    withContext(Dispatchers.IO) {
      repository.getTranscript(
          audioFile,
          onSuccess = { ad ->
            val transcription: String = ad.transcription.toString()
            offlinePromptsFunctions.changePromptStatus(id, context, "transcription", transcription)
            offlinePromptsFunctions.changePromptStatus(id, context, "GPTresponse", "1")
          },
          onFailure = { error ->
            _analysisError.value = error
            offlinePromptsFunctions.clearDisplayText()
            offlinePromptsFunctions.changePromptStatus(id, context, "transcribed", "0")
          })
    }

    Log.d("in speaking view model", "get transcript for offline mode has been called successfully")
    repository.startRecording()
    repository.stopRecording()
    // might have to suspend here
  }

  /**
   * Function that allows to get transcript and subsequently get a gpt response It is in this view
   * model because it is necessary to modify private variables in this view model for this to work
   *
   * @param audioFile : The audio file to transcript - corresponds to the offline mode recording
   * @param prompts : String -> String mapping that maps the interviews to the companies, target job
   *   position, interview ID, chatGPTresponse, transcription, and transcribed
   * @param viewModel : chat view model that is needed to get the GPT response
   * @param offlinePromptsFunctions : offline prompt functions and variables needed to write to
   *   files
   */
  fun getTranscriptAndGetGPTResponse(
      audioFile: File,
      prompts: Map<String, String>?,
      viewModel: ChatViewModel,
      context: Context,
      offlinePromptsFunctions: OfflinePromptsFunctionsInterface
  ) {
    // Launch a coroutine to have this run in the background and parallelize
    viewModelScope.launch {
      val ID = prompts?.get("ID") ?: "00000000"
      // offlinePromptsFunctions.stopFeedback(ID, context)
      // Wait for the transcript
      val notTranscribing =
          offlinePromptsFunctions.changePromptStatus(ID, context, "transcribed", "1")
      if (notTranscribing) {
        getTranscript(audioFile, offlinePromptsFunctions, ID, context)
        Log.d(
            "finished transcript",
            "finished transcript of file: ${offlinePromptsFunctions.getPromptMapElement(ID, "transcription", context)}")

        // polling the gpt response
        var promptGPTVal =
            offlinePromptsFunctions.getPromptMapElement(ID, "GPTresponse", context).toString()
        while (promptGPTVal != "1") {
          delay(500) // Check every 500ms
          promptGPTVal =
              offlinePromptsFunctions.getPromptMapElement(ID, "GPTresponse", context).toString()
        }
        // if the transcription did not fail then request a prompt for feedback
        if (promptGPTVal == "1") { // for debugging and testing
          val transcription =
              offlinePromptsFunctions.getPromptMapElement(ID, "transcription", context)
          Log.d("before response", "the transcription was successful $transcription")
          if (transcription != "") {
            viewModel.offlineRequest(
                transcription!!.trim(),
                prompts?.get("targetCompany") ?: "Apple",
                prompts?.get("jobPosition") ?: "engineer",
                ID,
                context)
          }
        } else {
          Log.d("error", "transcription might have failed")
          offlinePromptsFunctions.stopFeedback(ID, context)
        }
      } else {
        Log.d("in speaking view model", "already transcribing!!")
      }
    }
  }

  // Function to handle microphone button click
  fun onMicButtonClicked(permissionGranted: Boolean, audioFile: File) {
    if (permissionGranted) {
      if (isRecording.value) {
        repository.stopRecording()
        _isRecording.value = false
      } else {
        repository.setupAnalysisResultsUsage(
            onSuccess = { ad ->
              _analysisData.value = ad
              userProfileViewModel.addNewestData(_analysisData.value!!)
              userProfileViewModel.updateMetricMean()
              // Reset the error state
              _analysisError.value = SpeakingError.NO_ERROR
            },
            onFailure = { error -> _analysisError.value = error })
        repository.startRecording(audioFile)
        _isRecording.value = true
      }
    } else {
      Log.e("SpeakingViewModel", "Microphone permission not granted.")
    }
  }
}
