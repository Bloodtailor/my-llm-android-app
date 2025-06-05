package com.bloodtailor.myllmapp.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.bloodtailor.myllmapp.ui.components.SettingsDialog
import com.bloodtailor.myllmapp.ui.components.ModelSettingsDialog
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Centralized dialog management for the app
 */
@Composable
fun DialogManager(
    viewModel: LlmViewModel,
    dialogState: DialogState
) {
    // Auto-show settings on first launch if no models available
    LaunchedEffect(dialogState.showSettingsOnStart, viewModel.availableModels.size) {
        dialogState.checkInitialSetup(viewModel.availableModels.isNotEmpty())
    }

    // Settings Dialog
    SettingsDialog(
        showDialog = dialogState.showSettings,
        currentServerUrl = viewModel.serverUrl,
        onDismiss = { dialogState.hideSettingsDialog() },
        onSave = { newUrl ->
            viewModel.updateServerUrl(newUrl, true)
        }
    )

    // Model Settings Dialog
    ModelSettingsDialog(
        showDialog = dialogState.showModel,
        viewModel = viewModel,
        onDismiss = { dialogState.hideModelDialog() }
    )
}

// Note: Using the existing components from DialogComponents.kt and ModelComponents.kt
// SettingsDialog is in DialogComponents.kt
// ModelSettingsDialog is in ModelComponents.kt