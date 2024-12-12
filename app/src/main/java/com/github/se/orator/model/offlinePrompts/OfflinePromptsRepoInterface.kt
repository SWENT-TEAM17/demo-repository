package com.github.se.orator.model.offlinePrompts

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface OfflinePromptsRepoInterface {
    val fileData: StateFlow<String?>
    fun savePromptsToFile(prompts:Map<String, String>, context: Context)
    fun loadPromptsFromFile(context: Context): List<Map<String, String>>?
    fun createEmptyPromptFile(context: Context, ID: String)
    fun writeToPromptFile(context: Context, ID: String, prompt: String)
    fun readPromptTextFile(context: Context, ID: String)
    fun clearDisplayText()
}