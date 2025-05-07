package com.bloodtailor.myllmapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import androidx.compose.ui.unit.sp

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
    var expanded by remember { mutableStateOf(false) }
    
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
                    // Get formatted prompt example if successful
                    if (success && prompt.isNotEmpty() && showFormattedPrompt) {
                        viewModel.formatPrompt(prompt) { formatted ->
                            onFormattedPromptUpdated(formatted)
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
 * Prompt input with formatted preview component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptInput(
    prompt: String,
    onPromptChanged: (String) -> Unit,
    showFormattedPrompt: Boolean,
    onShowFormattedPromptChanged: (Boolean) -> Unit,
    formattedPrompt: String,
    viewModel: LlmViewModel,
    onFormattedPromptUpdated: (String) -> Unit
) {
    Column {
        // Prompt input field with scroll
        OutlinedTextField(
            value = prompt,
            onValueChange = { newPrompt ->
                onPromptChanged(newPrompt)
                // Update formatted prompt preview if needed
                if (viewModel.currentModelLoaded && showFormattedPrompt) {
                    viewModel.formatPrompt(newPrompt) { formatted ->
                        onFormattedPromptUpdated(formatted)
                    }
                }
            },
            label = { Text("Enter your prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp), // Set min and max height
            textStyle = LocalTextStyle.current.copy(lineHeight = 20.sp),
            maxLines = 10, // Allow more lines
        )

        // Formatted prompt toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showFormattedPrompt,
                onCheckedChange = { checked ->
                    onShowFormattedPromptChanged(checked)
                    if (checked && prompt.isNotEmpty() && viewModel.currentModelLoaded) {
                        viewModel.formatPrompt(prompt) { formatted ->
                            onFormattedPromptUpdated(formatted)
                        }
                    }
                }
            )
            Text("Show formatted prompt")
        }

        // Formatted prompt preview with better scrolling
        if (showFormattedPrompt) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Formatted Prompt:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 200.dp) // Limit height
                            .background(
                                Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = formattedPrompt,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()) // Add vertical scroll
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Send button component
 */
@Composable
fun SendButton(
    viewModel: LlmViewModel,
    prompt: String,
    useFormattedPrompt: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                viewModel.sendPrompt(
                    prompt = prompt,
                    systemPrompt = "", // Not using system prompts for now
                    useFormattedPrompt = useFormattedPrompt
                )
            },
            enabled = !viewModel.isLoading && 
                prompt.isNotEmpty() && 
                viewModel.currentModelLoaded
        ) {
            Text("Send")
        }
    }
}

/**
 * Response display component with copy button
 */
@Composable
fun ResponseDisplay(
    response: String,
    isLoading: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()  // Use fillMaxHeight instead of weight
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Response:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                // Response text with selection support and better scrolling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp) // Leave space for the button
                ) {
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = response,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                // Copy button at the bottom right
                FloatingActionButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("LLM Response", response)
                        clipboard.setPrimaryClip(clip)

                        // Show toast notification
                        Toast.makeText(context, "Response copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

/**
 * Settings dialog component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    showDialog: Boolean,
    currentServerUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempServerUrl by remember { mutableStateOf(currentServerUrl) }

    LaunchedEffect(currentServerUrl, showDialog) {
        if (showDialog) {
            tempServerUrl = currentServerUrl
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Server Settings") },
            text = {
                Column {
                    Text("Enter the server URL including port (e.g., http://192.168.1.100:5000)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: Clicking 'Save' will attempt to connect to the server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(tempServerUrl)
                        onDismiss()
                    }
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsDialog(
    showDialog: Boolean,
    viewModel: LlmViewModel,
    onDismiss: () -> Unit
) {
    var selectedModel by remember { mutableStateOf(viewModel.currentModel ?: "") }
    var contextLengthInput by remember { mutableStateOf(viewModel.currentContextLength?.toString() ?: "") }

    // Update selected model when viewModel.availableModels or viewModel.currentModel changes
    LaunchedEffect(viewModel.availableModels, viewModel.currentModel) {
        if (viewModel.availableModels.isNotEmpty() && selectedModel.isEmpty()) {
            selectedModel = viewModel.availableModels.first()
        }
        if (viewModel.currentModel != null) {
            selectedModel = viewModel.currentModel!!
        }
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
                    ExposedDropdownMenuBox(
                        expanded = false, // We'll handle this separately
                        onExpandedChange = { /* Handle separately */ }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = false, // We'll handle this separately
                            onDismissRequest = {}
                        ) {
                            viewModel.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = { selectedModel = model }
                                )
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

                    // Model status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Status: ${if (viewModel.currentModelLoaded) "Loaded" else "Not Loaded"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        if (viewModel.currentModelLoaded) {
                            Text(
                                "Current: ${viewModel.currentModel}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
                        enabled = !viewModel.isLoading &&
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
                        enabled = !viewModel.isLoading && viewModel.currentModelLoaded
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

/**
 * Status message display component
 */
@Composable
fun StatusMessage(message: String) {
    if (message.isNotEmpty()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}