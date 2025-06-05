package com.bloodtailor.myllmapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.bloodtailor.myllmapp.ui.screens.FullScreenPromptEditor
import com.bloodtailor.myllmapp.ui.screens.FullScreenResponseViewer
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

    // Handle full-screen modes - wrapped in theme to ensure proper dark mode
    when {
        uiState.showFullScreenInput -> {
            MyLLMAppTheme {
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
        }

        uiState.showFullScreenResponse -> {
            MyLLMAppTheme {
                FullScreenResponseViewer(
                    response = viewModel.llmResponse,
                    onClose = {
                        uiStateManager.hideFullScreenResponse()
                    }
                )
            }
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