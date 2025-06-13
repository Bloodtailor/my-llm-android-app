package com.bloodtailor.myllmapp.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bloodtailor.myllmapp.data.LlmRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.bloodtailor.myllmapp.network.ContextUsage
import com.bloodtailor.myllmapp.network.ModelParameters
import com.bloodtailor.myllmapp.network.LoadingParameters
import com.bloodtailor.myllmapp.network.LoadingParameterValues
import com.bloodtailor.myllmapp.network.LoadingParameter
import com.bloodtailor.myllmapp.network.InferenceParameters
import com.bloodtailor.myllmapp.network.InferenceParameterValues



/**
 * ViewModel for managing LLM-related state and operations with screen rotation persistence
 */
class LlmViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // Keys for saved state
    private companion object {
        const val SERVER_URL_KEY = "server_url"
        const val CURRENT_MODEL_KEY = "current_model"
        const val CURRENT_CONTEXT_LENGTH_KEY = "current_context_length"
        const val MODEL_LOADED_KEY = "model_loaded"
        const val STATUS_MESSAGE_KEY = "status_message"
        const val LLM_RESPONSE_KEY = "llm_response"
        const val LOADING_PARAM_VALUES_KEY = "loading_param_values"
        const val INFERENCE_PARAM_VALUES_KEY = "inference_param_values"
    }

    var currentModelParameters by mutableStateOf<ModelParameters?>(null)
        private set

    // Loading parameters state
    var availableLoadingParameters by mutableStateOf<LoadingParameters?>(null)
        private set

    var currentLoadingParameterValues by mutableStateOf<LoadingParameterValues>(
        // Try to restore from saved state
        savedStateHandle.get<Map<String, String>>(LOADING_PARAM_VALUES_KEY)?.let { savedValues ->
            LoadingParameterValues().apply {
                savedValues.forEach { (key, stringValue) ->
                    // Convert string back to appropriate type
                    val value = when {
                        stringValue == "true" -> true
                        stringValue == "false" -> false
                        stringValue.toIntOrNull() != null -> stringValue.toInt()
                        stringValue.toFloatOrNull() != null -> stringValue.toFloat()
                        else -> stringValue
                    }
                    values[key] = value
                }
            }
        } ?: LoadingParameterValues()
    )
        private set

    // Store the actual parameters used to load the current model
    var currentModelLoadingParameters by mutableStateOf<Map<String, Any>?>(null)
        private set

    // Repository for data operations
    private val repository = LlmRepository(application, "")

    // Server state with persistence
    var serverUrl by mutableStateOf(
        savedStateHandle.get<String>(SERVER_URL_KEY) ?: repository.getServerUrl()
    )
        private set

    // Model state with persistence
    var availableModels = mutableStateListOf<String>()
        private set

    var currentModelLoaded by mutableStateOf(
        savedStateHandle.get<Boolean>(MODEL_LOADED_KEY) ?: false
    )
        private set

    var currentModel by mutableStateOf(
        savedStateHandle.get<String?>(CURRENT_MODEL_KEY)
    )
        private set

    var currentContextLength by mutableStateOf(
        savedStateHandle.get<Int?>(CURRENT_CONTEXT_LENGTH_KEY)
    )
        private set

    // UI state with persistence
    var isLoading by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf(
        savedStateHandle.get<String>(STATUS_MESSAGE_KEY)
            ?: "Please configure server address in settings"
    )
        private set

    var llmResponse by mutableStateOf(
        savedStateHandle.get<String>(LLM_RESPONSE_KEY)
            ?: "Response will appear here..."
    )
        private set


    // Context usage state - now separate from formatting
    var contextUsage by mutableStateOf<ContextUsage?>(null)
        private set

    var availableInferenceParameters by mutableStateOf<InferenceParameters?>(null)
        private set

    var currentInferenceParameterValues by mutableStateOf<InferenceParameterValues>(
        // Try to restore from saved state
        savedStateHandle.get<Map<String, String>>(INFERENCE_PARAM_VALUES_KEY)?.let { savedValues ->
            InferenceParameterValues().apply {
                savedValues.forEach { (key, stringValue) ->
                    stringValue.toFloatOrNull()?.let { floatValue ->
                        values[key] = floatValue
                    }
                }
            }
        } ?: InferenceParameterValues()
    )
        private set

    init {
        // Don't call updateServerUrl here - let MainActivity handle initialization
        // This prevents auto-connecting during state restoration

        // Save initial state
        saveState()
    }

    /**
     * Save current state to handle
     */
    private fun saveState() {
        savedStateHandle[SERVER_URL_KEY] = serverUrl
        savedStateHandle[CURRENT_MODEL_KEY] = currentModel
        savedStateHandle[CURRENT_CONTEXT_LENGTH_KEY] = currentContextLength
        savedStateHandle[MODEL_LOADED_KEY] = currentModelLoaded
        savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
        savedStateHandle[LLM_RESPONSE_KEY] = llmResponse

        // Convert inference parameters to a simple map that SavedStateHandle can handle
        val inferenceSimpleMap = mutableMapOf<String, String>()
        currentInferenceParameterValues.toMap().forEach { (key, value) ->
            inferenceSimpleMap[key] = value.toString()
        }
        savedStateHandle[INFERENCE_PARAM_VALUES_KEY] = inferenceSimpleMap


        // Convert to a simple map that SavedStateHandle can handle
        val simpleMap = mutableMapOf<String, String>()
        currentLoadingParameterValues.toMap().forEach { (key, value) ->
            simpleMap[key] = value.toString()
        }
        savedStateHandle[LOADING_PARAM_VALUES_KEY] = simpleMap
    }

    /**
     * Fetch loading parameters from the server
     */
    fun fetchLoadingParameters() {
        viewModelScope.launch {
            repository.getLoadingParameters().fold(
                onSuccess = { parameters ->
                    availableLoadingParameters = parameters

                    // Only initialize defaults if we don't already have saved values
                    if (currentLoadingParameterValues.values.isEmpty()) {
                        initializeLoadingParameterDefaults()
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("LlmViewModel", "Error fetching loading parameters", error)
                    statusMessage = "Error loading parameters: ${error.message}"
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                }
            )
        }
    }

    /**
     * Initialize loading parameter values with defaults for the selected model
     */
    private fun initializeLoadingParameterDefaults(modelName: String? = null) {
        val parameters = availableLoadingParameters ?: return
        val targetModel = modelName ?: currentModel

        val newValues = LoadingParameterValues()

        // Set global defaults first
        for ((paramName, parameter) in parameters.globalDefaults) {
            newValues.setValue(paramName, parameter.default)
        }

        // Override with model-specific defaults if available
        if (targetModel != null && parameters.modelSpecific.containsKey(targetModel)) {
            val modelParams = parameters.modelSpecific[targetModel]!!
            for ((paramName, parameter) in modelParams) {
                newValues.setValue(paramName, parameter.default)
            }
        }

        currentLoadingParameterValues = newValues
        saveState()
    }

    /**
     * Update a loading parameter value
     */
    fun updateLoadingParameter(paramName: String, value: Any) {
        // Create a completely new instance to force recomposition
        val newValues = LoadingParameterValues()

        // Copy all existing values
        newValues.values.putAll(currentLoadingParameterValues.values)

        // Update the specific parameter
        newValues.setValue(paramName, value)

        // Set the new instance (this should trigger recomposition)
        currentLoadingParameterValues = newValues

        // Save state to persist across rotations
        saveState()
    }

    /**
     * Get all available loading parameters for the current model
     */
    fun getAvailableLoadingParametersForModel(modelName: String? = null): Map<String, LoadingParameter> {
        val parameters = availableLoadingParameters ?: return emptyMap()
        val targetModel = modelName ?: currentModel

        val allParams = mutableMapOf<String, LoadingParameter>()

        // Add global parameters
        allParams.putAll(parameters.globalDefaults)

        // Add model-specific parameters (these override global ones)
        if (targetModel != null && parameters.modelSpecific.containsKey(targetModel)) {
            allParams.putAll(parameters.modelSpecific[targetModel]!!)
        }

        return allParams
    }

    /**
     * Reset loading parameters to defaults for the selected model
     */
    fun resetLoadingParametersToDefaults(modelName: String? = null) {
        // Clear current values to force reinitialization
        currentLoadingParameterValues = LoadingParameterValues()
        initializeLoadingParameterDefaults(modelName)
    }

    fun updateServerUrl(url: String, autoConnect: Boolean = true) {
        repository.updateServerUrl(url)
        serverUrl = url

        // Save to savedStateHandle for rotation persistence
        savedStateHandle[SERVER_URL_KEY] = url

        // Add logging
        android.util.Log.d("LlmViewModel", "Server URL updated to: $url")

        // Only refresh data if autoConnect is true
        if (autoConnect) {
            statusMessage = "Connecting to server..."
            savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
            fetchAvailableModels()
            fetchLoadingParameters() // Fetch loading parameters when connecting
            checkModelStatus()
        }
    }

    /**
     * Fetch available models from the server
     */
    fun fetchAvailableModels() {
        viewModelScope.launch {
            isLoading = true
            statusMessage = "Loading models..."
            savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage

            repository.getAvailableModels().fold(
                onSuccess = { models ->
                    availableModels.clear()
                    availableModels.addAll(models)

                    if (models.isEmpty()) {
                        statusMessage = "No models found. Please check server configuration."
                        android.util.Log.w("LlmViewModel", "No models returned from server")
                    } else {
                        statusMessage = "${models.size} models loaded"
                        android.util.Log.d("LlmViewModel", "Models loaded: ${models.joinToString()}")
                    }
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                },
                onFailure = { error ->
                    statusMessage = "Error loading models: ${error.message}"
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                    android.util.Log.e("LlmViewModel", "Error loading models", error)
                }
            )

            isLoading = false
        }
    }

    /**
     * Check the current model status
     */
    fun checkModelStatus() {
        viewModelScope.launch {
            repository.checkModelStatus().fold(
                onSuccess = { status ->
                    currentModelLoaded = status.loaded
                    currentModel = status.currentModel
                    currentContextLength = status.contextLength

                    // Save state
                    savedStateHandle[MODEL_LOADED_KEY] = currentModelLoaded
                    savedStateHandle[CURRENT_MODEL_KEY] = currentModel
                    savedStateHandle[CURRENT_CONTEXT_LENGTH_KEY] = currentContextLength

                    // Clear status message if connected
                    if (statusMessage == "Please configure server address in settings" ||
                        statusMessage == "Connecting to server...") {
                        statusMessage = ""
                        savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("LlmViewModel", "Error checking model status", error)
                    statusMessage = "Error checking model status: ${error.message}"
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                }
            )
        }
    }

    /**
     * Load a model with custom loading parameters
     */
    fun loadModelWithParameters(modelName: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            isLoading = true
            statusMessage = "Loading model..."
            savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage

            // Get the loading parameters to send
            val loadingParams = currentLoadingParameterValues.toMap().toMutableMap()
            loadingParams["model"] = modelName

            repository.loadModelWithParameters(modelName, loadingParams).fold(
                onSuccess = { result ->
                    currentModelLoaded = true
                    currentModel = modelName
                    currentContextLength = result.contextLength
                    currentModelLoadingParameters = loadingParams // Store the actual parameters used
                    statusMessage = result.message

                    // Save state
                    savedStateHandle[MODEL_LOADED_KEY] = currentModelLoaded
                    savedStateHandle[CURRENT_MODEL_KEY] = currentModel
                    savedStateHandle[CURRENT_CONTEXT_LENGTH_KEY] = currentContextLength
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage

                    onComplete?.invoke(true)
                },
                onFailure = { error ->
                    statusMessage = "Error: ${error.message}"
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                    onComplete?.invoke(false)
                }
            )

            isLoading = false
        }
    }

    /**
     * Unload the current model
     */
    fun unloadModel(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            isLoading = true
            statusMessage = "Unloading model..."
            savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage

            repository.unloadModel().fold(
                onSuccess = { message ->
                    currentModelLoaded = false
                    currentModel = null
                    currentContextLength = null
                    currentModelLoadingParameters = null // Clear loading parameters
                    statusMessage = message
                    contextUsage = null  // Clear context usage when unloading

                    // Save state
                    savedStateHandle[MODEL_LOADED_KEY] = currentModelLoaded
                    savedStateHandle[CURRENT_MODEL_KEY] = currentModel
                    savedStateHandle[CURRENT_CONTEXT_LENGTH_KEY] = currentContextLength
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage

                    onComplete?.invoke(true)
                },
                onFailure = { error ->
                    statusMessage = "Error: ${error.message}"
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                    onComplete?.invoke(false)
                }
            )

            isLoading = false
        }
    }

    /**
     * Count tokens in text and update context usage (replaces formatPrompt)
     */
    fun updateContextUsage(text: String, onComplete: ((String) -> Unit)? = null) {
        if (currentModel == null) {
            onComplete?.invoke("No model selected")
            return
        }

        viewModelScope.launch {
            repository.countTokens(text, currentModel!!).fold(
                onSuccess = { result ->
                    contextUsage = result.contextUsage
                    // For the "show formatted prompt" feature, we just return the original text
                    // since we're not doing any formatting now
                    onComplete?.invoke(result.text)
                },
                onFailure = { error ->
                    onComplete?.invoke("Error: ${error.message}")
                    contextUsage = null
                }
            )
        }
    }


    /**
     * Fetch inference parameters from the server
     */
    fun fetchInferenceParameters(modelName: String? = null) {
        viewModelScope.launch {
            repository.getInferenceParameters(modelName).fold(
                onSuccess = { parameters ->
                    availableInferenceParameters = parameters

                    // Only initialize defaults if we don't already have saved values
                    if (currentInferenceParameterValues.values.isEmpty()) {
                        initializeInferenceParameterDefaults()
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("LlmViewModel", "Error fetching inference parameters", error)
                    statusMessage = "Error loading inference parameters: ${error.message}"
                    savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
                }
            )
        }
    }

    /**
     * Initialize inference parameter values with defaults
     */
    private fun initializeInferenceParameterDefaults() {
        val parameters = availableInferenceParameters ?: return

        val newValues = InferenceParameterValues()

        // Set current values as defaults
        for ((paramName, parameter) in parameters.parameters) {
            newValues.values[paramName] = parameter.current
        }

        currentInferenceParameterValues = newValues
        saveState()
    }

    /**
     * Update an inference parameter value
     */
    fun updateInferenceParameter(paramName: String, value: Float) {
        // Create a completely new instance to force recomposition
        val newValues = InferenceParameterValues()

        // Copy all existing values
        newValues.values.putAll(currentInferenceParameterValues.values)

        // Update the specific parameter
        newValues.values[paramName] = value

        // Set the new instance (this should trigger recomposition)
        currentInferenceParameterValues = newValues

        // Save state to persist across rotations
        saveState()
    }

    /**
     * Reset inference parameters to current model defaults
     */
    fun resetInferenceParametersToDefaults() {
        // Clear current values to force reinitialization
        currentInferenceParameterValues = InferenceParameterValues()
        initializeInferenceParameterDefaults()
    }

    /**
     * Send a prompt to the model using current inference parameters
     */
    fun sendPrompt(
        prompt: String,
        systemPrompt: String = ""
    ) {
        if (!currentModelLoaded || currentModel == null) {
            statusMessage = "Please load a model first"
            savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
            return
        }

        isLoading = true
        llmResponse = "Generating response..."
        savedStateHandle[LLM_RESPONSE_KEY] = llmResponse

        // Always use current inference parameter values (defaults or user-modified)
        val inferenceParams = currentInferenceParameterValues.toMap()

        repository.sendPromptWithInferenceParameters(
            prompt = prompt,
            systemPrompt = systemPrompt,
            modelName = currentModel!!,
            inferenceParams = inferenceParams
        ) { status, content ->
            viewModelScope.launch(Dispatchers.Main) {
                when (status) {
                    "generating", "complete" -> {
                        llmResponse = content
                        savedStateHandle[LLM_RESPONSE_KEY] = llmResponse
                    }
                    "error" -> {
                        llmResponse = "Error: $content"
                        savedStateHandle[LLM_RESPONSE_KEY] = llmResponse
                    }
                }

                if (status == "complete" || status == "error") {
                    isLoading = false
                }
            }
        }
    }

    /**
     * Fetch model parameters for the current or specified model
     */
    fun fetchModelParameters(modelName: String? = null) {
        viewModelScope.launch {
            val targetModel = modelName ?: currentModel
            if (targetModel == null) {
                android.util.Log.w("LlmViewModel", "No model specified for parameter fetch")
                return@launch
            }

            repository.getModelParameters(targetModel).fold(
                onSuccess = { parameters ->
                    currentModelParameters = parameters
                    android.util.Log.d("LlmViewModel", "Model parameters loaded for $targetModel")
                },
                onFailure = { error ->
                    android.util.Log.e("LlmViewModel", "Error loading model parameters", error)
                    currentModelParameters = null
                }
            )
        }
    }

    /**
     * Get available prefix/suffix options that are not empty
     */
    fun getAvailablePrefixSuffixOptions(): List<Pair<String, String>> {
        val parameters = currentModelParameters ?: return emptyList()

        val options = mutableListOf<Pair<String, String>>()

        if (parameters.prePromptPrefix.isNotEmpty()) {
            options.add("System Prefix" to parameters.prePromptPrefix)
        }
        if (parameters.prePromptSuffix.isNotEmpty()) {
            options.add("System Suffix" to parameters.prePromptSuffix)
        }
        if (parameters.inputPrefix.isNotEmpty()) {
            options.add("User Prefix" to parameters.inputPrefix)
        }
        if (parameters.inputSuffix.isNotEmpty()) {
            options.add("User Suffix" to parameters.inputSuffix)
        }
        if (parameters.assistantPrefix.isNotEmpty()) {
            options.add("Assistant Prefix" to parameters.assistantPrefix)
        }
        if (parameters.assistantSuffix.isNotEmpty()) {
            options.add("Assistant Suffix" to parameters.assistantSuffix)
        }

        return options
    }

}