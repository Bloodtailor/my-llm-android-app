package com.bloodtailor.myllmapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

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