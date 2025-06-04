package com.bloodtailor.myllmapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Model selection dropdown component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: String,
    availableModels: List<String>,
    enabled: Boolean,
    onModelSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Model:", modifier = Modifier.width(80.dp))

        Box(
            modifier = Modifier.weight(1f)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded }
            ) {
                TextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    trailingIcon = {
                        if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    onModelSelected(model)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Context length input component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextLengthInput(
    contextLength: String,
    enabled: Boolean,
    defaultContextLength: Int,
    onContextLengthChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Context:", modifier = Modifier.width(80.dp))

        OutlinedTextField(
            value = contextLength,
            onValueChange = {
                // Only allow numeric input
                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                    onContextLengthChanged(it)
                }
            },
            enabled = enabled,
            label = { Text("Context Length") },
            placeholder = { Text("Default: $defaultContextLength") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Model control buttons component
 */
@Composable
fun ModelControlButtons(
    viewModel: LlmViewModel,
    selectedModel: String,
    contextLengthInput: String,
    prompt: String,
    showFormattedPrompt: Boolean,
    onFormattedPromptUpdated: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                // Parse context length if provided
                val contextLength = contextLengthInput.toIntOrNull()

                viewModel.loadModel(selectedModel, contextLength) { success ->
                    // Update context usage if prompt exists and checkbox is checked
                    if (success && prompt.isNotEmpty()) {
                        viewModel.updateContextUsage(prompt) { rawPrompt ->
                            // For "show formatted prompt", we just show the raw prompt now
                            if (showFormattedPrompt) {
                                onFormattedPromptUpdated(rawPrompt)
                            }
                        }
                    }
                }
            },
            enabled = !viewModel.isLoading &&
                    (!viewModel.currentModelLoaded ||
                            (viewModel.currentModel != selectedModel) ||
                            (contextLengthInput.toIntOrNull() != viewModel.currentContextLength)),
            modifier = Modifier.weight(1f)
        ) {
            Text("Load Model")
        }

        Button(
            onClick = {
                viewModel.unloadModel()
            },
            enabled = !viewModel.isLoading && viewModel.currentModelLoaded,
            modifier = Modifier.weight(1f)
        ) {
            Text("Unload Model")
        }
    }
}

/**
 * Model settings dialog component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsDialog(
    showDialog: Boolean,
    viewModel: LlmViewModel,
    onDismiss: () -> Unit
) {
    // State variables with rotation persistence
    var selectedModel by rememberSaveable { mutableStateOf("") }
    var contextLengthInput by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Update selectedModel when viewModel values change
    LaunchedEffect(viewModel.availableModels, viewModel.currentModel, showDialog) {
        if (showDialog) {
            // Initialize with current model if available, otherwise first available model
            selectedModel = viewModel.currentModel ?:
                    if (viewModel.availableModels.isNotEmpty()) viewModel.availableModels[0] else ""

            // Initialize context length input with current context length
            contextLengthInput = viewModel.currentContextLength?.toString() ?: ""
        }
    }

    // Debug log available models - you can remove this later
    LaunchedEffect(viewModel.availableModels) {
        android.util.Log.d("ModelSettingsDialog", "Available models: ${viewModel.availableModels.joinToString()}")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Model Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Model selector
                    Text("Select Model:", style = MaterialTheme.typography.labelLarge)

                    // Show a message if no models are available
                    if (viewModel.availableModels.isEmpty()) {
                        Text(
                            "No models available. Please check server connection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        // Dropdown for model selection
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedModel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Model") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                viewModel.availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            selectedModel = model
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Context length
                    Text("Context Length:", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = contextLengthInput,
                        onValueChange = {
                            // Only allow numeric input
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                contextLengthInput = it
                            }
                        },
                        label = { Text("Context Length") },
                        placeholder = { Text("Default: ${viewModel.DEFAULT_CONTEXT_LENGTH}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Model status display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Current Status",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                "Status: ${if (viewModel.currentModelLoaded) "Loaded" else "Not Loaded"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (viewModel.currentModelLoaded && viewModel.currentModel != null) {
                                Text(
                                    "Current Model: ${viewModel.currentModel}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (viewModel.currentContextLength != null) {
                                    Text(
                                        "Context Length: ${viewModel.currentContextLength}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Refresh button to fetch models
                    Button(
                        onClick = { viewModel.fetchAvailableModels() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Models"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh Models")
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            // Parse context length if provided
                            val contextLength = contextLengthInput.toIntOrNull()
                            viewModel.loadModel(selectedModel, contextLength)
                            onDismiss()
                        },
                        enabled = selectedModel.isNotEmpty() &&
                                (!viewModel.currentModelLoaded ||
                                        (viewModel.currentModel != selectedModel) ||
                                        (contextLengthInput.toIntOrNull() != viewModel.currentContextLength))
                    ) {
                        Text("Load Model")
                    }

                    Button(
                        onClick = {
                            viewModel.unloadModel()
                            onDismiss()
                        },
                        enabled = viewModel.currentModelLoaded
                    ) {
                        Text("Unload Model")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}