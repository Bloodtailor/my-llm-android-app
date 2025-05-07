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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.bloodtailor.myllmapp.ui.*
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LlmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get application's repository instance
        val repository = (application as LlmApplication).repository

        // Initialize ViewModel with the repository's URL
        viewModel = ViewModelProvider(this).get(LlmViewModel::class.java)

        // Update the view model with the current URL from repository
        viewModel.updateServerUrl(repository.getServerUrl(), true)

        setContent {
            LLMAppUI()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LLMAppUI() {
        // Local UI state
        var prompt by remember { mutableStateOf("") }
        var showFormattedPrompt by remember { mutableStateOf(false) }
        var localFormattedPrompt by remember { mutableStateOf("") }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showModelDialog by remember { mutableStateOf(false) }
        var showSettingsOnStart by remember { mutableStateOf(true) }

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
            android.util.Log.d("MainActivity", "Observed models change: ${viewModel.availableModels}")
        }

        MaterialTheme {
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
                                Icon(Icons.Default.Settings, contentDescription = "Server Settings")
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
                        Text(
                            viewModel.statusMessage,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
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

                    // Response display - ensure this is in a layout that supports weight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)  // This works in a Column
                    ) {
                        ResponseDisplay(
                            response = viewModel.llmResponse,
                            isLoading = viewModel.isLoading
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