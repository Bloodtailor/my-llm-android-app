package com.bloodtailor.myllmapp.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Top app bar with connection status and action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    viewModel: LlmViewModel,
    onSettingsClick: () -> Unit,
    onModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionStatus = remember {
        derivedStateOf {
            if (viewModel.availableModels.isNotEmpty()) "Connected" else "Not Connected"
        }
    }

    TopAppBar(
        title = {
            Text("LLM App")
        },
        actions = {
            // Connection status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (connectionStatus.value == "Connected")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                )
                Text(
                    text = connectionStatus.value,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Settings button
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Server Settings"
                )
            }

            // Model settings button
            IconButton(onClick = onModelClick) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Model Settings"
                )
            }
        },
        modifier = modifier
    )
}