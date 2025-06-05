package com.bloodtailor.myllmapp.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Centralized UI state management for the app
 */
@Stable
data class UiState(
    val currentPrompt: String = "",
    val showFormattedPrompt: Boolean = false,
    val localFormattedPrompt: String = "",
    val showFullScreenInput: Boolean = false,
    val showFullScreenResponse: Boolean = false
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

    fun clearPrompt() {
        uiState = uiState.copy(
            currentPrompt = "",
            localFormattedPrompt = ""
        )
    }
}