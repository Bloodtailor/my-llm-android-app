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
        // Prompt input field
        TextField(
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
                .height(120.dp)
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
        
        // Formatted prompt preview
        if (showFormattedPrompt) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Formatted Prompt:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = formattedPrompt,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        )
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
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(1f)  // Replace weight with fillMaxHeight
            .padding(vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                // Response text with selection support
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 48.dp)
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
    }
    
    if (isLoading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(tempServerUrl)
                        onDismiss()
                    }
                ) {
                    Text("Save")
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