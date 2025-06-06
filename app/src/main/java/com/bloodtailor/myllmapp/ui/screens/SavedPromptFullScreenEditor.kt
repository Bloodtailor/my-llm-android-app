package com.bloodtailor.myllmapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.data.database.SavedPrompt

/**
 * Full-screen editor for creating/editing saved prompts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPromptFullScreenEditor(
    prompt: SavedPrompt?, // null for new prompt
    onSave: (name: String, content: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle back gesture
    BackHandler(enabled = true) {
        onCancel()
    }

    // State for name and content
    var nameText by rememberSaveable {
        mutableStateOf(prompt?.name ?: "")
    }

    var contentFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val initialContent = prompt?.content ?: ""
        mutableStateOf(TextFieldValue(initialContent, TextRange(initialContent.length)))
    }

    // Validation state
    val canSave = nameText.trim().isNotEmpty() && contentFieldValue.text.trim().isNotEmpty()

    // Focus requesters
    val nameFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Focus name field for new prompts
    LaunchedEffect(Unit) {
        if (prompt == null) {
            nameFocusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
        ) {
            // Top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onCancel
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }

                    // Title
                    Text(
                        text = if (prompt == null) "New Prompt" else "Edit Prompt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Save button
                    Button(
                        onClick = {
                            onSave(nameText.trim(), contentFieldValue.text.trim())
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }

            // Content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Prompt Name") },
                    placeholder = { Text("Enter a name for this prompt") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    isError = nameText.trim().isEmpty()
                )

                // Content field
                OutlinedTextField(
                    value = contentFieldValue,
                    onValueChange = { contentFieldValue = it },
                    label = { Text("Prompt Content") },
                    placeholder = { Text("Enter your prompt text here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(contentFocusRequester),
                    isError = contentFieldValue.text.trim().isEmpty()
                )

                // Helper text
                if (!canSave) {
                    Text(
                        text = "Both name and content are required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}