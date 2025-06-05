package com.bloodtailor.myllmapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Dialog for selecting model prefix/suffix parameters to append to prompt
 */
@Composable
fun PrefixSuffixDialog(
    showDialog: Boolean,
    viewModel: LlmViewModel,
    onDismiss: () -> Unit,
    onAppendText: (String) -> Unit
) {
    // Fetch model parameters when dialog is shown
    LaunchedEffect(showDialog, viewModel.currentModel) {
        if (showDialog && viewModel.currentModel != null) {
            viewModel.fetchModelParameters()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Model Parameters")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp)
                ) {
                    if (viewModel.currentModelParameters == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading parameters...")
                        }
                    } else {
                        val availableOptions = viewModel.getAvailablePrefixSuffixOptions()

                        if (availableOptions.isEmpty()) {
                            Text(
                                "No prefix/suffix parameters available for model: ${viewModel.currentModel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Click any parameter to append it to your prompt:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(availableOptions) { (name, value) ->
                                    PrefixSuffixItem(
                                        name = name,
                                        value = value,
                                        onClick = {
                                            onAppendText(value)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Individual prefix/suffix item component
 */
@Composable
private fun PrefixSuffixItem(
    name: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = onClick,
                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        "Append",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                )
            }
        }
    }
}