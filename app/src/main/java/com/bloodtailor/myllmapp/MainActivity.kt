package com.bloodtailor.myllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.bloodtailor.myllmapp.ui.*
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import com.bloodtailor.myllmapp.viewmodel.LlmViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LlmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get application's repository instance
        val repository = (application as LlmApplication).repository

        // Initialize ViewModel with SavedStateHandle support for rotation persistence
        val factory = LlmViewModelFactory(application, this)
        viewModel = ViewModelProvider(this, factory).get(LlmViewModel::class.java)

        // Only update server URL if this is a fresh start (not a rotation)
        // The ViewModel will restore its saved state automatically after rotation
        if (savedInstanceState == null) {
            // Fresh start - use repository URL
            viewModel.updateServerUrl(repository.getServerUrl(), true)
        } else {
            // After rotation - sync repository with ViewModel's restored state
            repository.updateServerUrl(viewModel.serverUrl)
        }

        setContent {
            LLMAppUI()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LLMAppUI() {
        // UI state that persists across screen rotations
        var prompt by rememberSaveable { mutableStateOf("") }
        var showFormattedPrompt by rememberSaveable { mutableStateOf(false) }
        var localFormattedPrompt by rememberSaveable { mutableStateOf("") }
        var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
        var showModelDialog by rememberSaveable { mutableStateOf(false) }
        var showSettingsOnStart by rememberSaveable { mutableStateOf(true) }
        var showFullScreenInput by rememberSaveable { mutableStateOf(false) }
        var showFullScreenResponse by rememberSaveable { mutableStateOf(false) }

        // Add connection status state
        val connectionStatus = remember {
            derivedStateOf {
                if (viewModel.availableModels.isNotEmpty()) "Connected" else "Not Connected"
            }
        }

        // Show settings dialog on first launch
        LaunchedEffect(key1 = showSettingsOnStart) {
            if (showSettingsOnStart && viewModel.availableModels.isEmpty()) {
                showSettingsDialog = true
                showSettingsOnStart = false
            }
        }

        // Debugging composable effect
        LaunchedEffect(viewModel.availableModels) {
            android.util.Log.d(
                "MainActivity",
                "Observed models change: ${viewModel.availableModels}"
            )
        }

        MaterialTheme {
            if (showFullScreenInput) {
                FullScreenPromptEditor(
                    prompt = prompt,
                    onPromptChanged = { prompt = it },
                    onSend = {
                        viewModel.sendPrompt(
                            prompt = prompt,
                            systemPrompt = ""
                        )
                    },
                    onClose = { showFullScreenInput = false },
                    viewModel = viewModel
                )
            } else if (showFullScreenResponse) {
                FullScreenResponseViewer(
                    response = viewModel.llmResponse,
                    onClose = { showFullScreenResponse = false }
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("LLM App") },
                            actions = {
                                // Connection status indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = if (connectionStatus.value == "Connected")
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = connectionStatus.value,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Settings button
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Server Settings"
                                    )
                                }

                                // Model settings button
                                IconButton(onClick = { showModelDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = "Model Settings"
                                    )
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
                        // Status message if needed
                        if (viewModel.statusMessage.isNotEmpty()) {
                            StatusMessage(viewModel.statusMessage)
                        }

                        // Current model indicator
                        if (viewModel.currentModelLoaded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Using model: ${viewModel.currentModel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Prompt input with raw prompt preview and context usage
                        PromptInput(
                            prompt = prompt,
                            onPromptChanged = { prompt = it },
                            showFormattedPrompt = showFormattedPrompt,
                            onShowFormattedPromptChanged = { showFormattedPrompt = it },
                            formattedPrompt = localFormattedPrompt,
                            viewModel = viewModel,
                            onFormattedPromptUpdated = { localFormattedPrompt = it },
                            onExpandClick = { showFullScreenInput = true }
                        )

                        // Send button - simplified to always send raw prompts
                        SendButton(
                            viewModel = viewModel,
                            prompt = prompt
                        )

                        // Response display - ensure this is in a layout that supports weight
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)  // This works in a Column
                        ) {
                            ResponseDisplay(
                                response = viewModel.llmResponse,
                                isLoading = viewModel.isLoading,
                                onExpandClick = { showFullScreenResponse = true }
                            )
                        }
                    }
                }

                // Settings dialog
                SettingsDialog(
                    showDialog = showSettingsDialog,
                    currentServerUrl = viewModel.serverUrl,
                    onDismiss = { showSettingsDialog = false },
                    onSave = { newUrl ->
                        viewModel.updateServerUrl(newUrl, true) // true = autoConnect
                    }
                )

                // Model settings dialog
                ModelSettingsDialog(
                    showDialog = showModelDialog,
                    viewModel = viewModel,
                    onDismiss = { showModelDialog = false }
                )
            }
        }
    }
}