package com.bloodtailor.myllmapp.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bloodtailor.myllmapp.data.database.SavedPrompt

/**
 * Centralized UI state management for the app
 */
@Stable
data class UiState(
    val currentPrompt: String = "",
    val showFormattedPrompt: Boolean = false,
    val localFormattedPrompt: String = "",
    val showFullScreenInput: Boolean = false,
    val showFullScreenResponse: Boolean = false,
    val showSavedPromptEditor: Boolean = false,
    val editingSavedPrompt: SavedPrompt? = null
)

/**
 * UI state manager for handling app-wide UI state
 */
class UiStateManager {
    var uiState by mutableStateOf(UiState())
        private set

    fun updatePrompt(prompt: String) {
        uiState = uiState.copy(currentPrompt = prompt)
    }

    fun toggleFormattedPrompt(show: Boolean) {
        uiState = uiState.copy(showFormattedPrompt = show)
    }

    fun updateFormattedPrompt(formatted: String) {
        uiState = uiState.copy(localFormattedPrompt = formatted)
    }

    fun showFullScreenInput() {
        uiState = uiState.copy(showFullScreenInput = true)
    }

    fun hideFullScreenInput() {
        uiState = uiState.copy(showFullScreenInput = false)
    }

    fun showFullScreenResponse() {
        uiState = uiState.copy(showFullScreenResponse = true)
    }

    fun hideFullScreenResponse() {
        uiState = uiState.copy(showFullScreenResponse = false)
    }

    fun showSavedPromptEditor(prompt: SavedPrompt? = null) {
        uiState = uiState.copy(
            showSavedPromptEditor = true,
            editingSavedPrompt = prompt
        )
    }

    fun hideSavedPromptEditor() {
        uiState = uiState.copy(
            showSavedPromptEditor = false,
            editingSavedPrompt = null
        )
    }

    fun clearPrompt() {
        uiState = uiState.copy(
            currentPrompt = "",
            localFormattedPrompt = ""
        )
    }
}