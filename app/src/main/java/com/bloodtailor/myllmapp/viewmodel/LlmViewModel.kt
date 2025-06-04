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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.bloodtailor.myllmapp.network.ContextUsage
import com.bloodtailor.myllmapp.network.ModelParameters


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
    }

    var currentModelParameters by mutableStateOf<ModelParameters?>(null)
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

    // Default values
    val DEFAULT_CONTEXT_LENGTH = 2048

    // Context usage state - now separate from formatting
    var contextUsage by mutableStateOf<ContextUsage?>(null)
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
     * Load a model
     */
    fun loadModel(modelName: String, contextLength: Int? = null, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            isLoading = true
            statusMessage = "Loading model..."
            savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage

            repository.loadModel(modelName, contextLength).fold(
                onSuccess = { result ->
                    currentModelLoaded = true
                    currentModel = modelName
                    currentContextLength = result.contextLength
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
     * Send a prompt to the model and get a streaming response
     * Now sends raw prompts without any formatting
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

        // Send the raw prompt exactly as typed by the user
        repository.sendStreamingPrompt(
            prompt = prompt,  // Raw prompt, no formatting
            systemPrompt = systemPrompt,
            modelName = currentModel!!
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

    /**
     * Clear the current response
     */
    fun clearResponse() {
        llmResponse = "Response will appear here..."
        savedStateHandle[LLM_RESPONSE_KEY] = llmResponse
    }

    /**
     * Clear the status message
     */
    fun clearStatusMessage() {
        statusMessage = ""
        savedStateHandle[STATUS_MESSAGE_KEY] = statusMessage
    }
}