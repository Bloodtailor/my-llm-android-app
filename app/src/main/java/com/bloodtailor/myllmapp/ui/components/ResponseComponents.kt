package com.bloodtailor.myllmapp.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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