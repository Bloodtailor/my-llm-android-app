package com.bloodtailor.myllmapp.data

import android.content.Context
import com.bloodtailor.myllmapp.network.ApiService
import com.bloodtailor.myllmapp.network.ModelLoadResult
import com.bloodtailor.myllmapp.network.ModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing LLM-related data and operations
 */
class LlmRepository(
    private val context: Context,
    private var serverUrl: String
) {
    // Shared preferences name and keys
    private val PREFS_NAME = "LLMAppPreferences"
    private val SERVER_URL_KEY = "server_url"
    private val DEFAULT_SERVER_URL = "http://192.168.1.100:5000"
    
    // Initialize API service
    private var apiService = ApiService(serverUrl)
    
    init {
        // Load saved server URL from preferences
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        serverUrl = sharedPref.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        apiService = ApiService(serverUrl)
    }
    
    /**
     * Update the server URL and save it to preferences
     */
    fun updateServerUrl(url: String) {
        serverUrl = url
        apiService = ApiService(url)
        
        // Save to preferences
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(SERVER_URL_KEY, url)
            apply()
        }
    }
    
    /**
     * Get the current server URL
     */
    fun getServerUrl(): String = serverUrl
    
    /**
     * Fetch available models from the server
     */
    suspend fun getAvailableModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        apiService.fetchModels()
    }
    
    /**
     * Check the current model status
     */
    suspend fun checkModelStatus(): Result<ModelStatus> = withContext(Dispatchers.IO) {
        apiService.checkModelStatus()
    }
    
    /**
     * Load a model on the server
     */
    suspend fun loadModel(modelName: String, contextLength: Int? = null): Result<ModelLoadResult> = 
        withContext(Dispatchers.IO) {
            apiService.loadModel(modelName, contextLength)
        }
    
    /**
     * Unload the current model
     */
    suspend fun unloadModel(): Result<String> = withContext(Dispatchers.IO) {
        apiService.unloadModel()
    }
    
    /**
     * Format a prompt using the server's template
     */
    suspend fun formatPrompt(prompt: String, modelName: String): Result<String> = 
        withContext(Dispatchers.IO) {
            apiService.formatPrompt(prompt, modelName)
        }
    
    /**
     * Send a streaming prompt to the server
     * Note: This function doesn't use Dispatchers.IO as the ApiService
     * handles threading with OkHttp callback
     */
    fun sendStreamingPrompt(
        prompt: String,
        systemPrompt: String = "",
        formattedPrompt: String? = null,
        modelName: String,
        callback: (status: String, content: String) -> Unit
    ) {
        apiService.sendStreamingPrompt(
            prompt = prompt,
            systemPrompt = systemPrompt,
            formattedPromptOverride = formattedPrompt,
            modelName = modelName,
            callback = callback
        )
    }
}