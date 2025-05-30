package com.bloodtailor.myllmapp.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bloodtailor.myllmapp.data.LlmRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.bloodtailor.myllmapp.network.ContextUsage
import com.bloodtailor.myllmapp.network.PromptFormatResult


/**
 * ViewModel for managing LLM-related state and operations
 */
class LlmViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository for data operations
    private val repository = LlmRepository(application, "")
    
    // Server state
    var serverUrl by mutableStateOf(repository.getServerUrl())
        private set
    
    // Model state
    var availableModels = mutableStateListOf<String>()
        private set
    var currentModelLoaded by mutableStateOf(false)
        private set
    var currentModel by mutableStateOf<String?>(null)
        private set
    var currentContextLength by mutableStateOf<Int?>(null)
        private set
    
    // UI state
    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf("Please configure server address in settings")
        private set
    var formattedPrompt by mutableStateOf<String?>(null)
        private set
    var llmResponse by mutableStateOf("Response will appear here...")
        private set
    
    // Default values
    val DEFAULT_CONTEXT_LENGTH = 2048

    // Context usage state
    var contextUsage by mutableStateOf<ContextUsage?>(null)
        private set


    init {
        // Initialize with current server URL but don't fetch models automatically
        updateServerUrl(repository.getServerUrl(), autoConnect = false)
    }

    // Update this method to include an autoConnect parameter
    fun updateServerUrl(url: String, autoConnect: Boolean = true) {
        repository.updateServerUrl(url)
        serverUrl = url

        // Add logging
        android.util.Log.d("LlmViewModel", "Server URL updated to: $url")

        // Only refresh data if autoConnect is true
        if (autoConnect) {
            statusMessage = "Connecting to server..."
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
                },
                onFailure = { error ->
                    statusMessage = "Error loading models: ${error.message}"
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

                    // Clear status message if connected
                    if (statusMessage == "Please configure server address in settings" ||
                        statusMessage == "Connecting to server...") {
                        statusMessage = ""
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("LlmViewModel", "Error checking model status", error)
                    statusMessage = "Error checking model status: ${error.message}"
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
            
            repository.loadModel(modelName, contextLength).fold(
                onSuccess = { result ->
                    currentModelLoaded = true
                    currentModel = modelName
                    currentContextLength = result.contextLength
                    statusMessage = result.message
                    onComplete?.invoke(true)
                },
                onFailure = { error ->
                    statusMessage = "Error: ${error.message}"
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
            
            repository.unloadModel().fold(
                onSuccess = { message ->
                    currentModelLoaded = false
                    currentModel = null
                    currentContextLength = null
                    statusMessage = message
                    onComplete?.invoke(true)
                },
                onFailure = { error ->
                    statusMessage = "Error: ${error.message}"
                    onComplete?.invoke(false)
                }
            )
            
            isLoading = false
        }
    }

    /**
     * Format a prompt using the current model's template and update context usage
     */
    fun formatPrompt(prompt: String, onComplete: ((String) -> Unit)? = null) {
        if (currentModel == null) {
            onComplete?.invoke("No model selected")
            return
        }

        viewModelScope.launch {
            repository.formatPrompt(prompt, currentModel!!).fold(
                onSuccess = { result ->
                    formattedPrompt = result.formattedPrompt
                    contextUsage = result.contextUsage
                    onComplete?.invoke(result.formattedPrompt)
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
     */
    fun sendPrompt(
        prompt: String,
        systemPrompt: String = "",
        useFormattedPrompt: Boolean = false
    ) {
        if (!currentModelLoaded || currentModel == null) {
            statusMessage = "Please load a model first"
            return
        }
        
        isLoading = true
        llmResponse = "Generating response..."
        
        // Use formatted prompt if requested
        val formattedPromptToUse = if (useFormattedPrompt) formattedPrompt else null
        
        repository.sendStreamingPrompt(
            prompt = prompt,
            systemPrompt = systemPrompt,
            formattedPrompt = formattedPromptToUse,
            modelName = currentModel!!
        ) { status, content ->
            viewModelScope.launch(Dispatchers.Main) {
                when (status) {
                    "generating", "complete" -> llmResponse = content
                    "error" -> llmResponse = "Error: $content"
                }
                
                if (status == "complete" || status == "error") {
                    isLoading = false
                }
            }
        }
    }
    
    /**
     * Clear the current response
     */
    fun clearResponse() {
        llmResponse = "Response will appear here..."
    }
    
    /**
     * Clear the status message
     */
    fun clearStatusMessage() {
        statusMessage = ""
    }
}