package com.bloodtailor.myllmapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.data.database.SavedPrompt
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Prompts screen with Samsung Notes-style interface
 */
@Composable
fun PromptsScreen(
    viewModel: LlmViewModel,
    modifier: Modifier = Modifier
) {
    // Collect saved prompts from the ViewModel
    val savedPrompts by viewModel.getAllSavedPrompts().collectAsState(initial = emptyList())

    // Selection state
    var selectedPrompts by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedPrompts.isNotEmpty()

    // Navigation state for full-screen editor
    var showEditor by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<SavedPrompt?>(null) }

    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Prompts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Selection counter
                if (isSelectionMode) {
                    Text(
                        text = "${selectedPrompts.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Content area
            if (savedPrompts.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No saved prompts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the + button to create your first saved prompt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Prompts grid
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(savedPrompts, key = { it.id }) { prompt ->
                        PromptCard(
                            prompt = prompt,
                            isSelected = selectedPrompts.contains(prompt.id),
                            isSelectionMode = isSelectionMode,
                            onCardClick = {
                                if (isSelectionMode) {
                                    // Toggle selection
                                    selectedPrompts = if (selectedPrompts.contains(prompt.id)) {
                                        selectedPrompts - prompt.id
                                    } else {
                                        selectedPrompts + prompt.id
                                    }
                                } else {
                                    // Open in editor
                                    editingPrompt = prompt
                                    showEditor = true
                                }
                            },
                            onCardLongClick = {
                                // Enter selection mode
                                selectedPrompts = setOf(prompt.id)
                            },
                            onCopyClick = {
                                // Copy prompt content to clipboard
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Saved Prompt", prompt.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button (hide when in selection mode)
        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = {
                    // Create new prompt
                    editingPrompt = null
                    showEditor = true
                },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add new prompt",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Bottom Action Bar (show when in selection mode)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel selection
                    TextButton(
                        onClick = {
                            selectedPrompts = emptySet()
                        }
                    ) {
                        Text("Cancel")
                    }

                    // Delete button
                    Button(
                        onClick = {
                            viewModel.deleteSavedPrompts(selectedPrompts.toList()) { success, error ->
                                if (success) {
                                    selectedPrompts = emptySet()
                                    Toast.makeText(context, "Prompts deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete (${selectedPrompts.size})")
                    }
                }
            }
        }
    }

    // Full-screen editor
    if (showEditor) {
        SavedPromptFullScreenEditor(
            prompt = editingPrompt,
            onSave = { name, content ->
                if (editingPrompt != null) {
                    // Update existing prompt
                    val updatedPrompt = editingPrompt!!.copy(name = name, content = content)
                    viewModel.updateSavedPrompt(updatedPrompt) { success, error ->
                        if (success) {
                            showEditor = false
                            editingPrompt = null
                            Toast.makeText(context, "Prompt updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Create new prompt
                    viewModel.createSavedPrompt(name, content) { success, error ->
                        if (success) {
                            showEditor = false
                            editingPrompt = null
                            Toast.makeText(context, "Prompt saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCancel = {
                showEditor = false
                editingPrompt = null
            }
        )
    }
}

/**
 * Individual prompt card component
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromptCard(
    prompt: SavedPrompt,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with name and copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = prompt.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Copy button (hide in selection mode)
                if (!isSelectionMode) {
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy prompt",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Content preview
            Text(
                text = prompt.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .align(Alignment.End),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Selected",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}