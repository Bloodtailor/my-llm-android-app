package com.bloodtailor.myllmapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.ui.components.StatusMessage
import com.bloodtailor.myllmapp.ui.components.PromptInput
import com.bloodtailor.myllmapp.ui.components.ResponseDisplay
import com.bloodtailor.myllmapp.ui.state.UiStateManager
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Main chat screen with prompt input and response display
 */
@Composable
fun ChatScreen(
    viewModel: LlmViewModel,
    uiStateManager: UiStateManager,
    modifier: Modifier = Modifier
) {
    val uiState = uiStateManager.uiState

    // Remove the separate currentPrompt state - use only uiState.currentPrompt
    // Keep the prompt synchronized between local state and UiStateManager
    LaunchedEffect(Unit) {
        // Initialize uiState.currentPrompt if it's empty but we have saved state
        if (uiState.currentPrompt.isEmpty()) {
            // This will trigger if we're coming back from a process death
            // The UiStateManager should maintain the state, but just in case
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status message if needed
        if (viewModel.statusMessage.isNotEmpty()) {
            StatusMessage(viewModel.statusMessage)
        }

        // Current model indicator
        if (viewModel.currentModelLoaded) {
            Text(
                text = "Using model: ${viewModel.currentModel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Prompt input - use uiState.currentPrompt directly
        PromptInput(
            prompt = uiState.currentPrompt, // Use uiState.currentPrompt instead of local currentPrompt
            onPromptChanged = { newPrompt ->
                uiStateManager.updatePrompt(newPrompt) // This updates uiState.currentPrompt
            },
            showFormattedPrompt = uiState.showFormattedPrompt,
            onShowFormattedPromptChanged = { show ->
                uiStateManager.toggleFormattedPrompt(show)
            },
            formattedPrompt = uiState.localFormattedPrompt,
            viewModel = viewModel,
            onFormattedPromptUpdated = { formatted ->
                uiStateManager.updateFormattedPrompt(formatted)
            },
            onExpandClick = {
                uiStateManager.showFullScreenInput()
            }
        )

        // Response display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ResponseDisplay(
                response = viewModel.llmResponse,
                isLoading = viewModel.isLoading,
                onExpandClick = {
                    uiStateManager.showFullScreenResponse()
                }
            )
        }
    }
}