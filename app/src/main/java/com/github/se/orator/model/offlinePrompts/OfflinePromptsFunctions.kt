package com.github.se.orator.model.offlinePrompts

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflinePromptsFunctions : OfflinePromptsFunctionsInterface {

  private val _fileData = MutableStateFlow<String?>("")
  override val fileData: StateFlow<String?> = _fileData.asStateFlow()
  override val TRANSCRIBED: String
    get() = "transcribed"
  /** Loads the prompts and returns a MUTABLE LIST unlike loadpromptsfromfile */
  private fun retrievePrompts(context: Context): MutableList<Map<String, String>> {
    val file = File(context.cacheDir, "prompts_cache.json")
    val existingPrompts: MutableList<Map<String, String>> =
        if (file.exists()) {
          val json = file.readText()
          Gson().fromJson(json, List::class.java).toMutableList()
              as MutableList<Map<String, String>>
        } else {
          mutableListOf()
        }

    return existingPrompts
  }
  /**
   * Helper function that allows to save offline recording context
   *
   * @param prompts : dictionary mapping strings to strings. It is of the following format:
   *   targetCompany: the target company for this interview jobPosition: the target job position for
   *   this interview ID: the uniquely identifying, randomly generated string that is the title of
   *   the feedback prompt text file and the audio recording of the interview writtenTo transcribed
   */
  override fun savePromptsToFile(prompts: Map<String, String>, context: Context) {
    val file = File(context.cacheDir, "prompts_cache.json")

    // Load existing list of prompts if the file exists
    val existingPrompts = retrievePrompts(context)

    // Add the new prompts to the list
    existingPrompts.add(prompts)

    // Save the updated list back to the file
    val json = Gson().toJson(existingPrompts)
    file.writeText(json)
  }

  /**
   * Retrieves the prompt index in the prompts mapping
   *
   * @param id: ID of the interview
   * @return the index
   */
  private fun retrievePromptIndex(id: String, context: Context): Int {
    val existingPrompts = retrievePrompts(context)
    val index = existingPrompts.indexOfFirst { it["ID"] == id }

    if (index == -1) {
      throw IllegalArgumentException("No entry found with ID: $id")
    }
    return index
  }

  /**
   * @param entry: The entry in the mapping we want to change: Can either be the prompt has been
   *   written to mapping or has been transcribed
   * @param value: The new value for transcribed mapping. Can be set to 0 in case the transcription
   *   failed and we want to try again
   * @return Whether the change of value has been successful
   */
  override fun changePromptStatus(
      id: String,
      context: Context,
      entry: String,
      value: String
  ): Boolean {
    val existingPrompts: MutableList<Map<String, String>> = retrievePrompts(context)
    val index = retrievePromptIndex(id, context)

    // We want to update the "transcribed" mapping
    val updatedEntry = existingPrompts[index].toMutableMap()

    // if we're trying to write 1 when the entry is already 1 then stop
    // this is to fix the glitch of sending multiple transcriptions at once
    if ((updatedEntry[entry] == "1" && value == "1") ||
        (entry == "transcribed" && "transcription" == "")) {
      Log.d("offpr", "could not transcribe $value to $entry for prompt $id")
      return false // prompt is already being transcribed to
    }

    updatedEntry[entry] = value
    updatePromptById(id, updatedEntry, context)

    Log.d("offpr", "successfuly changed $entry value to $value for prompt $id")
    return true
  }

  /**
   * Function that takes an already existing prompt and changes its mappings' values.
   *
   * @param id: prompt's uniquely identifying ID.
   * @param updatedData: new mapping we want
   * @param context: context (needed to open the file containing the prompts)
   */
  private fun updatePromptById(id: String, updatedData: Map<String, String>, context: Context) {
    val file = File(context.cacheDir, "prompts_cache.json")
    val index = retrievePromptIndex(id, context)
    val existingPrompts = retrievePrompts(context)

    val updatedEntry = existingPrompts[index].toMutableMap()
    updatedData.forEach { (key, value) -> updatedEntry[key] = value }
    existingPrompts[index] = updatedEntry

    // Save the updated list back to the file
    val json = Gson().toJson(existingPrompts)
    file.writeText(json)
  }

  /**  */
  override fun loadPromptsFromFile(context: Context): List<Map<String, String>>? {
    val file = File(context.cacheDir, "prompts_cache.json")
    return if (file.exists()) {
      val json = file.readText()
      Gson().fromJson(json, List::class.java) as List<Map<String, String>>
    } else null
  }

  /**
   * Function to create an empty file that will soon contain the GPT response to the offline
   * interview question Data will be stored in a .txt file
   *
   * @param context: context
   * @param ID: ID of the offline interview to know where to store the gpt response
   */
  override fun createEmptyPromptFile(context: Context, ID: String) {
    val fileName = "$ID.txt"
    val fileIsEmptyHeader = "0//!"
    val file = File(context.cacheDir, fileName)
    file.writeText(fileIsEmptyHeader)
  }

  /** Function used to overwrite a gpt prompt response to a file */
  override fun writeToPromptFile(context: Context, ID: String, prompt: String) {
    val fileName = "$ID.txt"
    val file = File(context.cacheDir, fileName)
    file.writeText(prompt)
    _fileData.value = prompt
    Log.d("wrote to file $fileName", "$prompt")
  }

  /**
   * Reads the value in a file containing the gpt prompt response to the interview question stores
   * the value in the mutable state flow fileData to be accessable outside and mockable
   *
   * @param ID: id of the interview
   * @param context: context
   */
  override fun readPromptTextFile(context: Context, ID: String) {
    val fileName = "$ID.txt"
    Log.d("repo readptf", "inside the txt file $fileName")
    val file = File(context.cacheDir, fileName)
    val fileContents: String = file.readText()

    if (fileContents == "0//!" || fileContents == "") {
      _fileData.value = "Loading interviewer response..."
    } else {
      _fileData.value = fileContents
      Log.d("off prompt", "file name: $fileName; repo stuff $fileContents")
    }
  }

  override fun clearDisplayText() {
    _fileData.value = ""
  }

  override fun getPromptMapElement(id: String, element: String, context: Context): String? {
    val existingPrompts: MutableList<Map<String, String>> = retrievePrompts(context)
    val index = retrievePromptIndex(id, context)

    // We want to update the "transcribed" mapping
    val entry = existingPrompts[index].toMutableMap()
    return entry[element]
  }

  /**
   * Function to change the requesting GPT feedback and transcribing states for the prompt in the
   * mapping Used whenever an error happens with transcribing
   */
  override fun stopFeedback(ID: String, context: Context) {
    readPromptTextFile(context, ID)
    if (_fileData.value == "Loading interviewer response...") {
      changePromptStatus(ID, context, "transcribed", "0")
      changePromptStatus(ID, context, "GPTresponse", "0")
    }
    if (getPromptMapElement(ID, "transcription", context) == "") {
      changePromptStatus(ID, context, "transcribed", "0")
      changePromptStatus(ID, context, "GPTresponse", "0")
    }
  }
}
