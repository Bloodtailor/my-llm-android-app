package com.bloodtailor.myllmapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.ui.*
import com.bloodtailor.myllmapp.ui.components.PromptInput
import com.bloodtailor.myllmapp.ui.components.StatusMessage
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
    onNavigateToParameters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = uiStateManager.uiState

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Using model: ${viewModel.currentModel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Quick parameters access
                TextButton(
                    onClick = onNavigateToParameters,
                    enabled = viewModel.currentModelLoaded
                ) {
                    Text("Parameters")
                }
            }
        }

        // Prompt input
        PromptInput(
            prompt = uiState.currentPrompt,
            onPromptChanged = { newPrompt ->
                uiStateManager.updatePrompt(newPrompt)
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