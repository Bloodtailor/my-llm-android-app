package com.bloodtailor.myllmapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import com.bloodtailor.myllmapp.network.LoadingParameter

/**
 * Loading parameter input components - All using number inputs now
 */

@Composable
fun LoadingParameterToggle(
    name: String,
    parameter: LoadingParameter,
    currentValue: Any,
    onValueChange: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple boolean conversion
    val checked = when (currentValue) {
        is Boolean -> currentValue
        true, "true", 1 -> true
        false, "false", 0 -> false
        else -> parameter.default as? Boolean ?: false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = parameter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    onValueChange(newValue)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingParameterNumberInput(
    name: String,
    parameter: LoadingParameter,
    currentValue: Any,
    onValueChange: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val value = (currentValue as? Number)?.toString() ?: parameter.default.toString()
    var textValue by remember(name, currentValue) { mutableStateOf(value) }
    var isError by remember(name) { mutableStateOf(false) }

    // Update text value when currentValue changes from outside
    LaunchedEffect(currentValue) {
        val newTextValue = (currentValue as? Number)?.toString() ?: parameter.default.toString()
        if (textValue != newTextValue) {
            textValue = newTextValue
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                isError = false

                // Try to parse and validate the input
                try {
                    val parsedValue = if (parameter.type == "integer") {
                        newText.toInt()
                    } else {
                        newText.toFloat()
                    }

                    // Check bounds
                    val min = parameter.min as? Number
                    val max = parameter.max as? Number

                    val isValid = (min == null || parsedValue.toDouble() >= min.toDouble()) &&
                            (max == null || parsedValue.toDouble() <= max.toDouble())

                    if (isValid) {
                        onValueChange(parsedValue)
                    } else {
                        isError = true
                    }
                } catch (e: NumberFormatException) {
                    // Invalid input
                    if (newText.isNotEmpty()) {
                        isError = true
                    }
                }
            },
            label = { Text("Value") },
            placeholder = { Text("Default: ${parameter.default}") },
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val rangeText = buildString {
            if (parameter.min != null || parameter.max != null) {
                append("Range: ")
                append(parameter.min ?: "−∞")
                append(" to ")
                append(parameter.max ?: "∞")
            }
        }

        Column {
            if (rangeText.isNotEmpty()) {
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = parameter.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Loading parameters section - Now using only number inputs and toggles
 */
@Composable
fun LoadingParametersSection(
    viewModel: LlmViewModel,
    selectedModel: String,
    modifier: Modifier = Modifier
) {
    val availableParams = viewModel.getAvailableLoadingParametersForModel(selectedModel)
    val currentValues = viewModel.currentLoadingParameterValues

    if (availableParams.isEmpty()) {
        Text(
            text = "Loading parameters...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Loading Parameters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Group parameters by type for better organization
        val globalParams = availableParams.filter { (key, _) ->
            viewModel.availableLoadingParameters?.globalDefaults?.containsKey(key) == true
        }

        val modelSpecificParams = availableParams.filter { (key, _) ->
            viewModel.availableLoadingParameters?.modelSpecific?.get(selectedModel)?.containsKey(key) == true
        }

        // Global parameters
        if (globalParams.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Global Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    globalParams.forEach { (paramName, parameter) ->
                        val currentValue = currentValues.getValueOrDefault(paramName, parameter)

                        when (parameter.type) {
                            "boolean" -> {
                                LoadingParameterToggle(
                                    name = paramName.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    parameter = parameter,
                                    currentValue = currentValue,
                                    onValueChange = { newValue ->
                                        viewModel.updateLoadingParameter(paramName, newValue)
                                    }
                                )
                            }
                            "integer", "float", "number" -> {
                                LoadingParameterNumberInput(
                                    name = paramName.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    parameter = parameter,
                                    currentValue = currentValue,
                                    onValueChange = { newValue ->
                                        viewModel.updateLoadingParameter(paramName, newValue)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Model-specific parameters
        if (modelSpecificParams.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Model-Specific Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    modelSpecificParams.forEach { (paramName, parameter) ->
                        val currentValue = currentValues.getValueOrDefault(paramName, parameter)

                        when (parameter.type) {
                            "boolean" -> {
                                LoadingParameterToggle(
                                    name = paramName.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    parameter = parameter,
                                    currentValue = currentValue,
                                    onValueChange = { newValue ->
                                        viewModel.updateLoadingParameter(paramName, newValue)
                                    }
                                )
                            }
                            "integer", "float", "number" -> {
                                LoadingParameterNumberInput(
                                    name = paramName.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    parameter = parameter,
                                    currentValue = currentValue,
                                    onValueChange = { newValue ->
                                        viewModel.updateLoadingParameter(paramName, newValue)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reset button
        OutlinedButton(
            onClick = {
                viewModel.resetLoadingParametersToDefaults(selectedModel)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Reset to Defaults")
        }
    }
}

/**
 * Enhanced model settings dialog component with loading parameters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsDialog(
    showDialog: Boolean,
    viewModel: LlmViewModel,
    onDismiss: () -> Unit
) {
    // State variables with rotation persistence
    var selectedModel by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Update selectedModel when viewModel values change
    LaunchedEffect(viewModel.availableModels, viewModel.currentModel, showDialog) {
        if (showDialog) {
            // Initialize with current model if available, otherwise first available model
            selectedModel = viewModel.currentModel ?:
                    if (viewModel.availableModels.isNotEmpty()) viewModel.availableModels[0] else ""

            // Fetch loading parameters when dialog opens (only if not already available)
            if (viewModel.availableLoadingParameters == null) {
                viewModel.fetchLoadingParameters()
            }

            // ONLY reset loading parameters if we have no saved values AND no current values
            if (selectedModel.isNotEmpty() &&
                viewModel.currentLoadingParameterValues.values.isEmpty() &&
                viewModel.availableLoadingParameters != null) {
                viewModel.resetLoadingParametersToDefaults(selectedModel)
            }
        }
    }

    // Only update loading parameters when model selection actually changes to a different model
    LaunchedEffect(selectedModel) {
        if (selectedModel.isNotEmpty() &&
            selectedModel != viewModel.currentModel &&
            viewModel.currentModel != null) { // Only reset if we're changing from one model to another
            viewModel.resetLoadingParametersToDefaults(selectedModel)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Model Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 600.dp)
                        .verticalScroll(rememberScrollState()),
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

                    // Loading parameters section
                    if (selectedModel.isNotEmpty()) {
                        LoadingParametersSection(
                            viewModel = viewModel,
                            selectedModel = selectedModel
                        )
                    }

                    // Model status display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Current Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            StatusRow(
                                label = "Status",
                                value = if (viewModel.currentModelLoaded) "Loaded" else "Not Loaded",
                                isPositive = viewModel.currentModelLoaded
                            )

                            if (viewModel.currentModelLoaded && viewModel.currentModel != null) {
                                StatusRow(
                                    label = "Current Model",
                                    value = viewModel.currentModel!!
                                )

                                // Show all loading parameters that were used
                                if (viewModel.currentModelLoadingParameters != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Loading Parameters Used:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    viewModel.currentModelLoadingParameters!!.forEach { (key, value) ->
                                        if (key != "model") { // Don't show the model name again
                                            StatusRow(
                                                label = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                value = value.toString()
                                            )
                                        }
                                    }
                                }
                            }

                            if (viewModel.isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Text(
                                        "Loading...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.loadModelWithParameters(selectedModel) { success ->
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = selectedModel.isNotEmpty() && !viewModel.isLoading &&
                                (!viewModel.currentModelLoaded || viewModel.currentModel != selectedModel),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Load Model")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.unloadModel { success ->
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = viewModel.currentModelLoaded && !viewModel.isLoading,
                        modifier = Modifier.weight(1f)
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
 * Helper composable for status display rows
 */
@Composable
private fun StatusRow(
    label: String,
    value: String,
    isPositive: Boolean? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when (isPositive) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
