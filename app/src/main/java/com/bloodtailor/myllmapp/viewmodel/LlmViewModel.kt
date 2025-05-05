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
    var statusMessage by mutableStateOf("")
        private set
    var formattedPrompt by mutableStateOf<String?>(null)
        private set
    var llmResponse by mutableStateOf("Response will appear here...")
        private set
    
    // Default values
    val DEFAULT_CONTEXT_LENGTH = 2048
    
    init {
        // Initialize with current server URL
        updateServerUrl(repository.getServerUrl())
    }
    
    /**
     * Update the server URL and refresh data
     */
    fun updateServerUrl(url: String) {
        repository.updateServerUrl(url)
        serverUrl = url
        
        // Refresh data with new URL
        fetchAvailableModels()
        checkModelStatus()
    }
    
    /**
     * Fetch available models from the server
     */
    fun fetchAvailableModels() {
        viewModelScope.launch {
            isLoading = true
            
            repository.getAvailableModels().fold(
                onSuccess = { models ->
                    availableModels.clear()
                    availableModels.addAll(models)
                },
                onFailure = { error ->
                    statusMessage = "Error loading models: ${error.message}"
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
                },
                onFailure = { error ->
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
     * Format a prompt using the current model's template
     */
    fun formatPrompt(prompt: String, onComplete: ((String) -> Unit)? = null) {
        if (currentModel == null) {
            onComplete?.invoke("No model selected")
            return
        }
        
        viewModelScope.launch {
            repository.formatPrompt(prompt, currentModel!!).fold(
                onSuccess = { formatted ->
                    formattedPrompt = formatted
                    onComplete?.invoke(formatted)
                },
                onFailure = { error ->
                    onComplete?.invoke("Error: ${error.message}")
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