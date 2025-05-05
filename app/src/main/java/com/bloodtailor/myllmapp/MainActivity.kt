package com.bloodtailor.myllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.bloodtailor.myllmapp.ui.*
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LlmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(LlmViewModel::class.java)

        setContent {
            LLMAppUI()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LLMAppUI() {
        // Local UI state
        var prompt by remember { mutableStateOf("") }
        var contextLengthInput by remember { mutableStateOf(viewModel.currentContextLength?.toString() ?: "") }
        var selectedModel by remember { mutableStateOf("") }
        var showFormattedPrompt by remember { mutableStateOf(false) }
        var localFormattedPrompt by remember { mutableStateOf("") }
        var showSettingsDialog by remember { mutableStateOf(false) }

        // Update model selection when availableModels or currentModel changes
        LaunchedEffect(viewModel.availableModels, viewModel.currentModel) {
            if (viewModel.availableModels.isNotEmpty() && selectedModel.isEmpty()) {
                selectedModel = viewModel.availableModels.first()
            }

            if (viewModel.currentModel != null && viewModel.availableModels.contains(viewModel.currentModel)) {
                selectedModel = viewModel.currentModel!!
            }
        }

        // Update context length input when current value changes
        LaunchedEffect(viewModel.currentContextLength) {
            if (viewModel.currentContextLength != null) {
                contextLengthInput = viewModel.currentContextLength.toString()
            }
        }

        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("LLM App") },
                        actions = {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Server connection info
                    Text(
                        "Server: ${viewModel.serverUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Model selector
                    ModelSelector(
                        selectedModel = selectedModel,
                        availableModels = viewModel.availableModels,
                        enabled = !viewModel.isLoading,
                        onModelSelected = { selectedModel = it }
                    )

                    // Context length input
                    ContextLengthInput(
                        contextLength = contextLengthInput,
                        enabled = !viewModel.isLoading,
                        defaultContextLength = viewModel.DEFAULT_CONTEXT_LENGTH,
                        onContextLengthChanged = { contextLengthInput = it }
                    )

                    // Model control buttons
                    ModelControlButtons(
                        viewModel = viewModel,
                        selectedModel = selectedModel,
                        contextLengthInput = contextLengthInput,
                        prompt = prompt,
                        showFormattedPrompt = showFormattedPrompt,
                        onFormattedPromptUpdated = { localFormattedPrompt = it }
                    )

                    // Status message
                    StatusMessage(message = viewModel.statusMessage)

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Prompt input with formatted preview
                    PromptInput(
                        prompt = prompt,
                        onPromptChanged = { prompt = it },
                        showFormattedPrompt = showFormattedPrompt,
                        onShowFormattedPromptChanged = { showFormattedPrompt = it },
                        formattedPrompt = localFormattedPrompt,
                        viewModel = viewModel,
                        onFormattedPromptUpdated = { localFormattedPrompt = it }
                    )

                    // Send button
                    SendButton(
                        viewModel = viewModel,
                        prompt = prompt,
                        useFormattedPrompt = showFormattedPrompt
                    )

                    // Response display
                    ResponseDisplay(
                        response = viewModel.llmResponse,
                        isLoading = viewModel.isLoading
                    )
                }
            }

            // Settings dialog
            SettingsDialog(
                showDialog = showSettingsDialog,
                currentServerUrl = viewModel.serverUrl,
                onDismiss = { showSettingsDialog = false },
                onSave = { newUrl ->
                    viewModel.updateServerUrl(newUrl)
                }
            )
        }
    }
}