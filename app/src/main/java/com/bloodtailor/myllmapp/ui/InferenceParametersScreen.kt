package com.bloodtailor.myllmapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import com.bloodtailor.myllmapp.network.InferenceParameter
import com.bloodtailor.myllmapp.network.InferenceParameterValues

/**
 * Full-screen inference parameters management screen
 */
@Composable
fun InferenceParametersScreen(
    viewModel: LlmViewModel,
    onBackSwipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle back gesture
    BackHandler(enabled = true) {
        onBackSwipe()
    }

    // Fetch inference parameters when screen is displayed
    LaunchedEffect(viewModel.currentModel) {
        if (viewModel.currentModel != null && viewModel.availableInferenceParameters == null) {
            viewModel.fetchInferenceParameters()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inference Parameters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (viewModel.currentModel != null) {
                Text(
                    text = "Model: ${viewModel.currentModel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (viewModel.currentModel == null) {
            // No model loaded
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Model Loaded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please load a model first to configure inference parameters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else if (viewModel.availableInferenceParameters == null) {
            // Loading parameters
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Loading inference parameters...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Parameters loaded - show UI
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Quick Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        val changedParams = getChangedParameters(viewModel)
                        if (changedParams.isEmpty()) {
                            Text(
                                text = "All parameters are at their default values",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Text(
                                text = "${changedParams.size} parameter(s) modified from defaults",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Modified: ${changedParams.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Parameter sliders
                InferenceParameterSliders(
                    viewModel = viewModel,
                    parameters = viewModel.availableInferenceParameters!!.parameters,
                    currentValues = viewModel.currentInferenceParameterValues
                )
            }

            // Bottom action bar
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetInferenceParametersToDefaults()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset to defaults",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }

                    // Apply button (sends a test prompt)
                    Button(
                        onClick = {
                            // Send a test prompt with current parameters
                            viewModel.sendPrompt(
                                prompt = "Hello! Please respond briefly to test these inference parameters.",
                                systemPrompt = ""
                            )
                            onBackSwipe()
                        },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Test parameters",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test")
                    }
                }
            }
        }
    }
}

/**
 * Inference parameter sliders component
 */
@Composable
private fun InferenceParameterSliders(
    viewModel: LlmViewModel,
    parameters: Map<String, InferenceParameter>,
    currentValues: InferenceParameterValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        parameters.forEach { (paramName, parameter) ->
            InferenceParameterSlider(
                name = paramName,
                parameter = parameter,
                currentValue = currentValues.getValueOrDefault(paramName, parameter),
                onValueChange = { newValue ->
                    viewModel.updateInferenceParameter(paramName, newValue)
                }
            )
        }
    }
}

/**
 * Individual inference parameter slider with text input option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InferenceParameterSlider(
    name: String,
    parameter: InferenceParameter,
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChanged = currentValue != parameter.current
    var showTextInput by remember { mutableStateOf(false) }
    var textValue by remember(currentValue) {
        mutableStateOf(formatParameterValue(name, currentValue))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isChanged) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Parameter header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isChanged) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = parameter.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isChanged) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Current value display with click to edit
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clickable { showTextInput = !showTextInput }
                        .padding(8.dp)
                ) {
                    Text(
                        text = formatParameterValue(name, currentValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isChanged) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    if (isChanged) {
                        Text(
                            text = "Modified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        Text(
                            text = "Tap to edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isChanged) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Text input or slider based on mode
            if (showTextInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newText ->
                            textValue = newText
                            // Try to parse and validate
                            newText.toFloatOrNull()?.let { parsedValue ->
                                val clampedValue = parsedValue.coerceIn(parameter.min, parameter.max)
                                if (clampedValue == parsedValue) {
                                    onValueChange(clampedValue)
                                }
                            }
                        },
                        label = { Text("Value") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = { showTextInput = false }
                    ) {
                        Text("Done")
                    }
                }

                // Update text value when slider changes
                LaunchedEffect(currentValue) {
                    textValue = formatParameterValue(name, currentValue)
                }
            } else {
                // Slider
                Slider(
                    value = currentValue,
                    onValueChange = onValueChange,
                    valueRange = parameter.min..parameter.max,
                    steps = getSliderSteps(name, parameter),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Value range info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${formatParameterValue(name, parameter.min)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isChanged) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Default: ${formatParameterValue(name, parameter.default)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isChanged) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Max: ${formatParameterValue(name, parameter.max)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isChanged) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Quick reset button for this parameter
            if (isChanged) {
                OutlinedButton(
                    onClick = {
                        onValueChange(parameter.current)
                        showTextInput = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Reset to default",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Format parameter values based on their type and expected precision
 */
private fun formatParameterValue(paramName: String, value: Float): String {
    return when (paramName.lowercase()) {
        "max_tokens", "top_k" -> {
            // Integer parameters - no decimal places
            value.toInt().toString()
        }
        "temperature", "top_p", "repeat_penalty", "min_p" -> {
            // Float parameters - 2 decimal places
            String.format("%.2f", value)
        }
        else -> {
            // Default - 2 decimal places
            String.format("%.2f", value)
        }
    }
}

/**
 * Get appropriate number of slider steps based on parameter type
 */
private fun getSliderSteps(paramName: String, parameter: InferenceParameter): Int {
    return when (paramName.lowercase()) {
        "max_tokens" -> {
            // For integer parameters, each step is 1 unit
            ((parameter.max - parameter.min).toInt() - 1).coerceAtLeast(0)
        }
        "top_k" -> {
            // For top_k, reasonable steps
            ((parameter.max - parameter.min).toInt() - 1).coerceAtLeast(0)
        }
        else -> {
            // For float parameters, reasonable number of steps
            99 // Gives good granularity for most float ranges
        }
    }
}

/**
 * Helper function to get list of changed parameters
 */
private fun getChangedParameters(viewModel: LlmViewModel): List<String> {
    val availableParams = viewModel.availableInferenceParameters?.parameters ?: return emptyList()
    val currentValues = viewModel.currentInferenceParameterValues

    return availableParams.mapNotNull { (name, parameter) ->
        val currentValue = currentValues.getValueOrDefault(name, parameter)
        if (currentValue != parameter.current) name else null
    }
}