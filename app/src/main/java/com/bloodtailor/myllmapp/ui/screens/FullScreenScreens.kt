package com.bloodtailor.myllmapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.ui.components.PrefixSuffixDialog
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

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
    viewModel: LlmViewModel,
    modifier: Modifier = Modifier
) {
    // Handle back gesture
    BackHandler(enabled = true) {
        onClose()
    }

    // State for prefix/suffix dialog
    var showPrefixSuffixDialog by remember { mutableStateOf(false) }

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

    // Add Surface for proper dark mode background
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
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

                    // More button - functional for prefix/suffix parameters
                    OutlinedButton(
                        onClick = {
                            showPrefixSuffixDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        enabled = viewModel.currentModelLoaded
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Model parameters"
                        )
                    }
                }
            }

            // Prefix/Suffix dialog
            PrefixSuffixDialog(
                showDialog = showPrefixSuffixDialog,
                viewModel = viewModel,
                onDismiss = { showPrefixSuffixDialog = false },
                onAppendText = { text ->
                    // Append the selected text to the current prompt
                    val newText = textFieldValue.text + text
                    val newSelection = TextRange(newText.length)
                    textFieldValue = textFieldValue.copy(
                        text = newText,
                        selection = newSelection
                    )
                }
            )
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

    // Add Surface for proper dark mode background
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Full-screen text display with proper system bar padding
        SelectionContainer(
            modifier = Modifier
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
}