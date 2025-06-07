package com.bloodtailor.myllmapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bloodtailor.myllmapp.data.database.SavedPrompt
import com.bloodtailor.myllmapp.ui.screens.FullScreenPromptEditor
import com.bloodtailor.myllmapp.ui.screens.FullScreenResponseViewer
import com.bloodtailor.myllmapp.ui.screens.SavedPromptFullScreenEditor
import com.bloodtailor.myllmapp.ui.dialogs.DialogManager
import com.bloodtailor.myllmapp.ui.dialogs.DialogState
import com.bloodtailor.myllmapp.ui.layout.AppScaffold
import com.bloodtailor.myllmapp.ui.navigation.AppNavigation
import com.bloodtailor.myllmapp.ui.navigation.rememberNavigationState
import com.bloodtailor.myllmapp.ui.state.UiStateManager
import com.bloodtailor.myllmapp.ui.theme.MyLLMAppTheme
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Root app composable that coordinates navigation, dialogs, and full-screen modes
 */
@Composable
fun LLMApp(
    viewModel: LlmViewModel,
    modifier: Modifier = Modifier
) {
    // Create state managers
    val navigationState = rememberNavigationState()
    val uiStateManager = remember { UiStateManager() }
    val dialogState = remember { DialogState() }

    val uiState = uiStateManager.uiState

    val context = LocalContext.current

    // Handle full-screen modes
    when {
        uiState.showFullScreenInput -> {
            FullScreenPromptEditor(
                prompt = uiState.currentPrompt,
                onPromptChanged = { prompt ->
                    uiStateManager.updatePrompt(prompt)
                },
                onSend = {
                    viewModel.sendPrompt(
                        prompt = uiState.currentPrompt,
                        systemPrompt = ""
                    )
                },
                onClose = {
                    uiStateManager.hideFullScreenInput()
                },
                viewModel = viewModel
            )
        }

        uiState.showFullScreenResponse -> {
            FullScreenResponseViewer(
                response = viewModel.llmResponse,
                onClose = {
                    uiStateManager.hideFullScreenResponse()
                }
            )
        }

        uiState.showSavedPromptEditor -> {
            SavedPromptFullScreenEditor(
                prompt = uiState.editingSavedPrompt,
                onSave = { name, content ->
                    if (uiState.editingSavedPrompt != null) {
                        // Update existing prompt
                        val updatedPrompt = uiState.editingSavedPrompt!!.copy(name = name, content = content)
                        viewModel.updateSavedPrompt(updatedPrompt) { success, error ->
                            if (success) {
                                uiStateManager.hideSavedPromptEditor()
                                Toast.makeText(context, "Prompt updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Create new prompt
                        viewModel.createSavedPrompt(name, content) { success, error ->
                            if (success) {
                                uiStateManager.hideSavedPromptEditor()
                                Toast.makeText(context, "Prompt saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onCancel = {
                    uiStateManager.hideSavedPromptEditor()
                }
            )
        }

        else -> {
            // Normal app layout
            AppScaffold(
                viewModel = viewModel,
                navigationState = navigationState,
                onSettingsClick = {
                    dialogState.showSettingsDialog()
                },
                onModelClick = {
                    dialogState.showModelDialog()
                },
                modifier = modifier
            ) { innerPadding ->
                AppNavigation(
                    pagerState = navigationState.pagerState,
                    viewModel = viewModel,
                    uiStateManager = uiStateManager,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // Dialog management
            DialogManager(
                viewModel = viewModel,
                dialogState = dialogState
            )
        }
    }
}