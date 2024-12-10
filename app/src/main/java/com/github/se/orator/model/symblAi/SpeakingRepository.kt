package com.github.se.orator.model.symblAi

import com.github.se.orator.model.speaking.AnalysisData
import java.io.File
import kotlinx.coroutines.flow.StateFlow

interface SpeakingRepository {
  val analysisState: StateFlow<AnalysisState>

  fun startRecording()

  fun getTranscript(
      audioFile: File,
      onSuccess: (AnalysisData) -> Unit,
      onFailure: (SpeakingError) -> Unit
  )

  fun startRecordingToFile(audioFile: File)

  fun stopRecording()

  fun setupAnalysisResultsUsage(
      onSuccess: (AnalysisData) -> Unit,
      onFailure: (SpeakingError) -> Unit
  )

  fun resetRecorder()

  enum class AnalysisState {
    IDLE,
    RECORDING,
    PROCESSING,
    FINISHED
  }
}
