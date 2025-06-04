package com.bloodtailor.myllmapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp
import com.bloodtailor.myllmapp.network.ContextUsage

/**
 * Full-screen prompt editor for editing long prompts with navigation controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPromptEditor(
    prompt: String,
    onPromptChanged: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle back gesture
    BackHandler(enabled = true) {
        onClose()
    }

    // Text field state for cursor management - preserve cursor position across rotations
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(prompt, TextRange(prompt.length)))
    }

    // Focus requester for keyboard control
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Update parent state when text changes, but preserve cursor position
    LaunchedEffect(textFieldValue.text) {
        onPromptChanged(textFieldValue.text)
    }

    // Only update local state when prompt changes externally and text is different
    LaunchedEffect(prompt) {
        if (textFieldValue.text != prompt) {
            // Preserve cursor position if possible
            val newSelection = if (textFieldValue.selection.start <= prompt.length) {
                textFieldValue.selection
            } else {
                TextRange(prompt.length)
            }
            textFieldValue = TextFieldValue(prompt, newSelection)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding() // This handles keyboard padding
    ) {
        // Main text editor
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(focusRequester)
                .padding(16.dp),
            label = { Text("Edit your prompt") },
            placeholder = { Text("Enter your prompt here...") }
        )

        // Bottom control bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top button - icon only
                OutlinedButton(
                    onClick = {
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(0)
                        )
                        keyboardController?.hide()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Go to top"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Bottom button - icon only
                OutlinedButton(
                    onClick = {
                        val textLength = textFieldValue.text.length
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(textLength)
                        )
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Go to bottom and show keyboard"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send button - icon only
                Button(
                    onClick = {
                        onSend()
                        onClose()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = textFieldValue.text.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send prompt"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // More button - icon only (placeholder for future features)
                OutlinedButton(
                    onClick = {
                        // TODO: Implement prefix/suffix popup
                    },
                    modifier = Modifier.weight(1f),
                    enabled = false // Disabled for now
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "More options"
                    )
                }
            }
        }
    }
}

/**
 * Full-screen response viewer for reading and copying long responses
 */
@Composable
fun FullScreenResponseViewer(
    response: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle back gesture
    BackHandler(enabled = true) {
        onClose()
    }

    // Full-screen text display with proper system bar padding
    SelectionContainer(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        Text(
            text = response,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

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
 * Enhanced prompt input with expand button and context usage component
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
    onFormattedPromptUpdated: (String) -> Unit,
    onExpandClick: (() -> Unit)? = null
) {
    Column {
        // Prompt input field with optional expand button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { newPrompt ->
                    onPromptChanged(newPrompt)
                    // Always update context usage when model is loaded and prompt is not empty
                    if (viewModel.currentModelLoaded && newPrompt.isNotEmpty()) {
                        viewModel.updateContextUsage(newPrompt) { rawPrompt ->
                            // Only update the "formatted" prompt display if the checkbox is checked
                            // Since we're not formatting, this just shows the raw prompt
                            if (showFormattedPrompt) {
                                onFormattedPromptUpdated(rawPrompt)
                            }
                        }
                    } else if (newPrompt.isEmpty()) {
                        // Clear context usage when prompt is empty
                        if (showFormattedPrompt) {
                            onFormattedPromptUpdated("")
                        }
                    }
                },
                label = { Text("Enter your prompt") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 120.dp, max = 200.dp),
                textStyle = LocalTextStyle.current.copy(lineHeight = 20.sp),
                maxLines = 10,
            )

            // Expand button (optional)
            if (onExpandClick != null) {
                IconButton(
                    onClick = onExpandClick,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Expand prompt editor"
                    )
                }
            }
        }

        // Context usage display with send button
        if (viewModel.contextUsage != null && viewModel.currentModelLoaded) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Context usage takes most of the space
                Box(modifier = Modifier.weight(1f)) {
                    ContextUsageDisplay(
                        contextUsage = viewModel.contextUsage
                    )
                }

                // Send button on the right
                Button(
                    onClick = {
                        viewModel.sendPrompt(
                            prompt = prompt,
                            systemPrompt = ""
                        )
                    },
                    enabled = !viewModel.isLoading &&
                            prompt.isNotEmpty() &&
                            viewModel.currentModelLoaded
                ) {
                    Text("Send")
                }
            }
        } else if (viewModel.currentModelLoaded) {
            // Show send button even without context usage if model is loaded
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        viewModel.sendPrompt(
                            prompt = prompt,
                            systemPrompt = ""
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

        // "Show formatted prompt" toggle - now shows raw prompt copy
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
                        // Just show the raw prompt as "formatted" prompt
                        viewModel.updateContextUsage(prompt) { rawPrompt ->
                            onFormattedPromptUpdated(rawPrompt)
                        }
                    }
                }
            )
            Text("Show formatted prompt (currently shows raw prompt)")
        }

        // Formatted prompt preview - now shows raw prompt copy
        if (showFormattedPrompt) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Prompt sent to model:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 200.dp)
                            .background(
                                Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = formattedPrompt.ifEmpty { "Type a prompt above to see what will be sent to the model..." },
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                color = if (formattedPrompt.isEmpty())
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Send button component - simplified without useFormattedPrompt
 * NOTE: This component is now integrated into PromptInput above
 */
@Composable
fun SendButton(
    viewModel: LlmViewModel,
    prompt: String,
    useFormattedPrompt: Boolean = false  // Keep parameter for compatibility but ignore it
) {
    // This component is kept for backward compatibility but the Send button
    // is now integrated into the PromptInput component above
    // You can remove this from MainActivity since it's handled in PromptInput
}

/**
 * Enhanced response display component with optional expand button
 */
@Composable
fun ResponseDisplay(
    response: String,
    isLoading: Boolean,
    onExpandClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()  // Use fillMaxHeight instead of weight
            .padding(vertical = 8.dp)
    ) {
        // Header with optional expand button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Response:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Expand button (optional)
            if (onExpandClick != null) {
                IconButton(onClick = onExpandClick) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Expand response viewer"
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp) // Same size as input box
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                // Response text with selection support and better scrolling
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
                        .size(32.dp), // Smaller size for the fixed height box
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp) // Smaller icon
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
    var tempServerUrl by rememberSaveable { mutableStateOf(currentServerUrl) }

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

/**
 * Context usage display component
 */
@Composable
fun ContextUsageDisplay(
    contextUsage: ContextUsage?,
    modifier: Modifier = Modifier
) {
    if (contextUsage == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Context Usage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${contextUsage.tokenCount} / ${contextUsage.maxContext} tokens",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${contextUsage.usagePercentage}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = when {
                        contextUsage.usagePercentage < 75 -> MaterialTheme.colorScheme.primary
                        contextUsage.usagePercentage < 90 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )

                // Progress indicator
                LinearProgressIndicator(
                    progress = (contextUsage.usagePercentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.width(80.dp),
                    color = when {
                        contextUsage.usagePercentage < 75 -> MaterialTheme.colorScheme.primary
                        contextUsage.usagePercentage < 90 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}